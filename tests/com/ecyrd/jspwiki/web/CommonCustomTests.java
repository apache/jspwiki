package com.ecyrd.jspwiki.web;


public abstract class CommonCustomTests extends CommonTests
{
    public CommonCustomTests( String name, String uRL )
    {
        super( name, uRL );
    }
    
    public void testCreateProfile()
    {
        createProfile( "pvilla", "Pancho Villa" );
        
        // We should see the user name & the g'day
        t.assertTextNotPresent( "Could not save profile: You must log in before creating a profile." );
        t.assertTextPresent( "G&#8217;day" );
        t.assertTextPresent( "Pancho" ); // This is a hack
        t.assertTextPresent( "(authenticated)" );
    }
}