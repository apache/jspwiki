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

import java.util.*;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.Session;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.wiki.TestEngine;
import org.apache.wiki.api.WikiException;
import org.apache.wiki.content.ContentManager;
import org.apache.wiki.content.ReferenceManager;
import org.apache.wiki.content.WikiPath;
import org.apache.wiki.providers.ProviderException;

/**
 * @author Torsten Hildebrandt.
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

        engine.shutdown();
    }

    /**
     * Tests protected method
     * {@link ReferenceManager#addReferredBy(WikiPath, List)}, which sets inbound
     * links to a page from multiple sources. The destination page exists.
     * 
     * @throws Exception
     */
    public void testAddReferredBy() throws Exception
    {
        WikiPath source = WikiPath.valueOf( "SetReferredBy" );
        WikiPath destination1 = WikiPath.valueOf( "PageOne" );
        WikiPath destination2 = WikiPath.valueOf( "PageTwo" );
        WikiPath destination3 = WikiPath.valueOf( "PageThree" );
        
        List<WikiPath> destinations = new ArrayList<WikiPath>();
        destinations.add( WikiPath.valueOf( "PageOne" ) );
        destinations.add( WikiPath.valueOf( "PageTwo" ) );
        destinations.add( WikiPath.valueOf( "PageThree" ) );
        Session s = engine.getContentManager().getCurrentSession();
        for ( WikiPath destination : destinations )
        {
            mgr.addReferredBy( destination, source );
        }
        s.save();

        List<WikiPath> links = mgr.getReferredBy( source );
        assertEquals( 0, links.size() );
        
        links = mgr.getReferredBy( destination1 );
        assertEquals( 1, links.size() );
        assertTrue( links.contains(  source ) );
               
        links = mgr.getReferredBy( destination2 );
        assertEquals( 1, links.size() );
        assertTrue( links.contains(  source ) );
        
        links = mgr.getReferredBy( destination3 );
        assertEquals( 1, links.size() );
        assertTrue( links.contains(  source ) );
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
        
        mgr.addToProperty( jcrPath, "foo","Value1", true );
        s.save();
        node = cm.getJCRNode( jcrPath );
        prop = node.getProperty( "foo" );
        assertNotNull( prop.getValues() );
        assertEquals( 1, prop.getValues().length );
        assertEquals( "Value1", prop.getValues()[0].getString() );
        
        mgr.addToProperty( jcrPath, "foo","Value2", true );
        s.save();
        node = cm.getJCRNode( jcrPath );
        prop = node.getProperty( "foo" );
        assertNotNull( prop.getValues() );
        assertEquals( 2, prop.getValues().length );
        assertEquals( "Value2", prop.getValues()[1].getString() );
        
        // Add the same Value1 again!
        mgr.addToProperty( jcrPath, "foo","Value1", true );
        s.save();
        node = cm.getJCRNode( jcrPath );
        prop = node.getProperty( "foo" );
        assertNotNull( prop.getValues() );
        assertEquals( 3, prop.getValues().length );
        assertEquals( "Value1", prop.getValues()[2].getString() );
        
        // Add the same Value1 again, without 'addAgain' flag
        mgr.addToProperty( jcrPath, "foo","Value1", false );
        s.save();
        node = cm.getJCRNode( jcrPath );
        prop = node.getProperty( "foo" );
        assertNotNull( prop.getValues() );
        assertEquals( 3, prop.getValues().length );
        assertEquals( "Value1", prop.getValues()[2].getString() );
        
        // Clean up
        node = s.getRootNode().getNode( jcrPath );
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
        List<WikiPath> results = mgr.extractLinks( WikiPath.valueOf( "Test" ) );

        assertEquals( 2, results.size() );
        assertEquals( "item 0", PATH_FOOBAR, results.get( 0 ) );
        assertEquals( "item 1", WikiPath.valueOf( "Main:This is a link" ), results.get( 1 ) );
    }

    public void testGetReferredBy() throws Exception
    {
        //engine.saveText( "TestPage", "Reference to [Foobar]." );
        //engine.saveText( "Foobar", "Reference to [Foobar2], [Foobars], [Foobar]" );
        
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
        Node node = (Node)cm.getJCRNode( ReferenceManager.REFERENCES_ROOT );
        assertNotNull( node );
        assertNotSame( 0, node.getNodes().getSize() );
        mgr.rebuild();
        
        // Should see just 3 children of REFERENCES_ROOT
        node = (Node)cm.getJCRNode( ReferenceManager.REFERENCES_ROOT );
        assertNotNull( node );
        assertEquals( 3, node.getNodes().getSize() );
        
        // Make sure all of the inbound references got deleted
        node = (Node)cm.getJCRNode( ReferenceManager.REFERRED_BY );
        assertNotNull( node );
        assertEquals( 0, node.getNodes().getSize() );
        
        // Make sure all of the uncreated references got deleted
        node = (Node)cm.getJCRNode( ReferenceManager.NOT_CREATED );
        assertNotNull( node );
        assertEquals( 0, node.getNodes().getSize() );
        
        // Make sure all of the unreferenced references got deleted
        node = (Node)cm.getJCRNode( ReferenceManager.NOT_REFERENCED );
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
        
        mgr.addToProperty( jcrPath, "foo","Value1", true );
        mgr.addToProperty( jcrPath, "foo","Value2", true );
        s.save();
        node = cm.getJCRNode( jcrPath );
        prop = node.getProperty( "foo" );
        assertNotNull( prop.getValues() );
        assertEquals( 2, prop.getValues().length );

        // Remove the first value
        mgr.removeFromProperty( jcrPath, "foo", "Value1" );
        s.save();
        node = cm.getJCRNode( jcrPath );
        prop = node.getProperty( "foo" );
        assertNotNull( prop.getValues() );
        assertEquals( 1, prop.getValues().length );
        assertEquals( "Value2", prop.getValues()[0].getString() );
        
        // Try removing a value that does not exist in the property
        mgr.removeFromProperty( jcrPath, "foo", "NonExistentValue" );
        s.save();
        node = cm.getJCRNode( jcrPath );
        prop = node.getProperty( "foo" );
        assertNotNull( prop.getValues() );
        assertEquals( 1, prop.getValues().length );
        assertEquals( "Value2", prop.getValues()[0].getString() );
        
        // Remove the last value
        mgr.removeFromProperty( jcrPath, "foo", "Value2" );
        s.save();
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
        mgr.addToProperty( jcrPath, "foo","Value1", true );
        mgr.addToProperty( jcrPath, "foo","Value1", true );
        s.save();
        node = cm.getJCRNode( jcrPath );
        prop = node.getProperty( "foo" );
        assertNotNull( prop.getValues() );
        assertEquals( 2, prop.getValues().length );
        
        // Remove the first value -- ALL should be gone now
        mgr.removeFromProperty( jcrPath, "foo", "Value1" );
        s.save();
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
        node = s.getRootNode().getNode( jcrPath );
        node.remove();
        s.save();
    }

    /**
     * Tests protected method {@link ReferenceManager#removeLinks(WikiPath)},
     * which removes all outbound links from a page to multiple destinations,
     * and removes all inbound links to the page as well. The source page
     * exists.
     * 
     * @throws Exception
     */
    public void testRemoveLinks() throws Exception
    {
        // Set up some test pages
        engine.saveText( "RemoveLinks", "Test page." );
        engine.saveText( "Destination1", "Test page." );
        engine.saveText( "Destination2", "Test page." );
        engine.saveText( "Destination3", "Test page." );

        // Set source-->dest1,2,3,4
        WikiPath source = WikiPath.valueOf( "RemoveLinks" );
        List<WikiPath> destinations = new ArrayList<WikiPath>();
        WikiPath destination1 = WikiPath.valueOf( "Destination1" );
        WikiPath destination2 = WikiPath.valueOf( "Destination2" );
        WikiPath destination3 = WikiPath.valueOf( "Destination3" );
        WikiPath destination4 = WikiPath.valueOf( "Destination4" );
        destinations.add( destination1 );
        destinations.add( destination2 );
        destinations.add( destination3 );
        destinations.add( destination4 );
        mgr.setLinks( source, destinations );
        engine.getContentManager().getCurrentSession().save();
        
        // We should see four outbound links from source-->dest1,2,3,4
        assertEquals( 4, mgr.getRefersTo( source ).size() );
        assertEquals( 0, mgr.getRefersTo( destination1 ).size() );
        assertEquals( 0, mgr.getRefersTo( destination2 ).size() );
        assertEquals( 0, mgr.getRefersTo( destination3 ).size() );
        assertEquals( 0, mgr.getRefersTo( destination4 ).size() );
        
        // We should see four inbound links dest1,2,3,4<--source
        assertEquals( 0, mgr.getReferredBy( source ).size() );
        assertEquals( 1, mgr.getReferredBy( destination1 ).size() );
        assertEquals( 1, mgr.getReferredBy( destination2 ).size() );
        assertEquals( 1, mgr.getReferredBy( destination3 ).size() );
        assertEquals( 1, mgr.getReferredBy( destination4 ).size() );
        
        // Now, remove all links from the source to dest1,2,3, and all inbound links too
        mgr.removeLinks( source );
        engine.getContentManager().getCurrentSession().save();
        assertEquals( 0, mgr.getRefersTo( source ).size() );
        assertEquals( 0, mgr.getReferredBy( source ).size() );
        assertEquals( 0, mgr.getReferredBy( destination1 ).size() );
        assertEquals( 0, mgr.getReferredBy( destination2 ).size() );
        assertEquals( 0, mgr.getReferredBy( destination3 ).size() );
        assertEquals( 0, mgr.getReferredBy( destination4 ).size() );
        assertEquals( 0, mgr.getRefersTo( destination1 ).size() );
        assertEquals( 0, mgr.getRefersTo( destination2 ).size() );
        assertEquals( 0, mgr.getRefersTo( destination3 ).size() );
        assertEquals( 0, mgr.getRefersTo( destination4 ).size() );
    }

    public void testSelf() throws WikiException
    {
        engine.saveText( "BugOne", "BugOne" );
        Collection<WikiPath> ref = mgr.getReferredBy( WikiPath.valueOf( "BugOne" ));
        assertEquals( "wrong size", 1, ref.size() );
        assertEquals( "ref", "Main:BugOne", ref.iterator().next().toString() );
    }

    /**
     * Tests protected method {@link ReferenceManager#setLinks(WikiPath, List)},
     * which sets bi-directional links from a source page to multiple
     * destinations, and vice-versa. The source and destination pages exist.
     * 
     * @throws Exception
     */
    public void testSetLinks() throws Exception
    {
        // Set up some test pages
        engine.saveText( "SetLinks", "Test page." );
        engine.saveText( "Destination1", "Test page." );
        engine.saveText( "Destination2", "Test page." );
        engine.saveText( "Destination3", "Test page." );

        // Set source-->dest1,2,3
        WikiPath source = WikiPath.valueOf( "SetLinks" );
        List<WikiPath> destinations = new ArrayList<WikiPath>();
        WikiPath destination1 = WikiPath.valueOf( "Destination1" );
        WikiPath destination2 = WikiPath.valueOf( "Destination2" );
        WikiPath destination3 = WikiPath.valueOf( "Destination3" );
        destinations.add( destination1 );
        destinations.add( destination2 );
        destinations.add( destination3 );
        mgr.setLinks( source, destinations );
        engine.getContentManager().getCurrentSession().save();

        // We should see three outbound links from source-->dest1,2,3
        List<WikiPath> links;
        links = mgr.getRefersTo( source );
        assertEquals( 3, links.size() );
        assertTrue( links.contains( destination1 ) );
        assertTrue( links.contains( destination2 ) );
        assertTrue( links.contains( destination3 ) );
        
        // Each dest1,2,3 should NOT have created an inbound link to source
        assertEquals( 0, mgr.getReferredBy( source ).size() );

        // Each of the destination pages should have 1 inbound link from source
        links = mgr.getReferredBy( destination1 );
        assertEquals( 1, links.size() );
        assertTrue( links.contains( source ) );

        links = mgr.getReferredBy( destination2 );
        assertEquals( 1, links.size() );
        assertTrue( links.contains( source ) );

        links = mgr.getReferredBy( destination3 );
        assertEquals( 1, links.size() );
        assertTrue( links.contains( source ) );
    }
    
    /**
     * Tests protected method
     * {@link ReferenceManager#setRefersTo(WikiPath, List)}, which sets
     * outbound links from a page to multiple destinatiions. The source page
     * exists.
     * 
     * @throws Exception
     */
    public void testSetRefersTo() throws Exception
    {
        WikiPath source = WikiPath.valueOf( "TestPage" );
        List<WikiPath> destinations = new ArrayList<WikiPath>();
        destinations.add( WikiPath.valueOf( "PageOne" ) );
        destinations.add( WikiPath.valueOf( "PageTwo" ) );
        destinations.add( WikiPath.valueOf( "PageThree" ) );
        mgr.setRefersTo( source, destinations );
        engine.getContentManager().getCurrentSession().save();

        List<WikiPath> links = mgr.getRefersTo( source );
        assertEquals( 3, links.size() );
        assertTrue( links.contains( WikiPath.valueOf( "PageOne" ) ) );
        assertTrue( links.contains( WikiPath.valueOf( "PageTwo" ) ) );
        assertTrue( links.contains( WikiPath.valueOf( "PageThree" ) ) );
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
        
        String scandic = "ÄitiSyöÖljyä";        
        mgr.addToProperty( jcrPath, "foo",scandic, false );
        
        for ( int i = 0; i < 10; i++ )
        {
            mgr.addToProperty( jcrPath, "foo",scandic, false );
        }
    }

    
}