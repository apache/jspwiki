
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

        engine = new TestEngine(props);
        context = new WikiContext( engine, "testpage" );
        manager = new PluginManager( props );
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

    public void testSimpleInsertNoPackage()
        throws Exception
    {
        String res = manager.execute( context,
                                      "{INSERT SamplePlugin WHERE text=foobar}");

        assertEquals( "foobar",
                      res );
    }


    public void testSimpleInsertNoPackage2()
        throws Exception
    {
        props.setProperty( PluginManager.PROP_SEARCHPATH, "com.foo" );
        PluginManager m = new PluginManager( props );
        String res = m.execute( context,
                                "{INSERT SamplePlugin2 WHERE text=foobar}");

        assertEquals( "foobar",
                      res );
    }

    public void testSimpleInsertNoPackage3()
        throws Exception
    {
        props.setProperty( PluginManager.PROP_SEARCHPATH, "com.foo" );
        PluginManager m = new PluginManager( props );
        String res = m.execute( context,
                                "{INSERT SamplePlugin3 WHERE text=foobar}");

        assertEquals( "foobar",
                      res );
    }

    /** Check that in all cases com.ecyrd.jspwiki.plugin is searched. */
    public void testSimpleInsertNoPackage4()
        throws Exception
    {
        props.setProperty( PluginManager.PROP_SEARCHPATH, "com.foo,blat.blaa" );
        PluginManager m = new PluginManager( props );
        String res = m.execute( context,
                                "{INSERT SamplePlugin WHERE text=foobar}");

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

    public void testQuotedArgs()
        throws Exception
    {
        String res = manager.execute( context,
                                      "{INSERT SamplePlugin WHERE text='this is a space'}");

        assertEquals( "this is a space",
                      res );
    }

    public void testNumberArgs()
        throws Exception
    {
        String res = manager.execute( context,
                                      "{INSERT SamplePlugin WHERE text=15}");

        assertEquals( "15",
                      res );
    }

    

    public static Test suite()
    {
        return new TestSuite( PluginManagerTest.class );
    }
}
