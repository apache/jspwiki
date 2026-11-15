/*
 * Copyright 2026 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.wiki.http.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import org.apache.commons.io.FileUtils;
import org.apache.wiki.TestEngine;
import org.apache.wiki.auth.UserManager;
import org.apache.wiki.auth.authorize.GroupManager;
import org.apache.wiki.auth.authorize.XMLGroupDatabase;
import org.apache.wiki.auth.user.XMLUserDatabase;
import org.apache.wiki.rss.RSSGenerator;
import org.apache.wiki.util.HttpUtil;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * This class really should be the http filters jar file, however due to
 * dependencies issues, it had to be here. maybe the forwarded header filter
 * should be in the main jar.
 */
public class ForwardedHeaderFilterTest {

    @Test
    public void testGetRemoteAddressTrustsForwardedForOnlyBehindTrustedProxy() throws Exception {
        Properties props = TestEngine.getTestProperties();
        File userTarget = new File("target/" + UUID.randomUUID() + ".xml");
        FileUtils.copyFile(new File("src/test/resources/userdatabase.xml"), userTarget);
        props.setProperty(XMLUserDatabase.PROP_USERDATABASE, userTarget.getAbsolutePath());
        props.put(RSSGenerator.PROP_GENERATE_RSS, "true");
        File groupTarget = new File("target/" + UUID.randomUUID() + ".xml");
        FileUtils.copyFile(new File("src/test/resources/groupdatabase.xml"), groupTarget);
        props.setProperty(XMLGroupDatabase.PROP_DATABASE, groupTarget.getAbsolutePath());
        props.setProperty(GroupManager.PROP_GROUPDATABASE, XMLGroupDatabase.class.getCanonicalName());
        props.setProperty(UserManager.PROP_DATABASE, XMLUserDatabase.class.getCanonicalName());

        TestEngine m_testEngine = TestEngine.build(props);

        // 1. Initialize the Filter
        ForwardedHeaderFilter filter = new ForwardedHeaderFilter();
        TestFitlerConfig testFitlerConfig = new TestFitlerConfig();
        testFitlerConfig.settings.put("trustedProxies", "10.0.0.1");
        testFitlerConfig.settings.put("allowedHosts", "localhost.localdomain");
        filter.init(testFitlerConfig);

        HttpServletResponse response = mock(HttpServletResponse.class);

        // --- Test Case 1: Untrusted peer (header should be ignored) ---
        HttpServletRequest untrustedRequest = mock(HttpServletRequest.class);
        when(untrustedRequest.getRemoteAddr()).thenReturn("203.0.113.10");
        when(untrustedRequest.getHeader("X-Forwarded-For")).thenReturn("6.6.6.6");
        when(untrustedRequest.getHeaders("X-Forwarded-For")).thenReturn(Collections.enumeration(List.of("6.6.6.6")));
        when(untrustedRequest.getHeader("X-Forwarded-Host")).thenReturn("evil.apache.org");

        FilterChain untrustedChain = mock(FilterChain.class);

        // Execute the filter
        filter.doFilter(untrustedRequest, response, untrustedChain);

        // Since the peer is not trusted, the filter passes the original request down the chain
        assertEquals("203.0.113.10", HttpUtil.getRemoteAddress(untrustedRequest));

        // --- Test Case 2: Trusted peer (header should be processed) ---
        HttpServletRequest trustedRequest = mock(HttpServletRequest.class);
        when(trustedRequest.getRemoteAddr()).thenReturn("10.0.0.1");
        when(trustedRequest.getHeader("X-Forwarded-For")).thenReturn("198.51.100.7");
        when(trustedRequest.getHeaders("X-Forwarded-For")).thenReturn(Collections.enumeration(List.of("198.51.100.7")));
        when(trustedRequest.getHeader("X-Forwarded-Host")).thenReturn("localhost.localdomain");

        FilterChain trustedChain = mock(FilterChain.class);
        ArgumentCaptor<HttpServletRequest> requestCaptor = ArgumentCaptor.forClass(HttpServletRequest.class);

        // Execute the filter
        filter.doFilter(trustedRequest, response, trustedChain);

        verify(trustedChain).doFilter(requestCaptor.capture(), eq(response));
        HttpServletRequest wrappedRequest = requestCaptor.getValue();

        assertEquals("198.51.100.7", HttpUtil.getRemoteAddress(wrappedRequest));

        // --- Test Case 3: comma-parse regression ---
        HttpServletRequest proxied = mock(HttpServletRequest.class);
        when(proxied.getRemoteAddr()).thenReturn("10.0.0.1");
        when(proxied.getHeader("X-Forwarded-For")).thenReturn("198.51.100.7, 6.6.6.6");
        when(proxied.getHeaders("X-Forwarded-For")).thenReturn(Collections.enumeration(List.of("198.51.100.7, 6.6.6.6")));
        when(proxied.getHeader("X-Forwarded-Host")).thenReturn("localhost.localdomain");

        FilterChain proxiedChain = mock(FilterChain.class);
        filter.doFilter(proxied, response, proxiedChain);
        verify(proxiedChain).doFilter(requestCaptor.capture(), eq(response));
        HttpServletRequest wrappedProxied = requestCaptor.getValue();
        assertEquals("198.51.100.7", HttpUtil.getRemoteAddress(wrappedProxied));

        // --- Test Case 4: garbage value handling ---
        HttpServletRequest garbage = mock(HttpServletRequest.class);
        when(garbage.getRemoteAddr()).thenReturn("10.0.0.1");
        when(garbage.getHeader("X-Forwarded-For")).thenReturn("evil injected\r\nvalue");
        when(garbage.getHeaders("X-Forwarded-For")).thenReturn(Collections.enumeration(List.of("evil injected\r\nvalue")));
        when(garbage.getHeader("X-Forwarded-Host")).thenReturn("localhost.localdomain");

        FilterChain garbageChain = mock(FilterChain.class);
        filter.doFilter(garbage, response, garbageChain);
        verify(garbageChain).doFilter(requestCaptor.capture(), eq(response));
        HttpServletRequest wrappedGarbage = requestCaptor.getValue();
        assertEquals("evil injected\r\nvalue", HttpUtil.getRemoteAddress(wrappedGarbage));
    }

    @Test
    public void testCidrRangeMatching() throws Exception {
        ForwardedHeaderFilter filter = new ForwardedHeaderFilter();
        TestFitlerConfig config = new TestFitlerConfig();
        config.settings.put("trustedProxies", "192.168.1.0/24");
        config.settings.put("allowedHosts", "wiki.apache.org");
        filter.init(config);

        HttpServletResponse response = mock(HttpServletResponse.class);
        ArgumentCaptor<HttpServletRequest> captor = ArgumentCaptor.forClass(HttpServletRequest.class);

        // 1. IP inside CIDR range (should be trusted)
        HttpServletRequest insideRequest = mock(HttpServletRequest.class);
        when(insideRequest.getRemoteAddr()).thenReturn("192.168.1.50");
        when(insideRequest.getHeader("X-Forwarded-For")).thenReturn("203.0.113.5");
        when(insideRequest.getHeader("X-Forwarded-Host")).thenReturn("wiki.apache.org");

        FilterChain chain1 = mock(FilterChain.class);
        filter.doFilter(insideRequest, response, chain1);
        verify(chain1).doFilter(captor.capture(), eq(response));
        assertEquals("203.0.113.5", HttpUtil.getRemoteAddress(captor.getValue()));

        // 2. IP outside CIDR range (should be ignored, falls back to its own remote addr)
        HttpServletRequest outsideRequest = mock(HttpServletRequest.class);
        when(outsideRequest.getRemoteAddr()).thenReturn("192.168.2.50");
        when(outsideRequest.getHeader("X-Forwarded-For")).thenReturn("203.0.113.5");
        when(outsideRequest.getHeader("X-Forwarded-Host")).thenReturn("wiki.apache.org");

        FilterChain chain2 = mock(FilterChain.class);
        filter.doFilter(outsideRequest, response, chain2);
        verify(chain2).doFilter(captor.capture(), eq(response));
        assertEquals("192.168.2.50", HttpUtil.getRemoteAddress(captor.getValue()));
    }

    @Test
    public void testPortAndProtocolHandling() throws Exception {
        ForwardedHeaderFilter filter = new ForwardedHeaderFilter();
        TestFitlerConfig config = new TestFitlerConfig();
        config.settings.put("trustedProxies", "10.0.0.1");
        config.settings.put("allowedHosts", "wiki.apache.org"); // Matches host portion
        filter.init(config);

        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        when(request.getServerName()).thenReturn("wiki.apache.org");
        when(request.getRemotePort()).thenReturn(8443);
        // Ensure X-Forwarded-Host matches the allowed host or strip port matching
        when(request.getHeader("X-Forwarded-Host")).thenReturn("wiki.apache.org");
        when(request.getHeader("X-Forwarded-Proto")).thenReturn("https");
        when(request.getHeader("X-Forwarded-Port")).thenReturn("8443");

        FilterChain chain = mock(FilterChain.class);
        ArgumentCaptor<HttpServletRequest> captor = ArgumentCaptor.forClass(HttpServletRequest.class);

        filter.doFilter(request, response, chain);
        verify(chain).doFilter(captor.capture(), eq(response));
        HttpServletRequest wrapped = captor.getValue();

        // Assert port and protocol extractions
        assertEquals("wiki.apache.org", wrapped.getServerName());
        assertEquals(8443, wrapped.getRemotePort());
        assertEquals("https", wrapped.getScheme());
        assertTrue(wrapped.isSecure());
    }

    @Test
    public void testNonHttpServletRequestPassthrough() throws Exception {
        ForwardedHeaderFilter filter = new ForwardedHeaderFilter();
        filter.init(new TestFitlerConfig()); // Default/empty config

        // Use a generic ServletRequest instead of HttpServletRequest
        jakarta.servlet.ServletRequest rawRequest = mock(jakarta.servlet.ServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        // Should pass straight through without throwing any ClassCastException
        filter.doFilter(rawRequest, response, chain);
        verify(chain).doFilter(rawRequest, response);
    }

    @Test
    public void testUnconfiguredSecureDefaults() throws Exception {
        ForwardedHeaderFilter filter = new ForwardedHeaderFilter();
        // Unconfigured (empty config)
        filter.init(new TestFitlerConfig());

        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        when(request.getHeader("X-Forwarded-For")).thenReturn("198.51.100.7");
        when(request.getHeader("X-Forwarded-Host")).thenReturn("wiki.apache.org");

        FilterChain chain = mock(FilterChain.class);
        ArgumentCaptor<HttpServletRequest> captor = ArgumentCaptor.forClass(HttpServletRequest.class);

        filter.doFilter(request, response, chain);
        verify(chain).doFilter(captor.capture(), eq(response));

        // Because no trusted proxies or allowed hosts are configured, headers must be ignored
        assertEquals("10.0.0.1", HttpUtil.getRemoteAddress(captor.getValue()));
    }
}
