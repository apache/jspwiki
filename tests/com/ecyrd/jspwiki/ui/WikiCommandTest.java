/*
 * (C) Janne Jalkanen 2005
 * 
 */
package com.ecyrd.jspwiki.ui;

import java.util.Properties;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.ecyrd.jspwiki.TestEngine;
import com.ecyrd.jspwiki.auth.permissions.WikiPermission;

public class WikiCommandTest extends TestCase
{
    TestEngine     testEngine;
    String         wiki;
    
    protected void setUp() throws Exception
    {
        Properties props = new Properties();
        props.load( TestEngine.findTestProperties() );
        testEngine = new TestEngine( props );
        wiki = testEngine.getApplicationName();
    }
    
    protected void tearDown() throws Exception
    {
    }

    public void testStaticCommand()
    {
        Command a = WikiCommand.CREATE_GROUP;
        assertEquals( "createGroup", a.getRequestContext() );
        assertEquals( "NewGroup.jsp", a.getJSP() );
        assertEquals( "%uNewGroup.jsp", a.getURLPattern() );
        assertEquals( "NewGroupContent.jsp", a.getContentTemplate() );
        assertNull( a.getTarget() );
        assertNull( a.requiredPermission() );
        assertEquals( a, WikiCommand.CREATE_GROUP );
        
        a = WikiCommand.ERROR;
        assertEquals( "error", a.getRequestContext() );
        assertEquals( "Error.jsp", a.getJSP() );
        assertEquals( "%uError.jsp", a.getURLPattern() );
        assertEquals( "DisplayMessage.jsp", a.getContentTemplate() );
        assertNull( a.getTarget() );
        assertNull( a.requiredPermission() );
        assertEquals( a, WikiCommand.ERROR );
        
        a = WikiCommand.FIND;
        assertEquals( "find", a.getRequestContext() );
        assertEquals( "Search.jsp", a.getJSP() );
        assertEquals( "%uSearch.jsp", a.getURLPattern() );
        assertEquals( "FindContent.jsp", a.getContentTemplate() );
        assertNull( a.getTarget() );
        assertNull( a.requiredPermission() );
        assertEquals( a, WikiCommand.FIND );
        
        a = WikiCommand.INSTALL;
        assertEquals( "install", a.getRequestContext() );
        assertEquals( "Install.jsp", a.getJSP() );
        assertEquals( "%uInstall.jsp", a.getURLPattern() );
        assertNull( a.getContentTemplate() );
        assertNull( a.getTarget() );
        assertNull( a.requiredPermission() );
        assertEquals( a, WikiCommand.INSTALL );
        
        a = WikiCommand.LOGIN;
        assertEquals( "login", a.getRequestContext() );
        assertEquals( "Login.jsp", a.getJSP() );
        assertEquals( "%uLogin.jsp?page=%n", a.getURLPattern() );
        assertEquals( "LoginContent.jsp", a.getContentTemplate() );
        assertNull( a.getTarget() );
        assertNull( a.requiredPermission() );
        assertEquals( a, WikiCommand.LOGIN );
        
        a = WikiCommand.LOGOUT;
        assertEquals( "logout", a.getRequestContext() );
        assertEquals( "Logout.jsp", a.getJSP() );
        assertEquals( "%uLogout.jsp", a.getURLPattern() );
        assertNull( a.getContentTemplate() );
        assertNull( a.getTarget() );
        assertNull( a.requiredPermission() );
        assertEquals( a, WikiCommand.LOGOUT );
        
        a = WikiCommand.PREFS;
        assertEquals( "prefs", a.getRequestContext() );
        assertEquals( "UserPreferences.jsp", a.getJSP() );
        assertEquals( "%uUserPreferences.jsp", a.getURLPattern() );
        assertEquals( "PreferencesContent.jsp", a.getContentTemplate() );
        assertNull( a.getTarget() );
        assertNull( a.requiredPermission() );
        assertEquals( a, WikiCommand.PREFS );
    }

    public void testTargetedCommand()
    {
        // Get view command
        Command a = WikiCommand.CREATE_GROUP;
        
        // Combine with wiki; make sure it's not equal to old command
        Command b = a.targetedCommand( wiki );
        assertNotSame( a, b );
        assertEquals( a.getRequestContext(), b.getRequestContext() );
        assertEquals( a.getJSP(), b.getJSP() );
        assertEquals( a.getURLPattern(), b.getURLPattern() );
        assertEquals( a.getContentTemplate(), b.getContentTemplate() );
        assertNotNull( b.getTarget() );
        assertNotNull( b.requiredPermission() );
        assertEquals( new WikiPermission( wiki, "createGroups" ), b.requiredPermission() );
        assertEquals( wiki, b.getTarget() );
        
        // Do the same with other commands
        
        a = WikiCommand.ERROR;
        b = a.targetedCommand( wiki );
        assertNotSame( a, b );
        assertNotNull( b.getTarget() );
        assertNull( b.requiredPermission() );
        assertEquals( wiki, b.getTarget() );
        
        a = WikiCommand.FIND;
        b = a.targetedCommand( wiki );
        assertNotSame( a, b );
        assertNotNull( b.getTarget() );
        assertNull( b.requiredPermission() );
        assertEquals( wiki, b.getTarget() );
        
        a = WikiCommand.INSTALL;
        b = a.targetedCommand( wiki );
        assertNotSame( a, b );
        assertNotNull( b.getTarget() );
        assertNull( b.requiredPermission() );
        assertEquals( wiki, b.getTarget() );
        
        a = WikiCommand.LOGIN;
        b = a.targetedCommand( wiki );
        assertNotSame( a, b );
        assertNotNull( b.getTarget() );
        assertNotNull( b.requiredPermission() );
        assertEquals( new WikiPermission( wiki, "login" ), b.requiredPermission() );
        assertEquals( wiki, b.getTarget() );
        
        a = WikiCommand.LOGOUT;
        b = a.targetedCommand( wiki );
        assertNotSame( a, b );
        assertNotNull( b.getTarget() );
        assertNotNull( b.requiredPermission() );
        assertEquals( new WikiPermission( wiki, "login" ), b.requiredPermission() );
        assertEquals( wiki, b.getTarget() );
        
        a = WikiCommand.PREFS;
        b = a.targetedCommand( wiki );
        assertNotSame( a, b );
        assertNotNull( b.getTarget() );
        assertNotNull( b.requiredPermission() );
        assertEquals( new WikiPermission( wiki, "editProfile" ), b.requiredPermission() );
        assertEquals( wiki, b.getTarget() );
    }
    
    public static Test suite()
    {
        return new TestSuite( WikiCommandTest.class );
    }
}
