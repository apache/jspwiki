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

import java.util.Properties;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.content.WikiPath;
import org.apache.wiki.content.resolver.EnglishPluralsPageNameResolver;

public class EnglishPluralsPageNameResolverTest extends TestCase
{
    private EnglishPluralsPageNameResolver resolver;
    private TestEngine m_engine;
    
    public void setUp() throws Exception
    {
        Properties props = new Properties();
        props.load( TestEngine.findTestProperties() );
        props.put( WikiEngine.PROP_MATCHPLURALS, "yes" );
        m_engine = new TestEngine( props );
        
        m_engine.saveText( "SinglePage", "This is a test." );
        m_engine.saveText( "PluralPages", "This is a test." );
        
        resolver = new EnglishPluralsPageNameResolver(m_engine);
    }

    public void tearDown() throws Exception
    {
        m_engine.deletePage( "SinglePage" );
        m_engine.deletePage( "PluralPages" );
    }
    
    public void testFinalPageName() throws Exception
    {
        WikiPath page;
        page = resolver.resolve( WikiPath.valueOf("SinglePage") );
        assertEquals( "Main:SinglePage", page.toString() );
        
        page = resolver.resolve( WikiPath.valueOf("SinglePages") );
        assertEquals( "Main:SinglePage", page.toString() );
        
        page = resolver.resolve( WikiPath.valueOf("PluralPages") );
        assertEquals( "Main:PluralPages", page.toString() );
        
        page = resolver.resolve( WikiPath.valueOf( "PluralPage" ) );
        assertEquals( "Main:PluralPages", page.toString() );
        
        page = resolver.resolve( WikiPath.valueOf("NonExistentPage") );
        assertEquals( null, page );
    }

    public static Test suite()
    {
        return new TestSuite(EnglishPluralsPageNameResolverTest.class);
    }

}
