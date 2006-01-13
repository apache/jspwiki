package com.ecyrd.jspwiki.web;

import java.io.IOException;

import junit.framework.TestCase;
import net.sourceforge.jwebunit.WebTester;

import com.meterware.httpunit.WebResponse;

public abstract class CommonTests extends TestCase
{
    protected static final String PASSWORD = "secreto";
    protected WebTester t;
    protected final String m_baseURL;

    public CommonTests( String name, String baseURL )
    {
        super( name );
        t = new WebTester();
        t.getTestContext().setBaseUrl( baseURL );
        m_baseURL = baseURL;
    }

    public void setUp()
    {
    }

    public void testAnonymousCreateGroup()
    {
        // Try to create a group; we should get redirected to login page
        t.beginAt( "/NewGroup.jsp" );
        t.assertFormNotPresent( "newGroup" );
        t.assertTextPresent( "Please sign in" );
    }
    
    public void testAnonymousView()
    {
        // Start at main, then navigate to About; verify user not logged in
        t.beginAt( "/Wiki.jsp?page=Main" );
        t.assertTextPresent( "You have successfully installed" );
        t.assertLinkPresentWithText( "About" );
        t.clickLinkWithText( "About" );
        t.assertTextPresent( "This Wiki is done using" );
    }
    
    public void testAnonymousViewImage() throws IOException
    {
        // See if we can view the JSPWiki logo
        t.beginAt( "/images/jspwiki_logo_s.png" );
        WebResponse r = t.getDialog().getResponse();
        assertEquals( 200, r.getResponseCode() );
        assertFalse( r.getText().length() == 0 );
    }
    
    public void testAssertedName()
    {
        // Navigate to Prefs page; set user cookie
        t.beginAt( "/Wiki.jsp?page=Main" );
        t.assertTextNotPresent( "G'day" );
        t.clickLinkWithText( "My Prefs" );
        t.assertTextPresent( "Set your user preferences here." );
        t.assertFormPresent( "setCookie" );
        t.setWorkingForm( "setCookie" );
        t.assertFormNotPresent( "clearCookie" );
        t.assertFormElementPresent( "assertedName" );
        t.assertSubmitButtonPresent( "ok" );
        t.setFormElement( "assertedName", "Don Quixote" );
        t.submit( "ok" );
        t.assertTextPresent( "G'day" );
        t.assertTextPresent( "Don Quixote" );
        t.assertTextPresent( "(not logged in)" );

        // Clear user cookie
        t.clickLinkWithText( "My Prefs" );
        t.assertFormNotPresent( "setCookie" );
        t.setWorkingForm( "clearCookie" );
        t.assertFormPresent( "clearCookie" );
        t.assertSubmitButtonPresent( "ok" );
        t.submit( "ok" );
        t.assertTextNotPresent( "G'day" );
        t.assertTextNotPresent( "Don Quixote" );
        t.assertTextNotPresent( "(not logged in)" );
    }
    
    public void testCreateGroupFullName()
    {
        createGroup( "Janne Jalkanen" );
    }
    
    public void testCreateGroupLoginName()
    {
        createGroup( "janne" );
    }

    public void testCreateGroupWikiName()
    {
        createGroup( "JanneJalkanen" );
    }
    
    public void testLogin()
    {
        // Start at front page; try to log in
        t.beginAt( "/Wiki.jsp?page=Main" );
        t.assertTextNotPresent( "G'day" );
        t.clickLinkWithText( "Log in" );
        t.assertTextPresent( "Please sign in" );
        t.assertFormPresent( "login" );
        t.setWorkingForm( "login" );
        t.assertFormElementPresent( "j_username" );
        t.assertFormElementPresent( "j_password" );
        t.assertSubmitButtonPresent( "action" );
        t.setFormElement( "j_username", "janne" );
        t.setFormElement( "j_password", "myP@5sw0rd" );
        t.submit( "action" );
        t.assertTextNotPresent( "Please sign in" );
        t.assertTextPresent( "G'day" );
        t.assertTextPresent( "Janne" ); // This is a hack: detecting full
                                            // name isn't working (?)
        t.assertTextPresent( "(authenticated)" );
    }

    public void testLogout()
    {
        // Start at front page; try to log in
        t.beginAt( "/Login.jsp" );
        t.setWorkingForm( "login" );
        t.setFormElement( "j_username", "janne" );
        t.setFormElement( "j_password", "myP@5sw0rd" );
        t.submit( "action" );
        t.assertTextNotPresent( "Please sign in" );
        t.assertTextPresent( "G'day" );
        t.assertTextPresent( "(authenticated)" );
        
        // Log out; we should still be asserted
        t.clickLinkWithText( "Log out" );
        t.assertTextPresent( "G'day" );
        t.assertTextNotPresent( "(authenticated)" );
        t.assertTextPresent( "(not logged in)" );
        
        // Clear cookies; we should be anonymous again
        t.clickLinkWithText( "My Prefs" );
        t.setWorkingForm( "clearCookie" );
        t.submit( "ok" );
        t.assertTextNotPresent( "G'day" );
        t.assertTextNotPresent( "(authenticated)" );
        t.assertTextNotPresent( "(not logged in)" );
    }
    
    protected void createGroup( String members ) 
    {
        login();
        String group = "Test" + String.valueOf( System.currentTimeMillis() );
        t.beginAt( "/NewGroup.jsp" );
        t.setWorkingForm( "newGroup" );
        t.setFormElement( "name", group );
        t.setFormElement( "members", members );
        t.submit( "ok" );
        
        // Verify the group was created 
        t.assertTextNotPresent( "Could not create group" );
        t.gotoPage("/Wiki.jsp?page=Group" + group );
        t.assertTextPresent( "This is a wiki group." );
        
        // Verifiy that anonymous users can't view the page
        t = new WebTester();
        t.getTestContext().setBaseUrl( m_baseURL );
        t.beginAt( "/Wiki.jsp" );
        t.gotoPage("/Wiki.jsp?page=Group" + group );
        t.assertTextPresent( "Please sign in" );
        
        // Log in again and verify we can read it
        login();
        t.gotoPage("/Wiki.jsp?page=Group" + group );
        t.assertTextPresent( "This is a wiki group." );
    }
    
    protected void createProfile()
    {
        createProfile( true );
    }
    
    
    protected void createProfile(boolean withPassword)
    {
        // Navigate to profile tab
        t.beginAt( "/Wiki.jsp?page=Main" );
        t.assertTextNotPresent( "G'day" );
        t.clickLinkWithText( "My Prefs" );
        t.assertFormPresent( "editProfile" );
        t.setWorkingForm( "editProfile" );
        t.assertFormElementPresent( "loginname" );
        t.assertSubmitButtonPresent( "ok" );

        // Create user profile with generated user name
        String suffix = generateSuffix();
        t.setFormElement( "loginname", "pvilla" + suffix );
        t.setFormElement( "wikiname", "PanchoVilla" + suffix );
        t.setFormElement( "fullname", "Pancho Villa" + suffix );
        t.setFormElement( "email", "pvilla@bandito.org" );
        if ( withPassword )
        {
            t.assertFormElementPresent( "password" );
            t.assertFormElementPresent( "password2" );
            t.setFormElement( "password", PASSWORD + suffix );
            t.setFormElement( "password2", PASSWORD + suffix );
        }
        t.submit( "ok" );
    }
    
    protected String generateSuffix()
    {
        return String.valueOf( System.currentTimeMillis() );
    }
    
    public void login() 
    {
        // Start at front page; try to log in
        t.beginAt( "/Wiki.jsp?page=Main" );
        t.clickLinkWithText( "Log in" );
        t.setWorkingForm( "login" );
        t.setFormElement( "j_username", "janne" );
        t.setFormElement( "j_password", "myP@5sw0rd" );
        t.submit( "action" );
    }
    
    /**
     * Generates and registers a user with a unique name.
     * @return the generated account name
     */
    protected String newUser()
    {
        String baseURL = t.getTestContext().getBaseUrl();
        String suffix = generateSuffix();
        String user = "pvilla" + suffix;
        t.beginAt( "/UserPreferences.jsp" );
        t.setFormElement( "loginname", user );
        t.setFormElement( "password", PASSWORD );
        t.setFormElement( "password2", PASSWORD );
        t.setFormElement( "wikiname", "PanchoVilla" + suffix );
        t.setFormElement( "fullname", "Pancho Villa" + suffix );
        t.setFormElement( "email", "pvilla@bandito.org" );
        t.submit( "ok" );
        t.assertTextNotPresent( "Could not save profile" );
        t = new WebTester();
        t.getTestContext().setBaseUrl( baseURL );
        return user;
    }
    
}