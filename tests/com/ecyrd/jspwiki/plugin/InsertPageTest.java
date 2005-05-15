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
    }

    public void testRecursive() throws Exception
    {
        String src = "[{InsertPage page='ThisPage'}]";
        
        testEngine.saveText("ThisPage",src);
        
        // Just check that it contains a proper error message; don't bother do HTML
        // checking.
        assertTrue( testEngine.getHTML("ThisPage").indexOf("Circular reference") != -1 );
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

    public static Test suite()
    {
        return new TestSuite( InsertPageTest.class );
    }
}
