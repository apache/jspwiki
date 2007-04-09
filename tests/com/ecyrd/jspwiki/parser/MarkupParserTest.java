package com.ecyrd.jspwiki.parser;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class MarkupParserTest extends TestCase
{

    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(MarkupParserTest.class);
    }

    protected void setUp() throws Exception
    {
        super.setUp();
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

    public void testCleanLink1()
    {
        assertEquals( "--CleanLink--", MarkupParser.cleanLink("--CleanLink--") );
    }

    public void testCleanLink2()
    {
        assertEquals( "CleanLink", MarkupParser.cleanLink("??CleanLink??") );
    }
    
    public void testCleanLink3()
    {
        assertEquals( "Clean (link)", MarkupParser.cleanLink("Clean (link)") );
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(MarkupParserTest.class);

        return suite;
    }
}
