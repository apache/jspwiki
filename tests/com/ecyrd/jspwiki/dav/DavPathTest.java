/*
 * (C) Janne Jalkanen 2005
 * 
 */
package com.ecyrd.jspwiki.dav;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class DavPathTest extends TestCase
{
    public void testCreate1()
    {
        String src = "/";
        
        DavPath dp = new DavPath( src );
        
        assertEquals( "path", "/", dp.pathPart() );
        assertEquals( "file", "", dp.filePart() );
    }

    public void testCreate2()
    {
        String src = "/test/foo/bar.txt";
        
        DavPath dp = new DavPath( src );
        
        assertEquals( "path", "/test/foo/", dp.pathPart() );
        assertEquals( "file", "bar.txt", dp.filePart() );        
    }

    public void testCreate3()
    {
        String src = "/test/foo/";
        
        DavPath dp = new DavPath( src );
        
        assertEquals( "path", "/test/foo/", dp.pathPart() );
        assertEquals( "file", "", dp.filePart() );        
    }

    public void testCreate4()
    {
        String src = "";
        
        DavPath dp = new DavPath( src );
        
        assertEquals( "path", "/", dp.pathPart() );
        assertEquals( "file", "", dp.filePart() );        
    }

    public static Test suite()
    {
        return new TestSuite( DavPathTest.class );
    }

}
