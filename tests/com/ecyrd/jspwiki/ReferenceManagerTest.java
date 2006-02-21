
package com.ecyrd.jspwiki;

import junit.framework.*;
import java.util.*;
import java.io.*;

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

        //
        //  We must make sure that the reference manager cache is cleaned first.
        //
        String workDir = props.getProperty( "jspwiki.workDir" );

        if( workDir != null )
        {
            File refmgrfile = new File( workDir, "refmgr.ser" );
            if( refmgrfile.exists() ) refmgrfile.delete();
        }

        engine = new TestEngine(props);

        engine.saveText( "TestPage", "Reference to [Foobar]." );
        engine.saveText( "Foobar", "Reference to [Foobar2], [Foobars], [Foobar]" );

        mgr = engine.getReferenceManager();
    }

    public void tearDown()
        throws Exception
    {
        engine.deletePage( "TestPage" );
        engine.deletePage( "Foobar" );
        engine.deletePage( "Foobars" );
        engine.deletePage( "Foobar2" );
        engine.deletePage( "Foobar2s" );
        engine.deletePage( "BugCommentPreviewDeletesAllComments" );
        engine.deletePage( "FatalBugs" );
        engine.deletePage( "RandomPage" );
        engine.deletePage( "NewBugs" );
        engine.deletePage( "OpenBug" );
        engine.deletePage( "OpenBugs" );
        engine.deletePage( "NewBug" );
        engine.deletePage( "BugOne" );
        engine.deletePage( "BugTwo" );
    }

    public void testNonExistant1()
        throws Exception
    {
        Collection c = mgr.findReferrers("Foobar2");
        
        assertTrue( c.size() == 1 && c.contains("Foobar") );
    }
    
    public void testNonExistant2()
    {
        Collection c = mgr.findReferrers("TestBug");
        
        assertNull( c );
    }
    
    public void testRemove()
        throws Exception
    {
        Collection c = mgr.findReferrers("Foobar2");
        
        assertTrue( c.size() == 1 && c.contains("Foobar") );

        engine.deletePage( "Foobar" );
        
        c = mgr.findReferrers("Foobar2");
        
        assertNull( c );
        
        engine.saveText( "Foobar", "[Foobar2]");
        
        c = mgr.findReferrers("Foobar2");
        
        assertTrue( c.size() == 1 && c.contains("Foobar") );        
    }
    
    public void testUnreferenced()
        throws Exception
    {
        Collection c = mgr.findUnreferenced();
        assertTrue( "Unreferenced page not found by ReferenceManager",
                    Util.collectionContains( c, "TestPage" ));
    }


    public void testBecomesUnreferenced()
        throws Exception
    {
        engine.saveText( "Foobar2", "[TestPage]" );

        Collection c = mgr.findUnreferenced();
        assertEquals( "Wrong # of orphan pages, stage 1", 0, c.size() );

        engine.saveText( "Foobar2", "norefs" );
        c = mgr.findUnreferenced();
        assertEquals( "Wrong # of orphan pages", 1, c.size() );

        Iterator i = c.iterator();
        String first = (String) i.next();
        assertEquals( "Not correct referrers", "TestPage", first );
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
        assertEquals( "Foobars referrers", 1, c.size() );
        assertEquals( "Foobars referrer 'TestPage'", "TestPage", (String) c.iterator().next() );
    }

    public void testRefersTo()
        throws Exception
    {
        Collection s = mgr.findRefersTo( "Foobar" );
        
        assertTrue( "does not have Foobar", s.contains("Foobar") );
        // assertTrue( "does not have Foobars", s.contains("Foobars") );
        assertTrue( "does not have Foobar2", s.contains("Foobar2") );
    }
    
    /**
     *  Should fail in 2.2.14-beta
     * @throws Exception
     */
    public void testSingularReferences()
    throws Exception
    {
        engine.saveText( "RandomPage", "FatalBugs" );
        engine.saveText( "FatalBugs", "<foo>" );
        engine.saveText( "BugCommentPreviewDeletesAllComments", "FatalBug" );
        
        Collection c = mgr.findReferrers( "FatalBugs" );
        
        assertEquals( "FatalBugs referrers number", 2, c.size()  );
    }

    /** 
     *  Is a page recognized as referenced if only plural form links exist.
     */

    // NB: Unfortunately, cleaning out self-references in the case there's
    //     a plural and a singular form of the page becomes nigh impossible, so we
    //     just don't do it.
    public void testUpdatePluralOnlyRef()
        throws Exception
    {
        engine.saveText( "TestPage", "Reference to [Foobars]." );
        Collection c = mgr.findUnreferenced();
        assertTrue( "Foobar unreferenced", c.size()==1 && ((String) c.iterator().next()).equals("TestPage") );

        c = mgr.findReferrers( "Foobar" );
        Iterator it = c.iterator();
        String s1 = (String)it.next();
        assertTrue( "Foobar referrers", 
                    c.size()==1 && 
                    s1.equals("TestPage") );
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
        assertEquals( "Foobars referrers", 1, c.size() );
        assertEquals( "Foobars referrer is not TestPage", "TestPage", ((String) c.iterator().next()) );
    }

    public void testUpdateBothExist2()
        throws Exception
    {
        engine.saveText( "Foobars", "qwertz" );
        engine.saveText( "TestPage", "Reference to [Foobar], [Foobars]." );
        
        Collection c = mgr.findReferrers( "Foobars" );
        assertEquals( "Foobars referrers count", 1, c.size() );

        Iterator i = c.iterator();
        String first = (String) i.next();

        assertTrue( "Foobars referrers", 
                    first.equals("TestPage") );
    }

    public void testCircularRefs()
        throws Exception
    {
        engine.saveText( "Foobar2", "ref to [TestPage]" );
        
        assertTrue( "no uncreated", mgr.findUncreated().size()==0 );
        assertTrue( "no unreferenced", mgr.findUnreferenced().size()==0 );
    }

    public void testPluralSingularUpdate1()
        throws Exception
    {
        engine.saveText( "BugOne", "NewBug" );
        engine.saveText( "NewBugs", "foo" );
        engine.saveText( "OpenBugs", "bar" );
        
        engine.saveText( "BugOne", "OpenBug" );
        
        Collection ref = mgr.findReferrers( "NewBugs" );
        assertNull("newbugs",ref); // No referrers must be found

        ref = mgr.findReferrers( "NewBug" );
        assertNull("newbug",ref); // No referrers must be found

        ref = mgr.findReferrers( "OpenBugs" );
        assertEquals("openbugs",1,ref.size());
        assertEquals("openbugs2","BugOne",ref.iterator().next());

        ref = mgr.findReferrers( "OpenBug" );
        assertEquals("openbug",1,ref.size());
        assertEquals("openbug2","BugOne",ref.iterator().next());

    }

    public void testPluralSingularUpdate2()
        throws Exception
    {
        engine.saveText( "BugOne", "NewBug" );
        engine.saveText( "NewBug", "foo" );
        engine.saveText( "OpenBug", "bar" );
    
        engine.saveText( "BugOne", "OpenBug" );
    
        Collection ref = mgr.findReferrers( "NewBugs" );
        assertNull("newbugs",ref); // No referrers must be found

        ref = mgr.findReferrers( "NewBug" );
        assertNull("newbug",ref); // No referrers must be found

        ref = mgr.findReferrers( "OpenBugs" );
        assertEquals("openbugs",1,ref.size());
        assertEquals("openbugs2","BugOne",ref.iterator().next());

        ref = mgr.findReferrers( "OpenBug" );
        assertEquals("openbug",1,ref.size());
        assertEquals("openbug2","BugOne",ref.iterator().next());

    }

    public void testPluralSingularUpdate3()
        throws Exception
    {
        engine.saveText( "BugOne", "NewBug" );
        engine.saveText( "BugTwo", "NewBug" );
        engine.saveText( "NewBugs", "foo" );
        engine.saveText( "OpenBugs", "bar" );
    
        engine.saveText( "BugOne", "OpenBug" );
    
        Collection ref = mgr.findReferrers( "NewBugs" );
        assertEquals("newbugs",1,ref.size()); 
        assertEquals("newbugs2","BugTwo",ref.iterator().next()); 

        ref = mgr.findReferrers( "NewBug" );
        assertEquals("newbugs",1,ref.size()); 
        assertEquals("newbugs2","BugTwo",ref.iterator().next()); 

        ref = mgr.findReferrers( "OpenBugs" );
        assertEquals("openbugs",1,ref.size());
        assertEquals("openbugs2","BugOne",ref.iterator().next());

        ref = mgr.findReferrers( "OpenBug" );
        assertEquals("openbug",1,ref.size());
        assertEquals("openbug2","BugOne",ref.iterator().next());

    }

    public static Test suite()
    {
        return new TestSuite( ReferenceManagerTest.class );
    }
    
    public static void main(String[] args)
    {
        junit.textui.TestRunner.main( new String[] { ReferenceManagerTest.class.getName() } );
    }
    
    
    /**
     * Test method: dumps the contents of  ReferenceManager link lists to stdout.
     * This method is NOT synchronized, and should be used in testing
     * with one user, one WikiEngine only.
     */
    public static String dumpReferenceManager( ReferenceManager rm )
    {
        StringBuffer buf = new StringBuffer();
        try
        {
            buf.append( "================================================================\n" );
            buf.append( "Referred By list:\n" );
            Set keys = rm.getReferredBy().keySet();
            Iterator it = keys.iterator();
            while( it.hasNext() )
            {
                String key = (String) it.next();
                buf.append( key + " referred by: " );
                Set refs = (Set)rm.getReferredBy().get( key );
                Iterator rit = refs.iterator();
                while( rit.hasNext() )
                {
                    String aRef = (String)rit.next();
                    buf.append( aRef + " " );
                }
                buf.append( "\n" );
            }
            
            
            buf.append( "----------------------------------------------------------------\n" );
            buf.append( "Refers To list:\n" );
            keys = rm.getRefersTo().keySet();
            it = keys.iterator();
            while( it.hasNext() )
            {
                String key = (String) it.next();
                buf.append( key + " refers to: " );
                Collection refs = (Collection)rm.getRefersTo().get( key );
                if(refs != null)
                {
                    Iterator rit = refs.iterator();
                    while( rit.hasNext() )
                    {
                        String aRef = (String)rit.next();
                        buf.append( aRef + " " );
                    }
                    buf.append( "\n" );
                }
                else
                    buf.append("(no references)\n");
            }
            buf.append( "================================================================\n" );
        }
        catch(Exception e)
        {
            buf.append("Problem in dump(): " + e + "\n" );
        }
        
        return( buf.toString() );
    }

}

