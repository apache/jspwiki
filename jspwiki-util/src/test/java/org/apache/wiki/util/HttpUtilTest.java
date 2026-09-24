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
package org.apache.wiki.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class HttpUtilTest {

    @Test
    public void testIsIPV4Address() {
        Assertions.assertFalse( HttpUtil.isIPV4Address( null ) );
        Assertions.assertFalse( HttpUtil.isIPV4Address( "" ) );
        Assertions.assertFalse( HttpUtil.isIPV4Address( ".123.123.123.123" ) );
        Assertions.assertFalse( HttpUtil.isIPV4Address( "123.123.123.123." ) );
        Assertions.assertFalse( HttpUtil.isIPV4Address( "123.123.123" ) );
        Assertions.assertFalse( HttpUtil.isIPV4Address( "123.123.123.123.123" ) );
        Assertions.assertFalse( HttpUtil.isIPV4Address( "abc.123.123.123" ) );
        Assertions.assertFalse( HttpUtil.isIPV4Address( "Me" ) );
        Assertions.assertFalse( HttpUtil.isIPV4Address( "Guest" ) );
        Assertions.assertFalse( HttpUtil.isIPV4Address( "1207.0.0.1" ) );
        Assertions.assertFalse( HttpUtil.isIPV4Address( "127..0.1" ) );
        Assertions.assertFalse( HttpUtil.isIPV4Address( "1207.0.0." ) );
        Assertions.assertFalse( HttpUtil.isIPV4Address( ".0.0.1" ) );
        Assertions.assertFalse( HttpUtil.isIPV4Address( "..." ) );

        Assertions.assertTrue( HttpUtil.isIPV4Address( "127.0.0.1" ) );
        Assertions.assertTrue( HttpUtil.isIPV4Address( "12.123.123.123" ) );
        Assertions.assertTrue( HttpUtil.isIPV4Address( "012.123.123.123" ) );
        Assertions.assertTrue( HttpUtil.isIPV4Address( "123.123.123.123" ) );
    }

    @Test
    public void testRetrieveCookieValue() {
        final Cookie[] cookies = new Cookie[] { new Cookie( "cookie1", "value1" ),
                                                new Cookie( "cookie2", "\"value2\"" ),
                                                new Cookie( "cookie3", "" ),
                                                new Cookie( "cookie4", null ) };
        final HttpServletRequest req = Mockito.mock( HttpServletRequest.class );
        Mockito.doReturn( cookies ).when( req ).getCookies();

        assertEquals( "value1", HttpUtil.retrieveCookieValue( req, "cookie1" ) );
        assertEquals( "value2", HttpUtil.retrieveCookieValue( req, "cookie2" ) );
        Assertions.assertNull( HttpUtil.retrieveCookieValue( req, "cookie3" ) );
        Assertions.assertNull( HttpUtil.retrieveCookieValue( req, "cookie4" ) );
        Assertions.assertNull( HttpUtil.retrieveCookieValue( req, "cookie5" ) );
    }

    @Test
    public void testGetAbsoluteUrlWithRelativeUrl() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getScheme()).thenReturn("http");
        when(request.getServerName()).thenReturn("localhost");
        when(request.getServerPort()).thenReturn(8080);

        String relativeUrl = "/login";
        String expected = "http://localhost:8080/login";

        String actual = HttpUtil.getAbsoluteUrl(request, relativeUrl);
        assertEquals(expected, actual);
    }

    @Test
    public void testGetAbsoluteUrlWithoutRelativeUrl() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getScheme()).thenReturn("https");
        when(request.getServerName()).thenReturn("localhost");
        when(request.getServerPort()).thenReturn(443);

        String expected = "https://localhost";

        String actual = HttpUtil.getAbsoluteUrl(request);
        assertEquals(expected, actual);
    }

    @Test
    public void testGetAbsoluteUrlWithDefaultHttpPort() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getScheme()).thenReturn("http");
        when(request.getServerName()).thenReturn("localhost");
        when(request.getServerPort()).thenReturn(80);

        String relativeUrl = "/login";
        String expected = "http://localhost/login";

        String actual = HttpUtil.getAbsoluteUrl(request, relativeUrl);
        assertEquals(expected, actual);
    }

    @Test
    public void testGetAbsoluteUrlWithDefaultHttpsPort() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getScheme()).thenReturn("https");
        when(request.getServerName()).thenReturn("localhost");
        when(request.getServerPort()).thenReturn(443);

        String relativeUrl = "/login";
        String expected = "https://localhost/login";

        String actual = HttpUtil.getAbsoluteUrl(request, relativeUrl);
        assertEquals(expected, actual);
    }

    @Test
    public void testGetAbsoluteUrlWithForwardedHostAndProto() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-Host")).thenReturn("proxyhost");
        when(request.getHeader("X-Forwarded-Proto")).thenReturn("https");

        String relativeUrl = "/login";
        String expected = "https://proxyhost/login";

        String actual = HttpUtil.getAbsoluteUrl(request, relativeUrl);
        assertEquals(expected, actual);
    }

    @Test
    public void testGetAbsoluteUrlWithForwardedServerAndProto() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-Server")).thenReturn("proxyserver");
        when(request.getHeader("X-Forwarded-Proto")).thenReturn("https");

        String relativeUrl = "/login";
        String expected = "https://proxyserver/login";

        String actual = HttpUtil.getAbsoluteUrl(request, relativeUrl);
        assertEquals(expected, actual);
    }

    @Test
    public void testGetAbsoluteUrlWithNoForwardedHeaders() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getScheme()).thenReturn("http");
        when(request.getServerName()).thenReturn("localhost");
        when(request.getServerPort()).thenReturn(8080);

        String relativeUrl = "/login";
        String expected = "http://localhost:8080/login";

        String actual = HttpUtil.getAbsoluteUrl(request, relativeUrl);
        assertEquals(expected, actual);
    }

    @Test
    public void testGetAbsoluteUrlWithAllHeaders() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-Host")).thenReturn("forwardedHost");
        when(request.getHeader("X-Forwarded-Proto")).thenReturn("forwardedProto");
        when(request.getHeader("X-Forwarded-Server")).thenReturn("forwardedServer");
        when(request.getScheme()).thenReturn("https");
        when(request.getServerName()).thenReturn("localhost");
        when(request.getServerPort()).thenReturn(443);

        String expected = "forwardedProto://forwardedHost";

        String actual = HttpUtil.getAbsoluteUrl(request);
        assertEquals(expected, actual);
    }

    @Test
    public void testGetRemoteAddressTrustsForwardedForOnlyBehindTrustedProxy() {
        try {
            // default: no trusted proxies configured, so X-Forwarded-For is ignored
            HttpUtil.resetTrustedProxies();
            HttpServletRequest direct = mock( HttpServletRequest.class );
            when( direct.getRemoteAddr() ).thenReturn( "203.0.113.10" );
            when( direct.getHeaders( "X-Forwarded-For" ) ).thenReturn( Collections.enumeration( List.of( "6.6.6.6" ) ) );
            assertEquals( "203.0.113.10", HttpUtil.getRemoteAddress( direct ) );

            System.setProperty( HttpUtil.PROP_TRUSTED_PROXIES, "10.0.0.1" );
            HttpUtil.resetTrustedProxies();

            // peer is not in the trusted list: header is still ignored
            assertEquals( "203.0.113.10", HttpUtil.getRemoteAddress( direct ) );

            // trusted peer, single entry: the forwarded client address is returned, trimmed
            HttpServletRequest single = mock( HttpServletRequest.class );
            when( single.getRemoteAddr() ).thenReturn( "10.0.0.1" );
            when( single.getHeaders( "X-Forwarded-For" ) ).thenReturn( Collections.enumeration( List.of( " 198.51.100.7 " ) ) );
            assertEquals( "198.51.100.7", HttpUtil.getRemoteAddress( single ) );

            // comma-parse regression: the client address is returned, not ", <addr>" nor the raw list; an entry
            // pre-populated by the client before the trusted proxy appended the real address is skipped over
            HttpServletRequest proxied = mock( HttpServletRequest.class );
            when( proxied.getRemoteAddr() ).thenReturn( "10.0.0.1" );
            when( proxied.getHeaders( "X-Forwarded-For" ) ).thenReturn( Collections.enumeration( List.of( "6.6.6.6, 198.51.100.7" ) ) );
            assertEquals( "198.51.100.7", HttpUtil.getRemoteAddress( proxied ) );

            // a forwarded value that does not look like an IP address falls back to the peer address
            HttpServletRequest garbage = mock( HttpServletRequest.class );
            when( garbage.getRemoteAddr() ).thenReturn( "10.0.0.1" );
            when( garbage.getHeaders( "X-Forwarded-For" ) ).thenReturn( Collections.enumeration( List.of( "evil injected\r\nvalue" ) ) );
            assertEquals( "10.0.0.1", HttpUtil.getRemoteAddress( garbage ) );
        } finally {
            System.clearProperty( HttpUtil.PROP_TRUSTED_PROXIES );
            HttpUtil.resetTrustedProxies();
        }
    }


}
