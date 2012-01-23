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

package org.apache.wiki.filters;

import junit.framework.*;
import java.util.*;

import org.apache.wiki.*;
import org.apache.wiki.filters.FilterManager;
import org.apache.wiki.filters.PageFilter;
import org.apache.wiki.filters.ProfanityFilter;


public class FilterManagerTest extends TestCase
{
    Properties props = new Properties();

    private TestEngine m_engine = null;

    public FilterManagerTest( String s )
    {
        super( s );
    }

    public void setUp()
        throws Exception
    {
        props.load( TestEngine.findTestProperties() );
        m_engine = new TestEngine(props);
    }

    public void tearDown()
    {
        m_engine.shutdown();
    }

    @SuppressWarnings("deprecation")
    public void testInitFilters()
        throws Exception
    {
        FilterManager m = new FilterManager( m_engine, props );

        List<PageFilter> l = m.getFilterList();

        assertEquals("Wrong number of filters", 2, l.size());

        Iterator<PageFilter> i = l.iterator();
        PageFilter f1 = i.next();

        assertTrue("Not a Profanityfilter", f1 instanceof ProfanityFilter);

        PageFilter f2 = i.next();

        assertTrue("Not a Testfilter", f2 instanceof TestFilter);
    }

    public void testInitParams()
        throws Exception
    {
        FilterManager m = new FilterManager( m_engine, props );

        List<PageFilter> l = m.getFilterList();

        Iterator<PageFilter> i = l.iterator();
        i.next();
        TestFilter f2 = (TestFilter)i.next();

        Properties p = f2.m_properties;

        assertEquals("no foobar", "Zippadippadai", p.getProperty("foobar"));

        assertEquals("no blatblaa", "5", p.getProperty( "blatblaa" ) );
    }

    public static Test suite()
    {
        return new TestSuite( FilterManagerTest.class );
    }

}
