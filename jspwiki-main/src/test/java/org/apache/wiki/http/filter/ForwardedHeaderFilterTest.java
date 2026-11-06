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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * This class really should be the http filters jar file, however due to 
 * dependencies issues, it had to be here. maybe the forwarded header filter
 * should be in the main jar.
 */
public class ForwardedHeaderFilterTest {

    @Test
    public void testGetRemoteAddressTrustsForwardedForOnlyBehindTrustedProxy() throws Exception{
          
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
        
        TestEngine m_testEngine = TestEngine.build( props);
        
        // default: no trusted proxies configured, so X-Forwarded-For is ignored
        HttpServletRequest direct = mock(HttpServletRequest.class);
        when(direct.getRemoteAddr()).thenReturn("203.0.113.10");
        when(direct.getHeaders("X-Forwarded-For")).thenReturn(Collections.enumeration(List.of("6.6.6.6")));
        assertEquals("203.0.113.10", HttpUtil.getRemoteAddress(direct));

        ForwardedHeaderFilter filter = new ForwardedHeaderFilter();
        TestFitlerConfig testFitlerConfig = new TestFitlerConfig();
        testFitlerConfig.settings.put("trustedProxies", "10.0.0.1");
        filter.init(testFitlerConfig);

        // peer is not in the trusted list: header is still ignored
        assertEquals("203.0.113.10", HttpUtil.getRemoteAddress(direct));

        // trusted peer, single entry: the forwarded client address is returned, trimmed
        HttpServletRequest single = mock(HttpServletRequest.class);
        
        when(single.getRemoteAddr()).thenReturn("10.0.0.1");
        when(single.getHeaders("X-Forwarded-For")).thenReturn(Collections.enumeration(List.of(" 198.51.100.7 ")));filter.doFilter(direct, mock(HttpServletResponse.class
        ), mock(FilterChain.class));
        assertEquals("198.51.100.7", HttpUtil.getRemoteAddress(single));

        // comma-parse regression: the client address is returned, not ", <addr>" nor the raw list; an entry
        // pre-populated by the client before the trusted proxy appended the real address is skipped over
        HttpServletRequest proxied = mock(HttpServletRequest.class);
        when(proxied.getRemoteAddr()).thenReturn("10.0.0.1");
        when(proxied.getHeaders("X-Forwarded-For")).thenReturn(Collections.enumeration(List.of("6.6.6.6, 198.51.100.7")));
        assertEquals("198.51.100.7", HttpUtil.getRemoteAddress(proxied));

        // a forwarded value that does not look like an IP address falls back to the peer address
        HttpServletRequest garbage = mock(HttpServletRequest.class);
        when(garbage.getRemoteAddr()).thenReturn("10.0.0.1");
        when(garbage.getHeaders("X-Forwarded-For")).thenReturn(Collections.enumeration(List.of("evil injected\r\nvalue")));
        assertEquals("10.0.0.1", HttpUtil.getRemoteAddress(garbage));
        
    }

}
