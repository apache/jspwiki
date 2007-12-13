package com.ecyrd.jspwiki.web;

import java.io.*;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.Properties;

import junit.framework.TestCase;

import com.ecyrd.jspwiki.auth.login.CookieAssertionLoginModule;

public abstract class CommonTests extends TestCase
{
    protected static final String TEST_PASSWORD = "myP@5sw0rd";
    protected static final String TEST_LOGINNAME = "janne";
    protected static final String TEST_FULLNAME = "Janne Jalkanen";
    protected static final String TEST_WIKINAME = "JanneJalkanen";
    protected final String m_baseURL;

    public CommonTests( String name, String uRL )
    {
        super( name );
        m_baseURL = getHostAndPort() + uRL;
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

        String text = "[{ALLOW edit janne}]\n"
                    + "This page was created with an ACL by janne";

        
        // Anonymous viewing should NOT succeed
        newSession();
        
        // Anonymous editing should fail
        
        // Now log in as janne again and view/edit it successfully
        login( TEST_LOGINNAME, TEST_PASSWORD );
        
    }
    
    public void testAclJanneEditAllView()
    {
        /** testCreatePage does all of the form validation tests */
        // Log in as 'janne' and create page with him as editor
        newSession();
        login( TEST_LOGINNAME, TEST_PASSWORD );
        String page = "AclViewAndEdit" + System.currentTimeMillis();
        
        String text = "[{ALLOW edit janne}]\n"
                    + "[{ALLOW view All}]\n"
                    + "This page was created with an ACL by janne";
        
        // Anonymous viewing should succeed
        newSession();
        
        // Anonymous editing should fail
        
        // Now log in as janne again and view/edit it successfully
        login( TEST_LOGINNAME, TEST_PASSWORD );

    }
    
    public void testAnonymousCreateGroup()
    {
        // Try to create a group; we should get redirected to login page
        
    }
    
    public void testAnonymousView()
    {
        // Start at main, then navigate to About; verify user not logged in
        
    }
    
    public void testAnonymousViewImage() throws IOException
    {
        // See if we can view the JSPWiki logo

    }
    
    public void testAssertedName()
    {
        // Navigate to Prefs page; see the 'G'day message' for the anonymous user
        
        // Go to the UserPreferences page; see the set-cookie form, plus welcome text that invites user to set cookie
        
        // Set the cookie to our test user name
        
        // Now navigate back to the main page; see the 'G'day message' for the test user
        
        String cookie = getCookie( CookieAssertionLoginModule.PREFS_COOKIE_NAME );
        assertNotNull( cookie );
        assertEquals( "Don+Quixote", cookie );

        // Clear user cookie
        
        // Go back to the main page, and see the 'G'day message for the anonymous user again

        cookie = getCookie( CookieAssertionLoginModule.PREFS_COOKIE_NAME );
        assertEquals( "", cookie );
    }
    
    public void testAssertedPermissions( ) 
    {
        // Create new group with 'janne' and 'FredFlintstone' as members

        login( TEST_LOGINNAME, TEST_PASSWORD );
        String group = "AssertedPermissions" + String.valueOf( System.currentTimeMillis() );
        String members = TEST_LOGINNAME + " \n FredFlintstone";

        // First, create the group
        
        // Verify the group was created 
        
        // Verifiy that anonymous users can't view the group
        newSession();
        
        // Log in again and verify we can read it
        login( TEST_LOGINNAME, TEST_PASSWORD );
        
        // Verify that asserted user 'Fred' can view the group but not edit
        newSession();
        
        // Try to edit -- it should not be allowed
        
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

    }
    
    public void testLogin()
    {
        // Start at front page; try to log in

    }

    public void testLogout()
    {
        // Start at front page; try to log in

        String cookie = getCookie( CookieAssertionLoginModule.PREFS_COOKIE_NAME );
        assertNotNull( cookie );
        assertEquals( TEST_WIKINAME, cookie );
        
        // Log out; we should NOT see any asserted identities

        cookie = getCookie( CookieAssertionLoginModule.PREFS_COOKIE_NAME );
        assertEquals( "", cookie );
    }
    
    public void testRedirectPageAfterLogin()
    {
        // Create new page 
        login( TEST_LOGINNAME, TEST_PASSWORD );
        String page = "CreatePage" + System.currentTimeMillis();

        String redirectText = "We created this page to test redirects.";
        
        // Now, from an anonymous session, try to view it, fail, then login
        newSession();

        // We should be able to see the page now

    }
    
    public void testRenameProfile()
    {
        // Create a new user and group (and log in)
        String loginName = createProfile("TestRenameProfileUser", "TestRenameProfileUser");
        
        newSession();
        String group = createGroup( loginName, loginName );
        
        // Create a page with a view ACL restricted to the new user
        String page = "TestRenameProfilePage" + System.currentTimeMillis();

        String text = "[{ALLOW edit " + loginName + "}]\nThis page was created with an ACL by " + loginName;
        
        // Anonymous editing should fail
        newSession();
        
        // Now log in as the test user and view/edit it successfully
        login( loginName, TEST_PASSWORD );
        
        // Verify that our ACL test is present (note the extra linebreak at the end of the text
        
        // OK -- now that we've got a user, a protected page and a group  successfully set up, let's change the profile name

        String newLoginName = "Renamed" + loginName;
        
        // Now, the main page should show the new authenticated user name
        
        // When we navigate to the protected page, the ACL should have the NEW name in it
        
        // Also, when we navigate to the group page, the group member should be the NEW name (we will see this inside a <td> element)
        
    }
    
    protected String createGroup( String user, String members ) 
    {

        login( user, TEST_PASSWORD );
        
        String group = "Test" + String.valueOf( System.currentTimeMillis() );
        
        // First, name the group
        
        // Verify the group was created 
        
        // Verifiy that anonymous users can't view the group
        newSession();
        
        // Log in again and verify we can read it
        login( user, TEST_PASSWORD );
        
        // Try to edit -- it should be allowed
        
        return group;
    }
    
    protected String createProfile( String loginname, String fullname )
    {
        return createProfile( loginname, fullname, true );
    }
    
    
    protected String createProfile( String loginname, String fullname, boolean withPassword )
    {
        // Navigate to profile tab

        // Create user profile with generated user name
        String suffix = generateSuffix();
        
        if ( withPassword )
        {

        }
        return loginname + suffix;
    }
    
    protected String generateSuffix()
    {
        return String.valueOf( System.currentTimeMillis() );
    }
    
    protected void login( String user, String password ) 
    {
        // Start at front page; try to log in

    }
    
    protected String getCookie( String cookie )
    {
        return "to-be-fixed";
    }
    
    protected void newSession()
    {
    }
    
    private String getHostAndPort() 
    {
        Properties p = new Properties();
        String buildFile = "build.properties";
        String host = "http://localhost";
        String port = ":8080/";
        BufferedReader in = null;
        
        try
        {
            // search which properties file is being used by build.xml.
            // build.xml is NOT loaded by the classloader, so we have to do some trickery
            // in order to avoid instantiating a File object with a hard-coded, absolute
            // path. Instead of doing that, we determine from where the class was loaded
            // (cfr. http://www.exampledepot.com/egs/java.lang/ClassOrigin.html) and, from
            // there, we set the relative location of build.xml
            
            Class cls = this.getClass();
            ProtectionDomain pDomain = cls.getProtectionDomain();
            CodeSource cSource = pDomain.getCodeSource();
            URL loc = cSource.getLocation(); // ${JSPWiki}/classes
            in = new BufferedReader(new FileReader(new File(loc.getFile() + "../build.xml")));
            
            String line;
            while ( ( line = in.readLine() ) != null ) 
            {
                line = line.trim();
                if ( line.startsWith( "<property name=\"build.properties\" value=\"" ) ) 
                {
                    int beginsIn = "<property name=\"build.properties\" value=\"".length();
                    int endsAt = line.lastIndexOf("\"");
                    buildFile = line.substring( beginsIn, endsAt );
                }
            }
            
            // buildFile is also NOT loaded by the classloader. Luckily it's path is relative 
            // to build.xml path, as it is defined inside it.
            p.load(new FileInputStream(loc.getFile() + "../" + buildFile));
            if (p.getProperty("tomcat.host") != null) {
                host = "http://" + p.getProperty("tomcat.host"); 
            }
            if (p.getProperty("tomcat.port") != null) {
                port = ":" + p.getProperty("tomcat.port") + "/"; 
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.out.println("Error loading build.properties");
        }
        return host + port;
    }
    
}