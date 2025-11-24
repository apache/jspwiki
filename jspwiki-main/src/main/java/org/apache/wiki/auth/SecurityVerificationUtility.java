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
package org.apache.wiki.auth;

import jakarta.mail.MessagingException;
import jakarta.mail.event.MailEvent;
import java.util.logging.Level;
import org.apache.commons.validator.routines.EmailValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.wiki.WikiSession;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.core.Session;
import org.apache.wiki.util.MailUtil;

/**
 *
 * @since 3.0.0
 */
public class SecurityVerificationUtility {

    private static final Logger LOG = LogManager.getLogger(SecurityVerificationUtility.class);

    public void verify(Engine wiki) {
        //Context wikiContext = Wiki.context().create(wiki, request, ContextEnum.PAGE_NONE.getRequestContext());
        

        Session m_session = WikiSession.guestSession(wiki);
        new SecurityVerifier(wiki, m_session);
        StringBuilder sb = new StringBuilder();
        String[] messages = m_session.getMessages(org.apache.wiki.auth.SecurityVerifier.ERROR_JAAS);
        apply(sb, messages, org.apache.wiki.auth.SecurityVerifier.ERROR_JAAS);

        messages = m_session.getMessages(org.apache.wiki.auth.SecurityVerifier.WARNING_JAAS);
        apply(sb, messages, org.apache.wiki.auth.SecurityVerifier.WARNING_JAAS);

        messages = m_session.getMessages(org.apache.wiki.auth.SecurityVerifier.ERROR_POLICY);
        apply(sb, messages, org.apache.wiki.auth.SecurityVerifier.ERROR_POLICY);

        messages = m_session.getMessages(org.apache.wiki.auth.SecurityVerifier.WARNING_POLICY);
        apply(sb, messages, org.apache.wiki.auth.SecurityVerifier.WARNING_POLICY);

        messages = m_session.getMessages(org.apache.wiki.auth.SecurityVerifier.ERROR_DB);
        apply(sb, messages, org.apache.wiki.auth.SecurityVerifier.ERROR_DB);

        messages = m_session.getMessages(org.apache.wiki.auth.SecurityVerifier.WARNING_DB);
        apply(sb, messages, org.apache.wiki.auth.SecurityVerifier.WARNING_DB);

        messages = m_session.getMessages(org.apache.wiki.auth.SecurityVerifier.ERROR_GROUPS);
        apply(sb, messages, org.apache.wiki.auth.SecurityVerifier.ERROR_GROUPS);

        messages = m_session.getMessages(org.apache.wiki.auth.SecurityVerifier.WARNING_GROUPS);
        apply(sb, messages, org.apache.wiki.auth.SecurityVerifier.WARNING_GROUPS);

        messages = m_session.getMessages(org.apache.wiki.auth.SecurityVerifier.ERROR_ROLES);
        apply(sb, messages, org.apache.wiki.auth.SecurityVerifier.ERROR_ROLES);

        if (sb.length() > 0) {
            //uh oh
            LOG.warn("The following errors/warnings were found when verifying the security profile of this server. You might want to look at this. " + sb.toString());
            //TODO dispatch an email to the sysadmins
            if ("true".equalsIgnoreCase(wiki.getWikiProperties().getProperty("jspwiki.securitycheck.enableEmailOfBootCheck", "false"))) {
                String addresses = wiki.getWikiProperties().getProperty("jspwiki.securitycheck.destination", "");
                if (addresses != null && addresses.length() > 0) {
                    String[] addresslist = addresses.split("\\;");
                    for (String addr : addresslist) {
                        try {
                            MailUtil.sendMessage(wiki.getWikiProperties(), addr, "JSPWIki Security Check", sb.toString());
                        } catch (MessagingException ex) {
                            LOG.warn("send mail failed to " + addr + " " + ex.getMessage(), ex);
                        }
                    }
                }
            }
        }
    }

    private void apply(StringBuilder sb, String[] messages, String category) {
        if (messages == null) {
            return;
        }
        for (String s : messages) {
            sb.append(category).append(",").append(s).append("\n");
        }
    }
}
