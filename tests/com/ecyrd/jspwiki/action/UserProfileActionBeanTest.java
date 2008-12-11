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
package com.ecyrd.jspwiki.action;

import java.util.Properties;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.sourceforge.stripes.mock.MockHttpServletRequest;
import net.sourceforge.stripes.mock.MockRoundtrip;
import net.sourceforge.stripes.validation.ValidationErrors;

import com.ecyrd.jspwiki.TestEngine;
import com.ecyrd.jspwiki.WikiSession;
import com.ecyrd.jspwiki.auth.NoSuchPrincipalException;
import com.ecyrd.jspwiki.auth.user.UserDatabase;
import com.ecyrd.jspwiki.auth.user.UserProfile;

public class UserProfileActionBeanTest extends TestCase
{
    TestEngine m_engine;

    public void setUp()
    {
        // Start the WikiEngine, and stash reference
        Properties props = new Properties();
        try
        {
            props.load( TestEngine.findTestProperties() );
            m_engine = new TestEngine( props );
        }
        catch( Exception e )
        {
            throw new RuntimeException( "Could not set up TestEngine: " + e.getMessage() );
        }
    }

    public void testMissingParameters() throws Exception
    {
        MockRoundtrip trip;
        UserProfileActionBean bean;
        ValidationErrors errors;

        // Get the profile, but don't set any parameters; should fail with 3
        // errors
        // profile.fullname
        // profile.loginName
        // profile.passwordAgain
        trip = m_engine.guestTrip( UserProfileActionBean.class );
        trip.execute( "save" );
        bean = trip.getActionBean( UserProfileActionBean.class );
        // Check to make sure all our expected validation errors are here...
        errors = bean.getContext().getValidationErrors();
        assertEquals( 3, errors.size() );
        assertTrue( errors.containsKey( "profile.loginName" ) );
        assertTrue( errors.containsKey( "profile.fullname" ) );
        assertTrue( errors.containsKey( "profile.password" ) );
        // Validate that the bean values are set (or not!) as expected
        assertEquals( null, bean.getProfile().getLoginName() );
        assertEquals( null, bean.getProfile().getFullname() );
        assertEquals( null, bean.getProfile().getPassword() );
        assertEquals( null, bean.getPasswordAgain() );
        assertEquals( null, bean.getProfile().getEmail() );
        assertEquals( MockRoundtrip.DEFAULT_SOURCE_PAGE, trip.getDestination() );

        // Submit just the e-mail param
        trip = m_engine.guestTrip( "/UserProfile.action" );
        trip.setParameter( "profile.email", "fred@friendly.org" );
        trip.execute( "save" );
        bean = trip.getActionBean( UserProfileActionBean.class );
        errors = bean.getContext().getValidationErrors();
        // Check to make sure all our expected validation errors are here...
        errors = bean.getContext().getValidationErrors();
        assertEquals( 3, errors.size() );
        assertTrue( errors.containsKey( "profile.loginName" ) );
        assertTrue( errors.containsKey( "profile.fullname" ) );
        assertTrue( errors.containsKey( "profile.password" ) );
        // Validate that the bean values are set (or not!) as expected
        assertEquals( null, bean.getProfile().getLoginName() );
        assertEquals( null, bean.getProfile().getFullname() );
        assertEquals( null, bean.getProfile().getPassword() );
        assertEquals( null, bean.getPasswordAgain() );
        assertEquals( "fred@friendly.org", bean.getProfile().getEmail() );
        assertEquals( MockRoundtrip.DEFAULT_SOURCE_PAGE, trip.getDestination() );

        // Submit just the full name param
        trip = m_engine.guestTrip( "/UserProfile.action" );
        trip.setParameter( "profile.fullname", "Fred Friendly" );
        trip.execute( "save" );
        bean = trip.getActionBean( UserProfileActionBean.class );
        // Check to make sure all our expected validation errors are here...
        errors = bean.getContext().getValidationErrors();
        assertEquals( 2, errors.size() );
        assertTrue( errors.containsKey( "profile.loginName" ) );
        assertTrue( errors.containsKey( "profile.password" ) );
        // Validate that the bean values are set (or not!) as expected
        assertEquals( null, bean.getProfile().getLoginName() );
        assertEquals( "Fred Friendly", bean.getProfile().getFullname() );
        assertEquals( null, bean.getProfile().getPassword() );
        assertEquals( null, bean.getPasswordAgain() );
        assertEquals( null, bean.getProfile().getEmail() );
        assertEquals( MockRoundtrip.DEFAULT_SOURCE_PAGE, trip.getDestination() );

        // Submit just the login name param
        trip = m_engine.guestTrip( "/UserProfile.action" );
        trip.setParameter( "profile.loginName", "fred" );
        trip.execute( "save" );
        bean = trip.getActionBean( UserProfileActionBean.class );
        // Check to make sure all our expected validation errors are here...
        errors = bean.getContext().getValidationErrors();
        assertEquals( 2, errors.size() );
        assertTrue( errors.containsKey( "profile.fullname" ) );
        assertTrue( errors.containsKey( "profile.password" ) );
        // Validate that the bean values are set (or not!) as expected
        assertEquals( "fred", bean.getProfile().getLoginName() );
        assertEquals( null, bean.getProfile().getFullname() );
        assertEquals( null, bean.getProfile().getPassword() );
        assertEquals( null, bean.getPasswordAgain() );
        assertEquals( null, bean.getProfile().getEmail() );
        assertEquals( MockRoundtrip.DEFAULT_SOURCE_PAGE, trip.getDestination() );

        // Submit just the first password field
        trip = m_engine.guestTrip( "/UserProfile.action" );
        trip.setParameter( "profile.password", "myPassword" );
        trip.execute( "save" );
        bean = trip.getActionBean( UserProfileActionBean.class );
        // Check to make sure all our expected validation errors are here...
        errors = bean.getContext().getValidationErrors();
        assertEquals( 3, errors.size() );
        assertTrue( errors.containsKey( "profile.loginName" ) );
        assertTrue( errors.containsKey( "profile.fullname" ) );
        assertTrue( errors.containsKey( "profile.password" ) );
        // Validate that the bean values are set (or not!) as expected
        assertEquals( null, bean.getProfile().getLoginName() );
        assertEquals( null, bean.getProfile().getFullname() );
        assertEquals( "myPassword", bean.getProfile().getPassword() );
        assertEquals( null, bean.getPasswordAgain() );
        assertEquals( null, bean.getProfile().getEmail() );
        assertEquals( MockRoundtrip.DEFAULT_SOURCE_PAGE, trip.getDestination() );

        // Submit just the second password field
        trip = m_engine.guestTrip( "/UserProfile.action" );
        trip.setParameter( "passwordAgain", "myPassword" );
        trip.execute( "save" );
        bean = trip.getActionBean( UserProfileActionBean.class );
        // Check to make sure all our expected validation errors are here...
        errors = bean.getContext().getValidationErrors();
        assertEquals( 3, errors.size() );
        assertTrue( errors.containsKey( "profile.loginName" ) );
        assertTrue( errors.containsKey( "profile.fullname" ) );
        assertTrue( errors.containsKey( "profile.password" ) );
        // Validate that the bean values are set (or not!) as expected
        assertEquals( null, bean.getProfile().getLoginName() );
        assertEquals( null, bean.getProfile().getFullname() );
        assertEquals( null, bean.getProfile().getPassword() );
        assertEquals( "myPassword", bean.getPasswordAgain() );
        assertEquals( null, bean.getProfile().getEmail() );
        assertEquals( MockRoundtrip.DEFAULT_SOURCE_PAGE, trip.getDestination() );
    }

    public void testMismatchedPasswords() throws Exception
    {
        MockRoundtrip trip;
        UserProfileActionBean bean;
        ValidationErrors errors;

        // Set different passwords
        trip = m_engine.guestTrip( "/UserProfile.action" );
        trip.setParameter( "profile.loginName", "fred" );
        trip.setParameter( "profile.fullname", "Fred Friendly" );
        trip.setParameter( "profile.email", "fred@friendly.org" );
        trip.setParameter( "profile.password", "mypassword" );
        trip.setParameter( "passwordAgain", "Mypassword" );
        trip.execute( "save" );
        bean = trip.getActionBean( UserProfileActionBean.class );
        // Check to make sure all our expected validation errors are here...
        errors = bean.getContext().getValidationErrors();
        assertEquals( 1, errors.size() );
        assertTrue( errors.containsKey( "profile.password" ) );
        assertEquals( MockRoundtrip.DEFAULT_SOURCE_PAGE, trip.getDestination() );
    }

    public void testIllegalEmail() throws Exception
    {
        MockRoundtrip trip;
        UserProfileActionBean bean;
        ValidationErrors errors;

        // Set an illegal e-mail address
        trip = m_engine.guestTrip( "/UserProfile.action" );
        trip.setParameter( "profile.loginName", "fred" );
        trip.setParameter( "profile.fullname", "Fred Friendly" );
        trip.setParameter( "profile.email", "illegalEmail" );
        trip.setParameter( "profile.password", "mypassword" );
        trip.setParameter( "passwordAgain", "mypassword" );
        trip.execute( "save" );
        bean = trip.getActionBean( UserProfileActionBean.class );
        // Check to make sure all our expected validation errors are here...
        errors = bean.getContext().getValidationErrors();
        assertEquals( 1, errors.size() );
        assertTrue( errors.containsKey( "profile.email" ) );
        assertEquals( MockRoundtrip.DEFAULT_SOURCE_PAGE, trip.getDestination() );
    }

    public void testSaveProfile() throws Exception
    {
        MockRoundtrip trip;
        UserProfileActionBean bean;
        ValidationErrors errors;

        // Generate user ID and validate it doesn't exist already
        String suffix = String.valueOf( System.currentTimeMillis() );
        assertFalse( userExists( "user" + suffix ) );

        // Create new user
        trip = m_engine.guestTrip( "/UserProfile.action" );
        trip.setParameter( "profile.loginName", "user" + suffix );
        trip.setParameter( "profile.fullname", "Fred Friendly" + suffix );
        trip.setParameter( "profile.email", "fred@friendly.org" );
        trip.setParameter( "profile.password", "mypassword" );
        trip.setParameter( "passwordAgain", "mypassword" );
        trip.execute( "save" );
        bean = trip.getActionBean( UserProfileActionBean.class );
        errors = bean.getContext().getValidationErrors();
        // Check to make sure no validation errors here...
        assertEquals( 0, errors.size() );
        assertEquals( "/Wiki.action", trip.getDestination() );

        // Verify user was saved
        assertTrue( userExists( "user" + suffix ) );

        // Verify that wikiname and timestamps were set too
        UserDatabase db = m_engine.getUserManager().getUserDatabase();
        UserProfile profile = db.findByLoginName( "user" + suffix );
        assertEquals( "FredFriendly" + suffix, profile.getWikiName() );
        assertNotNull( profile.getCreated() );
        assertNotNull( profile.getLastModified() );

        // Delete the user we just created
        m_engine.getUserManager().getUserDatabase().deleteByLoginName( "user" + suffix );
    }

    public void testSaveProfileWithCollisions() throws Exception
    {
        MockRoundtrip trip;
        UserProfileActionBean bean;
        ValidationErrors errors;

        // Create user #1; save; verify it saved ok
        String suffix1 = String.valueOf( System.currentTimeMillis() );
        trip = m_engine.guestTrip( "/UserProfile.action" );
        trip.setParameter( "profile.loginName", "user" + suffix1 );
        trip.setParameter( "profile.fullname", "Fred Friendly" + suffix1 );
        trip.setParameter( "profile.email", "fred1@friendly.org" );
        trip.setParameter( "profile.password", "mypassword" );
        trip.setParameter( "passwordAgain", "mypassword" );
        trip.execute( "save" );
        bean = trip.getActionBean( UserProfileActionBean.class );
        // Check to make sure no validation errors here...
        errors = bean.getContext().getValidationErrors();
        assertEquals( 0, errors.size() );
        assertEquals( "/Wiki.action", trip.getDestination() );
        assertTrue( userExists( "user" + suffix1 ) );

        // Create user #2, but same loginName as #1; save; verify it did NOT
        // save
        // (because loginnames collided), and redirected back to .Action
        String suffix2 = String.valueOf( System.currentTimeMillis() );
        trip = m_engine.guestTrip( "/UserProfile.action" );
        trip.setParameter( "profile.loginName", "user" + suffix1 );
        trip.setParameter( "profile.fullname", "Fred Friendly" + suffix2 );
        trip.setParameter( "profile.email", "fred2@friendly.org" );
        trip.setParameter( "profile.password", "mypassword" );
        trip.setParameter( "passwordAgain", "mypassword" );
        trip.execute( "save" );
        bean = trip.getActionBean( UserProfileActionBean.class );
        // Check to make sure all our expected validation errors are here...
        errors = bean.getContext().getValidationErrors();
        assertEquals( 1, errors.size() );
        assertTrue( errors.containsKey( "profile.loginName" ) );
        assertEquals( MockRoundtrip.DEFAULT_SOURCE_PAGE, trip.getDestination() );
        assertFalse( userExists( "user" + suffix2 ) );

        // Create user #2, but same fullname as #1; save; verify it did NOT save
        // (because fullnames collided), and redirected back to .Action
        trip = m_engine.guestTrip( "/UserProfile.action" );
        trip.setParameter( "profile.loginName", "user" + suffix2 );
        trip.setParameter( "profile.fullname", "Fred Friendly" + suffix1 );
        trip.setParameter( "profile.email", "fred2@friendly.org" );
        trip.setParameter( "profile.password", "mypassword" );
        trip.setParameter( "passwordAgain", "mypassword" );
        trip.execute( "save" );
        bean = trip.getActionBean( UserProfileActionBean.class );
        // Check to make sure all our expected validation errors are here...
        errors = bean.getContext().getValidationErrors();
        assertEquals( 1, errors.size() );
        assertTrue( errors.containsKey( "profile.fullname" ) );
        assertEquals( MockRoundtrip.DEFAULT_SOURCE_PAGE, trip.getDestination() );
        assertFalse( userExists( "user" + suffix2 ) );

        // Delete the first user we just created
        m_engine.getUserManager().getUserDatabase().deleteByLoginName( "user" + suffix1 );
    }

    public void testSaveProfileAgain() throws Exception
    {
        MockRoundtrip trip;
        UserProfileActionBean bean;
        ValidationErrors errors;

        // Create user; save; verify it saved ok
        String suffix = String.valueOf( System.currentTimeMillis() );
        trip = m_engine.guestTrip( "/UserProfile.action" );
        trip.setParameter( "profile.loginName", "user" + suffix );
        trip.setParameter( "profile.fullname", "Fred Friendly" + suffix );
        trip.setParameter( "profile.email", "fred1@friendly.org" );
        trip.setParameter( "profile.password", "mypassword" );
        trip.setParameter( "passwordAgain", "mypassword" );
        trip.execute( "save" );
        bean = trip.getActionBean( UserProfileActionBean.class );
        errors = bean.getContext().getValidationErrors();
        assertEquals( 0, errors.size() );
        assertEquals( "/Wiki.action", trip.getDestination() );
        assertTrue( userExists( "user" + suffix ) );

        // Create new session and login as new user...
        trip = m_engine.guestTrip( "/UserProfile.action" );
        MockHttpServletRequest request = trip.getRequest();
        WikiSession wikiSession = WikiSession.getWikiSession( m_engine, request );
        boolean login = m_engine.getAuthenticationManager().login( wikiSession, "user" + suffix, "mypassword" );
        assertTrue( "Could not log in.", login );

        // Make sure the saved profile is loaded when we access prefs page
        trip.execute( "save" );
        bean = trip.getActionBean( UserProfileActionBean.class );
        assertEquals( "user" + suffix, bean.getProfile().getLoginName() );
        assertEquals( "Fred Friendly" + suffix, bean.getProfile().getFullname() );
        assertEquals( "fred1@friendly.org", bean.getProfile().getEmail() );

        // Now, create another session, and log in again....
        trip = m_engine.guestTrip( "/UserProfile.action" );
        request = trip.getRequest();
        wikiSession = WikiSession.getWikiSession( m_engine, request );
        login = m_engine.getAuthenticationManager().login( wikiSession, "user" + suffix, "mypassword" );
        assertTrue( "Could not lot in.", login );

        // Pass new values for the mutable fields (except the password).
        // The e-mails, loginname and fullname should all change
        trip.addParameter( "profile.loginName", "wilma" );
        trip.addParameter( "profile.fullname", "Wilma Flintstone" );
        trip.addParameter( "profile.email", "wilma@flintstone.org" );
        trip.execute( "save" );
        bean = trip.getActionBean( UserProfileActionBean.class );
        assertEquals( "wilma", bean.getProfile().getLoginName() );
        assertEquals( "Wilma Flintstone", bean.getProfile().getFullname() );
        assertEquals( "wilma@flintstone.org", bean.getProfile().getEmail() );
        assertNull( bean.getProfile().getPassword() );
        assertNull( bean.getPasswordAgain() );

        // Delete the user we just created
        m_engine.getUserManager().getUserDatabase().deleteByLoginName( "wilma" );
    }

    public static Test suite()
    {
        return new TestSuite( UserProfileActionBeanTest.class );
    }

    private boolean userExists( String name )
    {
        UserDatabase db = m_engine.getUserManager().getUserDatabase();
        boolean found = false;
        try
        {
            db.find( name );
            found = true;
        }
        catch( NoSuchPrincipalException e )
        {
            // Swallow
        }
        return found;
    }

}
