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
    }
}