package com.ecyrd.jspwiki.web;

public class ContainerTest extends CommonContainerTests
{
    protected static final String USER = "pancho";

    public ContainerTest( String s )
    {
        super( s, "http://localhost:8080/test-container/" );
    }

    public void testCreateProfile()
    {
        String loginname = "pvilla";
        String fullname = "Pancho Villa";
        String suffix = generateSuffix();
        
        // Start at the front page
        t.gotoPage( "/Wiki.jsp?page=Main" );
        t.assertTextPresent( "G&#8217;day <br />(anonymous guest)" );
        
        // Navigate to the profile page
        t.clickLinkWithText( "My Prefs" );
        t.assertFormPresent( "editProfile" );
        t.setWorkingForm( "editProfile" );
        t.assertSubmitButtonPresent( "ok" );
        
        // We should NOT see a loginname field, OR a password field
        // and a warning message advising the user to log in
        t.assertFormElementNotPresent( "loginname" );
        t.assertFormElementNotPresent( "password" );
        t.assertTextPresent( "You cannot set your login name because you are not logged in yet." );

        // Try creating user profile with generated user name
        t.setFormElement( "fullname", fullname + suffix );
        t.setFormElement( "email", loginname + suffix + "@bandito.org" );
        t.submit( "ok" );
        
        // We should NOT be able to create profile, because we're not logged in
        t.assertTextPresent( "You must log in before creating a profile." );
        
        // OK, very nice. We've shown that you CANNOT create a profile unless there's a container ID already.
        // Now, let's try doing it with an existing account (but, we will stop just short of creating the profile)
        
        // Log in first!
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
        
        // Great. Now, let's try creating a profile
        t.clickLinkWithText( "My Prefs" );
        t.assertFormPresent( "editProfile" );
        t.setWorkingForm( "editProfile" );
        t.assertSubmitButtonPresent( "ok" );
        
        // We should NOT see a loginname field or a password field,
        // and we should see a warning message that the ID can't be changed because it's a container ID
        t.assertFormElementNotPresent( "loginname" );
        t.assertFormElementNotPresent( "password" );
        t.assertTextPresent( "You cannot set your login name because your credentials are managed by the web container" );
    }

    /**
     *This test is a no-op because container-managed login IDs can't be renamed.
     */
    public void testRenameProfile() { }
}
