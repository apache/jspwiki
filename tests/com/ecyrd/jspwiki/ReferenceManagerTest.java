
package com.ecyrd.jspwiki;

import junit.framework.*;
import java.util.*;

/**
 *  @author Torsten Hildebrandt.
 */
public class ReferenceManagerTest extends TestCase
{
    Properties props = new Properties();
    TestEngine engine;
    ReferenceManager mgr;
    
    public ReferenceManagerTest( String s )
    {
        super( s );
    }

    public void setUp()
        throws Exception
    {
        props.load( TestEngine.findTestProperties() );
        props.setProperty( "jspwiki.translatorReader.matchEnglishPlurals", "true");

        engine = new TestEngine(props);

        engine.saveText( "TestPage", "Reference to [Foobar]." );
        engine.saveText( "Foobar", "Reference to [Foobar2], [Foobars], [Foobar]" );

        mgr = engine.getReferenceManager();
    }

    public void tearDown()
    {
        engine.deletePage( "TestPage" );
        engine.deletePage( "Foobar" );
        engine.deletePage( "Foobars" );
        engine.deletePage( "Foobar2" );
        engine.deletePage( "Foobar2s" );
    }

    public void testUnreferenced()
        throws Exception
    {
        Collection c = mgr.findUnreferenced();
        
        assertTrue( c.size()==1 && ((String) c.iterator().next()).equals("TestPage") );
    }

    public void testBecomesUnreferenced()
        throws Exception
    {
        engine.saveText( "TestPage", "norefs" );

        Collection c = mgr.findUnreferenced();
        assertTrue( c.size()==2 );

        Iterator i = c.iterator();
        String first = (String) i.next();
        String second = (String) i.next();
        assertTrue( ( first.equals("Foobar") && second.equals("TestPage") )
            || ( first.equals("TestPage") && second.equals("Foobar") ));
    }

    public void testUncreated()
        throws Exception
    {
        Collection c = mgr.findUncreated();
        
        assertTrue( c.size()==1 && ((String) c.iterator().next()).equals("Foobar2") );
    }

    public void testReferrers()
        throws Exception
    {
        Collection c = mgr.findReferrers( "TestPage" );
        assertNull( "TestPage referrers", c );

        c = mgr.findReferrers( "Foobar" );
        assertTrue( "Foobar referrers", c.size()==1 && ((String) c.iterator().next()).equals("TestPage") );

        c = mgr.findReferrers( "Foobar2" );
        assertTrue( "Foobar2 referrers", c.size()==1 && ((String) c.iterator().next()).equals("Foobar") );

        c = mgr.findReferrers( "Foobars" );
        assertTrue( "Foobars referrers", c.size()==1 && ((String) c.iterator().next()).equals("Foobar") );
    }

    /** 
     *  Is a page recognized as referenced if only plural form links exist.
     */

    public void testUpdatePluralOnlyRef()
        throws Exception
    {
        engine.saveText( "TestPage", "Reference to [Foobars]." );
        Collection c = mgr.findUnreferenced();
        assertTrue( "Foobar unreferenced", c.size()==1 && ((String) c.iterator().next()).equals("TestPage") );

        c = mgr.findReferrers( "Foobar" );
        assertTrue( "Foobar referrers", c.size()==1 && ((String) c.iterator().next()).equals("TestPage") );
    }


    /** 
     *  Opposite to testUpdatePluralOnlyRef(). Is a page with plural form recognized as
     *  the page referenced by a singular link.
     */

    public void testUpdateFoobar2s()
        throws Exception
    {
        engine.saveText( "Foobar2s", "qwertz" );
        assertTrue( "no uncreated", mgr.findUncreated().size()==0 );

        Collection c = mgr.findReferrers( "Foobar2s" );
        assertTrue( "referrers", c!=null && c.size()==1 && ((String) c.iterator().next()).equals("Foobar") );
    }

    public void testUpdateBothExist()
        throws Exception
    {
        engine.saveText( "Foobars", "qwertz" );
        Collection c = mgr.findReferrers( "Foobars" );
        assertTrue( "Foobars referrers", c.size()==1 && ((String) c.iterator().next()).equals("Foobar") );
    }

    public void testUpdateBothExist2()
        throws Exception
    {
        engine.saveText( "Foobars", "qwertz" );
        engine.saveText( "TestPage", "Reference to [Foobar], [Foobars]." );
        
        Collection c = mgr.findReferrers( "Foobars" );
        assertEquals( "Foobars referrers count", c.size(), 2);

        Iterator i = c.iterator();
        String first = (String) i.next();
        String second = (String) i.next();
        assertTrue( "Foobars referrers", 
            ( first.equals("Foobar") && second.equals("TestPage") )
            || ( first.equals("TestPage") && second.equals("Foobar") ));
    }

    public void testCircularRefs()
        throws Exception
    {
        engine.saveText( "Foobar2", "ref to [TestPage]" );
        
        assertTrue( "no uncreated", mgr.findUncreated().size()==0 );
        assertTrue( "no unreferenced", mgr.findUnreferenced().size()==0 );
    }

    public static Test suite()
    {
        return new TestSuite( ReferenceManagerTest.class );
    }
    
    public static void main(String[] args)
    {
        junit.textui.TestRunner.main( new String[] { ReferenceManagerTest.class.getName() } );
    }
    
}

