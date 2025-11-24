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
package org.apache.wiki.security;

import com.google.gson.Gson;
import jakarta.mail.MessagingException;
import java.io.File;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import org.apache.log4j.Logger;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.event.WikiEvent;
import org.apache.wiki.event.WikiEventListener;
import org.apache.wiki.event.WikiEventManager;
import org.apache.wiki.event.WikiSecurityEvent;
import org.apache.wiki.i18n.InternationalizationManager;
import org.apache.wiki.util.MailUtil;

/**
 * Audit logger - listens to WikiEvent, logging specific events and actions that
 * audit worthy
 *
 * @since 3.0.0
 */
public final class AuditLogger implements WikiEventListener {

    private static final Logger LOG = Logger.getLogger(AuditLogger.class);

    private static AuditLogger INSTANCE;

    public static void initialize(WikiEngine engine) {

        if ("true".equals(engine.getWikiProperties().get("audit.enabled"))) {
            INSTANCE = new AuditLogger(true);
            INSTANCE.engine = engine;
            String minuteCheck = engine.getWikiProperties().getProperty("audit.alert.lowDiskSpaceFrequency", "30");
            INSTANCE.timer.scheduleAtFixedRate(new DiskSpaceCheck(), 0, Integer.parseInt(minuteCheck) * 60 * 1000L);
        } else {
            INSTANCE = new AuditLogger(false);
        }
    }

    public static AuditLogger getInstance() {
        return INSTANCE;
    }
    private Timer timer;
    private WikiEngine engine;
    private final Gson gson = new Gson();
    private ThreadPoolExecutor threadPool = null;

    private AuditLogger(boolean enabled) {
        //listen to all events
        if (enabled) {
            WikiEventManager.addWikiEventListener(WikiEventManager.class, this);
            timer = new Timer("AuditLogTasks", true);
            threadPool = new ThreadPoolExecutor(1, Runtime.getRuntime().availableProcessors(),
                    30, TimeUnit.SECONDS, new LinkedBlockingDeque<>(100), new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r);
                    t.setName("Audit Log Email alert worker");
                    t.setDaemon(true);
                    return t;
                }
            }
            );
        } else {
            LOG.info("Audit logging is disabled, as well as low disk space monitoring");
        }
    }

    public void shutdown() {
        engine = null;
        if (timer != null) {
            timer.cancel();
        }
        if (threadPool != null) {
            threadPool.shutdown();
        }
    }

    @Override
    public void actionPerformed(WikiEvent event) {
        try {
            LOG.info(String.format(
                    "Class=%s, Description=%s, At=%d, AsString=%s, Name=%s, HttpsBits=%s",
                    event.getClass().getSimpleName(),
                    event.getTypeDescription(),
                    event.getWhen(),
                    event.toString(),
                    event.eventName(),
                    gson.toJson(event.getAttributes())));
            if (event instanceof WikiSecurityEvent wse) {
                String filters = engine.getWikiProperties().getProperty("audit.alert.filter", "41,42,43,46,47,52");
                String[] alertsWeCareAbout = filters.split("\\,");
                boolean keep = false;
                for (String s : alertsWeCareAbout) {
                    if (s.equals(wse.getType() + "")) {
                        keep = true;
                        break;
                    }
                }
                if (!keep) {
                    //not on the list of alerts the system owner wants emails on. 
                    //so we stop processing here
                    return;
                }
                final Locale m_loc = Locale.getDefault();
                //TODO maybe we can look up the admin's account and get their
                //desired locale at some point
                final InternationalizationManager i18n = engine.getManager(InternationalizationManager.class);
                final String app = engine.getApplicationName();
                final String destinations = engine.getWikiProperties().getProperty("audit.alert.to");
                if (destinations == null) {
                    return;
                }
                final String[] addrs = destinations.split("\\;");
                final String subject = i18n.get(InternationalizationManager.DEF_TEMPLATE, m_loc,
                        "notification.auditlog.subject", app);
                /*
                Class: {0}\n\
                Event Name: {1}\n\
                Type Description: {2}\n\
                When: {3}\n\
                Event Details: {4}\n\
                Headers and Connection Details: {5}
                 */
                final String content = i18n.get(InternationalizationManager.DEF_TEMPLATE, m_loc,
                        "notification.auditlog.content",
                        event.getClass().getSimpleName(),
                        event.eventName(),
                        event.getTypeDescription(),
                        new Date(event.getWhen()).toString(),
                        event.toString(),
                        gson.toJson(event.getAttributes()));
                for (String to : addrs) {
                    threadPool.submit(() -> {
                        try {
                            MailUtil.sendMessage(engine.getWikiProperties(),
                                    to, subject, content);
                        } catch (Exception ex) {
                            LOG.warn("Audit alert email to " + to + " failed with " + ex.getMessage());
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("Audit alert email to " + to + " failed with " + ex.getMessage(), ex);
                            }
                        }
                    });

                }
            }

        } catch (Exception ex) {
            LOG.error("Failed to log audit event " + ex.getMessage(), ex);
        }
    }

    private static class DiskSpaceCheck extends TimerTask {

        @Override
        public void run() {
            double threshold = Double.parseDouble(INSTANCE.engine.getWikiProperties().getProperty("audit.alert.lowDiskSpaceThreshold", "75"));
            File f = new File(".");
            long free = f.getFreeSpace();
            long total = f.getTotalSpace();
            long used = total - free;
            if (((double) used / (double) total * 100d) >= threshold) {
                WikiSecurityEvent wse = new WikiSecurityEvent(this, WikiSecurityEvent.LOW_STORAGE, null, this);
                org.apache.wiki.event.WikiEventManager.fireEvent(this, wse);
            }
        }

    }

}
