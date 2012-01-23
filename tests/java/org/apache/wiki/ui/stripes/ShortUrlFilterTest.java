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
package org.apache.wiki.ui.stripes;

import java.util.Properties;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.sourceforge.stripes.mock.MockRoundtrip;

import org.apache.wiki.TestEngine;
import org.apache.wiki.action.ViewActionBean;

public class ShortUrlFilterTest extends TestCase
{
    private TestEngine m_engine = null;

    public void setUp() throws Exception
    {
        super.setUp();
        Properties props = new Properties();
        props.load( TestEngine.findTestProperties() );
        m_engine = new TestEngine( props );
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
        m_engine.shutdown();
    }

    public void testRedirectEdit() throws Exception
    {
        // Inverse test for ShortURLConstructorTest#testEditURL1
        MockRoundtrip trip = m_engine.guestTrip( "/wiki/Foo?do=Edit" );
        trip.execute();
        ViewActionBean bean = trip.getActionBean( ViewActionBean.class );
        assertNull( bean );
        assertEquals( "/Edit.jsp?edit=&page=Foo", trip.getDestination() );
    }

    public void testRedirectView() throws Exception
    {
        // Inverse test for ShortURLConstructorTest#testViewURL1
        MockRoundtrip trip = m_engine.guestTrip( "/wiki/Foo" );
        trip.execute();
        ViewActionBean bean = trip.getActionBean( ViewActionBean.class );
        assertNull( bean );
        assertEquals( "/Wiki.jsp?view=&page=Foo", trip.getDestination() );
    }

    public void testRedirectViewNoPage() throws Exception
    {
        // Inverse test for ShortURLConstructorTest#testViewURL4
        MockRoundtrip trip = m_engine.guestTrip( "/wiki" );
        trip.execute();
        ViewActionBean bean = trip.getActionBean( ViewActionBean.class );
        assertNull( bean );
        assertEquals( "/Wiki.jsp", trip.getDestination() );
    }

    public static Test suite()
    {
        return new TestSuite( ShortUrlFilterTest.class );
    }
}
