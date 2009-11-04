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

import java.util.HashMap;
import java.util.Map;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.sourceforge.stripes.controller.DispatcherServlet;
import net.sourceforge.stripes.controller.StripesFilter;
import net.sourceforge.stripes.mock.MockRoundtrip;
import net.sourceforge.stripes.mock.MockServletContext;

import org.apache.wiki.WikiEngine;
import org.apache.wiki.action.ViewActionBean;


public class ShortUrlRedirectFilterTest extends TestCase
{
    private MockServletContext m_servletContext = null;

    public void setUp()
    {
        // Configure the filter and servlet
        MockServletContext servletContext = new MockServletContext( "test" );
        servletContext.setServlet(DispatcherServlet.class, "StripesDispatcher", null);
        
        // Add extension classes
        Map<String,String> filterParams = new HashMap<String,String>();
        filterParams.put("ActionResolver.Packages", "org.apache.wiki.action");
        filterParams.put("Extension.Packages", "org.apache.wiki.ui.stripes");
        filterParams.put( "ExceptionHandler.Class", "org.apache.wiki.ui.stripes.WikiExceptionHandler" );
        servletContext.addFilter(StripesFilter.class, "StripesFilter", filterParams);
        servletContext.addFilter( ShortUrlRedirectFilter.class, "Redirect filter", null );

        // Set the configured servlet context
        m_servletContext = servletContext;
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
        WikiEngine engine = WikiEngine.getInstance( m_servletContext, null );
        engine.shutdown();
    }
    
    public void testRedirectEdit() throws Exception
    {
        // Inverse test for ShortURLConstructorTest#testEditURL1
        MockRoundtrip trip = new MockRoundtrip( m_servletContext, "/wiki/Foo?do=Edit" );
        trip.execute();
        ViewActionBean bean = trip.getActionBean( ViewActionBean.class );
        assertNull( bean );
        assertEquals( "/Edit.action?edit=&page=Foo", trip.getDestination() );
    }
    
    public void testRedirectView() throws Exception
    {
        // Inverse test for ShortURLConstructorTest#testViewURL1
        MockRoundtrip trip = new MockRoundtrip( m_servletContext, "/wiki/Foo" );
        trip.execute();
        ViewActionBean bean = trip.getActionBean( ViewActionBean.class );
        assertNull( bean );
        assertEquals( "/Wiki.action?view=&page=Foo", trip.getDestination() );
    }

    public void testRedirectViewNoPage() throws Exception
    {
        // Inverse test for ShortURLConstructorTest#testViewURL4
        MockRoundtrip trip = new MockRoundtrip( m_servletContext, "/wiki" );
        trip.execute();
        ViewActionBean bean = trip.getActionBean( ViewActionBean.class );
        assertNull( bean );
        assertEquals( "/Wiki.action", trip.getDestination() );
    }

    public static Test suite()
    {
        return new TestSuite( ShortUrlRedirectFilterTest.class );
    }
}
