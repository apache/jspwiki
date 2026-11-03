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


public class PreferencesTest {

    private static Preferences parse( final String cookieValue ) {
        final HttpServletRequest request = Mockito.mock( HttpServletRequest.class );
        if( cookieValue != null ) {
            Mockito.when( request.getCookies() ).thenReturn( new Cookie[] { new Cookie( Preferences.COOKIE_USER_PREFS_NAME, cookieValue ) } );
        }
        final Preferences prefs = new Preferences();
        prefs.put( "SkinName", "PlainVanilla" );
        Preferences.parseJSONPreferences( request, prefs );
        return prefs;
    }

    /** https://issues.apache.org/jira/browse/JSPWIKI-1323 */
    @Test
    public void cookieOverridesTheDefaults() {
        final Preferences prefs = parse( "%7B%22SkinName%22%3A%22Smart%22%2C%22Layout%22%3A%22fixed%22%7D" );
        Assertions.assertEquals( "Smart", prefs.get( "SkinName" ) );
        Assertions.assertEquals( "fixed", prefs.get( "Layout" ) );
    }

    /** https://issues.apache.org/jira/browse/JSPWIKI-1323 */
    @Test
    public void missingCookieKeepsTheDefaults() {
        final Preferences prefs = parse( null );
        Assertions.assertEquals( "PlainVanilla", prefs.get( "SkinName" ) );
        Assertions.assertEquals( 1, prefs.size() );
    }

    /** https://issues.apache.org/jira/browse/JSPWIKI-1323 */
    @Test
    public void cookieThatIsNotAJsonObjectIsIgnored() {
        for( final String value : new String[] { "%7Bnot-json", "%5B1%2C2%5D", "123", "text", "%22text%22", "null", "true", "%7B", "%7B%22a%22%3A" } ) {
            final Preferences prefs = Assertions.assertDoesNotThrow( () -> parse( value ), value );
            Assertions.assertEquals( "PlainVanilla", prefs.get( "SkinName" ), value );
            Assertions.assertEquals( 1, prefs.size(), value );
        }
    }

    /** https://issues.apache.org/jira/browse/JSPWIKI-1323 */
    @Test
    public void cookieWithBrokenPercentEncodingIsIgnored() {
        for( final String value : new String[] { "%zz", "%7B%22a%22%3A%22b%22%7D%", "%E0%A4%A" } ) {
            final Preferences prefs = Assertions.assertDoesNotThrow( () -> parse( value ), value );
            Assertions.assertEquals( "PlainVanilla", prefs.get( "SkinName" ), value );
        }
    }

    /** https://issues.apache.org/jira/browse/JSPWIKI-1323 */
    @Test
    public void valuesThatAreNotStringsAreKeptAsText() {
        final Preferences prefs = parse( "%7B%22Sidebar%22%3Anull%2C%22Layout%22%3A%5B1%5D%2C%22Flag%22%3Atrue%7D" );
        Assertions.assertEquals( "PlainVanilla", prefs.get( "SkinName" ) );
        Assertions.assertFalse( prefs.containsKey( "Sidebar" ) );
        Assertions.assertEquals( "[1.0]", prefs.get( "Layout" ) );
        Assertions.assertEquals( "true", prefs.get( "Flag" ) );
    }

}
