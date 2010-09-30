/*
    JSPWiki - a JSP-based WikiWiki clone.

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

package org.apache.wiki.content.resolver;

import java.net.URI;
import java.util.Properties;

import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.content.resolver.SpecialPageNameResolver;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class SpecialPageNameResolverTest extends TestCase
{
    private SpecialPageNameResolver resolver;
    WikiEngine m_engine ;
    
    public void setUp() throws Exception
    {
        Properties props = new Properties();
        props.load( TestEngine.findTestProperties() );
        m_engine = new TestEngine( props );
        
        resolver = new SpecialPageNameResolver(m_engine);
    }

    public void tearDown() throws Exception
    {
        m_engine.deletePage( "SinglePage" );
        m_engine.deletePage( "PluralPages" );
        m_engine.shutdown();
    }
        
    public void testSpecialPageReference()
    {
        URI uri;
        uri = resolver.getSpecialPageURI( "RecentChanges" );
        assertEquals( "/RecentChanges.jsp", uri.toString() );
        
        uri = resolver.getSpecialPageURI( "Search" );
        assertEquals( "/Search.jsp", uri.toString() );
        
        // UserPrefs doesn't exist in our test properties
        uri = resolver.getSpecialPageURI( "UserPrefs" );
        assertNull( uri );
    }

    public static Test suite()
    {
        return new TestSuite(SpecialPageNameResolverTest.class);
    }

}
