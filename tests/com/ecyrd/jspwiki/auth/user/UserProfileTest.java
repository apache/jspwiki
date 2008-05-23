package com.ecyrd.jspwiki.auth.user;

import java.util.Date;
import java.util.Properties;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.log4j.PropertyConfigurator;

import com.ecyrd.jspwiki.TestEngine;
import com.ecyrd.jspwiki.WikiEngine;

/**
 *  Tests the DefaultUserProfile class.
 *  @author Janne Jalkanen
 */
public class UserProfileTest extends TestCase
{
    private UserDatabase m_db;
    
    public void setUp()
        throws Exception
    {
            Properties props = new Properties();
            props.load( TestEngine.findTestProperties() );
            PropertyConfigurator.configure(props);
            WikiEngine engine  = new TestEngine(props);
            m_db = engine.getUserManager().getUserDatabase();
    }

    public void tearDown()
    {
    }
    
    public void testSetAttribute()
    {
        UserProfile p = m_db.newProfile();
        assertEquals( 0, p.getAttributes().size() );
        
        p.getAttributes().put( "MyAttribute", "some arbitrary value." );
        assertEquals( 1, p.getAttributes().size() );
        
        p.getAttributes().put( "YourAttribute", "another arbitrary value." );
        assertEquals( 2, p.getAttributes().size() );
        assertTrue( p.getAttributes().containsKey( "MyAttribute" ) );
        assertTrue( p.getAttributes().containsKey( "YourAttribute" ) );
        
        p.getAttributes().remove( "MyAttribute" );
        assertEquals( 1, p.getAttributes().size() );
    }
    
    public void testSetLockExpiry()
    {
        UserProfile p = m_db.newProfile();
        assertNull( p.getLockExpiry() );
        assertFalse( p.isLocked() );
        
        // Set a lock expiry for 1 second in the past; should cause lock to report as null
        p.setLockExpiry( new Date( System.currentTimeMillis() - 1000 ) );
        assertNull( p.getLockExpiry() );
        assertFalse( p.isLocked() );
        
        // Set a lock expiry for 1 second in the past; should say it's not locked
        p.setLockExpiry( new Date( System.currentTimeMillis() - 1000 ) );
        assertFalse( p.isLocked() );
        assertNull( p.getLockExpiry() );
        
        // Now set a lock for 100 seconds in the future; lock should be reported correctly
        Date future = new Date( System.currentTimeMillis() + 100000 );
        p.setLockExpiry( future );
        assertTrue( p.isLocked() );
        assertEquals( future, p.getLockExpiry() );
        
        // Clear the lock
        p.setLockExpiry( null );
        assertFalse( p.isLocked() );
        assertNull( p.getLockExpiry() );
    }
    
    public void testSetUid()
    {
        UserProfile p = m_db.newProfile();
        assertNotSame( 1234567890, p.getUid() );
        p.setUid( 1234567890 );
        assertEquals( 1234567890, p.getUid() );
    }

    public void testEquals()
    {
        UserProfile p = m_db.newProfile();
        UserProfile p2 = m_db.newProfile();

        p.setFullname("Alice");
        p2.setFullname("Bob");

        assertFalse( p.equals( p2 ) );
    }

    public void testEquals2()
    {
        UserProfile p = m_db.newProfile();
        UserProfile p2 = m_db.newProfile();

        p.setFullname("Alice");
        p2.setFullname("Alice");

        assertTrue( p.equals( p2 ) );
    }

    public static Test suite()
    {
        return new TestSuite( UserProfileTest.class );
    }
}
