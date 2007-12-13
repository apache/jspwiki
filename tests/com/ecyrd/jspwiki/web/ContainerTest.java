package com.ecyrd.jspwiki.web;

public class ContainerTest extends CommonContainerTests
{
    protected static final String USER = "pancho";

    public ContainerTest( String s )
    {
        super( s, "test-container/" );
    }

    public void testCreateProfile()
    {
        String loginname = "pvilla";
        String fullname = "Pancho Villa";
        String suffix = generateSuffix();
        
        // Start at the front page
        
        // Navigate to the profile page
        
        // We should NOT see a loginname field, OR a password field
        // and a warning message advising the user to log in

        // Try creating user profile with generated user name
        
        // We should NOT be able to create profile, because we're not logged in
        
        // OK, very nice. We've shown that you CANNOT create a profile unless there's a container ID already.
        // Now, let's try doing it with an existing account (but, we will stop just short of creating the profile)
        
        // Log in first!
        
        // Great. Now, let's try creating a profile
        
        // We should NOT see a loginname field or a password field,
        // and we should see a warning message that the ID can't be changed because it's a container ID
    }

    /**
     *This test is a no-op because container-managed login IDs can't be renamed.
     */
    public void testRenameProfile() { }
}
