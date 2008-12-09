package com.ecyrd.jspwiki.content;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class WikiNameTest extends TestCase
{
    public void testParse1()
    {
        WikiName wn = WikiName.valueOf( "Foo:Bar/Blob 2" );
        
        assertEquals("space", "Foo", wn.getSpace() );
        assertEquals("path", "Bar/Blob 2", wn.getPath() );
    }

    public void testParse2()
    {
        WikiName wn = WikiName.valueOf( "BarBrian" );
        
        assertEquals("space", ContentManager.DEFAULT_SPACE, wn.getSpace() );
        assertEquals("path", "BarBrian", wn.getPath() );
    }

    public void testResolve1()
    {
        WikiName wn = new WikiName("Test","TestPage");
        
        WikiName newname = wn.resolve("Barbapapa");
        
        assertEquals( "Test:Barbapapa", newname.toString() );
    }

    public void testResolveAbsolute()
    {
        WikiName wn = new WikiName("Test","TestPage");
        
        WikiName newname = wn.resolve("Foo:Barbapapa");
        
        assertEquals( "Foo:Barbapapa", newname.toString() );
    }

    public static Test suite()
    {
        return new TestSuite(WikiNameTest.class);
    }

}
