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
package org.apache.wiki.url;

import java.util.Properties;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.api.exceptions.WikiException;

public class ShortURLConstructorTest extends TestCase
{
    TestEngine testEngine;

    Properties props = TestEngine.getTestProperties();
    
    private URLConstructor getConstructor(String prefix)
        throws WikiException
    {
        if( prefix != null ) props.setProperty( ShortURLConstructor.PROP_PREFIX, prefix );
        
        testEngine = new TestEngine(props);
        URLConstructor constr = new ShortURLConstructor();
        
        constr.initialize( testEngine, props );
        
        return constr;
    }
    
    public void testViewURL1()
        throws Exception
    {
        URLConstructor c = getConstructor("wiki/" );
        
        assertEquals( "/test/wiki/Main", c.makeURL(WikiContext.VIEW,"Main",true,null) );
    }

    public void testViewURL2()
       throws Exception
    {
        URLConstructor c = getConstructor(null );
    
        assertEquals( "/test/wiki/Main", c.makeURL(WikiContext.VIEW,"Main",true,null) );
    }

    public void testViewURL3()
       throws Exception
    { 
        URLConstructor c = getConstructor(null );
 
        assertEquals( "/test/wiki/Main", c.makeURL(WikiContext.VIEW,"Main",true,null) );
    }

    public void testViewURL4()
        throws Exception
    {
        URLConstructor c = getConstructor(null );
 
        assertEquals( "/test/wiki/Main", c.makeURL(WikiContext.VIEW,"Main",false,null) );
    }

    public void testViewURL5()
        throws Exception
    {
        URLConstructor c = getConstructor("" );
 
        assertEquals( "/test/Main", c.makeURL(WikiContext.VIEW,"Main",true,null) );
    }
    
    public void testViewURL6()
       throws Exception
    {
        URLConstructor c = getConstructor(null );
 
        assertEquals( "/test/wiki/Main", c.makeURL(WikiContext.VIEW,"Main",true,null) );
    }

    public void testViewURL7()
       throws Exception
    {
        URLConstructor c = getConstructor("view/" );

        assertEquals( "/test/view/Main", c.makeURL(WikiContext.VIEW,"Main",true,null) );
    }

    public void testEditURL1()
        throws Exception
    {
        URLConstructor c = getConstructor(null );
 
        assertEquals( "/test/wiki/Main?do=Edit", c.makeURL(WikiContext.EDIT,"Main",true,null) );
    }

    public void testAttachURL1()
        throws Exception
    {
        URLConstructor c = getConstructor(null );

        assertEquals( "/test/attach/Main/foo.txt", c.makeURL(WikiContext.ATTACH,"Main/foo.txt",true,null) );
    }

    public void testAttachURLRelative1()
        throws Exception
    {
        URLConstructor c = getConstructor(null );

        assertEquals( "/test/attach/Main/foo.txt", c.makeURL(WikiContext.ATTACH,"Main/foo.txt",false,null) );
    }

    public void testOtherURL1()
        throws Exception
    {
        URLConstructor c = getConstructor(null );

        assertEquals( "/test/foo.jsp", c.makeURL(WikiContext.NONE,"foo.jsp",true,null) );
    }
    
    public void testOtherURL2()
        throws Exception
    {
        URLConstructor c = getConstructor(null );
    
        assertEquals( "/test/foo.jsp?a=1&amp;b=2", c.makeURL(WikiContext.NONE,"foo.jsp",true,"a=1&amp;b=2") );
    }

    public static Test suite()
    {
        return new TestSuite( ShortURLConstructorTest.class );
    }
}

