/*
    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.    
 */
package org.apache.wiki.ui;

import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiSession;
import org.apache.wiki.auth.SessionMonitor;
import org.apache.wiki.auth.authorize.Role;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.security.Principal;

/**
 * Servlet request wrapper that encapsulates an incoming HTTP request and overrides its security methods so that the request returns
 * JSPWiki-specific values.
 * 
 * @since 2.8
 */
public class WikiRequestWrapper extends HttpServletRequestWrapper {

    private final WikiSession m_session;

    /**
     * Constructs a new wrapped request.
     * 
     * @param engine the wiki engine
     * @param request the request to wrap
     */
    public WikiRequestWrapper( final WikiEngine engine, final HttpServletRequest request ) {
        super( request );

        // Get and stash a reference to the current WikiSession
        m_session = SessionMonitor.getInstance( engine ).find( request.getSession() );
    }

    /**
     * Returns the remote user for the HTTP request, taking into account both container and JSPWiki custom authentication status.
     * Specifically, if the wrapped request contains a remote user, this method returns that remote user. Otherwise, if the user's
     * WikiSession is an authenticated session (that is, {@link WikiSession#isAuthenticated()} returns <code>true</code>,
     * this method returns the name of the principal returned by {@link WikiSession#getLoginPrincipal()}.
     */
    public String getRemoteUser() {
        if( super.getRemoteUser() != null ) {
            return super.getRemoteUser();
        }

        if( m_session.isAuthenticated() ) {
            return m_session.getLoginPrincipal().getName();
        }
        return null;
    }

    /**
     * Returns the user principal for the HTTP request, taking into account both container and JSPWiki custom authentication status.
     * Specifically, if the wrapped request contains a user principal, this method returns that principal. Otherwise, if the user's
     * WikiSession is an authenticated session (that is, {@link WikiSession#isAuthenticated()} returns
     * <code>true</code>, this method returns the value of {@link WikiSession#getLoginPrincipal()}.
     */
    public Principal getUserPrincipal() {
        if( super.getUserPrincipal() != null ) {
            return super.getUserPrincipal();
        }

        if( m_session.isAuthenticated() ) {
            return m_session.getLoginPrincipal();
        }
        return null;
    }

    /**
     * Determines whether the current user possesses a supplied role, taking into account both container and JSPWIki custom authentication
     * status. Specifically, if the wrapped request shows that the user possesses the role, this method returns <code>true</code>. If not,
     * this method iterates through the built-in Role objects (<em>e.g.</em>, ANONYMOUS, ASSERTED, AUTHENTICATED) returned by
     * {@link WikiSession#getRoles()} and checks to see if any of these principals' names match the supplied role.
     */
    public boolean isUserInRole( final String role ) {
        final boolean hasContainerRole = super.isUserInRole(role);
        if( hasContainerRole ) {
            return true;
        }

        // Iterate through all of the built-in roles and look for a match
        final Principal[] principals = m_session.getRoles();
        for( final Principal value : principals ) {
            if( value instanceof Role ) {
                final Role principal = ( Role )value;
                if( Role.isBuiltInRole( principal ) && principal.getName().equals( role ) ) {
                    return true;
                }
            }
        }

        // None of the built-in roles match, so no luck
        return false;
    }

}
