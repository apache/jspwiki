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

import java.util.Properties;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class OidcSettingsTest {

    private Properties required() {
        final Properties props = new Properties();
        props.setProperty( "oidc.clientId", "id" );
        props.setProperty( "oidc.secret", "secret" );
        props.setProperty( "oidc.discoveryUri", "https://idp.example.org/.well-known/openid-configuration" );
        props.setProperty( "oidc.callbackUrl", "/oidc/callback" );
        return props;
    }

    @Test
    void usesDefaults() {
        final OidcSettings settings = new OidcSettings( required() );
        Assertions.assertEquals( "openid email profile", settings.scope() );
        Assertions.assertEquals( "preferred_username", settings.usernameClaim() );
        Assertions.assertNull( settings.rolesClaim() );
        Assertions.assertTrue( settings.roleMapping().isEmpty() );
    }

    @Test
    void failsWhenARequiredSettingIsMissing() {
        final Properties props = required();
        props.remove( "oidc.discoveryUri" );
        final IllegalStateException e = Assertions.assertThrows( IllegalStateException.class, () -> new OidcSettings( props ).discoveryUri() );
        Assertions.assertTrue( e.getMessage().contains( "oidc.discoveryUri" ) );
    }

    @Test
    void readsTheClaimsAndTheRoleMapping() {
        final Properties props = required();
        props.setProperty( "oidc.usernameClaim", " email " );
        props.setProperty( "oidc.rolesClaim", "roles" );
        props.setProperty( "oidc.roleMapping", "1111-aaaa = Admin, 2222-bbbb=Editors,broken,=x,y=" );
        final OidcSettings settings = new OidcSettings( props );
        Assertions.assertEquals( "email", settings.usernameClaim() );
        Assertions.assertEquals( "roles", settings.rolesClaim() );
        Assertions.assertEquals( 2, settings.roleMapping().size() );
        Assertions.assertEquals( "Admin", settings.roleMapping().get( "1111-aaaa" ) );
        Assertions.assertEquals( "Editors", settings.roleMapping().get( "2222-bbbb" ) );
    }

}
