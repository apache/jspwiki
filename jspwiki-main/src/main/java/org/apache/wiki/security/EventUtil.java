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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.jsp.PageContext;
import java.util.Enumeration;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.Session;
import org.apache.wiki.event.WikiEvent;
import org.apache.wiki.event.WikiSecurityEvent;

/**
 * A utility class to append audit log attributes to a WikiEvent
 *
 * @since 3.0.0
 */
public final class EventUtil {

    private EventUtil() {
    }

    public static WikiEvent applyFrom(WikiSecurityEvent event) {
        if (event.getTarget() != null && event.getTarget() instanceof Session session) {
            applyFrom(event, session);
        }
        if (event.getSrc() != null && event.getSrc() instanceof Session session) {
            applyFrom(event, session);
        }

        return event;
    }

    public static WikiEvent applyFrom(WikiEvent event, PageContext request) {
        if (request.getRequest() != null) {
            applyFrom(event, (HttpServletRequest) request.getRequest());
        }

        return event;
    }

    public static WikiEvent applyFrom(WikiEvent event, Session request) {
        if (request == null) {
            return event;
        }
        if (request.getUserPrincipal() != null) {
            event.getAttributes().put("getUserPrincipal", request.getUserPrincipal().getName());
        }
        event.getAttributes().put("getLoginPrincipal", request.getLoginPrincipal().getName());
        event.getAttributes().put("getStatus", request.getStatus());
        if (request.getSubject() != null) {
            event.getAttributes().put("getSubject", request.getSubject().toString());
        }

        return event;
    }

    public static WikiEvent applyFrom(WikiEvent event, Context request) {
        if (request == null) {
            return event;
        }
        if (request.getHttpRequest() != null) {
            applyFrom(event, request.getHttpRequest());
        }
        return event;
    }

    public static WikiEvent applyFrom(WikiEvent event, HttpServletRequest request) {
        if (request == null) {
            return event;
        }
        if (event instanceof WikiSecurityEvent e) {
            if (e.getTarget() != null && e.getTarget() instanceof Session session) {
                applyFrom(event, session);
            }
        }
        if (event.getSrc() != null && event.getSrc() instanceof Session session) {
            applyFrom(event, session);
        }

        if (request.getUserPrincipal() != null) {
            event.getAttributes().put("username", request.getUserPrincipal().getName());
        }
        event.getAttributes().put("RemoteAddr", request.getRemoteAddr());
        event.getAttributes().put("RemoteHost", request.getRemoteHost());
        event.getAttributes().put("RemoteUser", request.getRemoteUser());
        event.getAttributes().put("RequestURL", request.getRequestURL());
        event.getAttributes().put("AuthType", request.getAuthType());
        event.getAttributes().put("Method", request.getMethod());
        event.getAttributes().put("SessionId", request.getSession().getId());
        event.getAttributes().put("sessionCreatedAt", request.getSession().getCreationTime());
        event.getAttributes().put("sesionLastAccessAt", request.getSession().getLastAccessedTime());
        Enumeration<String> it = request.getHeaderNames();
        if (it != null) {
            while (it.hasMoreElements()) {
                String h = it.nextElement();
                event.getAttributes().put(h, request.getHeader(h));
            }
        }
        return event;
    }
}
