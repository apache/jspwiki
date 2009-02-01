/*
    JSPWiki - a JSP-based WikiWiki clone.

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

import java.security.Principal;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiSession;
import org.apache.wiki.auth.SessionMonitor;
import org.apache.wiki.auth.authorize.Role;
import org.apache.wiki.preferences.Preferences;


/**
 * Servlet request wrapper that encapsulates an incoming HTTP request and
 * overrides its security methods so that the request returns JSPWiki-specific
 * values. It also ensures that the user's {@link java.util.Locale} preference
 * is respected by ensuring that the {@link #getLocale()} returns a preferred
 * Locale matching what the user supplies in the
 * {@link org.apache.wiki.preferences.Preferences#PREFS_LOCALE} cookie. This
 * Locale will also be returned at the top of the enumeration returned by
 * {@link #getLocales()}.
 * 
 * @author Andrew Jaquith
 * @since 2.8
 */
public class WikiRequestWrapper extends HttpServletRequestWrapper
{
    private final WikiSession m_session;

    private final Locale m_locale;

    /**
     * Constructs a new wrapped request.
     * 
     * @param engine the wiki engine
     * @param request the request to wrap
     */
    public WikiRequestWrapper( WikiEngine engine, HttpServletRequest request )
    {
        super( request );

        // Get and stash a reference to the current WikiSession
        m_session = SessionMonitor.getInstance( engine ).find( request.getSession() );

        // Figure out the user's preferred Locale based on the cookie value, if supplied
        Locale locale = Preferences.getLocale( request );
        m_locale = request.getLocale().equals( locale ) ? null : locale;
    }

    /**
     * {@inheritDoc}. If the user supplied a preferred locale via the cookie
     * value {@link org.apache.wiki.preferences.Preferences#PREFS_LOCALE},
     * that value is returned in preference.
     */
    public Locale getLocale()
    {
        return m_locale == null ? super.getLocale() : m_locale;
    }

    /**
     * {@inheritDoc}. If the user supplied a preferred locale via the cookie
     * value {@link org.apache.wiki.preferences.Preferences#PREFS_LOCALE},
     * that value is returned as the first item in the array.
     */
    public Enumeration getLocales()
    {
        Enumeration requestLocales = super.getLocales();
        if( m_locale == null )
        {
            return requestLocales;
        }

        Vector<Locale> locales = new Vector<Locale>();
        locales.add( m_locale );
        for( Enumeration e = requestLocales; e.hasMoreElements(); )
        {
            locales.add( (Locale)e.nextElement() );
        }
        return locales.elements();
    }

    /**
     * Returns the remote user for the HTTP request, taking into account both
     * container and JSPWiki custom authentication status. Specifically, if the
     * wrapped request contains a remote user, this method returns that remote
     * user. Otherwise, if the user's WikiSession is an authenticated session
     * (that is, {@link WikiSession#isAuthenticated()} returns <code>true</code>,
     * this method returns the name of the principal returned by
     * {@link WikiSession#getLoginPrincipal()}.
     */
    public String getRemoteUser()
    {
        if( super.getRemoteUser() != null )
        {
            return super.getRemoteUser();
        }

        if( m_session.isAuthenticated() )
        {
            return m_session.getLoginPrincipal().getName();
        }
        return null;
    }

    /**
     * Returns the user principal for the HTTP request, taking into account both
     * container and JSPWiki custom authentication status. Specifically, if the
     * wrapped request contains a user principal, this method returns that
     * principal. Otherwise, if the user's WikiSession is an authenticated
     * session (that is, {@link WikiSession#isAuthenticated()} returns
     * <code>true</code>, this method returns the value of
     * {@link WikiSession#getLoginPrincipal()}.
     */
    public Principal getUserPrincipal()
    {
        if( super.getUserPrincipal() != null )
        {
            return super.getUserPrincipal();
        }

        if( m_session.isAuthenticated() )
        {
            return m_session.getLoginPrincipal();
        }
        return null;
    }

    /**
     * Determines whether the current user possesses a supplied role, taking
     * into account both container and JSPWIki custom authentication status.
     * Specifically, if the wrapped request shows that the user possesses the
     * role, this method returns <code>true</code>. If not, this method
     * iterates through the built-in Role objects (<em>e.g.</em>, ANONYMOUS,
     * ASSERTED, AUTHENTICATED) returned by {@link WikiSession#getRoles()} and
     * checks to see if any of these principals' names match the supplied role.
     */
    public boolean isUserInRole( String role )
    {
        boolean hasContainerRole = super.isUserInRole( role );
        if( hasContainerRole )
        {
            return true;
        }

        // Iterate through all of the built-in roles and look for a match
        Principal[] principals = m_session.getRoles();
        for( int i = 0; i < principals.length; i++ )
        {
            if( principals[i] instanceof Role )
            {
                Role principal = (Role) principals[i];
                if( Role.isBuiltInRole( principal ) && principal.getName().equals( role ) )
                {
                    return true;
                }
            }
        }

        // None of the built-in roles match, so no luck
        return false;
    }

}
