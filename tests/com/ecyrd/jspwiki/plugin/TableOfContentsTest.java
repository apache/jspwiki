/*
 * (C) Janne Jalkanen 2005
 * 
 */
package com.ecyrd.jspwiki.plugin;

import java.util.Properties;

import com.ecyrd.jspwiki.TestEngine;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 *  @author jalkanen
 *
 *  @since 
 */
public class TableOfContentsTest extends TestCase
{
    TestEngine testEngine;
    
    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception
    {
        super.setUp();
        
        Properties props = new Properties();
        
        props.load(TestEngine.findTestProperties());
        
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

    public void testHeadingVariables()
        throws Exception
    {
        String src="[{SET foo=bar}]\n\n[{TableOfContents}]\n\n!!!Heading [{$foo}]";
        
        testEngine.saveText( "Test", src );
        
        String res = testEngine.getHTML( "Test" );
        
        // FIXME: There's an extra space before the <a>...  Where does it come from?
        // FIXME: The <p> should not be here.
        assertEquals( "\n<p><div class=\"toc\">\n"+
                      "<h4>Table of Contents</h4>\n"+
                      "<ul>"+
                      "<li> <a class=\"wikipage\" href=\"/Wiki.jsp?page=Test#section-Test-HeadingBar\">Heading bar</a>\n</li>"+
                      "</ul></div>\n\n</p>"+
                      "\n<h2 id=\"section-Test-HeadingBar\">Heading bar</h2>\n",
                      res );
    }

    public static Test suite()
    {
        return new TestSuite( TableOfContentsTest.class );
    }
    
}
