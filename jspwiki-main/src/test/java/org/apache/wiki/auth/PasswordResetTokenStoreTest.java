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
package org.apache.wiki.auth;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PasswordResetTokenStoreTest {

    long now = 1_000_000L;
    PasswordResetTokenStore store = new PasswordResetTokenStore( () -> now );

    @Test
    public void tokenRedeemsOnceOnly() {
        final String token = store.issue( "alice" );
        Assertions.assertNotNull( token, "token should be issued" );
        Assertions.assertEquals( "alice", store.redeem( token ), "first redemption returns the login name" );
        Assertions.assertNull( store.redeem( token ), "replaying a redeemed token must fail" );
    }

    @Test
    public void tokenExpires() {
        final String token = store.issue( "alice" );
        now += PasswordResetTokenStore.TOKEN_TTL_MS + 1;
        Assertions.assertNull( store.redeem( token ), "expired token must not redeem" );
    }

    @Test
    public void unknownOrEmptyTokenRejected() {
        store.issue( "alice" );
        Assertions.assertNull( store.redeem( "deadbeef" ) );
        Assertions.assertNull( store.redeem( "" ) );
        Assertions.assertNull( store.redeem( null ) );
    }

    @Test
    public void singleOutstandingTokenPerAccount() {
        Assertions.assertNotNull( store.issue( "alice" ), "first issue succeeds" );
        Assertions.assertNull( store.issue( "alice" ), "second issue while one is outstanding is throttled" );
        now += PasswordResetTokenStore.TOKEN_TTL_MS + 1;
        Assertions.assertNotNull( store.issue( "alice" ), "issue succeeds again after expiry" );
    }

    @Test
    public void tokensAreBoundToTheirAccount() {
        final String tokenA = store.issue( "alice" );
        final String tokenB = store.issue( "bob" );
        Assertions.assertEquals( "bob", store.redeem( tokenB ) );
        Assertions.assertEquals( "alice", store.redeem( tokenA ) );
    }

}
