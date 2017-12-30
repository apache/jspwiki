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
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import net.sf.ehcache.CacheManager;

/**
 * The ReferenceManager maintains all hyperlinks between wiki pages.
 */
public class ReferenceManagerTest
{
    Properties props = TestEngine.getTestProperties();
    TestEngine engine;
    ReferenceManager mgr;

    @Before
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

    @After
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

        Assert.assertNotNull( "referrers expected", c );
        Assert.assertTrue( c.size() == 1 && c.contains("Foobar") );
    }

    @Test
    public void testNonExistant2()
    {
        Collection< String > c = mgr.findReferrers("TestBug");

        Assert.assertNull( c );
    }

    @Test
    public void testRemove()
        throws Exception
    {
        Collection< String > c = mgr.findReferrers("Foobar2");

        Assert.assertNotNull( "referrers expected", c );
        Assert.assertTrue( c.size() == 1 && c.contains("Foobar") );

        engine.deletePage( "Foobar" );

        c = mgr.findReferrers("Foobar2");

        Assert.assertNull( c );

        engine.saveText( "Foobar", "[Foobar2]");

        c = mgr.findReferrers("Foobar2");

        Assert.assertNotNull( "referrers expected", c );
        Assert.assertTrue( c.size() == 1 && c.contains("Foobar") );
    }

    @Test
    public void testUnreferenced()
        throws Exception
    {
        Collection< String > c = mgr.findUnreferenced();
        Assert.assertTrue( "Unreferenced page not found by ReferenceManager",
                    Util.collectionContains( c, "TestPage" ));
    }


    @Test
    public void testBecomesUnreferenced()
        throws Exception
    {
        engine.saveText( "Foobar2", "[TestPage]" );

        Collection< String > c = mgr.findUnreferenced();
        Assert.assertEquals( "Wrong # of orphan pages, stage 1", 0, c.size() );

        engine.saveText( "Foobar2", "norefs" );
        c = mgr.findUnreferenced();
        Assert.assertEquals( "Wrong # of orphan pages", 1, c.size() );

        Iterator i = c.iterator();
        String first = (String) i.next();
        Assert.assertEquals( "Not correct referrers", "TestPage", first );
    }

    @Test
    public void testUncreated()
        throws Exception
    {
        Collection< String > c = mgr.findUncreated();

        Assert.assertTrue( c.size()==1 && ((String) c.iterator().next()).equals("Foobar2") );
    }

    @Test
    public void testReferrers()
        throws Exception
    {
        Collection< String > c = mgr.findReferrers( "TestPage" );
        Assert.assertNull( "TestPage referrers", c );

        c = mgr.findReferrers( "Foobar" );
        Assert.assertNotNull( "referrers expected", c );
        Assert.assertTrue( "Foobar referrers", c.size()==2  );

        c = mgr.findReferrers( "Foobar2" );
        Assert.assertNotNull( "referrers expected", c );
        Assert.assertTrue( "Foobar2 referrers", c.size()==1 && ((String) c.iterator().next()).equals("Foobar") );

        c = mgr.findReferrers( "Foobars" );
        Assert.assertNotNull( "referrers expected", c );
        Assert.assertEquals( "Foobars referrers", 2, c.size() );
        //Assert.assertEquals( "Foobars referrer 'TestPage'", "TestPage", (String) c.iterator().next() );
    }

    @Test
    public void testRefersTo()
        throws Exception
    {
        Collection s = mgr.findRefersTo( "Foobar" );

        Assert.assertTrue( "does not have Foobar", s.contains("Foobar") );
        // Assert.assertTrue( "does not have Foobars", s.contains("Foobars") );
        Assert.assertTrue( "does not have Foobar2", s.contains("Foobar2") );
    }

    /**
     *  Should Assert.fail in 2.2.14-beta
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

        Assert.assertNotNull( "referrers expected", c );
        Assert.assertEquals( "FatalBugs referrers number", 2, c.size()  );
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
        Assert.assertTrue( "Foobar unreferenced", c.size()==1 && ((String) c.iterator().next()).equals("TestPage") );

        c = mgr.findReferrers( "Foobar" );
        Assert.assertNotNull( "referrers expected", c );
        Iterator it = c.iterator();
        String s1 = (String)it.next();
        Assert.assertTrue( "Foobar referrers",
                    c.size()==2 );
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
        Assert.assertTrue( "no uncreated", mgr.findUncreated().size()==0 );

        Collection< String > c = mgr.findReferrers( "Foobar2s" );
        Assert.assertNotNull( "referrers expected", c );
        Assert.assertTrue( "referrers", c!=null && c.size()==1 && ((String) c.iterator().next()).equals("Foobar") );
    }

    @Test
    public void testUpdateBothExist()
        throws Exception
    {
        engine.saveText( "Foobars", "qwertz" );
        Collection< String > c = mgr.findReferrers( "Foobars" );
        Assert.assertNotNull( "referrers expected", c );
        Assert.assertEquals( "Foobars referrers", 2, c.size() );
        Assert.assertTrue( "Foobars referrer is not TestPage", c.contains( "TestPage" ) && c.contains("Foobar"));
    }

    @Test
    public void testUpdateBothExist2()
        throws Exception
    {
        engine.saveText( "Foobars", "qwertz" );
        engine.saveText( "TestPage", "Reference to [Foobar], [Foobars]." );

        Collection< String > c = mgr.findReferrers( "Foobars" );
        Assert.assertNotNull( "referrers expected", c );
        Assert.assertEquals( "Foobars referrers count", 2, c.size() );

        Iterator< String > i = c.iterator();
        String first = i.next();

        Assert.assertTrue( "Foobars referrers", c.contains("TestPage") && c.contains("Foobar"));
    }

    @Test
    public void testCircularRefs()
        throws Exception
    {
        engine.saveText( "Foobar2", "ref to [TestPage]" );

        Assert.assertTrue( "no uncreated", mgr.findUncreated().size()==0 );
        Assert.assertTrue( "no unreferenced", mgr.findUnreferenced().size()==0 );
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
        Assert.assertNull("newbugs",ref); // No referrers must be found

        ref = mgr.findReferrers( "NewBug" );
        Assert.assertNull("newbug",ref); // No referrers must be found

        ref = mgr.findReferrers( "OpenBugs" );
        Assert.assertNotNull("referrers expected", ref);
        Assert.assertEquals("openbugs",1,ref.size());
        Assert.assertEquals("openbugs2","BugOne",ref.iterator().next());

        ref = mgr.findReferrers( "OpenBug" );
        Assert.assertNotNull("referrers expected", ref);
        Assert.assertEquals("openbug",1,ref.size());
        Assert.assertEquals("openbug2","BugOne",ref.iterator().next());

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
        Assert.assertNull("newbugs",ref); // No referrers must be found

        ref = mgr.findReferrers( "NewBug" );
        Assert.assertNull("newbug",ref); // No referrers must be found

        ref = mgr.findReferrers( "OpenBugs" );
        Assert.assertNotNull("referrers expected", ref);
        Assert.assertEquals("openbugs",1,ref.size());
        Assert.assertEquals("openbugs2","BugOne",ref.iterator().next());

        ref = mgr.findReferrers( "OpenBug" );
        Assert.assertNotNull("referrers expected", ref);
        Assert.assertEquals("openbug",1,ref.size());
        Assert.assertEquals("openbug2","BugOne",ref.iterator().next());

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
        Assert.assertNotNull("referrers expected", ref);
        Assert.assertEquals("newbugs",1,ref.size());
        Assert.assertEquals("newbugs2","BugTwo",ref.iterator().next());

        ref = mgr.findReferrers( "NewBug" );
        Assert.assertNotNull("referrers expected", ref);
        Assert.assertEquals("newbugs",1,ref.size());
        Assert.assertEquals("newbugs2","BugTwo",ref.iterator().next());

        ref = mgr.findReferrers( "OpenBugs" );
        Assert.assertNotNull("referrers expected", ref);
        Assert.assertEquals("openbugs",1,ref.size());
        Assert.assertEquals("openbugs2","BugOne",ref.iterator().next());

        ref = mgr.findReferrers( "OpenBug" );
        Assert.assertNotNull("referrers expected", ref);
        Assert.assertEquals("openbug",1,ref.size());
        Assert.assertEquals("openbug2","BugOne",ref.iterator().next());

    }

    @Test
    public void testSelf() throws WikiException
    {
        engine.saveText( "BugOne", "BugOne" );
        Collection< String > ref = mgr.findReferrers( "BugOne" );
        Assert.assertNotNull("referrers expected", ref);
        Assert.assertEquals("wrong size",1,ref.size());
        Assert.assertEquals("ref", "BugOne", ref.iterator().next());
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

