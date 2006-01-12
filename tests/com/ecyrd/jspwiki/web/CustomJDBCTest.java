package com.ecyrd.jspwiki.web;

public class CustomJDBCTest extends CommonTests
{
    public CustomJDBCTest( String s )
    {
        super( s, "http://localhost:8080/test-custom-jdbc/" );
    }

    public void testCreateProfile()
    {
        // We should see the user name & the g'day
        createProfile();
        t.assertTextNotPresent( "Could not save profile: You must log in before creating a profile." );
        t.assertTextPresent( "G'day" );
        t.assertTextPresent( "Pancho" ); // This is a hack
        t.assertTextPresent( "(authenticated)" );
    }

}
