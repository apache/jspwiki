
package com.ecyrd.jspwiki.filters;

import junit.framework.*;
import java.util.*;

import org.apache.log4j.*;

import com.ecyrd.jspwiki.*;

public class FilterManagerTest extends TestCase
{
    Properties props = new Properties();

    TestEngine engine;

    public FilterManagerTest( String s )
    {
        super( s );
    }

    public void setUp()
        throws Exception
    {
        props.load( TestEngine.findTestProperties() );
        PropertyConfigurator.configure(props);
        engine = new TestEngine(props);
    }

    public void tearDown()
    {
    }

    public void testInitFilters()
        throws Exception
    {
        FilterManager m = new FilterManager( engine, props );

        List l = m.getFilterList();

        assertEquals("Wrong number of filters", 2, l.size());

        Iterator i = l.iterator();
        PageFilter f1 = (PageFilter)i.next();

        assertTrue("Not a Profanityfilter", f1 instanceof ProfanityFilter);

        PageFilter f2 = (PageFilter)i.next();

        assertTrue("Not a Testfilter", f2 instanceof TestFilter);
    }

    public void testInitParams()
        throws Exception
    {
        FilterManager m = new FilterManager( engine, props );

        List l = m.getFilterList();

        Iterator i = l.iterator();
        PageFilter f1 = (PageFilter)i.next();
        TestFilter f2 = (TestFilter)i.next();

        Properties p = f2.m_properties;

        assertEquals("no foobar", "Zippadippadai", p.getProperty("foobar"));

        assertEquals("no blatblaa", "5", p.getProperty( "blatblaa" ) );
    }

    public static Test suite()
    {
        return new TestSuite( FilterManagerTest.class );
    }

}
