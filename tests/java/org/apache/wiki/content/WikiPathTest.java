/*
    JSPWiki - a JSP-based WikiWiki clone.

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
package org.apache.wiki.content;

import org.apache.wiki.content.ContentManager;
import org.apache.wiki.content.WikiPath;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class WikiPathTest extends TestCase
{
    public void testParse1()
    {
        WikiPath path = WikiPath.valueOf( "Foo:Bar/Blob 2" );
        
        assertEquals("space", "Foo", path.getSpace() );
        assertEquals("path", "Bar/Blob 2", path.getPath() );
        assertEquals( "name", "Blob 2", path.getName() );
    }

    public void testParse2()
    {
        WikiPath path = WikiPath.valueOf( "BarBrian" );
        
        assertEquals("space", ContentManager.DEFAULT_SPACE, path.getSpace() );
        assertEquals("path", "BarBrian", path.getPath() );
        assertEquals( "name", "BarBrian", path.getName() );
    }
    
    public void testParseHierarchicalPath()
    {
        WikiPath path = WikiPath.valueOf( "Test/Foo/Bar" );
        
        assertEquals("space", ContentManager.DEFAULT_SPACE, path.getSpace() );
        assertEquals("path", "Test/Foo/Bar", path.getPath() );
        assertEquals( "name", "Bar", path.getName() );
    }
    
    public void testParseNoName()
    {
        WikiPath path = WikiPath.valueOf( "BarBrian/" );
        
        assertEquals("space", ContentManager.DEFAULT_SPACE, path.getSpace() );
        assertEquals("path", "BarBrian/", path.getPath() );
        assertEquals( "name", "", path.getName() );
    }

    public void testResolve1()
    {
        WikiPath wn = new WikiPath("Test","TestPage");
        
        WikiPath newname = wn.resolve("Barbapapa");
        
        assertEquals( "Test:Barbapapa", newname.toString() );
    }

    public void testResolveAbsolute()
    {
        WikiPath wn = new WikiPath("Test","TestPage");
        
        WikiPath newname = wn.resolve("Foo:Barbapapa");
        
        assertEquals( "Foo:Barbapapa", newname.toString() );
    }

    public static Test suite()
    {
        return new TestSuite(WikiPathTest.class);
    }

}
