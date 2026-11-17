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
import java.security.Principal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.apache.wiki.ajax.WikiAjaxServlet;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.exceptions.PluginException;
import org.apache.wiki.api.plugin.Plugin;
import org.apache.wiki.auth.AuthenticationManager;
import org.apache.wiki.auth.AuthorizationManager;
import org.apache.wiki.auth.UserManager;
import org.apache.wiki.auth.WikiSecurityException;
import org.apache.wiki.auth.permissions.AllPermission;
import org.apache.wiki.auth.permissions.PermissionFactory;
import org.apache.wiki.auth.user.UserDatabase;
import org.apache.wiki.auth.user.UserProfile;
import org.apache.wiki.util.TextUtil;

/**
 *
 * @author Dad
 */
public class UserManagementPlugin implements Plugin, WikiAjaxServlet {

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
        StringBuilder sb = new StringBuilder("""
            <table class="wikitable">
        <thead>
      <tr>
        <th>Wiki Name</th>
        <th>Logon Name</th>
        <th>Full Name</th>
        <th>Created</th>
        <th>Last Modified</th>
        <th>Password last set at</th>
        <th>Email</th>
        <th>Status</th>      
        <th>Actions</th>                                     
      </tr>
    </thead>
    <tbody>
                
                """);
        try {
            Principal[] wikiNames = userDatabase.getWikiNames();
            for (Principal p : wikiNames) {
                UserProfile profile = userDatabase.findByWikiName(p.getName());
                String status = "OK";
                if (profile.isLocked()) {
                    status = "LOCKED";
                }
                Boolean check = authn.isPasswordExpiredByLoginName(profile.getLoginName());
                if (Objects.equals(check, Boolean.TRUE)) {
                    status = "PW EXPIRED";
                }
                
                sb.append("""
    <tr>
        <th title="wikiname: %s\n loginname: %s">%s</th>
        <th>%s</th>
      
        <th>Created %s<br>Modified %s<br>Password Change %s</th>
        <th>%s</th>
        <th><button type='button' class='btn btn-warn'>Lock</button>
            <button type='button' class='btn btn-info'>Unlock</button>
            <button type='button' class='btn btn-warn'>Expire Pass</button>
            <button type='button' class='btn btn-danger'>Delete</button>
        </th>  
    </tr>
                          """.formatted(TextUtil.replaceEntities(profile.getWikiName()),
                        TextUtil.replaceEntities(profile.getLoginName()),
                        TextUtil.replaceEntities(profile.getFullname()),
                        profile.getCreated() != null ? TextUtil.replaceEntities(profile.getCreated().toString()) : "",
                        profile.getLastModified() != null ? TextUtil.replaceEntities(profile.getLastModified().toString()) : "",
                        profile.getPasswordChanged() != null ? TextUtil.replaceEntities(profile.getPasswordChanged().toString()) : "",
                        TextUtil.replaceEntities(profile.getEmail()),
                        status)
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
        return "admin-user-management";
    }

    @Override
    public void service(HttpServletRequest request, HttpServletResponse response, String actionName, List<String> params) throws ServletException, IOException {
        //first authorize the request
        //check for csrf token
        //check the action and execute
        //return a useful message
        

    }

}
