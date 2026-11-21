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
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import org.apache.commons.io.IOUtils;
import org.apache.wiki.ajax.WikiAjaxServlet;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.ContextEnum;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.exceptions.PluginException;
import org.apache.wiki.api.plugin.Plugin;
import org.apache.wiki.api.spi.Wiki;
import org.apache.wiki.auth.AuthenticationManager;
import org.apache.wiki.auth.AuthorizationManager;
import org.apache.wiki.auth.NoSuchPrincipalException;
import org.apache.wiki.auth.UserManager;
import org.apache.wiki.auth.WikiSecurityException;
import org.apache.wiki.auth.permissions.AllPermission;
import org.apache.wiki.auth.user.UserDatabase;
import org.apache.wiki.auth.user.UserProfile;
import org.apache.wiki.util.TextUtil;

/**
 *
 */
public class UserManagementPlugin implements Plugin, WikiAjaxServlet {

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
            return "DENY";
        }
        AuthenticationManager authn = engine.getManager(AuthenticationManager.class);
        if (authn.isContainerAuthenticated()) {
            return "Not supported.";
        }
        UserManager mgr = engine.getManager(UserManager.class);
        UserDatabase userDatabase = mgr.getUserDatabase();
        StringBuilder sb = new StringBuilder("<script type='text/javascript'>");
        try {
            String content = IOUtils.resourceToString("/org/apache/wiki/plugin/UserManagementPlugin.js", StandardCharsets.UTF_8);
            sb.append(content);
        } catch (IOException ex) {
            System.getLogger(UserManagementPlugin.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
        sb.append("""
                  
            </script>
            <table class="wikitable">
        <thead>
      <tr>
        <th>Wiki Name</th>
        <th>Dates</th>       
        <th>Status</th>      
        <th>Actions</th>                                     
      </tr>
    </thead>
    <tbody>
                
                """);
        try {
            Principal[] wikiNames = userDatabase.getWikiNames();
            for (Principal p : wikiNames) {
                boolean locked = false;
                boolean passwordExpired = false;
                boolean isAdmin = false;
                UserProfile profile = userDatabase.findByWikiName(p.getName());
                //TODO find a way to determine is a user is an admin or not. Not trivial
                String status = "OK";
                if (profile.isLocked()) {
                    status = "LOCKED";
                }
                Boolean check = authn.isPasswordExpiredByLoginName(profile.getLoginName());
                if (Objects.equals(check, Boolean.TRUE)) {
                    status = "PW EXPIRED";
                    passwordExpired = true;
                }
                String disabledLock = locked ? disabled : "";
                String disabledUnlock = locked ? "" : disabled;
                String expirePassword = passwordExpired ? disabled :"";
                String disableDelete = isAdmin ? disabled: "";

                String lockId = idCssSafeId();
                String unlockId = idCssSafeId();
                String expireId = idCssSafeId();
                String rowId = idCssSafeId();

                sb.append("""
    <tr id="%s">
        <th title="wikiname: %s\nlogin: %s\nEmail: %s\nFull Name: %s">%s</th>
        <th>Created %s<br>Modified %s<br>Password Change %s</th>
        <th>%s</th>
        <th><button id="%s" onclick="UserManagementPluginLockuser('%s','%s','%s')" type='button' %s class='btn btn-warn'>Lock</button>
            <button id="%s" onclick="UserManagementPluginUnlockUser('%s','%s','%s')" type='button' %s class='btn btn-info'>Unlock</button>
            <button id="%s" onclick="UserManagementPluginExpirePassword('%s' ,'%s')" type='button' %s class='btn btn-warn'>Expire Pass</button>
            <button onclick="UserManagementPluginDeleteUser('%s','%s')" type='button' %s class='btn btn-danger'>Delete</button>
        </th>  
    </tr>
                          """.formatted(
                        rowId,
                        TextUtil.replaceEntities(profile.getWikiName()),
                        TextUtil.replaceEntities(profile.getLoginName()),
                        TextUtil.replaceEntities(profile.getEmail()),
                        TextUtil.replaceEntities(profile.getFullname()),
                        TextUtil.replaceEntities(profile.getLoginName()),
                        profile.getCreated() != null ? TextUtil.replaceEntities(profile.getCreated().toString()) : "",
                        profile.getLastModified() != null ? TextUtil.replaceEntities(profile.getLastModified().toString()) : "",
                        profile.getPasswordChanged() != null ? TextUtil.replaceEntities(profile.getPasswordChanged().toString()) : "",
                        status,
                        lockId, lockId, unlockId, TextUtil.replaceEntities(profile.getLoginName()), disabledLock,
                        unlockId, unlockId, lockId, TextUtil.replaceEntities(profile.getLoginName()), disabledUnlock,
                        expireId, expireId, TextUtil.replaceEntities(profile.getLoginName()), expirePassword,
                        rowId, TextUtil.replaceEntities(profile.getLoginName()), disableDelete)
                );
            }
        } catch (WikiSecurityException ex) {
            System.getLogger(UserManagementPlugin.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
        sb.append("</tbody></table>");
        return sb.toString();
    }

    @Override
    public String getServletMapping() {
        return PATH;
    }
    protected Engine m_engine;

    @Override
    public void service(HttpServletRequest request, HttpServletResponse response, String actionName, List<String> params) throws ServletException, IOException {
        //first authorize the request
        final Context wikiContext = Wiki.context().create(m_engine, request, ContextEnum.PAGE_VIEW.getRequestContext());
        final Engine engine = wikiContext.getEngine();
        AuthorizationManager manager = engine.getManager(AuthorizationManager.class);
        if (!manager.checkPermission(wikiContext.getWikiSession(), new AllPermission(wikiContext.getEngine().getApplicationName()))) {
            response.sendError(400, "DENY");
            return;
        }
        AuthenticationManager authn = engine.getManager(AuthenticationManager.class);
        if (authn.isContainerAuthenticated()) {
            response.sendError(400, "not supported");
            return;
        }
        String username = params.get(0);
        UserManager mgr = engine.getManager(UserManager.class);
        UserDatabase userDatabase = mgr.getUserDatabase();
        UserProfile findByLoginName;
        try {
            findByLoginName = mgr.getUserDatabase().findByLoginName(username);
        } catch (NoSuchPrincipalException ex) {
            response.sendError(400, "unknown user");
            return;
        }
        long lockDuration = 24 * 60 * 60 * 1000L;
        if (params.get(1) != null) {
            lockDuration = Long.parseLong(params.get(1));
        }
        //check for csrf token
        //check the action and execute
        //return a useful message
        switch (actionName) {
            case "unlock":
                if (!findByLoginName.isLocked()) {
                    response.sendError(400, "not locked");
                    return;
                }
                findByLoginName.setLockExpiry(null);

                try {
                    userDatabase.save(findByLoginName);
                    mgr.fireEvent(org.apache.wiki.event.WikiSecurityEvent.LOGIN_UNLOCKED, wikiContext.getWikiSession(), engine);

                } catch (WikiSecurityException ex) {
                    response.sendError(400, "internal error");
                    return;
                }

                break;

            case "lock":
                if (findByLoginName.isLocked()) {
                    response.sendError(400, "already locked");
                    return;
                }
                Date d = new Date(System.currentTimeMillis() + lockDuration);
                findByLoginName.setLockExpiry(d);

                try {
                    userDatabase.save(findByLoginName);
                    mgr.fireEvent(org.apache.wiki.event.WikiSecurityEvent.LOGIN_LOCKED, wikiContext.getWikiSession(), engine);

                } catch (WikiSecurityException ex) {
                    response.sendError(400, "internal error");
                    return;
                }

                break;

            case "delete": {
                try {
                    userDatabase.deleteByLoginName(actionName);
                    mgr.fireEvent(org.apache.wiki.event.WikiSecurityEvent.LOGIN_DELETED, wikiContext.getWikiSession(), engine);

                } catch (WikiSecurityException ex) {
                    response.sendError(400, "internal error");
                    return;
                }
            }

            break;

            default:
                response.sendError(400, "bad request");
                break;
        }

    }

}
