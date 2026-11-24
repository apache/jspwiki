/*
 * Copyright 2026 The Apache Software Foundation.
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
package org.apache.wiki.plugin;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.wiki.ajax.WikiAjaxServlet;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.ContextEnum;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.core.Session;
import org.apache.wiki.api.exceptions.PluginException;
import org.apache.wiki.api.plugin.Plugin;
import org.apache.wiki.api.spi.Wiki;
import org.apache.wiki.auth.AuthenticationManager;
import org.apache.wiki.auth.AuthorizationManager;
import org.apache.wiki.auth.NoSuchPrincipalException;
import org.apache.wiki.auth.SessionMonitor;
import org.apache.wiki.auth.UserManager;
import org.apache.wiki.auth.WikiSecurityException;
import org.apache.wiki.auth.permissions.AllPermission;
import org.apache.wiki.auth.user.UserDatabase;
import org.apache.wiki.auth.user.UserProfile;
import org.apache.wiki.i18n.InternationalizationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * UserManagementPlugin render a table with controls to admins on any wiki page.
 * provides some actions for
 *
 * locking/unlocking users
 *
 * expiring passwords (by setting the change date far in the past) and thus
 * letting the password policy take over
 *
 * deleting user accounts
 *
 * and expiring all passwords, including your own.
 *
 * in retrospect, this might have been easier and cleaner (especially with the
 * i18n bits) if this was a jsp instead.
 * 
 * TODO - figure out how to determine if a given user account is admin
 * - a confirmation prompt might be nice
 *
 * @since 3.0.2
 */
public class UserManagementPlugin implements Plugin, WikiAjaxServlet {

    private static final Logger LOG = LoggerFactory.getLogger(UserManagementPlugin.class);

    @Override
    public String getDisplayName(Locale locale) {
        return "User Management";
    }

    static String idCssSafeId() {
        return "id-" + UUID.randomUUID().toString().replace("-", "");
    }
    public static final String PATH = "admin-user-management";

    public UserManagementPlugin() {
    }

    public UserManagementPlugin(Engine m_engine) {
        this.m_engine = m_engine;
    }
    private static final String disabled = " disabled ";

    @Override
    public String execute(Context context, Map<String, String> params) throws PluginException {
        final Engine engine = context.getEngine();
        AuthorizationManager manager = engine.getManager(AuthorizationManager.class);
        if (!manager.checkPermission(context.getWikiSession(), new AllPermission(context.getEngine().getApplicationName()))) {
            return "";
        }
        Locale locale = context.getWikiSession().getLocale();
        InternationalizationManager i18n = engine.getManager(InternationalizationManager.class);

        AuthenticationManager authn = engine.getManager(AuthenticationManager.class);
        if (authn.isContainerAuthenticated()) {

            return i18n.get(InternationalizationManager.CORE_BUNDLE, locale, "action.notsupported") + "<br>";    //TODO i18n
        }

        UserManager mgr = engine.getManager(UserManager.class);
        UserDatabase userDatabase = mgr.getUserDatabase();
        StringBuilder sb = new StringBuilder("<script type='text/javascript'>");
        try {
            String content = IOUtils.resourceToString("/org/apache/wiki/plugin/UserManagementPlugin.js", StandardCharsets.UTF_8);
            sb.append(content);
        } catch (IOException ex) {
            LOG.error(ex.getMessage(), ex);
        }
        sb.append("</script><table class='wikitable'><thead><tr><th>");
        sb.append(i18n.get(InternationalizationManager.CORE_BUNDLE, locale, "userinfo.wikiname"));
        sb.append("</th><th>");
        sb.append(i18n.get(InternationalizationManager.CORE_BUNDLE, locale, "userinfo.dates"));
        sb.append("</th><th>");
        sb.append(i18n.get(InternationalizationManager.CORE_BUNDLE, locale, "userinfo.status"));
        sb.append("</th><th>");
        sb.append(i18n.get(InternationalizationManager.CORE_BUNDLE, locale, "userinfo.action"));
        sb.append("</th></tr></thead><tbody>\n");

        try {
            Principal[] wikiNames = userDatabase.getWikiNames();
            for (Principal p : wikiNames) {
                boolean locked = false;
                boolean passwordExpired = false;
                boolean isAdmin = false;
                UserProfile profile = userDatabase.findByWikiName(p.getName());

                //TODO find a way to determine is a user is an admin or not. Not trivial
                String status = i18n.get(InternationalizationManager.CORE_BUNDLE, locale,
                        "userinfo.status.ok");
                if (profile.isLocked()) {
                    status = i18n.get(InternationalizationManager.CORE_BUNDLE, locale,
                            "userinfo.status.locked");

                    locked = true;
                }
                Boolean check = authn.isPasswordExpiredByLoginName(profile.getLoginName());
                if (Objects.equals(check, Boolean.TRUE)) {
                    status = i18n.get(InternationalizationManager.CORE_BUNDLE, locale,
                            "userinfo.status.pwexpired");
                    passwordExpired = true;
                }
                if (locked && passwordExpired) {
                    status = i18n.get(InternationalizationManager.CORE_BUNDLE, locale,
                            "userinfo.status.locked");
                    status += i18n.get(InternationalizationManager.CORE_BUNDLE, locale,
                            "userinfo.status.pwexpired");
                }
                String disabledLock = locked ? disabled : "";
                String disabledUnlock = locked ? "" : disabled;
                String expirePassword = passwordExpired ? disabled : "";
                String disableDelete = isAdmin ? disabled : "";

                String lockId = idCssSafeId();
                String unlockId = idCssSafeId();
                String expireId = idCssSafeId();
                String rowId = idCssSafeId();
                sb.append("<tr id='");
                sb.append(StringEscapeUtils.escapeHtml4(rowId));
                sb.append("'><td title=\"");
                sb.append(i18n.get(InternationalizationManager.CORE_BUNDLE, locale,
                        "userinfo.wikiname"));
                sb.append(": ");
                sb.append(StringEscapeUtils.escapeHtml4(profile.getWikiName()));
                sb.append("\n");
                sb.append(i18n.get(InternationalizationManager.CORE_BUNDLE, locale,
                        "security.user.loginname"));
                sb.append(": ");
                sb.append(StringEscapeUtils.escapeHtml4(profile.getLoginName()));
                sb.append("\n");
                sb.append(i18n.get(InternationalizationManager.CORE_BUNDLE, locale,
                        "security.user.email"));
                sb.append(": ");
                sb.append(StringEscapeUtils.escapeHtml4(profile.getEmail()));
                sb.append("\n");
                sb.append(i18n.get(InternationalizationManager.CORE_BUNDLE, locale,
                        "security.user.fullname"));
                sb.append(": ");
                sb.append(StringEscapeUtils.escapeHtml4(profile.getFullname()));
                sb.append("\">");
                sb.append(StringEscapeUtils.escapeHtml4(profile.getLoginName()));
                sb.append("</td>");

                //dates
                //TODO figure out what api we use for date time formatting.
                sb.append("<td>");
                sb.append(i18n.get(InternationalizationManager.CORE_BUNDLE, locale,
                        "userinfo.dates.created"));
                sb.append(": ");
                sb.append(profile.getCreated() != null
                        ? StringEscapeUtils.escapeHtml4(profile.getCreated().toString()) : ""
                );

                sb.append("<br>");
                sb.append(i18n.get(InternationalizationManager.CORE_BUNDLE, locale,
                        "userinfo.dates.modified"));
                sb.append(": ");
                sb.append(profile.getLastModified() != null
                        ? StringEscapeUtils.escapeHtml4(profile.getLastModified().toString()) : ""
                );
                sb.append("<br>");
                sb.append(i18n.get(InternationalizationManager.CORE_BUNDLE, locale,
                        "userinfo.dates.pwchanged"));
                sb.append(": ");
                sb.append(profile.getPasswordChanged() != null
                        ? StringEscapeUtils.escapeHtml4(profile.getPasswordChanged().toString()) : ""
                );
                sb.append("<br>");
                sb.append(i18n.get(InternationalizationManager.CORE_BUNDLE, locale,
                        "userinfo.dates.lastlogin"));
                sb.append(": ");
                sb.append(
                        profile.getAttributes().get(UserProfile.ATTR_CURRENT_LOGIN_TIMESTAMP) != null
                        ? StringEscapeUtils.escapeHtml4(
                                new Date((long) profile.getAttributes().get(UserProfile.ATTR_CURRENT_LOGIN_TIMESTAMP)).toString()
                        ) : ""
                );
                sb.append("<br>");
                sb.append(i18n.get(InternationalizationManager.CORE_BUNDLE, locale,
                        "userinfo.dates.lastloginaddress"));
                sb.append(": ");
                sb.append(
                        profile.getAttributes().get(UserProfile.ATTR_CURRENT_LOGIN_IP) != null
                        ? StringEscapeUtils.escapeHtml4((String) profile.getAttributes().get(UserProfile.ATTR_CURRENT_LOGIN_IP)
                        ) : ""
                );

                sb.append("</td>");
                sb.append("<td>");
                sb.append(StringEscapeUtils.escapeHtml4(status));
                sb.append("</td>");

                //actions
                sb.append("<td>");
                sb.append("<button class='btn btn-warning' type='button' id='");
                sb.append(lockId);
                sb.append("' onclick=\"UserManagementPluginLockuser('");
                sb.append(StringEscapeUtils.escapeEcmaScript(lockId));
                sb.append("','");
                sb.append(StringEscapeUtils.escapeEcmaScript(unlockId));
                sb.append("','");
                sb.append(StringEscapeUtils.escapeEcmaScript(profile.getWikiName()));
                sb.append("')\" ");
                sb.append(disabledLock);
                sb.append(">");
                sb.append(i18n.get(InternationalizationManager.CORE_BUNDLE, locale,
                        "userinfo.aciton.lock"));
                sb.append("</button>\n");

                sb.append("<button class='btn btn-info' type='button' id='");
                sb.append(lockId);
                sb.append("' onclick=\"UserManagementPluginUnlockUser('");
                sb.append(StringEscapeUtils.escapeEcmaScript(unlockId));
                sb.append("','");
                sb.append(StringEscapeUtils.escapeEcmaScript(lockId));
                sb.append("','");
                sb.append(StringEscapeUtils.escapeEcmaScript(profile.getWikiName()));
                sb.append("')\" ");
                sb.append(disabledUnlock);
                sb.append(">");
                sb.append(i18n.get(InternationalizationManager.CORE_BUNDLE, locale,
                        "userinfo.aciton.unlock"));
                sb.append("</button>\n");

                sb.append("<button class='btn btn-warning' type='button' id='");
                sb.append(lockId);
                sb.append("' onclick=\"UserManagementPluginExpirePassword('");
                sb.append(StringEscapeUtils.escapeEcmaScript(expireId));

                sb.append("','");
                sb.append(StringEscapeUtils.escapeEcmaScript(profile.getWikiName()));
                sb.append("')\" ");
                sb.append(expirePassword);
                sb.append(">");
                sb.append(i18n.get(InternationalizationManager.CORE_BUNDLE, locale,
                        "userinfo.aciton.expirepassword"));
                sb.append("</button>\n");

                sb.append("<button class='btn btn-danger' type='button' id='");
                sb.append(lockId);
                sb.append("' onclick=\"UserManagementPluginDeleteUser('");
                sb.append(StringEscapeUtils.escapeEcmaScript(rowId));

                sb.append("','");
                sb.append(StringEscapeUtils.escapeEcmaScript(profile.getWikiName()));
                sb.append("')\" ");
                sb.append(disableDelete);
                sb.append(">");
                sb.append(i18n.get(InternationalizationManager.CORE_BUNDLE, locale,
                        "userinfo.aciton.delete"));
                sb.append("</button>\n");
                sb.append("</td>");
                sb.append("</tr>\n");

            }
        } catch (WikiSecurityException ex) {
            LOG.error(ex.getMessage(), ex);
        }
        sb.append("</tbody></table>");
        sb.append("<button onclick='UserManagementPluginExpireAll()' type='button' class='btn btn-warning'>");
        sb.append(i18n.get(InternationalizationManager.CORE_BUNDLE, locale,
                "userinfo.actions.expireall"));
        sb.append("</button>");
        sb.append("<br>");
        sb.append(i18n.get(InternationalizationManager.CORE_BUNDLE, locale,
                "userinfo.actions.expire.note"));
        sb.append("<br>");

        return sb.toString();
    }

    @Override
    public String getServletMapping() {
        return PATH;
    }
    protected Engine m_engine;

    @Override
    public void service(HttpServletRequest request, HttpServletResponse response, String actionName, List<String> params) throws ServletException, IOException {
        final Context wikiContext = Wiki.context().create(m_engine, request, ContextEnum.PAGE_VIEW.getRequestContext());
        final Engine engine = wikiContext.getEngine();
        Locale locale = wikiContext.getWikiSession().getLocale();
        InternationalizationManager i18n = engine.getManager(InternationalizationManager.class);
        UserManager mgr = engine.getManager(UserManager.class);
        UserDatabase userDatabase = mgr.getUserDatabase();
        AuthorizationManager manager = engine.getManager(AuthorizationManager.class);
        AuthenticationManager authn = engine.getManager(AuthenticationManager.class);

        //first authorize the request
        if (!manager.checkPermission(wikiContext.getWikiSession(), new AllPermission(wikiContext.getEngine().getApplicationName()))) {
            response.setStatus(400);
            response.getWriter().write(i18n.get(InternationalizationManager.CORE_BUNDLE, locale,
                    "access.deny"));
            mgr.fireEvent(org.apache.wiki.event.WikiSecurityEvent.ACCESS_DENIED,
                    wikiContext.getWikiSession(), wikiContext.getCurrentUser());

            return;
        }
        if (authn.isContainerAuthenticated()) {
            response.setStatus(400);
            response.getWriter().write(i18n.get(InternationalizationManager.CORE_BUNDLE, locale,
                    "action.notsupported"));
            return;
        }
        String username = null;

        UserProfile findByLoginName = null;
        long lockDuration = 24 * 60 * 60 * 1000L;
        if (!params.isEmpty()) {
            username = params.get(0);
            try {
                findByLoginName = mgr.getUserDatabase().findByWikiName(username);
            } catch (NoSuchPrincipalException ex) {
                response.setStatus(400);
                response.getWriter().write(i18n.get(InternationalizationManager.CORE_BUNDLE, locale,
                        "errors.unknownuser"));

                return;
            }
            if (params.size() > 1 && params.get(1) != null) {
                lockDuration = Long.parseLong(params.get(1));
            }
        }
        //check for csrf token
        //check the action and execute
        //return a useful message
        switch (actionName) {
            case "unlock":
                if (!findByLoginName.isLocked()) {
                    response.setStatus(400);
                    response.getWriter().write(i18n.get(InternationalizationManager.CORE_BUNDLE, locale,
                            "errors.accountalreadyunlocked"));
                    return;
                }
                findByLoginName.setLockExpiry(null);

                try {
                    userDatabase.save(findByLoginName);
                    mgr.fireEvent(org.apache.wiki.event.WikiSecurityEvent.LOGIN_UNLOCKED, wikiContext.getWikiSession(), engine);

                } catch (WikiSecurityException ex) {
                    LOG.error(ex.getMessage(), ex);
                    response.setStatus(500);
                    response.getWriter().write(i18n.get(InternationalizationManager.CORE_BUNDLE, locale,
                            "errors.internalerror"));

                    return;
                }

                break;

            case "lock":
                if (findByLoginName.isLocked()) {
                    response.setStatus(400);
                    response.getWriter().write(i18n.get(InternationalizationManager.CORE_BUNDLE, locale,
                            "errors.accountalreadylocked"));

                    return;
                }
                Date d = new Date(System.currentTimeMillis() + lockDuration);
                findByLoginName.setLockExpiry(d);

                try {
                    userDatabase.save(findByLoginName);
                    mgr.fireEvent(org.apache.wiki.event.WikiSecurityEvent.LOGIN_LOCKED,
                            wikiContext.getWikiSession(), findByLoginName);
                    SessionMonitor instance = SessionMonitor.getInstance(engine);
                    List<Session> findOtherSessionsByUsername = instance.findOtherSessionsByUsername(findByLoginName.getLoginName());
                    for (Session s : findOtherSessionsByUsername) {
                        s.invalidate();
                    }
                } catch (WikiSecurityException ex) {
                    LOG.error(ex.getMessage(), ex);
                    response.setStatus(500);
                    response.getWriter().write(i18n.get(InternationalizationManager.CORE_BUNDLE, locale,
                            "errors.internalerror"));
                    return;
                }

                break;
            case "expireAll": {
                try {
                    Principal[] users = mgr.getUserDatabase().getWikiNames();
                    for (Principal p : users) {
                        try {
                            UserProfile findByWikiName = userDatabase.findByWikiName(p.getName());
                            findByWikiName.setPasswordChanged(new Date(0));
                            userDatabase.save(findByWikiName);
                            SessionMonitor instance = SessionMonitor.getInstance(engine);
                            List<Session> findOtherSessionsByUsername = instance.findOtherSessionsByUsername(findByWikiName.getLoginName());
                            for (Session s : findOtherSessionsByUsername) {
                                s.invalidate();
                            }
                            //not sure this is doing what needs to be done here
                            mgr.fireEvent(org.apache.wiki.event.WikiSecurityEvent.LOGIN_CREDENTIAL_EXPIRED,
                                    wikiContext.getWikiSession(), findByWikiName);
                        } catch (NoSuchPrincipalException ex) {
                            LOG.error(ex.getMessage(), ex);
                        }
                    }
                } catch (Exception ex) {
                    LOG.error(ex.getMessage(), ex);
                    response.setStatus(500);
                    response.getWriter().write(i18n.get(InternationalizationManager.CORE_BUNDLE, locale,
                            "errors.internalerror"));
                }
                break;
            }
            case "expire":

                findByLoginName.setPasswordChanged(new Date(0));
                try {
                    userDatabase.save(findByLoginName);
                    SessionMonitor instance = SessionMonitor.getInstance(engine);
                    List<Session> findOtherSessionsByUsername = instance.findOtherSessionsByUsername(findByLoginName.getLoginName());
                    for (Session s : findOtherSessionsByUsername) {
                        s.invalidate();
                    }
                    //not sure this is doing what needs to be done here
                    mgr.fireEvent(org.apache.wiki.event.WikiSecurityEvent.LOGIN_CREDENTIAL_EXPIRED,
                            wikiContext.getWikiSession(), findByLoginName);
                } catch (WikiSecurityException ex) {
                    LOG.error(ex.getMessage(), ex);
                    response.setStatus(500);
                    response.getWriter().write(i18n.get(InternationalizationManager.CORE_BUNDLE, locale,
                            "errors.internalerror"));
                    return;
                }

                break;

            case "delete": {
                try {
                    userDatabase.deleteByLoginName(findByLoginName.getLoginName());
                    //not sure this is doing what needs to be done here
                    mgr.fireEvent(org.apache.wiki.event.WikiSecurityEvent.LOGIN_DELETED,
                            wikiContext.getWikiSession(), findByLoginName);

                } catch (WikiSecurityException ex) {
                    LOG.error(ex.getMessage(), ex);
                    response.setStatus(500);
                    response.getWriter().write(i18n.get(InternationalizationManager.CORE_BUNDLE, locale,
                            "errors.internalerror"));
                    return;
                }
            }

            break;

            default:
                response.setStatus(400);
                response.getWriter().write(i18n.get(InternationalizationManager.CORE_BUNDLE, locale,
                        "errors.badrequest"));
                break;
        }

    }

}
