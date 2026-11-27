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
import org.pac4j.core.client.Client;
import org.pac4j.core.config.ConfigFactory;
import org.pac4j.core.context.CallContext;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.engine.DefaultLogoutLogic;
import org.pac4j.core.http.url.DefaultUrlResolver;
import org.pac4j.core.http.url.UrlResolver;
import org.pac4j.jee.context.JEEContext;
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
        final UrlResolver urlResolver = new DefaultUrlResolver( true );
        client.setUrlResolver( urlResolver );
        final Config config = new Config( new Clients( required( props, "oidc.callbackUrl" ), client ) );
        // the identity provider needs an absolute URL to return to after the logout; DefaultUrlResolver only adds the
        // scheme, host and port, so a webapp deployed under a context path (anything but ROOT) would lose it here
        config.setLogoutLogic( new DefaultLogoutLogic() {
            @Override
            protected String enhanceRedirectUrl( final CallContext ctx, final Config config, final Client client, final String redirectUrl ) {
                return logoutRedirectUrl( ctx.webContext(), redirectUrl, urlResolver );
            }
        } );
        return config;
    }

    /**
     * Builds the absolute URL the identity provider is told to send the user back to after the logout. {@code redirectUrl}
     * is the {@code defaultUrl} of the logout filter, a path relative to the root of the web application (for example
     * {@code /Wiki.jsp}); {@link DefaultUrlResolver} only turns it into {@code scheme://host:port} plus that path, which
     * drops the context path of a webapp that is not deployed at the server root.
     */
    static String logoutRedirectUrl( final WebContext webContext, final String redirectUrl, final UrlResolver urlResolver ) {
        if( redirectUrl.startsWith( "/" ) && webContext instanceof JEEContext ) {
            final String contextPath = ( ( JEEContext )webContext ).getNativeRequest().getContextPath();
            if( !contextPath.isEmpty() ) {
                return urlResolver.compute( contextPath + redirectUrl, webContext );
            }
        }
        return urlResolver.compute( redirectUrl, webContext );
    }

    private static String required( final Properties props, final String key ) {
        final String value = props.getProperty( key );
        if( value == null || value.isBlank() ) {
            throw new IllegalStateException( CONFIG_FILE + " does not define " + key );
        }
        return value;
    }

}
