/*
 * Copyright 2025 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.wiki;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.wiki.api.Release;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * At server start up and once a day (if enabled), this component will check for
 * an updated WAR file via maven central's APIs. The notification of the update
 * will then be disabled to users with admin permissions near the user
 * preference/login/out menu.
 *
 * It's usage does require internet access, so if you're not internet connected,
 * or cannot reach search.maven.org it won't do you much good. It can be
 * disabled via jspwiki properties.
 *
 * @since 3.0.0
 */
public final class ProductUpdateChecker implements Runnable {

    /*public static void main(String[] args) throws InterruptedException {
        Properties p = new Properties();
        p.setProperty("jspwiki.updateCheck.enabled", "true");
        ProductUpdateChecker.initialize(p);
        Thread.sleep(9999999);
    }*/
    private static final Logger LOG = LogManager.getLogger(ProductUpdateChecker.class);

    private ProductUpdateChecker(Properties props) {
        if ("true".equalsIgnoreCase(props.getProperty("jspwiki.updateCheck.enabled", "false"))) {
            int period = Integer.parseInt(props.getProperty("jspwiki.updateCheck.periodHours", "24"));
            if (period <= 0) {
                LOG.info("Configuration warning, product update checker is set for non positive value. Update check will only be performed at server start up. Set jspwiki.updateCheck.periodHours to a positive value to enable periodic checks.");
                pool.execute(this);
            } else {
                pool = new ScheduledThreadPoolExecutor(1);
                pool.scheduleAtFixedRate(this, 0, 1, TimeUnit.DAYS);
            }
        } else {
            LOG.info("Product update checker is disabled");
        }
    }
    private ScheduledThreadPoolExecutor pool = new ScheduledThreadPoolExecutor(1);

    public static void initialize(Properties props) {
        INSTANCE = new ProductUpdateChecker(props);
    }
    private static ProductUpdateChecker INSTANCE = null;

    /**
     * if the service is disabled, this will return null
     *
     * @return
     */
    public static synchronized ProductUpdateChecker getInstance() {
        return INSTANCE;
    }

    @Override
    public void run() {
        LOG.debug("checking for product updates");
        //option a) xml
        //https://repo1.maven.org/maven2/org/apache/jspwiki/jspwiki-war/maven-metadata.xml

        //option b) json
        URLConnection openConnection = null;
        InputStream inputStream = null;
        InputStreamReader reader = null;
        try {
            URL url = new URL("https://search.maven.org/solrsearch/select?q=g:%22org.apache.jspwiki%22+AND+a:%22jspwiki-war%22&wt=json");
            openConnection = url.openConnection();
            openConnection.connect();
            inputStream = openConnection.getInputStream();
            ObjectMapper mapper = new ObjectMapper();
            JsonNode readTree = mapper.readTree(inputStream);
            readTree = readTree.get("response").get("docs").get(0);

            String latestVersion = readTree.get("latestVersion").asString();
            long releaseTimestamp = readTree.get("timestamp").asLong();
            status.timeStampOfLastCheck = System.currentTimeMillis();
            status.lastestVersionReleaseDate = releaseTimestamp;
            status.latestVersion = latestVersion;
            if ((Release.VERSION + "." + Release.REVISION + "." + Release.MINORREVISION).equals(latestVersion)) {
                status.isUpToDate = Status.UP_TO_DATE;
            } else {
                status.isUpToDate = Status.UPDATE_AVAILABLE;
            }
            if (status.isUpToDate == Status.UPDATE_AVAILABLE) {
                LOG.info("There is a JSPWiki update available. Current version is " + Release.VERSION + "." + Release.REVISION + "." + Release.MINORREVISION + 
                        " update version is " + status.latestVersion + " which was released " + new Date(status.lastestVersionReleaseDate).toString());
            }
        } catch (Exception ex) {
            status.isUpToDate = Status.UNKNOWN;
            status.timeStampOfLastCheck = System.currentTimeMillis();
            LOG.warn("product update check failed " + ex.getMessage(), ex);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ex) {
                    LOG.debug(ex.getMessage());
                }
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ex) {
                    LOG.debug(ex.getMessage());
                }
            }

        }

    }
    private final UpdateStatus status = new UpdateStatus();

    public UpdateStatus getUpdateStatus() {
        return status;

    }

    public enum Status {
        UNKNOWN,
        UP_TO_DATE,
        UPDATE_AVAILABLE
    }

    public static class UpdateStatus {

        private Status isUpToDate = Status.UNKNOWN;
        private String latestVersion = null;
        private long lastestVersionReleaseDate = Long.MIN_VALUE;
        ;
        private long timeStampOfLastCheck = Long.MIN_VALUE;

        public Status getIsUpToDate() {
            return isUpToDate;
        }

        public String getLatestVersion() {
            return latestVersion;
        }

        public long getLastestVersionReleaseDate() {
            return lastestVersionReleaseDate;
        }

        public long getTimeStampOfLastCheck() {
            return timeStampOfLastCheck;
        }

    }

    public void shutdown() {
        if (pool != null) {
            pool.shutdown();
            pool.shutdownNow();
            pool = null;
        }
    }

}
