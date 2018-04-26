/*
    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.
 */
package org.apache.wiki.auth.user;
import java.util.Date;
import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;
import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiEngine;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 *  Tests the DefaultUserProfile class.
 */
public class UserProfileTest
{
    private UserDatabase m_db;

    @Before
    public void setUp()
        throws Exception
    {
            Properties props = TestEngine.getTestProperties();
            PropertyConfigurator.configure(props);
            WikiEngine engine  = new TestEngine(props);
            m_db = engine.getUserManager().getUserDatabase();
    }

    @Test
    public void testSetAttribute()
    {
        UserProfile p = m_db.newProfile();
        Assert.assertEquals( 0, p.getAttributes().size() );

        p.getAttributes().put( "MyAttribute", "some arbitrary value." );
        Assert.assertEquals( 1, p.getAttributes().size() );

        p.getAttributes().put( "YourAttribute", "another arbitrary value." );
        Assert.assertEquals( 2, p.getAttributes().size() );
        Assert.assertTrue( p.getAttributes().containsKey( "MyAttribute" ) );
        Assert.assertTrue( p.getAttributes().containsKey( "YourAttribute" ) );

        p.getAttributes().remove( "MyAttribute" );
        Assert.assertEquals( 1, p.getAttributes().size() );
    }

    @Test
    public void testSetLockExpiry()
    {
        UserProfile p = m_db.newProfile();
        Assert.assertNull( p.getLockExpiry() );
        Assert.assertFalse( p.isLocked() );

        // Set a lock expiry for 1 second in the past; should cause lock to report as null
        p.setLockExpiry( new Date( System.currentTimeMillis() - 1000 ) );
        Assert.assertNull( p.getLockExpiry() );
        Assert.assertFalse( p.isLocked() );

        // Set a lock expiry for 1 second in the past; should say it's not locked
        p.setLockExpiry( new Date( System.currentTimeMillis() - 1000 ) );
        Assert.assertFalse( p.isLocked() );
        Assert.assertNull( p.getLockExpiry() );

        // Now set a lock for 100 seconds in the future; lock should be reported correctly
        Date future = new Date( System.currentTimeMillis() + 100000 );
        p.setLockExpiry( future );
        Assert.assertTrue( p.isLocked() );
        Assert.assertEquals( future, p.getLockExpiry() );

        // Clear the lock
        p.setLockExpiry( null );
        Assert.assertFalse( p.isLocked() );
        Assert.assertNull( p.getLockExpiry() );
    }

    @Test
    public void testSetUid()
    {
        UserProfile p = m_db.newProfile();
        Assert.assertNotSame( "1234567890", p.getUid() );
        p.setUid( "1234567890" );
        Assert.assertEquals( "1234567890", p.getUid() );
    }

    @Test
    public void testEquals()
    {
        UserProfile p = m_db.newProfile();
        UserProfile p2 = m_db.newProfile();

        p.setFullname("Alice");
        p2.setFullname("Bob");

        Assert.assertFalse( p.equals( p2 ) );
    }

    @Test
    public void testEquals2()
    {
        UserProfile p = m_db.newProfile();
        UserProfile p2 = m_db.newProfile();

        p.setFullname("Alice");
        p2.setFullname("Alice");

        Assert.assertTrue( p.equals( p2 ) );
    }

}
