
package com.ecyrd.jspwiki.plugin;

import com.ecyrd.jspwiki.*;
import junit.framework.*;
import java.io.*;
import java.util.*;

public class PluginManagerTest extends TestCase
{
    public static final String NAME1 = "Test1";

    Properties props = new Properties();

    WikiEngine engine;

    WikiContext context;

    PluginManager manager;
    
    public PluginManagerTest( String s )
    {
        super( s );
    }

    public void setUp()
        throws Exception
    {
        props.load( getClass().getClassLoader().getResourceAsStream("/jspwiki.properties") );

        engine = new TestEngine2(props);
        context = new WikiContext( engine, "testpage" );
        manager = new PluginManager();
    }

    public void tearDown()
    {
    }

    public void testSimpleInsert()
        throws Exception
    {
        String res = manager.execute( context,
                                      "{INSERT com.ecyrd.jspwiki.plugin.SamplePlugin WHERE text=foobar}");

        assertEquals( "foobar",
                      res );
    }

    public void testSimpleInsert2()
        throws Exception
    {
        String res = manager.execute( context,
                                      "{INSERT   com.ecyrd.jspwiki.plugin.SamplePlugin  WHERE   text = foobar2, moo=blat}");

        assertEquals( "foobar2",
                      res );
    }

    /** Missing closing brace */
    public void testSimpleInsert3()
        throws Exception
    {
        String res = manager.execute( context,
                                      "{INSERT   com.ecyrd.jspwiki.plugin.SamplePlugin  WHERE   text = foobar2, moo=blat");

        assertEquals( "foobar2",
                      res );
    }

    public static Test suite()
    {
        return new TestSuite( PluginManagerTest.class );
    }
}
