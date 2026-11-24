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

import java.util.Arrays;
import java.util.Properties;

import org.apache.wiki.api.core.Engine;
import org.apache.wiki.auth.authorize.Role;
import org.apache.wiki.auth.authorize.WebContainerAuthorizer;

/**
 * Authorizer for wikis whose users log in with OpenID Connect. The login is done by filters and not by constraints of the web
 * application descriptor, so this authorizer declares that the container handles the login, and takes the names of the roles
 * from the property {@code jspwiki.oidc.roles}, a comma separated list that defaults to {@code Admin,Authenticated}.
 */
public class OidcAuthorizer extends WebContainerAuthorizer {

    /** Property with the comma separated names of the roles the identity provider may grant. */
    public static final String PROP_ROLES = "jspwiki.oidc.roles";

    private static final String DEFAULT_ROLES = "Admin,Authenticated";

    /** {@inheritDoc} */
    @Override
    public void initialize( final Engine engine, final Properties props ) {
        super.initialize( engine, props );
        m_containerAuthorized = true;
        m_containerRoles = Arrays.stream( props.getProperty( PROP_ROLES, DEFAULT_ROLES ).split( "," ) )
                                 .map( String::trim )
                                 .filter( name -> !name.isEmpty() )
                                 .map( Role::new )
                                 .toArray( Role[]::new );
    }

}
