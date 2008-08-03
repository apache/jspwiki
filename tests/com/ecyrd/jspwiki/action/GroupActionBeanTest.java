package com.ecyrd.jspwiki.action;

import java.lang.reflect.Method;
import java.security.Permission;
import java.util.Properties;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.sourceforge.stripes.mock.MockRoundtrip;
import net.sourceforge.stripes.mock.MockServletContext;
import net.sourceforge.stripes.util.UrlBuilder;

import com.ecyrd.jspwiki.TestEngine;
import com.ecyrd.jspwiki.auth.permissions.GroupPermission;

public class GroupActionBeanTest extends TestCase
{
    TestEngine m_engine;

    public void setUp()
    {
        // Start the WikiEngine, and stash reference
        Properties props = new Properties();
        try
        {
            props.load( TestEngine.findTestProperties() );
            m_engine = new TestEngine( props );
        }
        catch( Exception e )
        {
            throw new RuntimeException( "Could not set up TestEngine: " + e.getMessage() );
        }
    }
    
    public void testURLBuild() throws Exception
    {
        UrlBuilder builder = new UrlBuilder( null, GroupActionBean.class, false );
        builder.addParameter( "group", m_engine.getGroupManager().getGroup( "Admin" ).getName() );
        builder.addParameter( "foo", "bar" );
        String url = builder.toString();
        assertEquals( "/Group.jsp?group=Admin&foo=bar", url );
    }
    
    public void testURLParse() throws Exception
    {
        MockServletContext ctx = (MockServletContext) m_engine.getServletContext();
        MockRoundtrip trip;
        GroupActionBean bean;

        // Set up a new GroupActionBean with group Admin and event
        // "view"
        trip = new MockRoundtrip( ctx, "/Group.jsp/group=Admin&foo=bar" );
        trip.getRequest().setMethod( "GET" );
        trip.execute( "view" );
        bean = trip.getActionBean( GroupActionBean.class );
        assertNotNull( bean );
    }

    public static Test suite()
    {
        return new TestSuite( GroupActionBeanTest.class );
    }
}
