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

import org.pac4j.core.client.Client;
import org.pac4j.core.client.Clients;
import org.pac4j.core.config.Config;
import org.pac4j.core.config.ConfigFactory;
import org.pac4j.core.context.CallContext;
import org.pac4j.core.engine.DefaultLogoutLogic;
import org.pac4j.core.http.callback.NoParameterCallbackUrlResolver;
import org.pac4j.core.http.url.DefaultUrlResolver;
import org.pac4j.core.http.url.UrlResolver;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.OidcConfiguration;

import com.nimbusds.oauth2.sdk.auth.ClientAuthenticationMethod;

/**
 * Builds the pac4j configuration for any OpenID Connect provider, as Keycloak, Google or Microsoft Entra ID, from
 * {@code jspwiki-oidc.properties}.
 */
public class OidcConfigFactory implements ConfigFactory {

    /** {@inheritDoc} */
    @Override
    public Config build( final Object... parameters ) {
        final OidcSettings settings = OidcSettings.load();
        final OidcConfiguration oidc = new OidcConfiguration();
        oidc.setClientId( settings.clientId() );
        oidc.setSecret( settings.secret() );
        oidc.setScope( settings.scope() );
        oidc.setDiscoveryURI( settings.discoveryUri() );
        oidc.setClientAuthenticationMethod( ClientAuthenticationMethod.CLIENT_SECRET_BASIC );
        final OidcClient client = new OidcClient( oidc );
        client.setName( OidcSettings.CLIENT_NAME );
        if( settings.rolesClaim() != null ) {
            client.addAuthorizationGenerator( new ClaimRolesAuthorizationGenerator( settings.rolesClaim(), settings.roleMapping() ) );
        }
        final UrlResolver urlResolver = new DefaultUrlResolver( true );
        client.setUrlResolver( urlResolver );
        final Clients clients = new Clients( settings.callbackUrl(), client );
        // there is a single client, so the callback URL needs no parameter and can be registered as is in the provider
        clients.setCallbackUrlResolver( new NoParameterCallbackUrlResolver() );
        final Config config = new Config( clients );
        // the identity provider needs an absolute URL to return to after the logout
        config.setLogoutLogic( new DefaultLogoutLogic() {
            @Override
            protected String enhanceRedirectUrl( final CallContext ctx, final Config config, final Client client, final String redirectUrl ) {
                return urlResolver.compute( redirectUrl, ctx.webContext() );
            }
        } );
        return config;
    }

}
