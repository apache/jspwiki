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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.pac4j.core.http.url.DefaultUrlResolver;
import org.pac4j.core.http.url.UrlResolver;
import org.pac4j.jee.context.JEEContext;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

class OidcConfigFactoryTest {

    private final HttpServletRequest request = Mockito.mock( HttpServletRequest.class );
    private final HttpServletResponse response = Mockito.mock( HttpServletResponse.class );
    private final UrlResolver urlResolver = new DefaultUrlResolver( true );

    @Test
    void addsTheContextPathOfTheWebApplication() {
        Mockito.when( request.getScheme() ).thenReturn( "https" );
        Mockito.when( request.getServerName() ).thenReturn( "jspwiki.example.org" );
        Mockito.when( request.getServerPort() ).thenReturn( 443 );
        Mockito.when( request.getContextPath() ).thenReturn( "/department" );
        final JEEContext context = new JEEContext( request, response );

        Assertions.assertEquals( "https://jspwiki.example.org/department/Wiki.jsp",
                                  OidcConfigFactory.logoutRedirectUrl( context, "/Wiki.jsp", urlResolver ) );
    }

    @Test
    void leavesTheUrlAsIsWhenTheWebappIsDeployedAtTheServerRoot() {
        Mockito.when( request.getScheme() ).thenReturn( "https" );
        Mockito.when( request.getServerName() ).thenReturn( "jspwiki.example.org" );
        Mockito.when( request.getServerPort() ).thenReturn( 443 );
        Mockito.when( request.getContextPath() ).thenReturn( "" );
        final JEEContext context = new JEEContext( request, response );

        Assertions.assertEquals( "https://jspwiki.example.org/Wiki.jsp",
                                  OidcConfigFactory.logoutRedirectUrl( context, "/Wiki.jsp", urlResolver ) );
    }

}
