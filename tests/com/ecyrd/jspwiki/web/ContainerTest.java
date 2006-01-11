package com.ecyrd.jspwiki.web;

public class ContainerTest extends CommonTests
{
    protected static final String USER = "pancho";

    public ContainerTest( String s )
    {
        super( s, "http://localhost:8080/test-container/" );
    }

    public void testCreateProfile()
    {
        createProfile( false );
        
        // We should NOT be able to create profile, because we're not logged in
        t.assertTextPresent( "Could not save profile: You must log in before creating a profile." );
    }

    public void testLogin()
    {
        login( USER );
    }

}
