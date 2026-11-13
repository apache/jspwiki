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

}