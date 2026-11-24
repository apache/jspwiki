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
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Settings of the OpenID Connect login, read from {@code jspwiki-oidc.properties}, which has to be in the classpath of the web
 * application.
 */
final class OidcSettings {

    static final String CONFIG_FILE = "/jspwiki-oidc.properties";

    /** Name of the pac4j client, the one the filters of web.xml refer to. */
    static final String CLIENT_NAME = "jspwiki-oidc";

    private static final String DEFAULT_SCOPE = "openid email profile";
    private static final String DEFAULT_USERNAME_CLAIM = "preferred_username";

    private final Properties props;

    OidcSettings( final Properties props ) {
        this.props = props;
    }

    /**
     * Loads the settings from the classpath.
     *
     * @return the settings
     * @throws IllegalStateException if the file is missing or cannot be read
     */
    static OidcSettings load() {
        final Properties props = new Properties();
        try( InputStream in = OidcSettings.class.getResourceAsStream( CONFIG_FILE ) ) {
            if( in == null ) {
                throw new IllegalStateException( CONFIG_FILE + " not found in the classpath" );
            }
            props.load( in );
        } catch( final IOException e ) {
            throw new IllegalStateException( "Could not read " + CONFIG_FILE, e );
        }
        return new OidcSettings( props );
    }

    String clientId() {
        return required( "oidc.clientId" );
    }

    String secret() {
        return required( "oidc.secret" );
    }

    String discoveryUri() {
        return required( "oidc.discoveryUri" );
    }

    String callbackUrl() {
        return required( "oidc.callbackUrl" );
    }

    String scope() {
        return props.getProperty( "oidc.scope", DEFAULT_SCOPE ).trim();
    }

    /** @return the claim with the name of the user, {@code preferred_username} by default */
    String usernameClaim() {
        return props.getProperty( "oidc.usernameClaim", DEFAULT_USERNAME_CLAIM ).trim();
    }

    /** @return the claim with the roles, which may be a dotted path, or {@code null} if the provider does not send roles */
    String rolesClaim() {
        final String claim = props.getProperty( "oidc.rolesClaim" );
        return claim == null || claim.isBlank() ? null : claim.trim();
    }

    /**
     * Mapping from the values of the roles claim to the names of the roles of the wiki, for the providers that send identifiers
     * instead of names. It is written {@code value=Role,value2=Role2}.
     *
     * @return the mapping, empty if there is none
     */
    Map< String, String > roleMapping() {
        final Map< String, String > mapping = new LinkedHashMap<>();
        final String value = props.getProperty( "oidc.roleMapping" );
        if( value != null ) {
            for( final String pair : value.split( "," ) ) {
                final int eq = pair.indexOf( '=' );
                if( eq > 0 && eq < pair.length() - 1 ) {
                    mapping.put( pair.substring( 0, eq ).trim(), pair.substring( eq + 1 ).trim() );
                }
            }
        }
        return mapping;
    }

    private String required( final String key ) {
        final String value = props.getProperty( key );
        if( value == null || value.isBlank() ) {
            throw new IllegalStateException( CONFIG_FILE + " does not define " + key );
        }
        return value.trim();
    }

}
