/*
    JSPWiki - a JSP-based WikiWiki clone.

    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.    
 */

package org.apache.wiki.content;

import static org.apache.wiki.content.ReferenceManager.PROPERTY_REFERRED_BY;
import static org.apache.wiki.content.ReferenceManager.PROPERTY_REFERS_TO;

import java.util.Collection;
import java.util.List;
import java.util.Properties;

import javax.jcr.*;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.wiki.TestEngine;
import org.apache.wiki.content.jcr.JCRWikiPage;
import org.apache.wiki.providers.ProviderException;

/**
 */
public class ReferenceManagerTest extends TestCase
{
    private static final WikiPath PATH_FOOBAR = WikiPath.valueOf( "Foobar" );

    public static void main( String[] args )
    {
        junit.textui.TestRunner.main( new String[] { ReferenceManagerTest.class.getName() } );
    }

    public static Test suite()
    {
        return new TestSuite( ReferenceManagerTest.class );
    }

    Properties props = new Properties();

    TestEngine engine;

    ReferenceManager mgr;

    public ReferenceManagerTest( String s )
    {
        super( s );
    }

    public void setUp() throws Exception
    {
        props.load( TestEngine.findTestProperties() );
        props.setProperty( "jspwiki.translatorReader.matchEnglishPlurals", "true" );
        engine = new TestEngine( props );
        engine.emptyRepository();
        engine.getReferenceManager().rebuild();
        engine.saveText( "TestPage", "Reference to [Foobar]." );
        engine.saveText( "Foobar", "Reference to [Foobar2], [Foobars], [Foobar]" );

        mgr = engine.getReferenceManager();
    }

    public void tearDown() throws Exception
    {
        try
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
            engine.deletePage( "BugThree" );
            Session s = engine.getContentManager().getCurrentSession();
            try
            {
                s.getRootNode().getNode( "/TestAddToProperty" ).remove();
                s.save();
            }
            catch ( PathNotFoundException e ) {}
            try
            {
                s.getRootNode().getNode( "/TestRemoveFromProperty" ).remove();
                s.save();
            }
            catch ( PathNotFoundException e ) {}
        }
        finally
        {
            engine.shutdown();
        }
    }
    
    public void testAddReferrral() throws Exception
    {
        // Save 3 dummy pages
        engine.saveText( "BugOne", "Test 1." );
        engine.saveText( "BugTwo", "Test 2." );
        engine.saveText( "BugThree", "Test 3." );
        ContentManager cm = engine.getContentManager();
        
        // Make page 1 refer to 2, 1 to 3, and 2 to 3
        Node one = cm.getPage( WikiPath.valueOf( "BugOne" ) ).getJCRNode();
        Node two = cm.getPage( WikiPath.valueOf( "BugTwo" ) ).getJCRNode();
        Node three = cm.getPage( WikiPath.valueOf( "BugThree" ) ).getJCRNode();
        mgr.addReferral( one.getUUID(), two.getUUID() );
        mgr.addReferral( one.getUUID(), three.getUUID() );
        mgr.addReferral( two.getUUID(), three.getUUID() );
        cm.getCurrentSession().save();
        
        // Verify that 1 has two outbound links, and no inbound
        assertEquals( 2, mgr.getFromProperty( one.getPath(), PROPERTY_REFERS_TO ).length );
        assertEquals( two.getUUID(), mgr.getFromProperty( one.getPath(), PROPERTY_REFERS_TO )[0] );
        assertEquals( three.getUUID(), mgr.getFromProperty( one.getPath(), PROPERTY_REFERS_TO )[1] );
        assertEquals( 0, mgr.getFromProperty( one.getPath(), PROPERTY_REFERRED_BY ).length );
        
        // Verify that 2 has one inbound link (from 1) and one outbound (to 3)
        assertEquals( 1, mgr.getFromProperty( two.getPath(), PROPERTY_REFERS_TO ).length );
        assertEquals( 1, mgr.getFromProperty( two.getPath(), PROPERTY_REFERRED_BY ).length );
        assertEquals( one.getUUID(), mgr.getFromProperty( two.getPath(), PROPERTY_REFERRED_BY )[0] );
        assertEquals( three.getUUID(), mgr.getFromProperty( two.getPath(), PROPERTY_REFERS_TO )[0] );

        // Verify that 3 has two inbound links (from 1 and 2), and no outbound
        assertEquals( 0, mgr.getFromProperty( three.getPath(), PROPERTY_REFERS_TO ).length );
        assertEquals( 2, mgr.getFromProperty( three.getPath(), PROPERTY_REFERRED_BY ).length );
        assertEquals( one.getUUID(), mgr.getFromProperty( three.getPath(), PROPERTY_REFERRED_BY )[0] );
        assertEquals( two.getUUID(), mgr.getFromProperty( three.getPath(), PROPERTY_REFERRED_BY )[1] );
    }

    public void testRemoveReferrral() throws Exception
    {
        // Save 3 dummy pages
        engine.saveText( "BugOne", "Test 1." );
        engine.saveText( "BugTwo", "Test 2." );
        engine.saveText( "BugThree", "Test 3." );
        ContentManager cm = engine.getContentManager();
        
        // Make page 1 refer to 2, 1 to 3, and 2 to 3
        Node one = cm.getPage( WikiPath.valueOf( "BugOne" ) ).getJCRNode();
        Node two = cm.getPage( WikiPath.valueOf( "BugTwo" ) ).getJCRNode();
        Node three = cm.getPage( WikiPath.valueOf( "BugThree" ) ).getJCRNode();
        mgr.addReferral( one.getUUID(), two.getUUID() );
        mgr.addReferral( one.getUUID(), three.getUUID() );
        mgr.addReferral( two.getUUID(), three.getUUID() );
        cm.getCurrentSession().save();
        
        // Remove the link from 1 to 2
        mgr.removeReferral( one.getUUID(), two.getUUID() );
        cm.getCurrentSession().save();
        assertEquals( 1, mgr.getFromProperty( one.getPath(), PROPERTY_REFERS_TO ).length );
        assertEquals( 0, mgr.getFromProperty( one.getPath(), PROPERTY_REFERRED_BY ).length );
        assertEquals( 1, mgr.getFromProperty( two.getPath(), PROPERTY_REFERS_TO ).length );
        assertEquals( 0, mgr.getFromProperty( two.getPath(), PROPERTY_REFERRED_BY ).length );
        assertEquals( 0, mgr.getFromProperty( three.getPath(), PROPERTY_REFERS_TO ).length );
        assertEquals( 2, mgr.getFromProperty( three.getPath(), PROPERTY_REFERRED_BY ).length );
        
        // Remove the link from 1 to 3
        mgr.removeReferral( one.getUUID(), three.getUUID() );
        cm.getCurrentSession().save();
        assertEquals( 0, mgr.getFromProperty( one.getPath(), PROPERTY_REFERS_TO ).length );
        assertEquals( 0, mgr.getFromProperty( one.getPath(), PROPERTY_REFERRED_BY ).length );
        assertEquals( 1, mgr.getFromProperty( two.getPath(), PROPERTY_REFERS_TO ).length );
        assertEquals( 0, mgr.getFromProperty( two.getPath(), PROPERTY_REFERRED_BY ).length );
        assertEquals( 0, mgr.getFromProperty( three.getPath(), PROPERTY_REFERS_TO ).length );
        assertEquals( 1, mgr.getFromProperty( three.getPath(), PROPERTY_REFERRED_BY ).length );

        // Remove the link from 2 to 3
        mgr.removeReferral( two.getUUID(), three.getUUID() );
        cm.getCurrentSession().save();
        assertEquals( 0, mgr.getFromProperty( one.getPath(), PROPERTY_REFERS_TO ).length );
        assertEquals( 0, mgr.getFromProperty( one.getPath(), PROPERTY_REFERRED_BY ).length );
        assertEquals( 0, mgr.getFromProperty( two.getPath(), PROPERTY_REFERS_TO ).length );
        assertEquals( 0, mgr.getFromProperty( two.getPath(), PROPERTY_REFERRED_BY ).length );
        assertEquals( 0, mgr.getFromProperty( three.getPath(), PROPERTY_REFERS_TO ).length );
        assertEquals( 0, mgr.getFromProperty( three.getPath(), PROPERTY_REFERRED_BY ).length );
    }

    /**
     * Tests low-level method for adding to a multi-valued JCR Node property.
     * @throws Exception
     */
    public void testAddToProperty() throws Exception
    {
        ContentManager cm = engine.getContentManager();
        String jcrPath = "/TestAddToProperty";
        Node node;
        Property prop;
        Session s = cm.getCurrentSession();
        
        mgr.addToProperty( jcrPath, "foo","Value1" );
        node = cm.getJCRNode( jcrPath );
        prop = node.getProperty( "foo" );
        assertNotNull( prop.getValues() );
        assertEquals( 1, prop.getValues().length );
        assertEquals( "Value1", prop.getValues()[0].getString() );
        
        mgr.addToProperty( jcrPath, "foo","Value2" );
        node = cm.getJCRNode( jcrPath );
        prop = node.getProperty( "foo" );
        assertNotNull( prop.getValues() );
        assertEquals( 2, prop.getValues().length );
        assertEquals( "Value2", prop.getValues()[1].getString() );
        
        // Add the same Value1 again; should not be added
        mgr.addToProperty( jcrPath, "foo","Value1" );
        node = cm.getJCRNode( jcrPath );
        prop = node.getProperty( "foo" );
        assertNotNull( prop.getValues() );
        assertEquals( 2, prop.getValues().length );
        assertEquals( "Value1", prop.getValues()[0].getString() );

        // Clean up
        node.remove();
        s.save();
    }

    public void testBecomesUnreferenced() throws Exception
    {
        engine.saveText( "Foobar2", "[TestPage]" );

        List<WikiPath> c = mgr.findUnreferenced();
        assertEquals( 0, c.size() );

        engine.saveText( "Foobar2", "norefs" );
        c = mgr.findUnreferenced();
        assertEquals( 1, c.size() );
        assertEquals( WikiPath.valueOf( "Main:TestPage" ), c.get( 0 ) );
    }

    public void testCircularRefs() throws Exception
    {
        engine.saveText( "Foobar2", "ref to [TestPage]" );

        assertEquals( 0, mgr.findUncreated().size() );
        assertEquals( 0, mgr.findUnreferenced().size() );
    }

    public void testDeletePage() throws Exception
    {
        Collection<WikiPath> c = mgr.getReferredBy( WikiPath.valueOf( "Foobar2" ));
        assertEquals( 1, c.size() );
        assertTrue( c.contains( WikiPath.valueOf( "Foobar" ) ) );

        engine.deletePage( "Foobar" );
        c = mgr.getReferredBy( WikiPath.valueOf("Foobar2" ));
        assertEquals( 0, c.size() );

        engine.saveText( "Foobar", "[Foobar2]" );
        c = mgr.getReferredBy( WikiPath.valueOf( "Foobar2" ));
        assertEquals( 1, c.size() );
        assertTrue( c.contains( WikiPath.valueOf( "Foobar" ) ) );
    }
    
    public void testExtractLinks() throws Exception
    {
        String src = "Foobar. [Foobar].  Frobozz.  [This is a link].";
        engine.deletePage( "Test" );
        engine.saveText( "Test", src );
        JCRWikiPage page = (JCRWikiPage)engine.getPage( "Test" );
        List<String> results = mgr.extractLinks( page );
        WikiPathResolver cache = WikiPathResolver.getInstance( engine.getContentManager() );

        assertEquals( 2, results.size() );
        assertEquals( "item 0", PATH_FOOBAR, cache.getByUUID( results.get( 0 ) ) );
        assertEquals( "item 1", WikiPath.valueOf( "Main:This is a link" ), cache.getByUUID( results.get( 1 ) ) );
    }

    public void testGetReferredBy() throws Exception
    {
        List<WikiPath> c = mgr.getReferredBy( WikiPath.valueOf( "TestPage" ));
        assertEquals( 0, c.size() );

        c = mgr.getReferredBy( WikiPath.valueOf( "Foobar" ));
        assertEquals( 2, c.size() );

        c = mgr.getReferredBy( WikiPath.valueOf( "Foobar2" ));
        assertEquals( 1, c.size() );
        assertEquals( WikiPath.valueOf( "Foobar" ), c.get( 0 ) );

        // The singular 'Foobar' exists, but this variant does not
        c = mgr.getReferredBy( WikiPath.valueOf( "Foobars" ));
        assertEquals( 0, c.size() );
    }

    public void testGetUUID() throws Exception
    {
        WikiPath path = WikiPath.valueOf( "TestPage" );
        JCRWikiPage page;
        Node node;
        String uuid;
        WikiPathResolver cache = WikiPathResolver.getInstance( engine.getContentManager() );
        
        // Save a page and verify that it has its JCR path is in the "pages" branch
        engine.deletePage( "TestPage" );
        engine.saveText( "TestPage", "This page exists." );
        page = engine.getContentManager().getPage( path );
        assertNotNull( page );
        assertNotNull( page.getJCRNode() );
        assertEquals( path, page.getPath() );
        assertEquals( "/pages/main/testpage", page.getJCRNode().getPath() );
        uuid = page.getJCRNode().getUUID();
        
        assertEquals( uuid, cache.getUUID( path ) );
        
        // Now, verify that a non-existent page is in the "not created" branch
        WikiPath fakePath = WikiPath.valueOf( "NotCreatedPage" );
        assertFalse( engine.pageExists( "NotCreatedPage" ) );
        try
        {
            uuid = cache.getUUID( fakePath );
        }
        catch ( ItemNotFoundException e )
        {
            // Good! That is what we expect. Now try gain with "safe" method
        }
        uuid = mgr.getSafeNodeByUUID( fakePath );
        Session session = engine.getContentManager().getCurrentSession();
        node = session.getNodeByUUID( uuid );
        assertEquals( "/wiki:references/wiki:notCreated/main/notcreatedpage", node.getPath() );
    }
    
    public void testGetByUUID() throws Exception
    {
        WikiPath path = WikiPath.valueOf( "TestPage" );
        JCRWikiPage page;
        Node node;
        String uuid;
        WikiPathResolver cache = WikiPathResolver.getInstance( engine.getContentManager() );
        
        // Verify we can retrieve a saved page by its UUID
        engine.deletePage( "TestPage" );
        engine.saveText( "TestPage", "This page exists." );
        page = engine.getContentManager().getPage( path );
        assertNotNull( page );
        node = page.getJCRNode();
        assertNotNull( node );
        uuid = page.getJCRNode().getUUID();
        assertEquals( path, cache.getByUUID( uuid ) );
        
        // Now, verify we can retrieve a non-existent page by its UUID also
        WikiPath fakePath = WikiPath.valueOf( "NotCreatedPage" );
        assertFalse( engine.pageExists( "NotCreatedPage" ) );
        try
        {
            uuid = cache.getUUID( fakePath );
        }
        catch ( ItemNotFoundException e )
        {
            // Good! That is what we expect. Now try gain with "safe" method
        }
        uuid = mgr.getSafeNodeByUUID( fakePath );
        assertNotNull( uuid );
        assertEquals( fakePath, cache.getByUUID( uuid ) );
    }

    public void testGetRefersTo() throws Exception
    {
        List<WikiPath> links;

        links = mgr.getRefersTo( WikiPath.valueOf( "TestPage" ) );
        assertEquals( 1, links.size() );
        assertTrue( "Does not have Foobar, but it should have", links.contains( WikiPath.valueOf( "Foobar" ) ) );

        links = mgr.getRefersTo( WikiPath.valueOf( "Foobar" ) );
        assertEquals( 2, links.size() );
        assertTrue( "Does not have Foobar, but it should have", links.contains( WikiPath.valueOf( "Foobar" ) ) );
        assertTrue( "Does not have Foobar2, but it should have", links.contains( WikiPath.valueOf( "Foobar2" ) ) );

        links = mgr.getRefersTo( WikiPath.valueOf( "Foobar2" ) );
        assertEquals( 0, links.size() );
    }

    /**
     * Tests the link-pattern matcher in ReferenceManager, used for changing references.
     * We test combinations of attachments, anchors and sub-pages.
     */
    public void testLinkPatternPage()
    {
        assertTrue( ReferenceManager.LINK_PATTERN.matcher( "Some [FooBar] text" ).find() );
        assertTrue( ReferenceManager.LINK_PATTERN.matcher( "Some [Foo/Bar] text" ).find() );
        assertTrue( ReferenceManager.LINK_PATTERN.matcher( "Some [Foo Bar] text" ).find() );
        assertTrue( ReferenceManager.LINK_PATTERN.matcher( "Some [FooBar#anchor] text" ).find() );
        assertTrue( ReferenceManager.LINK_PATTERN.matcher( "Some [linktext|FooBar] text" ).find() );
        assertTrue( ReferenceManager.LINK_PATTERN.matcher( "Some [linktext|FooBar#anchor] text" ).find() );
        assertTrue( ReferenceManager.LINK_PATTERN.matcher( "Some [FooBar/foo.txt] text" ).find() );
        assertTrue( ReferenceManager.LINK_PATTERN.matcher( "Some [linktext|FooBar/bar.jpg] text" ).find() );
        assertTrue( ReferenceManager.LINK_PATTERN.matcher( "Some [linktext|FooBar#anchor/bar.jpg] text" ).find() );
        assertTrue( ReferenceManager.LINK_PATTERN.matcher( "Some [linktext|Foo/Bar#anchor/bar.jpg] text" ).find() );
        assertFalse( ReferenceManager.LINK_PATTERN.matcher( "Some linktext|FooBar#anchor/bar.jpg] text" ).find() );
    }

    public void testNonExistant1() throws Exception
    {
        Collection<WikiPath> c = mgr.getReferredBy( WikiPath.valueOf( "Foobar2" ) );

        assertTrue( c.size() == 1 && c.contains( WikiPath.valueOf( "Foobar" ) ) );
    }

    public void testNonExistant2() throws ProviderException
    {
        Collection<WikiPath> c = mgr.getReferredBy( WikiPath.valueOf( "TestBug" ));

        assertTrue( c.size() == 0 );
    }

    public void testPluralSingularUpdate() throws Exception
    {
        engine.saveText( "BugOne", "NewBug" );
        engine.saveText( "NewBugs", "foo" );
        engine.saveText( "OpenBugs", "bar" );

        engine.saveText( "BugOne", "OpenBug" );

        Collection<WikiPath> links = mgr.getReferredBy( WikiPath.valueOf( "NewBug" ));
        assertEquals( "newbugs", 0, links.size() ); // No referrers must be found

        links = mgr.getReferredBy( WikiPath.valueOf( "NewBug" ));
        assertEquals( "newbug", 0, links.size() ); // No referrers must be found

        links = mgr.getReferredBy( WikiPath.valueOf( "OpenBugs" ));
        assertEquals( "openbugs", 1, links.size() );
        assertEquals( "openbugs2", "Main:BugOne", links.iterator().next().toString() );
    }

    public void testRebuild() throws Exception
    {
        ContentManager cm = engine.getContentManager();
        Node node = cm.getJCRNode( ReferenceManager.REFERENCES_ROOT );
        assertNotNull( node );
        assertNotSame( 0, node.getNodes().getSize() );
        mgr.rebuild();
        
        // Should see just 2 children of REFERENCES_ROOT
        node = cm.getJCRNode( ReferenceManager.REFERENCES_ROOT );
        assertNotNull( node );
        assertEquals( 2, node.getNodes().getSize() );
        
        // Make sure all of the unreferenced references got deleted
        node = cm.getJCRNode( ReferenceManager.NOT_REFERENCED );
        assertNotNull( node );
        assertEquals( 0, node.getNodes().getSize() );
        
        // Make sure the not-created references got deleted
        node = cm.getJCRNode( ReferenceManager.NOT_REFERENCED );
        assertNotNull( node );
        assertEquals( 0, node.getNodes().getSize() );
    }

    /**
     * Tests low-level method for removing items from a multi-valued JCR Node property.
     * @throws Exception
     */
    public void testRemoveFromProperty() throws Exception
    {
        ContentManager cm = engine.getContentManager();
        String jcrPath = "/TestRemoveFromProperty";
        Node node;
        Property prop;
        Session s = cm.getCurrentSession();
        
        mgr.addToProperty( jcrPath, "foo","Value1" );
        mgr.addToProperty( jcrPath, "foo","Value2" );
        node = cm.getJCRNode( jcrPath );
        prop = node.getProperty( "foo" );
        assertNotNull( prop.getValues() );
        assertEquals( 2, prop.getValues().length );

        // Remove the first value
        mgr.removeFromProperty( jcrPath, "foo", "Value1" );
        node = cm.getJCRNode( jcrPath );
        prop = node.getProperty( "foo" );
        assertNotNull( prop.getValues() );
        assertEquals( 1, prop.getValues().length );
        assertEquals( "Value2", prop.getValues()[0].getString() );
        
        // Try removing a value that does not exist in the property
        mgr.removeFromProperty( jcrPath, "foo", "NonExistentValue" );
        node = cm.getJCRNode( jcrPath );
        prop = node.getProperty( "foo" );
        assertNotNull( prop.getValues() );
        assertEquals( 1, prop.getValues().length );
        assertEquals( "Value2", prop.getValues()[0].getString() );
        
        // Remove the last value
        mgr.removeFromProperty( jcrPath, "foo", "Value2" );
        node = cm.getJCRNode( jcrPath );
        try
        {
            prop = node.getProperty( "foo" );
        }
        catch ( PathNotFoundException e )
        {
            // Good! This is what we expect.
        }
        
        // Add back in the first value, twice
        mgr.addToProperty( jcrPath, "foo","Value1" );
        mgr.addToProperty( jcrPath, "foo","Value1" );
        node = cm.getJCRNode( jcrPath );
        prop = node.getProperty( "foo" );
        assertNotNull( prop.getValues() );
        assertEquals( 1, prop.getValues().length );
        
        // Remove the first value -- ALL should be gone now
        mgr.removeFromProperty( jcrPath, "foo", "Value1" );
        node = cm.getJCRNode( jcrPath );
        try
        {
            prop = node.getProperty( "foo" );
        }
        catch ( PathNotFoundException e )
        {
            // Good! This is what we expect.
        }
        
        // Clean up
        node.remove();
        s.save();
    }

    public void testSelf() throws Exception
    {
        engine.saveText( "BugOne", "BugOne" );
        Collection<WikiPath> ref = mgr.getReferredBy( WikiPath.valueOf( "BugOne" ));
        assertEquals( "wrong size", 1, ref.size() );
        assertEquals( "ref", "Main:BugOne", ref.iterator().next().toString() );
    }
    
    /**
     * Should fail in 2.2.14-beta
     * 
     * @throws Exception
     */
    public void testSingularReferences() throws Exception
    {
        engine.saveText( "RandomPage", "FatalBugs" );
        engine.saveText( "FatalBugs", "<foo>" );
        engine.saveText( "BugCommentPreviewDeletesAllComments", "FatalBug" );

        Collection<WikiPath> c = mgr.getReferredBy( WikiPath.valueOf( "FatalBugs" ));

        assertEquals( "FatalBugs referrers number", 2, c.size() );
    }
    

    public void testUncreated() throws Exception
    {
        List<WikiPath> c = mgr.findUncreated();
        assertEquals( 1, c.size());
        assertEquals( WikiPath.valueOf( "Main:Foobar2" ), c.get( 0 ) );
    }

    public void testUnreferenced() throws Exception
    {
        List<WikiPath> c = mgr.findUnreferenced();
        assertEquals( 1, c.size() );
        assertEquals( WikiPath.valueOf( "Main:TestPage" ), c.get( 0 ) );
    }

    public void testPluralExists() throws Exception
    {
        engine.saveText( "Foobars", "qwertz" );
        Collection<WikiPath> c = mgr.getReferredBy( WikiPath.valueOf( "Foobars" ));
        assertEquals( 0, c.size() );
    }

    public void testPluralExists2() throws Exception
    {
        engine.saveText( "Foobars", "qwertz" );
        engine.saveText( "TestPage", "Reference to [Foobar], [Foobars]." );

        Collection<WikiPath> c = mgr.getReferredBy( WikiPath.valueOf( "Foobars" ));
        assertEquals( 1, c.size() );
        assertTrue( c.contains( WikiPath.valueOf( "TestPage" ) ) );
    }

    /**
     * Opposite to testUpdatePluralOnlyRef(). Is a page with plural form
     * recognized as the page referenced by a singular link.
     */

    public void testUpdateFoobar2s() throws Exception
    {
        engine.saveText( "Foobar2s", "qwertz" );
        List<WikiPath> c = mgr.findUncreated();
        assertEquals( 1, c.size() );
        assertTrue( c.contains( WikiPath.valueOf( "Main:Foobar2" ) ) );

        Collection<WikiPath> w = mgr.getReferredBy( WikiPath.valueOf( "Foobar2s" ));
        assertEquals( 0, w.size() );
    }

    /**
     * Is a page recognized as referenced if only plural form links exist.
     */

    // NB: Unfortunately, cleaning out self-references in the case there's
    // a plural and a singular form of the page becomes nigh impossible, so we
    // just don't do it.
    public void testUpdatePluralOnlyRef() throws Exception
    {
        engine.saveText( "TestPage", "Reference to [Foobars]." );
        List<WikiPath> c = mgr.findUnreferenced();
        assertEquals( 1, c.size() );
        assertEquals( WikiPath.valueOf( "Main:TestPage" ), c.get( 0 ) );

        Collection<WikiPath> p = mgr.getReferredBy( WikiPath.valueOf( "Foobar" ));
        assertEquals( 2, p.size() );
    }

    /**
     * A placeholder for methods that try to reproduce the Unicode issues plaguing the
     * current build. So far, this does not produce the desired side-effects.
     * @throws Exception
     */
    public void testPropertyStress() throws Exception
    {
        String jcrPath = "/PropertyStress";
        
        String scandic = "\ufffditiSy\ufffd\ufffdljy\ufffd";        
        mgr.addToProperty( jcrPath, "foo", scandic );
        
        for ( int i = 0; i < 10; i++ )
        {
            mgr.addToProperty( jcrPath, "foo", scandic );
        }
    }

    
}
