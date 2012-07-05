package org.apache.wiki.plugin;

import java.util.Properties;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.sourceforge.stripes.mock.MockHttpServletRequest;

import org.apache.wiki.*;
import org.apache.wiki.auth.Users;
import org.apache.wiki.providers.WikiPageProvider;

public class IfPluginTest extends TestCase
{
    
    TestEngine testEngine;
    
    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception
    {
        super.setUp();
        
        Properties props = new Properties();
        props.load( TestEngine.findTestProperties() );
        testEngine = new TestEngine( props );
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception
    {
        super.tearDown();
        
        testEngine.deletePage( "Test" );
    }
    
    /**
     * Returns a {@link WikiContext} for the given page, with user {@link Users#JANNE} logged in.
     * 
     * @param page given {@link WikiPage}.
     * @return {@link WikiContext} associated to given {@link WikiPage}.
     * @throws WikiException problems while logging in.
     */
    WikiContext getJanneBasedWikiContextFor( WikiPage page ) throws WikiException 
    {
        MockHttpServletRequest request = testEngine.newHttpRequest();
        WikiSession session =  WikiSession.getWikiSession( testEngine, request );
        testEngine.getAuthenticationManager().login( session, 
                                                     request,
                                                     Users.JANNE,
                                                     Users.JANNE_PASS );
        
        return new WikiContext( testEngine, request, page );
    }
    
    /**
     * Checks that user access is granted.
     * 
     * @throws WikiException test failing.
     */
    public void testIfPluginUserAllowed() throws WikiException 
    {
        String src = "[{IfPlugin user='Janne Jalkanen'\n" +
        		     "\n" +
        		     "Content visible for Janne Jalkanen}]";
        String expected = "<p>Content visible for Janne Jalkanen</p>\n";
        
        testEngine.saveText( "Test", src );
        WikiPage page = testEngine.getPage( "Test", WikiPageProvider.LATEST_VERSION );
        WikiContext context = getJanneBasedWikiContextFor( page );
        
        String res = testEngine.getHTML( context, page );
        assertEquals( expected, res );
    }
    
    /**
     * Checks that user access is forbidden.
     * 
     * @throws WikiException test failing.
     */
    public void testIfPluginUserNotAllowed() throws WikiException 
    {
        String src = "[{IfPlugin user='!Janne Jalkanen'\n" +
                     "\n" +
                     "Content NOT visible for Janne Jalkanen}]";
        String expected = "\n";
        
        testEngine.saveText( "Test", src );
        WikiPage page = testEngine.getPage( "Test", WikiPageProvider.LATEST_VERSION );
        WikiContext context = getJanneBasedWikiContextFor( page );
        
        String res = testEngine.getHTML( context, page );
        assertEquals( expected, res );
    }
    
    /**
     * Checks that IP address is granted.
     * 
     * @throws WikiException test failing.
     */
    public void testIfPluginIPAllowed() throws WikiException 
    {
        String src = "[{IfPlugin ip='127.0.0.1'\n" +
                     "\n" +
                     "Content visible for 127.0.0.1}]";
        String expected = "<p>Content visible for 127.0.0.1</p>\n";
        
        testEngine.saveText( "Test", src );
        WikiPage page = testEngine.getPage( "Test", WikiPageProvider.LATEST_VERSION );
        WikiContext context = getJanneBasedWikiContextFor( page );
        
        String res = testEngine.getHTML( context, page );
        assertEquals( expected, res );
    }
    
    /**
     * Checks that IP address is granted.
     * 
     * @throws WikiException test failing.
     */
    public void testIfPluginIPNotAllowed() throws WikiException 
    {
        String src = "[{IfPlugin ip='!127.0.0.1'\n" +
                     "\n" +
                     "Content NOT visible for 127.0.0.1}]";
        String expected = "\n";
        
        testEngine.saveText( "Test", src );
        WikiPage page = testEngine.getPage( "Test", WikiPageProvider.LATEST_VERSION );
        WikiContext context = getJanneBasedWikiContextFor( page );
        
        String res = testEngine.getHTML( context, page );
        assertEquals( expected, res );
    }
    
    public static Test suite()
    {
        return new TestSuite( IfPluginTest.class );
    }
    
}
