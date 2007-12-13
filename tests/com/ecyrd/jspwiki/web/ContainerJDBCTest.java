package com.ecyrd.jspwiki.web;

public class ContainerJDBCTest extends CommonContainerTests
{
    protected static final String USER = "pancho";
    
    public ContainerJDBCTest( String s )
    {
        super( s, "test-container-jdbc/" );
    }

    public void testCreateProfile()
    {
        createProfile( "pvilla", "Pancho Villa" );
        
        // We should see the user name & the g'day (asserted only)
    }
    
}
