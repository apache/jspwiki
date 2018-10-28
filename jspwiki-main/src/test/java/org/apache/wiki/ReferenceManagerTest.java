/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.wiki;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import org.apache.wiki.api.exceptions.WikiException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.sf.ehcache.CacheManager;

/**
 * The ReferenceManager maintains all hyperlinks between wiki pages.
 */
public class ReferenceManagerTest
{
    Properties props = TestEngine.getTestProperties();
    TestEngine engine;
    ReferenceManager mgr;

    @BeforeEach
    public void setUp()
        throws Exception
    {
        props.setProperty( "jspwiki.translatorReader.matchEnglishPlurals", "true");

        // make sure that the reference manager cache is cleaned first
        TestEngine.emptyWorkDir(null);

        engine = new TestEngine(props);

        // create two handy wiki pages used in most test cases
        // Danger! all wiki page names must start with a capital letter!
        engine.saveText( "TestPage", "Reference to [Foobar]." );
        engine.saveText( "Foobar", "Reference to [Foobar2], [Foobars], [Foobar]" );

        mgr = engine.getReferenceManager();
    }

    @AfterEach
    public void tearDown()
        throws Exception
    {
        // any wiki page that was created must be deleted!
        TestEngine.emptyWikiDir();

        // jspwiki always uses a singleton CacheManager, so
        // clear the cache at the end of every test case to avoid
        // polluting another test case
        CacheManager.getInstance().removeAllCaches();
    }

    @Test
    public void testNonExistant1()
        throws Exception
    {
        Collection< String > c = mgr.findReferrers("Foobar2");

        Assertions.assertNotNull( c, "referrers expected" );
        Assertions.assertTrue( c.size() == 1 && c.contains("Foobar") );
    }

    @Test
    public void testNonExistant2()
    {
        Collection< String > c = mgr.findReferrers("TestBug");

        Assertions.assertNull( c );
    }

    @Test
    public void testRemove()
        throws Exception
    {
        Collection< String > c = mgr.findReferrers("Foobar2");

        Assertions.assertNotNull( c, "referrers expected" );
        Assertions.assertTrue( c.size() == 1 && c.contains("Foobar") );

        engine.deletePage( "Foobar" );

        c = mgr.findReferrers("Foobar2");

        Assertions.assertNull( c );

        engine.saveText( "Foobar", "[Foobar2]");

        c = mgr.findReferrers("Foobar2");

        Assertions.assertNotNull( c, "referrers expected" );
        Assertions.assertTrue( c.size() == 1 && c.contains("Foobar") );
    }

    @Test
    public void testUnreferenced()
        throws Exception
    {
        Collection< String > c = mgr.findUnreferenced();
        Assertions.assertTrue( Util.collectionContains( c, "TestPage" ), "Unreferenced page not found by ReferenceManager" );
    }


    @Test
    public void testBecomesUnreferenced()
        throws Exception
    {
        engine.saveText( "Foobar2", "[TestPage]" );

        Collection< String > c = mgr.findUnreferenced();
        Assertions.assertEquals( 0, c.size(), "Wrong # of orphan pages, stage 1" );

        engine.saveText( "Foobar2", "norefs" );
        c = mgr.findUnreferenced();
        Assertions.assertEquals( 1, c.size(), "Wrong # of orphan pages" );

        Iterator< String > i = c.iterator();
        String first = i.next();
        Assertions.assertEquals( "TestPage", first, "Not correct referrers" );
    }

    @Test
    public void testUncreated()
        throws Exception
    {
        Collection< String > c = mgr.findUncreated();

        Assertions.assertTrue( c.size()==1 && ((String) c.iterator().next()).equals("Foobar2") );
    }

    @Test
    public void testReferrers()
        throws Exception
    {
        Collection< String > c = mgr.findReferrers( "TestPage" );
        Assertions.assertNull( c, "TestPage referrers" );

        c = mgr.findReferrers( "Foobar" );
        Assertions.assertNotNull( c, "referrers expected" );
        Assertions.assertTrue( c.size()==2, "Foobar referrers" );

        c = mgr.findReferrers( "Foobar2" );
        Assertions.assertNotNull( c, "referrers expected" );
        Assertions.assertTrue( c.size()==1 && ((String) c.iterator().next()).equals("Foobar"), "Foobar2 referrers" );

        c = mgr.findReferrers( "Foobars" );
        Assertions.assertNotNull( c, "referrers expected" );
        Assertions.assertEquals( 2, c.size(), "Foobars referrers" );
        //Assertions.assertEquals( "Foobars referrer 'TestPage'", "TestPage", (String) c.iterator().next() );
    }

    @Test
    public void testRefersTo()
        throws Exception
    {
        Collection< String > s = mgr.findRefersTo( "Foobar" );

        Assertions.assertTrue( s.contains("Foobar"), "does not have Foobar" );
        // Assertions.assertTrue( "does not have Foobars", s.contains("Foobars") );
        Assertions.assertTrue( s.contains("Foobar2"), "does not have Foobar2" );
    }

    /**
     *  Should Assertions.fail in 2.2.14-beta
     * @throws Exception
     */
    @Test
    public void testSingularReferences()
    throws Exception
    {
        engine.saveText( "RandomPage", "FatalBugs" );
        engine.saveText( "FatalBugs", "<foo>" );
        engine.saveText( "BugCommentPreviewDeletesAllComments", "FatalBug" );

        Collection< String > c = mgr.findReferrers( "FatalBugs" );

        Assertions.assertNotNull( c, "referrers expected" );
        Assertions.assertEquals( 2, c.size(), "FatalBugs referrers number" );
    }

    /**
     *  Is a page recognized as referenced if only plural form links exist.
     */

    // NB: Unfortunately, cleaning out self-references in the case there's
    //     a plural and a singular form of the page becomes nigh impossible, so we
    //     just don't do it.
    @Test
    public void testUpdatePluralOnlyRef()
        throws Exception
    {
        engine.saveText( "TestPage", "Reference to [Foobars]." );
        Collection< String > c = mgr.findUnreferenced();
        Assertions.assertTrue( c.size()==1 && ((String) c.iterator().next()).equals("TestPage"), "Foobar unreferenced" );

        c = mgr.findReferrers( "Foobar" );
        Assertions.assertNotNull( c, "referrers expected" );
        Assertions.assertTrue( c.size()==2, "Foobar referrers" );
    }


    /**
     *  Opposite to testUpdatePluralOnlyRef(). Is a page with plural form recognized as
     *  the page referenced by a singular link.
     */

    @Test
    public void testUpdateFoobar2s()
        throws Exception
    {
        engine.saveText( "Foobar2s", "qwertz" );
        Assertions.assertTrue( mgr.findUncreated().size()==0, "no uncreated" );

        Collection< String > c = mgr.findReferrers( "Foobar2s" );
        Assertions.assertNotNull( c, "referrers expected" );
        Assertions.assertTrue( c!=null && c.size()==1 && ((String) c.iterator().next()).equals("Foobar"), "referrers" );
    }

    @Test
    public void testUpdateBothExist()
        throws Exception
    {
        engine.saveText( "Foobars", "qwertz" );
        Collection< String > c = mgr.findReferrers( "Foobars" );
        Assertions.assertNotNull( c, "referrers expected" );
        Assertions.assertEquals( 2, c.size(), "Foobars referrers" );
        Assertions.assertTrue( c.contains( "TestPage" ) && c.contains("Foobar"), "Foobars referrer is not TestPage" );
    }

    @Test
    public void testUpdateBothExist2()
        throws Exception
    {
        engine.saveText( "Foobars", "qwertz" );
        engine.saveText( "TestPage", "Reference to [Foobar], [Foobars]." );

        Collection< String > c = mgr.findReferrers( "Foobars" );
        Assertions.assertNotNull( c, "referrers expected" );
        Assertions.assertEquals( 2, c.size(), "Foobars referrers count" );
        Assertions.assertTrue( c.contains("TestPage") && c.contains("Foobar"), "Foobars referrers" );
    }

    @Test
    public void testCircularRefs()
        throws Exception
    {
        engine.saveText( "Foobar2", "ref to [TestPage]" );

        Assertions.assertTrue( mgr.findUncreated().size()==0, "no uncreated" );
        Assertions.assertTrue( mgr.findUnreferenced().size()==0, "no unreferenced" );
    }

    @Test
    public void testPluralSingularUpdate1()
        throws Exception
    {
        engine.saveText( "BugOne", "NewBug" );
        engine.saveText( "NewBugs", "foo" );
        engine.saveText( "OpenBugs", "bar" );

        engine.saveText( "BugOne", "OpenBug" );

        Collection< String > ref = mgr.findReferrers( "NewBugs" );
        Assertions.assertNull( ref, "newbugs" ); // No referrers must be found

        ref = mgr.findReferrers( "NewBug" );
        Assertions.assertNull( ref, "newbug" ); // No referrers must be found

        ref = mgr.findReferrers( "OpenBugs" );
        Assertions.assertNotNull( ref, "referrers expected" );
        Assertions.assertEquals( 1, ref.size(), "openbugs" );
        Assertions.assertEquals( ref.iterator().next(), "BugOne", "openbugs2" );

        ref = mgr.findReferrers( "OpenBug" );
        Assertions.assertNotNull( ref, "referrers expected" );
        Assertions.assertEquals( 1, ref.size(), "openbug" );
        Assertions.assertEquals( ref.iterator().next(), "BugOne", "openbugs2" );

    }

    @Test
    public void testPluralSingularUpdate2()
        throws Exception
    {
        engine.saveText( "BugOne", "NewBug" );
        engine.saveText( "NewBug", "foo" );
        engine.saveText( "OpenBug", "bar" );

        engine.saveText( "BugOne", "OpenBug" );

        Collection< String > ref = mgr.findReferrers( "NewBugs" );
        Assertions.assertNull( ref, "newbugs" ); // No referrers must be found

        ref = mgr.findReferrers( "NewBug" );
        Assertions.assertNull( ref, "newbug" ); // No referrers must be found

        ref = mgr.findReferrers( "OpenBugs" );
        Assertions.assertNotNull( ref, "referrers expected" );
        Assertions.assertEquals( 1, ref.size(), "openbugs" );
        Assertions.assertEquals( "BugOne",ref.iterator().next(), "openbugs2" );

        ref = mgr.findReferrers( "OpenBug" );
        Assertions.assertNotNull( ref, "referrers expected" );
        Assertions.assertEquals( 1, ref.size(), "openbug" );
        Assertions.assertEquals( "BugOne",ref.iterator().next(), "openbug2" );

    }

    @Test
    public void testPluralSingularUpdate3()
        throws Exception
    {
        engine.saveText( "BugOne", "NewBug" );
        engine.saveText( "BugTwo", "NewBug" );
        engine.saveText( "NewBugs", "foo" );
        engine.saveText( "OpenBugs", "bar" );

        engine.saveText( "BugOne", "OpenBug" );

        Collection< String > ref = mgr.findReferrers( "NewBugs" );
        Assertions.assertNotNull( ref, "referrers expected" );
        Assertions.assertEquals( 1,ref.size(), "newbugs" );
        Assertions.assertEquals( "BugTwo",ref.iterator().next(), "newbugs2" );

        ref = mgr.findReferrers( "NewBug" );
        Assertions.assertNotNull( ref, "referrers expected" );
        Assertions.assertEquals( 1,ref.size(), "newbugs" );
        Assertions.assertEquals( "BugTwo",ref.iterator().next(), "newbugs2" );

        ref = mgr.findReferrers( "OpenBugs" );
        Assertions.assertNotNull( ref, "referrers expected" );
        Assertions.assertEquals( 1,ref.size(), "openbugs" );
        Assertions.assertEquals( "BugOne",ref.iterator().next(), "openbugs2" );

        ref = mgr.findReferrers( "OpenBug" );
        Assertions.assertNotNull( ref, "referrers expected" );
        Assertions.assertEquals( 1,ref.size(), "openbug" );
        Assertions.assertEquals( "BugOne",ref.iterator().next(), "openbug2" );

    }

    @Test
    public void testSelf() throws WikiException
    {
        engine.saveText( "BugOne", "BugOne" );
        Collection< String > ref = mgr.findReferrers( "BugOne" );
        Assertions.assertNotNull( ref, "referrers expected" );
        Assertions.assertEquals( 1, ref.size(), "wrong size" );
        Assertions.assertEquals( "BugOne", ref.iterator().next(), "ref");
    }

    /**
     * Test method: dumps the contents of  ReferenceManager link lists to stdout.
     * This method is NOT synchronized, and should be used in testing
     * with one user, one WikiEngine only.
     */
    public static String dumpReferenceManager( ReferenceManager rm )
    {
    	StringBuilder buf = new StringBuilder();
        try
        {
            buf.append( "================================================================\n" );
            buf.append( "Referred By list:\n" );
            Set< String > keys = rm.getReferredBy().keySet();
            Iterator< String > it = keys.iterator();
            while( it.hasNext() )
            {
                String key = it.next();
                buf.append( key + " referred by: " );
                Set< String > refs = rm.getReferredBy().get( key );
                Iterator< String > rit = refs.iterator();
                while( rit.hasNext() )
                {
                    String aRef = rit.next();
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
                Collection< String > refs = rm.getRefersTo().get( key );
                if(refs != null)
                {
                    Iterator< String > rit = refs.iterator();
                    while( rit.hasNext() )
                    {
                        String aRef = rit.next();
                        buf.append( aRef + " " );
                    }
                    buf.append( "\n" );
                } else {
                    buf.append("(no references)\n");
                }
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

