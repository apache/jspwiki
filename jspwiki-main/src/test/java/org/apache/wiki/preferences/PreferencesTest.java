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
package org.apache.wiki.preferences;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;


class PreferencesTest {

    private HttpServletRequest requestWithCookie( final String json ) {
        final HttpServletRequest request = Mockito.mock( HttpServletRequest.class );
        final Cookie cookie = new Cookie( Preferences.COOKIE_USER_PREFS_NAME, URLEncoder.encode( json, StandardCharsets.UTF_8 ) );
        Mockito.doReturn( new Cookie[] { cookie } ).when( request ).getCookies();
        return request;
    }

    @Test
    void testNothingIsSetByUserWithoutCookie() {
        final Preferences prefs = new Preferences();
        prefs.put( "SkinName", "default" );

        Preferences.parseJSONPreferences( Mockito.mock( HttpServletRequest.class ), prefs );

        Assertions.assertFalse( prefs.isSetByUser( "SkinName" ) );
        Assertions.assertEquals( "default", prefs.get( "SkinName" ) );
    }

    @Test
    void testSkinChosenAsTheDefaultOneIsSetByUser() {
        final Preferences prefs = new Preferences();
        prefs.put( "SkinName", "default" );

        Preferences.parseJSONPreferences( requestWithCookie( "{\"SkinName\":\"default\",\"Layout\":\"fluid\"}" ), prefs );

        Assertions.assertTrue( prefs.isSetByUser( "SkinName" ) );
        Assertions.assertTrue( prefs.isSetByUser( "Layout" ) );
    }

    @Test
    void testPreferenceMissingFromCookieIsNotSetByUser() {
        final Preferences prefs = new Preferences();
        prefs.put( "SkinName", "default" );
        prefs.put( "Layout", "fluid" );

        Preferences.parseJSONPreferences( requestWithCookie( "{\"Layout\":\"fixed\"}" ), prefs );

        Assertions.assertEquals( "fixed", prefs.get( "Layout" ) );
        Assertions.assertTrue( prefs.isSetByUser( "Layout" ) );
        Assertions.assertFalse( prefs.isSetByUser( "SkinName" ) );
    }

}
