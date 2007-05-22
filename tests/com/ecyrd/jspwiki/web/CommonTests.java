package com.ecyrd.jspwiki.web;

import java.io.IOException;

import com.ecyrd.jspwiki.auth.login.CookieAssertionLoginModule;
import com.meterware.httpunit.WebResponse;

import junit.framework.TestCase;
import net.sourceforge.jwebunit.WebTester;

public abstract class CommonTests extends TestCase
{
    protected static final String TEST_PASSWORD = "myP@5sw0rd";
    protected static final String TEST_LOGINNAME = "janne";
    protected static final String TEST_FULLNAME = "Janne Jalkanen";
    protected static final String TEST_WIKINAME = "JanneJalkanen";
    protected WebTester t;
    protected final String m_baseURL;

    public CommonTests( String name, String baseURL )
    {
        super( name );
        m_baseURL = baseURL;
        newSession();
    }

    public void setUp()
    {
        newSession();
    }

    public void testAclJanneEdit()
    {
        // Log in as 'janne' and create page with him as editor
        newSession();
        login( TEST_LOGINNAME, TEST_PASSWORD );
        String page = "AclEditOnly" + System.currentTimeMillis();
        t.beginAt( "/Edit.jsp?page=" + page );
        t.setWorkingForm( "editForm" );
        String text = "[{ALLOW edit janne}]\n"
                    + "This page was created with an ACL by janne";
        t.setFormElement( "_editedtext", text );
        t.submit( "ok" );
        
        // Anonymous viewing should NOT succeed
        newSession();
        t.gotoPage( "/Wiki.jsp?page=" + page );
        t.assertTextPresent( "Please sign in" );
        
        // Anonymous editing should fail
        t.gotoPage( "/Edit.jsp?page=" + page );
        t.assertTextPresent( "Please sign in" );
        
        // Now log in as janne again and view/edit it successfully
        login( TEST_LOGINNAME, TEST_PASSWORD );
        t.gotoPage( "/Wiki.jsp?page=" + page );
        t.assertTextPresent( "This page was created with an ACL by janne" );
        t.gotoPage( "/Edit.jsp?page=" + page );
        t.assertFormPresent( "editForm" );
        t.setWorkingForm( "editForm" );
        t.assertSubmitButtonPresent( "ok" );
        t.assertFormElementPresent("_editedtext" );
    }
    
    public void testAclJanneEditAllView()
    {
        /** testCreatePage does all of the form validation tests */
        // Log in as 'janne' and create page with him as editor
        newSession();
        login( TEST_LOGINNAME, TEST_PASSWORD );
        String page = "AclViewAndEdit" + System.currentTimeMillis();
        t.beginAt( "/Edit.jsp?page=" + page );
        t.setWorkingForm( "editForm" );
        String text = "[{ALLOW edit janne}]\n"
                    + "[{ALLOW view All}]\n"
                    + "This page was created with an ACL by janne";
        t.setFormElement( "_editedtext", text );
        t.submit( "ok" );
        
        // Anonymous viewing should succeed
        newSession();
        t.gotoPage( "/Wiki.jsp?page=" + page );
        t.assertTextPresent( "This page was created with an ACL by janne" );
        
        // Anonymous editing should fail
        t.gotoPage( "/Edit.jsp?page=" + page );
        t.assertTextPresent( "Please sign in" );
        
        // Now log in as janne again and view/edit it successfully
        login( TEST_LOGINNAME, TEST_PASSWORD );
        t.gotoPage( "/Wiki.jsp?page=" + page );
        t.assertTextPresent( "This page was created with an ACL by janne" );
        t.gotoPage( "/Edit.jsp?page=" + page );
        t.assertFormPresent( "editForm" );
        t.setWorkingForm( "editForm" );
        t.assertSubmitButtonPresent( "ok" );
        t.assertFormElementPresent("_editedtext" );
    }
    
    public void testAnonymousCreateGroup()
    {
        // Try to create a group; we should get redirected to login page
        t.gotoPage( "/NewGroup.jsp" );
        t.assertTextPresent( "Please sign in" );
    }
    
    public void testAnonymousView()
    {
        // Start at main, then navigate to About; verify user not logged in
        t.gotoPage( "/Wiki.jsp?page=Main" );
        t.assertTextPresent( "You have successfully installed" );
        t.assertLinkPresentWithText( "About" );
        t.clickLinkWithText( "About" );
        t.assertTextPresent( "This Wiki is done using" );
    }
    
    public void testAnonymousViewImage() throws IOException
    {
        // See if we can view the JSPWiki logo
        t.gotoPage( "/images/jspwiki_logo_s.png" );
        WebResponse r = t.getDialog().getResponse();
        assertEquals( 200, r.getResponseCode() );
        assertFalse( r.getText().length() == 0 );
    }
    
    public void testAssertedName()
    {
        // Navigate to Prefs page; see the 'G'day message' for the anonymous user
        t.gotoPage( "/Wiki.jsp?page=Main" );
        t.assertTextPresent( "G&#8217;day <br />(anonymous guest)" );
        
        // Go to the UserPreferences page; see the set-cookie form, plus welcome text that invites user to set cookie
        t.gotoPage( "/UserPreferences.jsp" );
        t.assertTextPresent( "You wouldn&#8217;t lie to us would you?" );
        t.assertFormPresent( "setCookie" );
        
        // Set the cookie to our test user name
        t.setWorkingForm( "setCookie" );
        t.assertFormElementPresent( "assertedName" );
        t.assertSubmitButtonPresent( "ok" );
        t.setFormElement( "assertedName", "Don Quixote" );
        t.submit( "ok" );
        
        // Now navigate back to the main page; see the 'G'day message' for the test user
        t.assertTextPresent( "G&#8217;day" );
        t.assertTextPresent( "Don Quixote" );
        t.assertTextPresent( "(not logged in)" );
        String cookie = getCookie( CookieAssertionLoginModule.PREFS_COOKIE_NAME );
        assertNotNull( cookie );
        assertEquals( "Don+Quixote", cookie );

        // Clear user cookie
        t.gotoPage( "/UserPreferences.jsp" );
        t.assertFormPresent( "setCookie" );
        t.setWorkingForm( "clearCookie" );
        t.assertFormPresent( "clearCookie" );
        t.assertSubmitButtonPresent( "ok" );
        t.submit( "ok" );
        
        // Go back to the main page, and see the 'G'day message for the anonymous user again
        t.assertTextPresent( "G&#8217;day <br />(anonymous guest)" );
        t.assertTextNotPresent( "Don Quixote" );
        cookie = getCookie( CookieAssertionLoginModule.PREFS_COOKIE_NAME );
        assertEquals( "", cookie );
    }
    
    public void testAssertedPermissions( ) 
    {
        // Create new group with 'janne' and 'FredFlintstone' as members
        t.gotoPage( "/Wiki.jsp" );
        login( TEST_LOGINNAME, TEST_PASSWORD );
        String group = "AssertedPermissions" + String.valueOf( System.currentTimeMillis() );
        String members = TEST_LOGINNAME + " \n FredFlintstone";

        // First, create the group
        t.gotoPage( "/NewGroup.jsp" );
        t.assertTextPresent( "Create New Group" );
        t.assertFormPresent( "createGroup" );
        t.setWorkingForm( "createGroup" );
        t.assertFormElementPresent("group" );
        t.assertFormElementPresent("members" );
        t.assertSubmitButtonPresent( "ok" );
        t.setFormElement( "group", group );
        t.setFormElement( "members", members );
        t.submit( "ok" );
        
        // Verify the group was created 
        t.assertTextNotPresent( "Could not create group" );
        t.gotoPage("/Group.jsp?group=" + group );
        t.assertTextPresent( "This is the wiki group called" );
        
        // Verifiy that anonymous users can't view the group
        newSession();
        t.gotoPage("/Group.jsp?group=" + group );
        t.assertTextPresent( "Please sign in" );
        
        // Log in again and verify we can read it
        login( TEST_LOGINNAME, TEST_PASSWORD );
        t.gotoPage("/Group.jsp?group=" + group );
        t.assertTextPresent( "This is the wiki group called" );
        
        // Verify that asserted user 'Fred' can view the group but not edit
        newSession();
        t.gotoPage( "/Wiki.jsp?page=Main" );
        // brushed NOK -- link is hidden in dropdown
        //t.clickLinkWithText( "My Prefs" );
        t.gotoPage( "/UserPreferences.jsp" );
        t.setWorkingForm( "setCookie" );
        t.setFormElement( "assertedName", "FredFlintstone" );
        t.submit( "ok" );
        t.assertTextPresent( "G&#8217;day" );
        t.assertTextPresent( "FredFlintstone" );
        t.assertTextPresent( "(not logged in)" );
        t.gotoPage("/Group.jsp?group=" + group );
        t.assertTextPresent( "This is the wiki group called" );
        
        // Try to edit -- it should not be allowed
        t.gotoPage("/EditGroup.jsp?group=" + group );
        t.assertTextPresent( "Please sign in" );
    }
    
    public void testCreateGroupFullName()
    {
        createGroup( TEST_LOGINNAME, "Janne Jalkanen" );
    }
    
    public void testCreateGroupLoginName()
    {
        createGroup( TEST_LOGINNAME, TEST_LOGINNAME );
    }

    public void testCreateGroupWikiName()
    {
        createGroup( TEST_LOGINNAME, "JanneJalkanen" );
    }
    
    public void testCreatePage()
    {
        login( TEST_LOGINNAME, TEST_PASSWORD );
        String page = "CreatePage" + System.currentTimeMillis();
        t.gotoPage( "/Edit.jsp?page=" + page );
        t.assertFormPresent( "editForm" );
        t.setWorkingForm( "editForm" );
        t.assertSubmitButtonPresent( "ok" );
        t.assertFormElementPresent("_editedtext" );
        t.setFormElement( "_editedtext", "This page was created by the web unit tests." );
        t.submit( "ok" );
        t.assertTextPresent( "This page was created by the web unit tests." );
    }
    
    public void testLogin()
    {
        // Start at front page; try to log in
        t.gotoPage( "/Wiki.jsp?page=Main" );
        t.assertTextPresent( "G&#8217;day <br />(anonymous guest)" );
        t.clickLinkWithText( "Log in" );
        t.assertTextPresent( "Please sign in" );
        t.assertFormPresent( "login" );
        t.setWorkingForm( "login" );
        t.assertFormElementPresent( "j_username" );
        t.assertFormElementPresent( "j_password" );
        t.assertSubmitButtonPresent( "submitlogin" );
        t.setFormElement( "j_username", TEST_LOGINNAME );
        t.setFormElement( "j_password", TEST_PASSWORD );
        t.assertSubmitButtonPresent("submitlogin");
        t.submit( "submitlogin" );
        t.assertTextNotPresent( "Please sign in" );
        t.assertTextPresent( "G&#8217;day" );
        t.assertTextPresent( "Janne" ); // This is a hack: detecting full
                                            // name isn't working (?)
        t.assertTextPresent( "(authenticated)" );
    }

    public void testLogout()
    {
        // Start at front page; try to log in
        t.gotoPage( "/Login.jsp" );
        t.setWorkingForm( "login" );
        t.setFormElement( "j_username", TEST_LOGINNAME );
        t.setFormElement( "j_password", TEST_PASSWORD );
        t.submit( "submitlogin" );
        t.assertTextNotPresent( "Please sign in" );
        t.assertTextPresent( "G&#8217;day" );
        t.assertTextPresent( "(authenticated)" );
        String cookie = getCookie( CookieAssertionLoginModule.PREFS_COOKIE_NAME );
        assertNotNull( cookie );
        assertEquals( TEST_WIKINAME, cookie );
        
        // Log out; we should NOT see any asserted identities
        t.gotoPage( "/Logout.jsp" );
        t.assertTextPresent( "G&#8217;day <br />(anonymous guest)" );
        cookie = getCookie( CookieAssertionLoginModule.PREFS_COOKIE_NAME );
        assertEquals( "", cookie );
    }
    
    public void testRedirectPageAfterLogin()
    {
        // Create new page 
        login( TEST_LOGINNAME, TEST_PASSWORD );
        String page = "CreatePage" + System.currentTimeMillis();
        t.gotoPage( "/Edit.jsp?page=" + page );
        t.setWorkingForm( "editForm" );
        String redirectText = "We created this page to test redirects.";
        t.setFormElement( "_editedtext", "[{ALLOW view Authenticated}]\n" + redirectText );
        t.submit( "ok" );
        t.assertTextPresent( redirectText );
        
        // Now, from an anonymous session, try to view it, fail, then login
        newSession();
        t.gotoPage( "/Wiki.jsp?page=" + page );
        t.assertTextNotPresent( redirectText );
        t.assertFormPresent( "login" );
        t.assertFormElementPresent( "j_username" );
        t.assertFormElementPresent( "j_password" );
        t.setWorkingForm( "login" );
        t.setFormElement( "j_username", TEST_LOGINNAME );
        t.setFormElement( "j_password", TEST_PASSWORD );
        t.submit( "submitlogin" );

        // We should be able to see the page now
        t.assertTextPresent( redirectText );
    }
    
    public void testRenameProfile()
    {
        // Create a new user and group (and log in)
        String loginName = createProfile("TestRenameProfileUser", "TestRenameProfileUser");
        t.assertTextNotPresent( "Please sign in" );
        t.assertTextPresent( "G&#8217;day" );
        t.assertTextPresent( loginName );
        
        newSession();
        String group = createGroup( loginName, loginName );
        
        // Create a page with a view ACL restricted to the new user
        String page = "TestRenameProfilePage" + System.currentTimeMillis();
        t.beginAt( "/Edit.jsp?page=" + page );
        String text = "[{ALLOW edit " + loginName + "}]\nThis page was created with an ACL by " + loginName;
        t.setFormElement( "_editedtext", text );
        t.submit( "ok" );
        
        // Anonymous editing should fail
        newSession();
        t.gotoPage( "/Edit.jsp?page=" + page );
        t.assertTextPresent( "Please sign in" );
        
        // Now log in as the test user and view/edit it successfully
        login( loginName, TEST_PASSWORD );
        t.gotoPage( "/Wiki.jsp?page=" + page );
        t.assertTextPresent( "This page was created with an ACL by " + loginName );
        t.gotoPage( "/Edit.jsp?page=" + page );
        t.assertFormPresent( "editForm" );
        
        // Verify that our ACL test is present (note the extra linebreak at the end of the text
        t.setWorkingForm( "editForm" );
        t.assertSubmitButtonPresent( "ok" );
        t.assertFormElementPresent("_editedtext" );
        String response = t.getDialog().getResponseText();
        assertTrue( response.contains("[{ALLOW edit " + loginName + "}]" ) );
        
        // OK -- now that we've got a user, a protected page and a group  successfully set up, let's change the profile name
        t.gotoPage("/UserPreferences.jsp");
        t.setWorkingForm( "editProfile" );
        t.assertFormElementPresent( "loginname" );
        t.assertSubmitButtonPresent( "ok" );
        String newLoginName = "Renamed" + loginName;
        t.setFormElement( "loginname", newLoginName );
        t.setFormElement( "fullname", newLoginName );
        t.submit( "ok" );
        
        // Now, the main page should show the new authenticated user name
        t.assertTextNotPresent( "Please sign in" );
        t.assertTextPresent( "G&#8217;day" );
        t.assertTextPresent( newLoginName );
        
        // When we navigate to the protected page, the ACL should have the NEW name in it
        t.gotoPage( "/Edit.jsp?page=" + page );
        t.setWorkingForm( "editForm" );
        response = t.getDialog().getResponseText();
        assertTrue( response.contains("[{ALLOW edit " + newLoginName + "}]" ) );
        
        // Also, when we navigate to the group page, the group member should be the NEW name (we will see this inside a <td> element)
        t.gotoPage("/Group.jsp?group=" + group );
        t.assertTextNotPresent( "Please sign in" );
        response = t.getDialog().getResponseText();
        assertTrue( response.contains( "<td>" + newLoginName ));
    }
    
    protected String createGroup( String user, String members ) 
    {
        t.gotoPage( "/Wiki.jsp" );
        login( user, TEST_PASSWORD );
        t.assertTextNotPresent( "Please sign in" );
        t.assertTextPresent( "G&#8217;day" );
        t.assertTextPresent( "(authenticated)" );
        
        String group = "Test" + String.valueOf( System.currentTimeMillis() );
        
        // First, name the group
        t.gotoPage( "/NewGroup.jsp" );
        t.assertTextPresent( "Create New Group" );
        t.assertFormPresent( "createGroup" );
        t.setWorkingForm( "createGroup" );
        t.assertFormElementPresent("group" );
        t.assertFormElementPresent("members" );
        t.assertSubmitButtonPresent( "ok" );
        t.setFormElement( "group", group );
        t.setFormElement( "members", members );
        t.submit( "ok" );
        
        // Verify the group was created 
        t.assertTextNotPresent( "Could not create group" );
        t.gotoPage("/Group.jsp?group=" + group );
        t.assertTextPresent( "This is the wiki group called" );
        
        // Verifiy that anonymous users can't view the group
        newSession();
        t.gotoPage("/Group.jsp?group=" + group );
        t.assertTextPresent( "Please sign in" );
        
        // Log in again and verify we can read it
        login( user, TEST_PASSWORD );
        t.gotoPage("/Group.jsp?group=" + group );
        t.assertTextPresent( "This is the wiki group called" );
        
        // Try to edit -- it should be allowed
        t.gotoPage("/EditGroup.jsp?group=" + group );
        t.assertTextNotPresent( "Please sign in" );
        t.assertFormPresent( "editGroup" );
        
        return group;
    }
    
    protected String createProfile( String loginname, String fullname )
    {
        return createProfile( loginname, fullname, true );
    }
    
    
    protected String createProfile( String loginname, String fullname, boolean withPassword )
    {
        // Navigate to profile tab
        t.gotoPage( "/Wiki.jsp?page=Main" );
        t.assertTextPresent( "G&#8217;day <br />(anonymous guest)" );
        t.clickLinkWithText( "My Prefs" );
        t.assertFormPresent( "editProfile" );
        t.setWorkingForm( "editProfile" );
        t.assertFormElementPresent( "loginname" );
        t.assertSubmitButtonPresent( "ok" );

        // Create user profile with generated user name
        String suffix = generateSuffix();
        t.setFormElement( "loginname", loginname + suffix );
        t.setFormElement( "fullname", fullname + suffix );
        t.setFormElement( "email", loginname + suffix + "@bandito.org" );
        if ( withPassword )
        {
            t.assertFormElementPresent( "password" );
            t.assertFormElementPresent( "password2" );
            t.setFormElement( "password", TEST_PASSWORD  );
            t.setFormElement( "password2", TEST_PASSWORD );
        }
        t.submit( "ok" );
        return loginname + suffix;
    }
    
    protected String generateSuffix()
    {
        return String.valueOf( System.currentTimeMillis() );
    }
    
    protected void login( String user, String password ) 
    {
        // Start at front page; try to log in
        t.gotoPage( "/Wiki.jsp?page=Main" );
        t.clickLinkWithText( "Log in" );
        t.setWorkingForm( "login" );
        t.setFormElement( "j_username", user );
        t.setFormElement( "j_password", password );
        t.assertSubmitButtonPresent("submitlogin");
        t.submit( "submitlogin" );
    }
    
    protected String getCookie( String cookie )
    {
        return t.getTestContext().getWebClient().getCookieValue( cookie );
    }
    
    protected void newSession()
    {
        t = new WebTester();
        t.getTestContext().getWebClient().getClientProperties().setAutoRedirect( true );
        t.getTestContext().getWebClient().getClientProperties().setAcceptCookies( true );
        t.getTestContext().setBaseUrl( m_baseURL );
        t.beginAt( "/Wiki.jsp" );
    }
    
}