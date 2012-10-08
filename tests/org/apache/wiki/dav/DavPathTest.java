/* 
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
/*
 * (C) Janne Jalkanen 2005
 * 
 */
package org.apache.wiki.dav;

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
        
        assertEquals( "path", "", dp.pathPart() );
        assertEquals( "file", "", dp.filePart() );        
    }

    public void testCreate5()
    {
        String src = "/foo//bar///goo";
        
        DavPath dp = new DavPath( src );
        
        assertEquals( "path", "/foo/bar/", dp.pathPart() );
        assertEquals( "file", "goo", dp.filePart() );        
    }
    
    public void testSubPath()
    {
        String src = "/foo/bar/goo/blot";
        
        DavPath dp = new DavPath( src );
        
        DavPath subdp = dp.subPath( 2 );
        
        assertEquals( "goo/blot", subdp.getPath() );
    }

    public void testSubPath2()
    {
        String src = "/foo/bar/goo/blot";
        
        DavPath dp = new DavPath( src );
        
        DavPath subdp = dp.subPath( 0 );
        
        assertEquals( "/foo/bar/goo/blot", subdp.getPath() );
    }

    public void testSubPath3()
    {
        String src = "/foo/bar/goo/blot";
        
        DavPath dp = new DavPath( src );
        
        DavPath subdp = dp.subPath( 3 );
        
        assertEquals( "blot", subdp.getPath() );
    }

    public void testGetPath()
    {
        String src = "/foo/bar/goo/blot";
        
        DavPath dp = new DavPath( src );
                
        assertEquals( "/foo/bar/goo/blot", dp.getPath() );
    }

    public void testRoot1()
    {
        String src = "";
        
        DavPath dp = new DavPath( src );
                
        assertTrue( dp.isRoot() );
    }

    public void testRoot2()
    {
        String src = "/";
        
        DavPath dp = new DavPath( src );
                
        assertTrue( dp.isRoot() );
    }

    public void testRoot3()
    {
        String src = "foo";
        
        DavPath dp = new DavPath( src );
                
        assertFalse( dp.isRoot() );
    }

    public void testRoot4()
    {
        String src = "/foo";
        
        DavPath dp = new DavPath( src );
                
        assertFalse( dp.isRoot() );
    }
    
    public void testAppend1()
    {
        DavPath dp = new DavPath("/foo/bar/");
        
        dp.append("/zorp");
        
        assertEquals( "/foo/bar/zorp", dp.getPath() );
    }

    public void testAppend2()
    {
        DavPath dp = new DavPath("/foo/bar/");
        
        dp.append("zorp/grub/");
        
        assertEquals( "/foo/bar/zorp/grub/", dp.getPath() );
    }

    
    public static Test suite()
    {
        return new TestSuite( DavPathTest.class );
    }

}
