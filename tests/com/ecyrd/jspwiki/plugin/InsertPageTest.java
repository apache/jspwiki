package com.ecyrd.jspwiki.plugin;

import java.util.Properties;

import com.ecyrd.jspwiki.TestEngine;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class InsertPageTest extends TestCase
{
    protected TestEngine testEngine;
    protected Properties props = new Properties();
    
    protected void setUp() throws Exception
    {
        props.load( TestEngine.findTestProperties() );

        testEngine = new TestEngine(props);
    }

    protected void tearDown() throws Exception
    {
        TestEngine.deleteTestPage( "ThisPage" );
        TestEngine.deleteTestPage( "ThisPage2" );
        TestEngine.deleteTestPage( "Test_Page" );
        TestEngine.deleteTestPage( "TestPage" );
        TestEngine.deleteTestPage( "Test Page" );
    }

    public void testRecursive() throws Exception
    {
        String src = "[{InsertPage page='ThisPage'}] [{ALLOW view Anonymous}]";
        
        testEngine.saveText("ThisPage",src);
        
        // Just check that it contains a proper error message; don't bother do HTML
        // checking.
        String res = testEngine.getHTML("ThisPage");
        assertTrue( res.indexOf("Circular reference") != -1 );
    }

    public void testRecursive2() throws Exception
    {
        String src  = "[{InsertPage page='ThisPage2'}]";
        String src2 = "[{InsertPage page='ThisPage'}]";
        
        testEngine.saveText("ThisPage",src);
        testEngine.saveText("ThisPage2",src2);
               
        // Just check that it contains a proper error message; don't bother do HTML
        // checking.
        assertTrue( testEngine.getHTML("ThisPage").indexOf("Circular reference") != -1 );
    }

    public void testMultiInvocation() throws Exception
    {
        String src  = "[{InsertPage page='ThisPage2'}] [{InsertPage page='ThisPage2'}]";
        String src2 = "foo[{ALLOW view Anonymous}]";

        testEngine.saveText("ThisPage",src);
        testEngine.saveText("ThisPage2",src2);

        assertTrue( "got circ ref", testEngine.getHTML("ThisPage").indexOf("Circular reference") == -1 );
        
        assertEquals( "found != 2", "<div style=\"\">foo\n</div> <div style=\"\">foo\n</div>\n", testEngine.getHTML("ThisPage") );
        
    }
    
    public void testUnderscore() throws Exception
    {
        String src  = "[{InsertPage page='Test_Page'}]";
        String src2 = "foo[{ALLOW view Anonymous}]";

        testEngine.saveText("ThisPage",src);
        testEngine.saveText("Test_Page",src2);

        assertTrue( "got circ ref", testEngine.getHTML("ThisPage").indexOf("Circular reference") == -1 );
        
        assertEquals( "found != 1", "<div style=\"\">foo\n</div>\n", testEngine.getHTML("ThisPage") );    
    }
    
    
    /**
     * a link containing a blank should work if there is a page with exact the
     * same name ('Test Page')
     */
    public void testWithBlanks1() throws Exception
    {
        testEngine.saveText( "ThisPage", "[{InsertPage page='Test Page'}]" );
        testEngine.saveText( "Test Page", "foo[{ALLOW view Anonymous}]" );

        assertEquals( "found != 1", "<div style=\"\">foo\n</div>\n", testEngine.getHTML( "ThisPage" ) );
    }

    /**
     * same as testWithBlanks1, but it should still work if the page does not
     * have the blank in it ( 'Test Page' should work if the included page is
     * called 'TestPage')
     */
    public void testWithBlanks2() throws Exception
    {
        testEngine.saveText( "ThisPage", "[{InsertPage page='Test Page'}]" );
        testEngine.saveText( "TestPage", "foo[{ALLOW view Anonymous}]" );

        assertEquals( "found != 1", "<div style=\"\">foo\n</div>\n", testEngine.getHTML( "ThisPage" ) );
    }
    
    public static Test suite()
    {
        return new TestSuite( InsertPageTest.class );
    }
}
