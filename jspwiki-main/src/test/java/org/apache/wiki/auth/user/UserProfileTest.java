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
import org.apache.wiki.TestEngine;
import org.apache.wiki.auth.UserManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Date;

/**
 *  Tests the DefaultUserProfile class.
 */
public class UserProfileTest {

    TestEngine engine = TestEngine.build();
    UserDatabase m_db = engine.getManager( UserManager.class ).getUserDatabase();

    @Test
    public void testSetAttribute() {
        final UserProfile p = m_db.newProfile();
        Assertions.assertEquals( 0, p.getAttributes().size() );

        p.getAttributes().put( "MyAttribute", "some arbitrary value." );
        Assertions.assertEquals( 1, p.getAttributes().size() );

        p.getAttributes().put( "YourAttribute", "another arbitrary value." );
        Assertions.assertEquals( 2, p.getAttributes().size() );
        Assertions.assertTrue( p.getAttributes().containsKey( "MyAttribute" ) );
        Assertions.assertTrue( p.getAttributes().containsKey( "YourAttribute" ) );

        p.getAttributes().remove( "MyAttribute" );
        Assertions.assertEquals( 1, p.getAttributes().size() );
    }

    @Test
    public void testSetLockExpiry() {
        final UserProfile p = m_db.newProfile();
        Assertions.assertNull( p.getLockExpiry() );
        Assertions.assertFalse( p.isLocked() );

        // Set a lock expiry for 1 second in the past; should cause lock to report as null
        p.setLockExpiry( new Date( System.currentTimeMillis() - 1000 ) );
        Assertions.assertNull( p.getLockExpiry() );
        Assertions.assertFalse( p.isLocked() );

        // Set a lock expiry for 1 second in the past; should say it's not locked
        p.setLockExpiry( new Date( System.currentTimeMillis() - 1000 ) );
        Assertions.assertFalse( p.isLocked() );
        Assertions.assertNull( p.getLockExpiry() );

        // Now set a lock for 100 seconds in the future; lock should be reported correctly
        final Date future = new Date( System.currentTimeMillis() + 100000 );
        p.setLockExpiry( future );
        Assertions.assertTrue( p.isLocked() );
        Assertions.assertEquals( future, p.getLockExpiry() );

        // Clear the lock
        p.setLockExpiry( null );
        Assertions.assertFalse( p.isLocked() );
        Assertions.assertNull( p.getLockExpiry() );
    }

    @Test
    public void testSetUid() {
        final UserProfile p = m_db.newProfile();
        Assertions.assertNotSame( "1234567890", p.getUid() );
        p.setUid( "1234567890" );
        Assertions.assertEquals( "1234567890", p.getUid() );
    }

    @Test
    public void testEquals() {
        final UserProfile p = m_db.newProfile();
        final UserProfile p2 = m_db.newProfile();

        p.setFullname("Alice");
        p2.setFullname("Bob");

        Assertions.assertNotEquals( p, p2 );
    }

    @Test
    public void testEquals2() {
        final UserProfile p = m_db.newProfile();
        final UserProfile p2 = m_db.newProfile();

        p.setFullname("Alice");
        p2.setFullname("Alice");

        Assertions.assertEquals( p, p2 );
    }

}
