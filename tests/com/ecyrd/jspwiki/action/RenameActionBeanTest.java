package com.ecyrd.jspwiki.action;

import java.util.Properties;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.sourceforge.stripes.mock.MockRoundtrip;
import net.sourceforge.stripes.validation.ValidationErrors;

import com.ecyrd.jspwiki.TestEngine;
import com.ecyrd.jspwiki.WikiPage;
import com.ecyrd.jspwiki.auth.Users;

public class RenameActionBeanTest extends TestCase
{
    TestEngine m_engine;
    
    public void setUp()
    {
        // Start the WikiEngine, and stash reference
        Properties props = new Properties();
        try 
        {
            TestEngine.emptyWorkDir();
            props.load( TestEngine.findTestProperties() );
            m_engine = new TestEngine( props );
        }
        catch (Exception e)
        {
            throw new RuntimeException("Could not set up TestEngine: " + e.getMessage());
        }
    }
    
    public void testValidation() throws Exception {
        // Save test page
        m_engine.saveText("Test", "This is a test.");
        WikiPage page = m_engine.getPage("Test");
        assertNotNull("Did not save page Test!", page);
        
        MockRoundtrip trip;
        ValidationErrors errors;
        
        // Try renaming without 'page' or 'renameTo' params; should see 2 validation errors
        trip = m_engine.authenticatedTrip( Users.ADMIN,Users.ADMIN_PASS, RenameActionBean.class );
        trip.execute("rename");
        errors = trip.getValidationErrors();
        assertEquals( 2, errors.size() );
        assertTrue ( errors.containsKey( "page" ) );
        assertTrue ( errors.containsKey( "renameTo" ) );
        
        // Try again, with 'page' param but 'renameTo'; should see 1 validation error
        trip = m_engine.authenticatedTrip( Users.ADMIN,Users.ADMIN_PASS, RenameActionBean.class );
        trip.setParameter("page", "Test");
        trip.execute("rename");
        RenameActionBean bean = trip.getActionBean(RenameActionBean.class);
        assertEquals( page, bean.getPage() );
        errors = trip.getValidationErrors();
        assertEquals( 1, errors.size() );
        assertTrue( errors.containsKey("renameTo") );
        assertEquals(MockRoundtrip.DEFAULT_SOURCE_PAGE, trip.getDestination() );
    
        // Delete test page
        m_engine.deletePage( "Test" );
    }
    
    public void testValidationWithCollision() throws Exception {
        // Save 2 test pages
        m_engine.saveText("Test", "This is a test.");
        WikiPage page = m_engine.getPage("Test");
        assertNotNull("Did not save page Test!", page );
        m_engine.saveText("TestCollision", "This is a second test page.");
        WikiPage collisionPage = m_engine.getPage("TestCollision");
        assertNotNull("Did not save page TestCollision!", collisionPage );
        
        MockRoundtrip trip;
        ValidationErrors errors;
        
        // Try renaming to 'TestCollision'; should see 1 validation error
        trip = m_engine.authenticatedTrip( Users.ADMIN,Users.ADMIN_PASS, RenameActionBean.class );
        trip.setParameter("page", "Test");
        trip.setParameter("renameTo", "TestCollision");
        trip.execute("rename");
        errors = trip.getValidationErrors();
        assertEquals( 1, errors.size() );
        assertTrue ( errors.containsKey( "renameTo" ) );
        assertEquals(MockRoundtrip.DEFAULT_SOURCE_PAGE, trip.getDestination() );
    
        // Delete test page
        m_engine.deletePage( "Test" );
        m_engine.deletePage( "TestCollision" );
    }
    
    public void testRename() throws Exception {
        // Save test page
        m_engine.saveText("Test", "This is a test.");
        WikiPage page = m_engine.getPage("Test");
        assertNotNull("Did not save page Test!", page );
        
        MockRoundtrip trip;
        ValidationErrors errors;
        
        // Try renaming to 'TestRenamed'; should save without error
        trip = m_engine.authenticatedTrip( Users.ADMIN,Users.ADMIN_PASS, RenameActionBean.class );
        trip.setParameter("page", "Test");
        trip.setParameter("renameTo", "TestRenamed");
        trip.execute("rename");
        errors = trip.getValidationErrors();
        assertEquals( 0, errors.size() );
        assertEquals( "/Wiki.action?page=TestRenamed", trip.getDestination() );
        assertFalse( m_engine.pageExists( "Test" ) );
        assertTrue( m_engine.pageExists( "TestRenamed" ) );
    
        // Delete test page
        m_engine.deletePage( "TestRenamed" );
    }
    
    public void testRenameReferences() throws Exception {
        // Save test page and referring page
        m_engine.saveText("Test", "This is a test.");
        WikiPage page = m_engine.getPage("Test");
        assertNotNull("Did not save page Test!", page );
        m_engine.saveText("ReferstoTest", "This page refers to [Test].\n");
        WikiPage referringPage = m_engine.getPage("ReferstoTest");
        assertNotNull("Did not save page ReferstoTest!", referringPage );
        assertNotNull( m_engine.getReferenceManager().findReferrers("Test") );
        assertEquals( 1, m_engine.getReferenceManager().findReferrers("Test").size() );
        
        MockRoundtrip trip;
        ValidationErrors errors;
        String referringText;
        
        // Try renaming to 'TestRenamed' with missing 'changeReferences' param. Referee should not change.
        trip = m_engine.authenticatedTrip( Users.ADMIN,Users.ADMIN_PASS, RenameActionBean.class );
        trip.setParameter("page", "Test");
        trip.setParameter("renameTo", "TestRenamed");
        trip.execute("rename");
        errors = trip.getValidationErrors();
        assertEquals( 0, errors.size() );
        assertEquals( "/Wiki.action?page=TestRenamed", trip.getDestination() );
        assertFalse( m_engine.pageExists( "Test" ) );
        assertTrue( m_engine.pageExists( "TestRenamed" ) );
        referringText = m_engine.getPureText( m_engine.getPage("ReferstoTest") );
        assertFalse( referringText.contains("[TestRenamed]"));
        m_engine.deletePage( "TestRenamed" );
    }
    
    public void testRenameReferencesChangeRefsFalse() throws Exception {
        MockRoundtrip trip;
        ValidationErrors errors;
        String referringText;
        
        // Try renaming to 'TestRenamed' with 'changeReferences' = 'false'. Referee should not change.
        m_engine.saveText("Test", "This is a test.");
        m_engine.saveText("ReferstoTest", "This page refers to [Test].");
        trip = m_engine.authenticatedTrip( Users.ADMIN,Users.ADMIN_PASS, RenameActionBean.class );
        trip.setParameter("page", "Test");
        trip.setParameter("renameTo", "TestRenamed");
        trip.setParameter("changeReferences", "false");
        trip.execute("rename");
         errors = trip.getValidationErrors();
        assertEquals( 0, errors.size() );
        assertEquals( "/Wiki.action?page=TestRenamed", trip.getDestination() );
        assertFalse( m_engine.pageExists( "Test" ) );
        assertTrue( m_engine.pageExists( "TestRenamed" ) );
        referringText = m_engine.getPureText( m_engine.getPage("ReferstoTest") );
        assertFalse( referringText.contains("[TestRenamed]"));
        m_engine.deletePage( "TestRenamed" );
    }
        
    public void testRenameReferencesChangeRefsTrue() throws Exception {
        MockRoundtrip trip;
        ValidationErrors errors;
        String referringText;
        
        // Try renaming to 'TestRenamed' with 'changeReferences' = 'true'. NOW, referee should change!
        m_engine.saveText("Test", "This is a test.");
        m_engine.saveText("ReferstoTest", "This page refers to [Test].");
        trip = m_engine.authenticatedTrip( Users.ADMIN,Users.ADMIN_PASS, RenameActionBean.class );
        trip.setParameter("page", "Test");
        trip.setParameter("renameTo", "TestRenamed");
        trip.setParameter("changeReferences", "true");
        trip.execute("rename");
        errors = trip.getValidationErrors();
        assertEquals( 0, errors.size() );
        assertEquals( "/Wiki.action?page=TestRenamed", trip.getDestination() );
        assertFalse( m_engine.pageExists( "Test" ) );
        assertTrue( m_engine.pageExists( "TestRenamed" ) );
        referringText = m_engine.getPureText( m_engine.getPage("ReferstoTest") );
        assertTrue( referringText.contains("[TestRenamed]"));
        m_engine.deletePage( "TestRenamed" );
        
        // Clean up
        m_engine.deletePage( "ReferstoTest" );
    }
    
    public static Test suite()
    {
        return new TestSuite( RenameActionBeanTest.class );
    }
}
