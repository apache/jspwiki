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
package org.apache.wiki.references;
import net.sf.ehcache.CacheManager;
import org.apache.wiki.TestEngine;
import org.apache.wiki.Util;
import org.apache.wiki.WikiPage;
import org.apache.wiki.api.exceptions.WikiException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

/**
 * The ReferenceManager maintains all hyperlinks between wiki pages.
 */
public class ReferenceManagerTest  {

    Properties props = TestEngine.getTestProperties();
    TestEngine engine;
    ReferenceManager mgr;

    @BeforeEach
    public void setUp() throws Exception {
        props.setProperty( "jspwiki.translatorReader.matchEnglishPlurals", "true");

        engine = new TestEngine(props);

        // create two handy wiki pages used in most test cases
        // Danger! all wiki page names must start with a capital letter!
        engine.saveText( "TestPage", "Reference to [Foobar]." );
        engine.saveText( "Foobar", "Reference to [Foobar2], [Foobars], [Foobar]" );

        mgr = engine.getReferenceManager();
    }

    @AfterEach
    public void tearDown() {
        // any wiki page that was created must be deleted!
        TestEngine.emptyWikiDir();

        // jspwiki always uses a singleton CacheManager, so clear the cache at the end of every test case to avoid polluting another test case
        CacheManager.getInstance().removeAllCaches();

        // make sure that the reference manager cache is cleaned
        TestEngine.emptyWorkDir(null);
    }

    @Test
    public void testNonExistant1() {
        final Collection< String > c = mgr.findReferrers("Foobar2");

        Assertions.assertNotNull( c, "referrers expected" );
        Assertions.assertTrue( c.size() == 1 && c.contains("Foobar") );
    }

    @Test
    public void testNonExistant2() {
        final Collection< String > c = mgr.findReferrers("TestBug");
        Assertions.assertNull( c );
    }

    @Test
    public void testRemove() throws Exception {
        Collection< String > c = mgr.findReferrers("Foobar2");
        Assertions.assertNotNull( c, "referrers expected" );
        Assertions.assertTrue( c.size() == 1 && c.contains("Foobar") );

        engine.getPageManager().deletePage( "Foobar" );
        c = mgr.findReferrers("Foobar2");
        Assertions.assertNull( c );

        engine.saveText( "Foobar", "[Foobar2]");
        c = mgr.findReferrers("Foobar2");

        Assertions.assertNotNull( c, "referrers expected" );
        Assertions.assertTrue( c.size() == 1 && c.contains("Foobar") );
    }

    @Test
    public void testUnreferenced() {
        final Collection< String > c = mgr.findUnreferenced();
        Assertions.assertTrue( Util.collectionContains( c, "TestPage" ), "Unreferenced page not found by ReferenceManager" );
    }


    @Test
    public void testBecomesUnreferenced() throws Exception {
        engine.saveText( "Foobar2", "[TestPage]" );

        Collection< String > c = mgr.findUnreferenced();
        Assertions.assertEquals( 0, c.size(), "Wrong # of orphan pages, stage 1" );

        engine.saveText( "Foobar2", "norefs" );
        c = mgr.findUnreferenced();
        Assertions.assertEquals( 1, c.size(), "Wrong # of orphan pages" );

        final Iterator< String > i = c.iterator();
        final String first = i.next();
        Assertions.assertEquals( "TestPage", first, "Not correct referrers" );
    }

    @Test
    public void testUncreated() {
        final Collection< String > c = mgr.findUncreated();
        Assertions.assertTrue( c.size()==1 && ( c.iterator().next() ).equals("Foobar2") );
    }

    @Test
    public void testReferrers() {
        Collection< String > c = mgr.findReferrers( "TestPage" );
        Assertions.assertNull( c, "TestPage referrers" );

        c = mgr.findReferrers( "Foobar" );
        Assertions.assertNotNull( c, "referrers expected" );
        Assertions.assertEquals( 2, c.size(), "Foobar referrers" );

        c = mgr.findReferrers( "Foobar2" );
        Assertions.assertNotNull( c, "referrers expected" );
        Assertions.assertTrue( c.size() == 1 && ( c.iterator().next() ).equals("Foobar"), "Foobar2 referrers" );

        c = mgr.findReferrers( "Foobars" );
        Assertions.assertNotNull( c, "referrers expected" );
        Assertions.assertEquals( 2, c.size(), "Foobars referrers" );
    }

    @Test
    public void testRefersTo() {
        final Collection< String > s = mgr.findRefersTo( "Foobar" );

        Assertions.assertTrue( s.contains("Foobar"), "does not have Foobar" );
        Assertions.assertTrue( s.contains("Foobar2"), "does not have Foobar2" );
    }

    /**
     *  Should Assertions.fail in 2.2.14-beta
     */
    @Test
    public void testSingularReferences() throws Exception {
        engine.saveText( "RandomPage", "FatalBugs" );
        engine.saveText( "FatalBugs", "<foo>" );
        engine.saveText( "BugCommentPreviewDeletesAllComments", "FatalBug" );
        final Collection< String > c = mgr.findReferrers( "FatalBugs" );

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
    public void testUpdatePluralOnlyRef() throws Exception {
        engine.saveText( "TestPage", "Reference to [Foobars]." );
        Collection< String > c = mgr.findUnreferenced();
        Assertions.assertTrue( c.size()==1 && ( c.iterator().next() ).equals("TestPage"), "Foobar unreferenced" );

        c = mgr.findReferrers( "Foobar" );
        Assertions.assertNotNull( c, "referrers expected" );
        Assertions.assertEquals( 2, c.size(), "Foobar referrers" );
    }


    /**
     *  Opposite to testUpdatePluralOnlyRef(). Is a page with plural form recognized as the page referenced by a singular link.
     */
    @Test
    public void testUpdateFoobar2s() throws Exception {
        engine.saveText( "Foobar2s", "qwertz" );
        Assertions.assertEquals( 0, mgr.findUncreated().size(), "no uncreated" );

        final Collection< String > c = mgr.findReferrers( "Foobar2s" );
        Assertions.assertNotNull( c, "referrers expected" );
        Assertions.assertTrue( c.size()==1 && ( c.iterator().next() ).equals("Foobar"), "referrers" );
    }

    @Test
    public void testUpdateBothExist() throws Exception {
        engine.saveText( "Foobars", "qwertz" );
        final Collection< String > c = mgr.findReferrers( "Foobars" );
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

        final Collection< String > c = mgr.findReferrers( "Foobars" );
        Assertions.assertNotNull( c, "referrers expected" );
        Assertions.assertEquals( 2, c.size(), "Foobars referrers count" );
        Assertions.assertTrue( c.contains("TestPage") && c.contains("Foobar"), "Foobars referrers" );
    }

    @Test
    public void testCircularRefs() throws Exception {
        engine.saveText( "Foobar2", "ref to [TestPage]" );

        Assertions.assertEquals( 0, mgr.findUncreated().size(), "no uncreated" );
        Assertions.assertEquals( 0, mgr.findUnreferenced().size(), "no unreferenced" );
    }

    @Test
    public void testPluralSingularUpdate1() throws Exception {
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
    public void testPluralSingularUpdate2() throws Exception {
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
    public void testPluralSingularUpdate3() throws Exception {
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
    public void testSelf() throws WikiException {
        engine.saveText( "BugOne", "BugOne" );
        final Collection< String > ref = mgr.findReferrers( "BugOne" );
        Assertions.assertNotNull( ref, "referrers expected" );
        Assertions.assertEquals( 1, ref.size(), "wrong size" );
        Assertions.assertEquals( "BugOne", ref.iterator().next(), "ref");
    }

    @Test
    public void testReadLinks() {
        final String src="Foobar. [Foobar].  Frobozz.  [This is a link].";
        final Object[] result = mgr.scanWikiLinks( new WikiPage( engine, "Test"), src ).toArray();

        Assertions.assertEquals( "Foobar", result[0], "item 0" );
        Assertions.assertEquals( "This is a link", result[1], "item 1" );
    }

    /**
     * Test method: dumps the contents of  ReferenceManager link lists to stdout.
     * This method is NOT synchronized, and should be used in testing
     * with one user, one WikiEngine only.
     */
    public static String dumpReferenceManager( final ReferenceManager rm ) {
        final DefaultReferenceManager drm = ( DefaultReferenceManager )rm;
    	final StringBuilder buf = new StringBuilder();
        try {
            buf.append( "================================================================\n" );
            buf.append( "Referred By list:\n" );
            Set< String > keys = drm.getReferredBy().keySet();
            for( final String key : keys ) {
                buf.append( key ).append( " referred by: " );
                final Set< String > refs = drm.getReferredBy().get( key );
                for( final String aRef : refs ) {
                    buf.append( aRef ).append( " " );
                }
                buf.append( "\n" );
            }


            buf.append( "----------------------------------------------------------------\n" );
            buf.append( "Refers To list:\n" );
            keys = drm.getRefersTo().keySet();
            for( final String key : keys ) {
                buf.append( key ).append( " refers to: " );
                final Collection< String > refs = drm.getRefersTo().get( key );
                if(refs != null) {
                    for( final String aRef : refs ) {
                        buf.append( aRef ).append( " " );
                    }
                    buf.append( "\n" );
                } else {
                    buf.append("(no references)\n");
                }
            }
            buf.append( "================================================================\n" );
        } catch( final Exception e ) {
            buf.append("Problem in dump(): " ).append( e ).append( "\n" );
        }

        return( buf.toString() );
    }

}

