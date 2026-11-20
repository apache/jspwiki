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

import java.security.Principal;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

/**
 * Presents the identity of an OpenID Connect login to the wiki as if the container had authenticated the user.
 */
class OidcRequestWrapper extends HttpServletRequestWrapper {

    private final Principal principal;
    private final Set< String > roles;

    OidcRequestWrapper( final HttpServletRequest request, final String name, final Set< String > roles ) {
        super( request );
        this.principal = () -> name;
        this.roles = roles;
    }

    /** {@inheritDoc} */
    @Override
    public String getRemoteUser() {
        return principal.getName();
    }

    /** {@inheritDoc} */
    @Override
    public Principal getUserPrincipal() {
        return principal;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isUserInRole( final String role ) {
        return roles.contains( role ) || super.isUserInRole( role );
    }

}
