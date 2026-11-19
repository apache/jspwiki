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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * In-memory store of single-use, expiring password reset tokens, used by {@code LostPassword.jsp}.
 * <p>
 * A password reset is a two-step operation: a cryptographically random token is issued and mailed to the
 * account owner ({@link #issue(String)}), and the stored credential is only changed when that token is
 * presented back ({@link #redeem(String)}). Only a SHA-256 hash of the token is kept server-side, tokens
 * expire after {@link #TOKEN_TTL_MS}, are removed on first use, and at most one token per account may be
 * outstanding at a time (which also acts as a simple per-account throttle). The store itself is bounded
 * to {@link #MAX_PENDING} entries as a global backstop.
 * <p>
 * Note: the store is per-JVM. On a multi-node deployment a token must be redeemed on the node that
 * issued it.
 *
 * @since 2.12.4
 */
public final class PasswordResetTokenStore {

    /** How long an issued token stays valid: 30 minutes. */
    public static final long TOKEN_TTL_MS = 30L * 60L * 1000L;

    /** Upper bound on simultaneously outstanding tokens. */
    static final int MAX_PENDING = 1_000;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final PasswordResetTokenStore INSTANCE = new PasswordResetTokenStore( System::currentTimeMillis );

    private final Map< String, PendingReset > pending = new ConcurrentHashMap<>();
    private final LongSupplier clock;

    /** Package-private for tests, which supply their own clock. */
    PasswordResetTokenStore( final LongSupplier clock ) {
        this.clock = clock;
    }

    /**
     * Returns the shared store instance.
     *
     * @return the shared {@link PasswordResetTokenStore}.
     */
    public static PasswordResetTokenStore getInstance() {
        return INSTANCE;
    }

    /**
     * Issues a new reset token for the given login name. The raw token is returned exactly once, for
     * inclusion in the reset e-mail; only its SHA-256 hash is retained. If a non-expired token is already
     * outstanding for the account, or the store is full, no token is issued.
     *
     * @param loginName the login name the token is bound to.
     * @return the raw token, or {@code null} if issuance was throttled.
     */
    public synchronized String issue( final String loginName ) {
        final long now = clock.getAsLong();
        purgeExpired( now );
        if( loginName == null || loginName.isEmpty() || pending.size() >= MAX_PENDING ) {
            return null;
        }
        for( final PendingReset reset : pending.values() ) {
            if( reset.loginName.equals( loginName ) ) {
                return null; // one outstanding token per account
            }
        }
        final byte[] raw = new byte[ 32 ];
        RANDOM.nextBytes( raw );
        final String token = toHex( raw );
        pending.put( toHex( sha256( token ) ), new PendingReset( loginName, now + TOKEN_TTL_MS ) );
        return token;
    }

    /**
     * Redeems a token: if it matches a stored, non-expired token hash, the token is removed (single use)
     * and the login name it was issued for is returned. Comparison of the hashed values is constant-time.
     *
     * @param token the raw token as received from the user.
     * @return the login name the token was bound to, or {@code null} if the token is unknown, already
     *         used or expired.
     */
    public synchronized String redeem( final String token ) {
        final long now = clock.getAsLong();
        purgeExpired( now );
        if( token == null || token.isEmpty() ) {
            return null;
        }
        final byte[] candidate = sha256( token );
        String matchedKey = null;
        // Scan every entry and compare via MessageDigest.isEqual, so neither the position of a match nor
        // a partial prefix match shows up as a timing difference.
        for( final String key : pending.keySet() ) {
            if( MessageDigest.isEqual( candidate, fromHex( key ) ) && matchedKey == null ) {
                matchedKey = key;
            }
        }
        if( matchedKey == null ) {
            return null;
        }
        final PendingReset reset = pending.remove( matchedKey ); // remove-on-use: replay gets null
        return reset != null && reset.expiry >= now ? reset.loginName : null;
    }

    private void purgeExpired( final long now ) {
        for( final Iterator< PendingReset > it = pending.values().iterator(); it.hasNext(); ) {
            if( it.next().expiry < now ) {
                it.remove();
            }
        }
    }

    private static byte[] sha256( final String value ) {
        try {
            return MessageDigest.getInstance( "SHA-256" ).digest( value.getBytes( StandardCharsets.UTF_8 ) );
        } catch( final NoSuchAlgorithmException e ) {
            throw new IllegalStateException( "JVM without SHA-256 support", e );
        }
    }

    private static String toHex( final byte[] bytes ) {
        final StringBuilder sb = new StringBuilder( bytes.length * 2 );
        for( final byte b : bytes ) {
            sb.append( Character.forDigit( ( b >> 4 ) & 0x0F, 16 ) ).append( Character.forDigit( b & 0x0F, 16 ) );
        }
        return sb.toString();
    }

    private static byte[] fromHex( final String hex ) {
        final byte[] bytes = new byte[ hex.length() / 2 ];
        for( int i = 0; i < bytes.length; i++ ) {
            bytes[ i ] = ( byte )( ( Character.digit( hex.charAt( 2 * i ), 16 ) << 4 ) + Character.digit( hex.charAt( 2 * i + 1 ), 16 ) );
        }
        return bytes;
    }

    private static final class PendingReset {
        final String loginName;
        final long expiry;

        PendingReset( final String loginName, final long expiry ) {
            this.loginName = loginName;
            this.expiry = expiry;
        }
    }

}
