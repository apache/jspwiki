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
package org.apache.wiki.action;

import java.util.Properties;

import org.apache.wiki.TestEngine;
import org.apache.wiki.action.RenameActionBean;
import org.apache.wiki.api.WikiPage;
import org.apache.wiki.auth.Users;
import org.apache.wiki.content.WikiPath;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.sourceforge.stripes.mock.MockRoundtrip;
import net.sourceforge.stripes.validation.ValidationErrors;


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

    public void tearDown() throws Exception
    {
        try
        {
            m_engine.deletePage( "ReferstoTest" );
            m_engine.deletePage( "Test" );
            m_engine.deletePage( "TestCollision" );
            m_engine.deletePage( "TestRenamed" );
        }
        finally
        {
            m_engine.shutdown();
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
    }
    
    public void testRenameReferences() throws Exception {
        // Save test page and referring page
        m_engine.saveText("Test", "This is a test.");
        WikiPage page = m_engine.getPage("Test");
        assertNotNull("Did not save page Test!", page );
        m_engine.saveText("ReferstoTest", "This page refers to [Test].\n");
        WikiPage referringPage = m_engine.getPage("ReferstoTest");
        assertNotNull("Did not save page ReferstoTest!", referringPage );
        assertNotNull( m_engine.getReferenceManager().getReferredBy(WikiPath.valueOf("Test")) );
        assertEquals( 1, m_engine.getReferenceManager().getReferredBy(WikiPath.valueOf("Test")).size() );
        
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
    }
    
    public void testRenameToSameName() throws Exception {
        MockRoundtrip trip;
        ValidationErrors errors;
        
        // Try renaming to 'TestRenamed' to same name; should fail
        m_engine.saveText("Test", "This is a test.");
        trip = m_engine.authenticatedTrip( Users.ADMIN,Users.ADMIN_PASS, RenameActionBean.class );
        trip.setParameter("page", "Test");
        trip.setParameter("renameTo", "Test");
        trip.execute("rename");
        errors = trip.getValidationErrors();
        assertEquals( 1, errors.size() );
        m_engine.deletePage( "Test" );
    }
    
    public static Test suite()
    {
        return new TestSuite( RenameActionBeanTest.class );
    }
}
