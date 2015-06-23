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

public class DefaultURLConstructorTest extends TestCase
{
    TestEngine testEngine;

    Properties props = TestEngine.getTestProperties();
    
    private URLConstructor getConstructor( String baseURL, String prefix )
        throws WikiException
    {
        props.setProperty( WikiEngine.PROP_BASEURL, baseURL );
        if( prefix != null ) props.setProperty( ShortViewURLConstructor.PROP_PREFIX, prefix );
        
        testEngine = new TestEngine(props);
        URLConstructor constr = new DefaultURLConstructor();
        
        constr.initialize( testEngine, props );
        
        return constr;
    }
    
    public void testViewURL1()
        throws Exception
    {
        URLConstructor c = getConstructor( "http://localhost/", "wiki/" );
        
        assertEquals( "http://localhost/Wiki.jsp?page=Main", c.makeURL(WikiContext.VIEW,"Main",true,null) );
    }

    public void testViewURL2()
       throws Exception
    {
        URLConstructor c = getConstructor( "http://localhost/mywiki/", null );
    
        assertEquals( "http://localhost/mywiki/Wiki.jsp?page=Main", c.makeURL(WikiContext.VIEW,"Main",true,null) );
    }

    public void testViewURL3()
       throws Exception
    { 
        URLConstructor c = getConstructor( "http://localhost:8080/", null );
 
        assertEquals( "http://localhost:8080/Wiki.jsp?page=Main", c.makeURL(WikiContext.VIEW,"Main",true,null) );
    }

    public void testViewURL4()
        throws Exception
    {
        URLConstructor c = getConstructor( "http://localhost/mywiki/", null );
 
        assertEquals( "/mywiki/Wiki.jsp?page=Main", c.makeURL(WikiContext.VIEW,"Main",false,null) );
    }

    public void testViewURL5()
        throws Exception
    {
        URLConstructor c = getConstructor( "http://localhost/", "" );
 
        assertEquals( "http://localhost/Wiki.jsp?page=Main", c.makeURL(WikiContext.VIEW,"Main",true,null) );
    }
    
    public void testViewURL6()
       throws Exception
    {
        URLConstructor c = getConstructor( "http://localhost/mywiki/app1/", null );
 
        assertEquals( "http://localhost/mywiki/app1/Wiki.jsp?page=Main", c.makeURL(WikiContext.VIEW,"Main",true,null) );
    }

    public void testViewURL7()
       throws Exception
    {
        URLConstructor c = getConstructor( "http://localhost/mywiki/app1/", "view/" );

        assertEquals( "http://localhost/mywiki/app1/Wiki.jsp?page=Main", c.makeURL(WikiContext.VIEW,"Main",true,null) );
    }

    public void testEditURL1()
        throws Exception
    {
        URLConstructor c = getConstructor( "http://localhost/mywiki/", null );
 
        assertEquals( "http://localhost/mywiki/Edit.jsp?page=Main", c.makeURL(WikiContext.EDIT,"Main",true,null) );
    }

    public void testAttachURL1()
        throws Exception
    {
        URLConstructor c = getConstructor( "http://localhost/mywiki/", null );

        assertEquals( "http://localhost/mywiki/attach/Main/foo.txt", c.makeURL(WikiContext.ATTACH,"Main/foo.txt",true,null) );
    }

    public void testAttachURLRelative1()
        throws Exception
    {
        URLConstructor c = getConstructor( "http://localhost/mywiki/", null );

        assertEquals( "/mywiki/attach/Main/foo.txt", c.makeURL(WikiContext.ATTACH,"Main/foo.txt",false,null) );
    }

    public void testOtherURL1()
        throws Exception
    {
        URLConstructor c = getConstructor( "http://localhost/mywiki/", null );

        assertEquals( "http://localhost/mywiki/foo.jsp", c.makeURL(WikiContext.NONE,"foo.jsp",true,null) );
    }
    
    public void testOtherURL2()
        throws Exception
    {
        URLConstructor c = getConstructor( "http://localhost/mywiki/dobble/", null );
    
        assertEquals( "http://localhost/mywiki/dobble/foo.jsp?a=1&amp;b=2", c.makeURL(WikiContext.NONE,"foo.jsp",true,"a=1&amp;b=2") );
    }

    public static Test suite()
    {
        return new TestSuite( DefaultURLConstructorTest.class );
    }
}
