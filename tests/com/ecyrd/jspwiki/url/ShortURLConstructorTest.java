/*
 * (C) Janne Jalkanen 2005
 * 
 */
package com.ecyrd.jspwiki.url;

import java.util.Properties;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.ecyrd.jspwiki.TestEngine;
import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiException;

public class ShortURLConstructorTest extends TestCase
{
    TestEngine testEngine;

    Properties props = new Properties();
    
    protected void setUp() throws Exception
    {
        props.load( TestEngine.findTestProperties() );
    }

    private URLConstructor getConstructor( String baseURL, String prefix )
        throws WikiException
    {
        props.setProperty( WikiEngine.PROP_BASEURL, baseURL );
        if( prefix != null ) props.setProperty( ShortURLConstructor.PROP_PREFIX, prefix );
        
        testEngine = new TestEngine(props);
        URLConstructor constr = new ShortURLConstructor();
        
        constr.initialize( testEngine, props );
        
        return constr;
    }
    
    public void testViewURL1()
        throws Exception
    {
        URLConstructor c = getConstructor( "http://localhost/", "wiki/" );
        
        assertEquals( "http://localhost/wiki/Main", c.makeURL(WikiContext.VIEW,"Main",true,null) );
    }

    public void testViewURL2()
       throws Exception
    {
        URLConstructor c = getConstructor( "http://localhost/mywiki/", null );
    
        assertEquals( "http://localhost/mywiki/wiki/Main", c.makeURL(WikiContext.VIEW,"Main",true,null) );
    }

    public void testViewURL3()
       throws Exception
    { 
        URLConstructor c = getConstructor( "http://localhost:8080/", null );
 
        assertEquals( "http://localhost:8080/wiki/Main", c.makeURL(WikiContext.VIEW,"Main",true,null) );
    }

    public void testViewURL4()
        throws Exception
    {
        URLConstructor c = getConstructor( "http://localhost/mywiki/", null );
 
        assertEquals( "/mywiki/wiki/Main", c.makeURL(WikiContext.VIEW,"Main",false,null) );
    }

    public void testViewURL5()
        throws Exception
    {
        URLConstructor c = getConstructor( "http://localhost/", "" );
 
        assertEquals( "http://localhost/Main", c.makeURL(WikiContext.VIEW,"Main",true,null) );
    }
    
    public void testViewURL6()
       throws Exception
    {
        URLConstructor c = getConstructor( "http://localhost/mywiki/app1/", null );
 
        assertEquals( "http://localhost/mywiki/app1/wiki/Main", c.makeURL(WikiContext.VIEW,"Main",true,null) );
    }

    public void testViewURL7()
       throws Exception
    {
        URLConstructor c = getConstructor( "http://localhost/mywiki/app1/", "view/" );

        assertEquals( "http://localhost/mywiki/app1/view/Main", c.makeURL(WikiContext.VIEW,"Main",true,null) );
    }

    public void testEditURL1()
        throws Exception
    {
        URLConstructor c = getConstructor( "http://localhost/mywiki/", null );
 
        assertEquals( "http://localhost/mywiki/wiki/Main?do=Edit", c.makeURL(WikiContext.EDIT,"Main",true,null) );
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
        return new TestSuite( ShortURLConstructorTest.class );
    }
}

