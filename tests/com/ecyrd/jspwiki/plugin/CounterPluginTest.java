
package com.ecyrd.jspwiki.plugin;

import com.ecyrd.jspwiki.*;
import junit.framework.*;
import java.io.*;
import java.util.*;
import javax.servlet.ServletException;

public class CounterPluginTest extends TestCase
{
    Properties props = new Properties();
    TestEngine testEngine;
    WikiContext context;
    PluginManager manager;
    
    public CounterPluginTest( String s )
    {
        super( s );
    }

    public void setUp()
        throws Exception
    {
        props.load( getClass().getClassLoader().getResourceAsStream("/jspwiki.properties") );

        testEngine = new TestEngine(props);
    }

    public void tearDown()
    {
    }

    private String translate( String src )
        throws IOException,
               NoRequiredPropertyException,
               ServletException
    {
        WikiContext context = new WikiContext( testEngine,
                                               "TestPage" );
        Reader r = new TranslatorReader( context, 
                                         new BufferedReader( new StringReader(src)) );
        StringWriter out = new StringWriter();
        int c;

        while( ( c=r.read()) != -1 )
        {
            out.write( c );
        }

        return out.toString();
    }

    public void testSimpleCount()
        throws Exception
    {
        String src = "[{Counter}], [{Counter}]";

        assertEquals( "1, 2\n",
                      translate(src) );
    }

    public void testSimpleVar()
        throws Exception
    {
        String src = "[{Counter}], [{Counter}], [{$counter}]";

        assertEquals( "1, 2, 2\n",
                      translate(src) );
    }

    public void testTwinVar()
        throws Exception
    {
        String src = "[{Counter}], [{Counter name=aa}], [{$counter-aa}]";

        assertEquals( "1, 1, 1\n",
                      translate(src) );
    }

    public static Test suite()
    {
        return new TestSuite( ReferringPagesPluginTest.class );
    }
}
