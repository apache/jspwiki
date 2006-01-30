package com.ecyrd.jspwiki.web;

public class ContainerJDBCTest extends CommonContainerTests
{
    protected static final String USER = "pancho";
    
    public ContainerJDBCTest( String s )
    {
        super( s, "http://localhost:8080/test-container-jdbc/" );
    }

    public void testCreateProfile()
    {
        createProfile( "pvilla", "Pancho Villa" );
        
        // We should see the user name & the g'day (asserted only)
        t.assertTextNotPresent( "Could not save profile: You must log in before creating a profile." );
        t.assertTextPresent( "G'day" );
        t.assertTextPresent( "Pancho" ); // This is a hack
        t.assertTextPresent( "(not logged in)" );
    }
    
}
