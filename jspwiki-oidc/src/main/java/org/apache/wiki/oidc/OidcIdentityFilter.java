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
package org.apache.wiki.oidc;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

import org.apache.wiki.api.core.Engine;
import org.apache.wiki.auth.UserManager;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.jee.context.JEEContext;
import org.pac4j.jee.context.session.JEESessionStore;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Hands the user logged in with OpenID Connect to the wiki. It has to run before {@code WikiServletFilter}, which takes
 * the user from {@code getRemoteUser()} and {@code getUserPrincipal()} of the request, as it does with a container login.
 */
public class OidcIdentityFilter implements Filter {

    /** Session attribute that says the profile of the user was looked for, so that it is done once per session. */
    private static final String PROVISIONED = "jspwiki.oidc.provisioned";

    /** Attribute of the servlet context where the wiki keeps its engine. */
    private static final String ENGINE = "org.apache.wiki.WikiEngine";

    private String usernameClaim;
    private ServletContext servletContext;

    /** {@inheritDoc} */
    @Override
    public void init( final FilterConfig filterConfig ) {
        usernameClaim = OidcSettings.load().usernameClaim();
        servletContext = filterConfig.getServletContext();
    }

    /** {@inheritDoc} */
    @Override
    public void doFilter( final ServletRequest request, final ServletResponse response, final FilterChain chain ) throws IOException, ServletException {
        final HttpServletRequest req = ( HttpServletRequest )request;
        if( req.getSession( false ) == null ) {
            chain.doFilter( request, response );
            return;
        }
        final JEEContext context = new JEEContext( req, ( HttpServletResponse )response );
        final ProfileManager profiles = new ProfileManager( context, new JEESessionStore() );
        final Optional< UserProfile > profile = profiles.getProfile();
        if( profile.isPresent() ) {
            final UserProfile user = profile.get();
            final String name = userName( user, usernameClaim );
            provision( req, name, user );
            chain.doFilter( new OidcRequestWrapper( req, name, Set.copyOf( user.getRoles() ) ), response );
        } else {
            chain.doFilter( request, response );
        }
    }

    /** Creates the profile of the user in the wiki, once per session. Nothing is done if the wiki is not running. */
    private void provision( final HttpServletRequest req, final String name, final UserProfile user ) {
        final HttpSession session = req.getSession( false );
        if( session == null || session.getAttribute( PROVISIONED ) != null ) {
            return;
        }
        final Object engine = servletContext == null ? null : servletContext.getAttribute( ENGINE );
        if( engine instanceof Engine ) {
            OidcUserProvisioner.provision( ( ( Engine )engine ).getManager( UserManager.class ).getUserDatabase(), name, user );
            session.setAttribute( PROVISIONED, Boolean.TRUE );
        }
    }

    /**
     * Name of the user in the wiki: the claim configured for it, or else the e-mail address, or else the identifier of the user at
     * the provider.
     */
    static String userName( final UserProfile profile, final String claim ) {
        for( final String name : new String[] { claim, "email" } ) {
            final Object value = profile.getAttribute( name );
            if( value != null && !value.toString().isBlank() ) {
                return value.toString().trim();
            }
        }
        return profile.getId();
    }

}
