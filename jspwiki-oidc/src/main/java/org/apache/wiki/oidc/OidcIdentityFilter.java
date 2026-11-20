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

import org.pac4j.core.profile.ProfileManager;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.jee.context.JEEContext;
import org.pac4j.jee.context.session.JEESessionStore;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Hands the user logged in with OpenID Connect to the wiki. It has to run before {@code WikiServletFilter}, which takes
 * the user from {@code getRemoteUser()} and {@code getUserPrincipal()} of the request, as it does with a container login.
 */
public class OidcIdentityFilter implements Filter {

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
            final String name = user.getUsername() != null ? user.getUsername() : user.getId();
            chain.doFilter( new OidcRequestWrapper( req, name, Set.copyOf( user.getRoles() ) ), response );
        } else {
            chain.doFilter( request, response );
        }
    }

}
