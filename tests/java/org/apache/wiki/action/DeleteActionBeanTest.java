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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.sourceforge.stripes.mock.MockRoundtrip;
import net.sourceforge.stripes.validation.ValidationErrors;

import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiProvider;
import org.apache.wiki.api.WikiPage;
import org.apache.wiki.auth.Users;

public class DeleteActionBeanTest extends TestCase
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
        catch( Exception e )
        {
            throw new RuntimeException( "Could not set up TestEngine: " + e.getMessage() );
        }
    }

    public void tearDown() throws Exception
    {
        // Delete test page
        m_engine.deletePage( "Test" );
        m_engine.deletePage( "TestDeleteAttachment" );
        m_engine.shutdown();
    }

/*
    public void testDeleteAttachment() throws Exception
    {
        // Re-initialized the WikiEngine with default test managers
        Properties props = new Properties();
        TestEngine.emptyWorkDir();
        props.load( TestEngine.findTestProperties() );
        props.setProperty( PageManager.PROP_USECACHE, "false" );
        m_engine = new TestEngine( props );
        
        // Create wiki page
        String pageName = "TestDeleteAttachment";
        m_engine.saveText( pageName, "This is a test." );
        WikiPage page = m_engine.getPage( "TestDeleteAttachment" );
        
        // Create attachment file
        String attachContents = "ABCDEFGHIJKLMNOPQRSTUVWxyz";
        File attachFile = File.createTempFile("Attach",".txt");
        FileWriter out = new FileWriter( attachFile );
        out.write( attachContents );
        out.close();
        
        // Store attachment
        Attachment att = new Attachment( m_engine, pageName, attachFile.getName() );
        AttachmentManager mgr = m_engine.getAttachmentManager();
        att.setAuthor( "AttachmentAuthor" );
        mgr.storeAttachment( att, attachFile );

        // Make sure it was saved
        WikiPage att2 = mgr.getAttachmentInfo( m_engine.getWikiContextFactory().newViewContext( page ), attachFile.getName() );
        assertNotNull( "Attachment disappeared! Is the AttachmentManager running?", att2 );
        
        // Now, delete the page
        MockRoundtrip trip;
        ValidationErrors errors;

        // Try deleting the attachment without specifying a version (==all pages)
        trip = m_engine.authenticatedTrip( Users.ADMIN, Users.ADMIN_PASS, DeleteActionBean.class );
        trip.setParameter( "page", att.getName() );
        trip.execute( "delete" );
        errors = trip.getValidationErrors();
        assertEquals( 0, errors.size() );

        // Verify that we deleted the attachment but not the page
        att2 = mgr.getAttachmentInfo( m_engine.getWikiContextFactory().newViewContext( page ), attachFile.getName() );
        assertNull( "Attachment wasn't removed!", att2 );
        assertTrue( m_engine.pageExists( pageName ) );
    }
*/
    public void testDeleteAllVersions() throws Exception
    {
        // Save two versions of the test page
        m_engine.saveText( "Test", "This is the first version" );
        m_engine.saveText( "Test", "This is the second version" );

        // Make sure they both saved ok
        WikiPage v1 = m_engine.getPage( "Test", 1 );
        WikiPage v2 = m_engine.getPage( "Test", 2 );
        assertNotNull( "Did not save page Test, v1!", v1 );
        assertNotNull( "Did not save page Test, v2!", v2 );
        assertEquals( "This is the first version", m_engine.getPureText( v1 ).trim() );
        assertEquals( "This is the second version", m_engine.getPureText( v2 ).trim() );

        MockRoundtrip trip;
        ValidationErrors errors;

        // Try deleting the page without specifying a version (==all pages)
        trip = m_engine.authenticatedTrip( Users.ADMIN, Users.ADMIN_PASS, DeleteActionBean.class );
        trip.setParameter( "page", "Test" );
        trip.execute( "delete" );
        errors = trip.getValidationErrors();
        assertEquals( 0, errors.size() );

        // Verify that we deleted all the pages
        assertFalse( m_engine.pageExists( "Test" ) );
    }

    public void testDeleteVersion() throws Exception
    {
        // Save two versions of the test page
        m_engine.saveText( "Test", "This is the first version" );
        m_engine.saveText( "Test", "This is the second version" );

        // Make sure they both saved ok
        WikiPage v1 = m_engine.getPage( "Test", 1 );
        WikiPage v2 = m_engine.getPage( "Test", 2 );
        assertNotNull( "Did not save page Test, v1!", v1 );
        assertNotNull( "Did not save page Test, v2!", v2 );
        assertEquals( "This is the first version", m_engine.getPureText( v1 ).trim() );
        assertEquals( "This is the second version", m_engine.getPureText( v2 ).trim() );

        MockRoundtrip trip;
        ValidationErrors errors;

        // Try deleting one version
        trip = m_engine.authenticatedTrip( Users.ADMIN, Users.ADMIN_PASS, DeleteActionBean.class );
        trip.setParameter( "page", "Test" );
        trip.setParameter( "version", "1" );
        trip.execute( "delete" );
        errors = trip.getValidationErrors();
        assertEquals( 0, errors.size() );

        // Verify that there is only one version left
        assertTrue( m_engine.pageExists( "Test", WikiProvider.LATEST_VERSION ) );
        assertFalse( m_engine.pageExists( "Test", 1 ) );
    }

    public void testValidation() throws Exception
    {
        // Save two versions of the test page
        m_engine.saveText( "Test", "This is the first version" );
        m_engine.saveText( "Test", "This is the second version" );

        // Make sure they both saved ok
        WikiPage v1 = m_engine.getPage( "Test", 1 );
        WikiPage v2 = m_engine.getPage( "Test", 2 );
        assertNotNull( "Did not save page Test, v1!", v1 );
        assertNotNull( "Did not save page Test, v2!", v2 );
        assertEquals( "This is the first version", m_engine.getPureText( v1 ).trim() );
        assertEquals( "This is the second version", m_engine.getPureText( v2 ).trim() );

        MockRoundtrip trip;
        ValidationErrors errors;

        // Try deleting without 'page' param; should see 1 validation errors
        trip = m_engine.authenticatedTrip( Users.ADMIN, Users.ADMIN_PASS, DeleteActionBean.class );
        trip.execute( "delete" );
        errors = trip.getValidationErrors();
        assertEquals( 1, errors.size() );
        assertTrue( errors.containsKey( "page" ) );

        // Try again, with value 'page' param but invalid 'version'; should see
        // 1 validation error
        trip = m_engine.authenticatedTrip( Users.ADMIN, Users.ADMIN_PASS, DeleteActionBean.class );
        trip.setParameter( "page", "Test" );
        trip.setParameter( "version", "10000" );
        trip.execute( "delete" );
        errors = trip.getValidationErrors();
        assertEquals( 1, errors.size() );
        assertTrue( errors.containsKey( "version" ) );
    }

    public static Test suite()
    {
        return new TestSuite( DeleteActionBeanTest.class );
    }
}
