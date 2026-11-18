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

import java.io.InputStream;
import java.util.Properties;

import com.nimbusds.oauth2.sdk.auth.ClientAuthenticationMethod;
import org.pac4j.core.client.Clients;
import org.pac4j.core.config.Config;
import org.pac4j.core.config.ConfigFactory;
import org.pac4j.core.http.url.DefaultUrlResolver;
import org.pac4j.oidc.authorization.generator.KeycloakRolesAuthorizationGenerator;
import org.pac4j.oidc.client.KeycloakOidcClient;
import org.pac4j.oidc.config.KeycloakOidcConfiguration;

/**
 * Builds the pac4j configuration from {@code jspwiki-oidc.properties}, which has to be in the classpath of the web application.
 */
public class OidcConfigFactory implements ConfigFactory {

    static final String CONFIG_FILE = "/jspwiki-oidc.properties";

    /** {@inheritDoc} */
    @Override
    public Config build( final Object... parameters ) {
        final Properties props = new Properties();
        try( InputStream in = OidcConfigFactory.class.getResourceAsStream( CONFIG_FILE ) ) {
            if( in == null ) {
                throw new IllegalStateException( CONFIG_FILE + " not found in the classpath" );
            }
            props.load( in );
        } catch( final java.io.IOException e ) {
            throw new IllegalStateException( "Could not read " + CONFIG_FILE, e );
        }
        final KeycloakOidcConfiguration oidc = new KeycloakOidcConfiguration();
        oidc.setClientId( required( props, "oidc.clientId" ) );
        oidc.setSecret( required( props, "oidc.secret" ) );
        oidc.setScope( props.getProperty( "oidc.scope", "openid email profile" ) );
        oidc.setClientAuthenticationMethod( ClientAuthenticationMethod.CLIENT_SECRET_BASIC );
        oidc.setRealm( required( props, "oidc.realm" ) );
        oidc.setBaseUri( required( props, "oidc.baseUri" ) );
        oidc.setDiscoveryURI( required( props, "oidc.discoveryUri" ) );
        final KeycloakOidcClient client = new KeycloakOidcClient( oidc );
        client.addAuthorizationGenerator( new KeycloakRolesAuthorizationGenerator() );
        client.setUrlResolver( new DefaultUrlResolver( true ) );
        return new Config( new Clients( required( props, "oidc.callbackUrl" ), client ) );
    }

    private static String required( final Properties props, final String key ) {
        final String value = props.getProperty( key );
        if( value == null || value.isBlank() ) {
            throw new IllegalStateException( CONFIG_FILE + " does not define " + key );
        }
        return value;
    }

}
