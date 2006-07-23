/*
 * (C) Janne Jalkanen 2005
 * 
 */
package com.ecyrd.jspwiki.ui;

import java.util.Properties;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.ecyrd.jspwiki.TestEngine;

public class RedirectCommandTest extends TestCase
{
    TestEngine     testEngine;

    protected void setUp() throws Exception
    {
        Properties props = new Properties();
        props.load( TestEngine.findTestProperties() );
        testEngine = new TestEngine( props );
    }
    
    protected void tearDown() throws Exception
    {
    }

    public void testStaticCommand()
    {
        Command a = RedirectCommand.REDIRECT;
        assertEquals( "", a.getRequestContext() );
        assertEquals( "", a.getJSP() );
        assertEquals( "%u%n", a.getURLPattern() );
        assertNull( a.getContentTemplate() );
        assertNull( a.getTarget() );
        assertEquals( a, RedirectCommand.REDIRECT );
    }
    
    public void testTargetedCommand()
    {
        Command a = RedirectCommand.REDIRECT;
        
        // Test with local JSP
        Command b = a.targetedCommand( "%uTestPage.jsp" );
        assertEquals( "", b.getRequestContext() );
        assertEquals( "TestPage.jsp", b.getJSP() );
        assertEquals( "%uTestPage.jsp", b.getURLPattern() );
        assertNull( b.getContentTemplate() );
        assertEquals( "%uTestPage.jsp", b.getTarget() );
        assertNotSame( RedirectCommand.REDIRECT, b );
        
        // Test with non-local URL
        b = a.targetedCommand( "http://www.yahoo.com" );
        assertEquals( "", b.getRequestContext() );
        assertEquals( "http://www.yahoo.com", b.getJSP() );
        assertEquals( "http://www.yahoo.com", b.getURLPattern() );
        assertNull( b.getContentTemplate() );
        assertEquals( "http://www.yahoo.com", b.getTarget() );
        assertNotSame( RedirectCommand.REDIRECT, b );
    }
    
    public static Test suite()
    {
        return new TestSuite( RedirectCommandTest.class );
    }
}
