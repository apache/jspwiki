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
import java.io.File;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
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

    //To be added
    public static final int USER_ACCOUNT_CREATED = 100;
    public static final int USER_ACCOUNT_LOCKED = 101;
    public static final int USER_ACCOUNT_UNLOCKED = 102;
    public static final int USER_ACCOUNT_PWD_CHANGED = 103;
    //public static final int USER_ACCOUNT_EXPIRED = 104;
    public static final int USER_ACCOUNT_ADMIN_DISABLE = 105;
    public static final int USER_ACCOUNT_ADMIN_ENABLE = 106;
    public static final int USER_ACCOUNT_DELETE = 107;
    public static final int USER_ACCOUNT_PWD_RESET = 108;
    public static final int USER_ACCOUNT_UPDATED = 109;

    public static final int ADMIN_CONFIG_CHANGE = 110;
    public static final int SYSTEM_BOOT_HASH_CHECK_FAILURE = 201;
    public static final int LOW_DISK_SPACE = 203;

    public static void initialize(WikiEngine engine) {
        INSTANCE = new AuditLogger();
        INSTANCE.engine = engine;
        String minuteCheck = engine.getWikiProperties().getProperty("audit.alert.lowDiskSpaceFrequency", "30");

        INSTANCE.timer.scheduleAtFixedRate(new DiskSpaceCheck(), 0, Integer.parseInt(minuteCheck) * 60 * 1000L);
    }

    public static AuditLogger getInstance() {
        return INSTANCE;
    }
    private Timer timer;
    private WikiEngine engine;
    private final Gson gson = new Gson();

    private AuditLogger() {
        //listen to all events
        WikiEventManager.addWikiEventListener(WikiEventManager.class, this);
        timer = new Timer("AuditLogTasks", true);

    }

    public void shutdown() {

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

                    MailUtil.sendMessage(engine.getWikiProperties(),
                            to, subject, content);
                }
            }

        } catch (Exception ex) {
            LOG.error(ex.getMessage());
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
