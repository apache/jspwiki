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
package org.apache.wiki.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.Random;


/**
 * Hashes and verifies salted SHA-1 passwords, which are compliant with RFC
 * 2307.
 */
public final class CryptoUtil {

    private static final String SSHA = "{SSHA}";

    private static final String SHA1 = "{SHA-1}";

    private static final String SHA256 = "{SHA-256}";

    private static final Random RANDOM = new SecureRandom();

    private static final int DEFAULT_SALT_SIZE = 8;

    /** Prefix of PBKDF2-HMAC-SHA256 password entries, the current password-storage format. */
    public static final String PBKDF2_PREFIX = "{PBKDF2-SHA256}";

    /** Iteration count for new PBKDF2-HMAC-SHA256 hashes, per current OWASP password-storage guidance. */
    private static final int PBKDF2_ITERATIONS = 600_000;

    private static final int PBKDF2_KEY_LENGTH_BYTES = 32;

    private static final int PBKDF2_SALT_SIZE = 16;

    private static final Object HELP = "--help";

    private static final Object HASH = "--hash";

    private static final Object VERIFY = "--verify";

    /**
     * Private constructor to prevent direct instantiation.
     */
    private CryptoUtil() {}

    /**
     * <p>
     * Convenience method for hashing and verifying salted SHA-1 or SHA-256 passwords from
     * the command line. This method requires <code>commons-codec-1.3.jar</code>
     * (or a newer version) to be on the classpath. Command line arguments are
     * as follows:
     * </p>
     * <ul>
     * <li><code>--hash <var>password</var> SSHA</code> - hashes <var>password</var></code>
     * and prints a password digest that looks like this: <blockquote><code>{SSHA}yfT8SRT/WoOuNuA6KbJeF10OznZmb28=</code></blockquote></li>
     * <li><code>--verify <var>password</var> <var>digest</var></code> -
     * verifies <var>password</var> by extracting the salt from <var>digest</var>
     * (which is identical to what is printed by <code>--hash</code>) and
     * re-computing the digest again using the password and salt. If the
     * password supplied is the same as the one used to create the original
     * digest, <code>true</code> will be printed; otherwise <code>false</code></li>
     * </ul>
     * <p>For example, one way to use this utility is to change to JSPWiki's <code>build</code> directory
     * and type the following command:</p>
     * <blockquote><code>java -cp JSPWiki.jar:../lib/commons-codec-1.3.jar org.apache.wiki.util.CryptoUtil --hash mynewpassword</code></blockquote>
     * 
     * @param args arguments for this method as described above
     * @throws Exception Catches nothing; throws everything up.
     */
    public static void main( final String[] args ) throws Exception {
        // Print help if the user requested it, or if no arguments
        if( args.length == 0 || ( args.length == 1 && HELP.equals( args[0] ) ) ) {
            System.out.println( "Usage: CryptoUtil [options] " );
            System.out.println( "   --hash   password algorithm             create hash for password" );
            System.out.println( "   --verify password digest algorithm      verify password for digest" );
            System.out.println( "Valid algorithm options are {SSHA} and {SHA-256}. If no algorithm is specified or an unsupported algorithm is specified, SHA-256 is used." );
        }

        if( HASH.equals( args[0] ) ) {
        // User wants to hash the password
            if( args.length < 2 ) {
                throw new IllegalArgumentException( "Error: --hash requires a 'password' argument." );
            }
            final String password = args[1].trim();
            final String algorithm = args.length > 2 ? args[2].trim() : SHA256;

            System.out.println( CryptoUtil.getSaltedPassword( password.getBytes( StandardCharsets.UTF_8 ), algorithm ) );
        } else if( VERIFY.equals( args[0] ) ) {
        // User wants to verify an existing password
            if( args.length < 3 ) {
                throw new IllegalArgumentException( "Error: --hash requires 'password' and 'digest' arguments." );
            }
            final String password = args[1].trim();
            final String digest = args[2].trim();

            System.out.println( CryptoUtil.verifySaltedPassword( password.getBytes( StandardCharsets.UTF_8 ), digest ) );
        } else {
            System.out.println( "Wrong usage. Try --help." );
        }
    }

    /**
     * <p>
     * Creates an RFC 2307-compliant salted, hashed password with the SHA1 or SHA-256
     * MessageDigest algorithm. After the password is digested, the first 20 or 32
     * bytes of the digest will be the actual password hash; the remaining bytes
     * will be a randomly generated salt of length {@link #DEFAULT_SALT_SIZE},
     * for example: <blockquote><code>{SSHA}3cGWem65NCEkF5Ew5AEk45ak8LHUWAwPVXAyyw==</code></blockquote>
     * </p>
     * <p>
     * In layman's terms, the formula is
     * <code>digest( secret + salt ) + salt</code>. The resulting digest is Base64-encoded.
     * </p>
     * <p>
     * Note that successive invocations of this method with the same password
     * will result in different hashes! (This, of course, is exactly the point.)
     * </p>
     * 
     * @param password the password to be digested
     * @return the Base64-encoded password hash, prepended by
     *         <code>{SSHA}</code> or <code>{SHA256}</code>.
     * @throws NoSuchAlgorithmException If your JVM does not supply the necessary algorithm. Should not happen.
     */
    public static String getSaltedPassword( final byte[] password, final String algorithm ) throws NoSuchAlgorithmException {
        final byte[] salt = new byte[ DEFAULT_SALT_SIZE ];
        RANDOM.nextBytes( salt );

        return getSaltedPassword( password, salt, algorithm );
    }

    /**
     * <p>
     * Helper method that creates an RFC 2307-compliant salted, hashed password with the SHA1 or SHA256
     * MessageDigest algorithm. After the password is digested, the first 20 or 32
     * bytes of the digest will be the actual password hash; the remaining bytes
     * will be the salt. Thus, supplying a password <code>testing123</code>
     * and a random salt <code>foo</code> produces the hash when using SHA1:
     * </p>
     * <blockquote><code>{SSHA}yfT8SRT/WoOuNuA6KbJeF10OznZmb28=</code></blockquote>
     * <p>
     * In layman's terms, the formula is
     * <code>digest( secret + salt ) + salt</code>. The resulting digest is Base64-encoded.</p>
     * 
     * @param password the password to be digested
     * @param salt the random salt
     * @return the Base64-encoded password hash, prepended by <code>{SSHA}</code> or <code>{SHA256}</code>.
     * @throws NoSuchAlgorithmException If your JVM does not supply the necessary algorithm. Should not happen.
     */
    static String getSaltedPassword( final byte[] password, final byte[] salt, final String algorithm ) throws NoSuchAlgorithmException {
        //The term SSHA is used as a password prefix for backwards compatibility, but we use SHA-1 when fetching an instance
        //of MessageDigest, as it is the guaranteed option. We also need to remove curly braces surrounding the string for
        //backwards compatibility.
        final String algorithmToUse = algorithm.equals(SSHA) ? SHA1 : algorithm;
        final MessageDigest digest = MessageDigest.getInstance( algorithmToUse.substring( 1, algorithmToUse.length() -1 ) );
        digest.update( password );
        final byte[] hash = digest.digest( salt );

        // Create an array with the hash plus the salt
        final byte[] all = new byte[hash.length + salt.length];
        System.arraycopy(hash, 0, all, 0, hash.length);
        System.arraycopy(salt, 0, all, hash.length, salt.length);
        final byte[] base64 = Base64.getEncoder().encode( all );
        
        return algorithm + new String( base64, StandardCharsets.UTF_8 );
    }

    /**
     * <p>Creates an iterated, salted PBKDF2-HMAC-SHA256 hash of the given password, suitable for storage. Unlike the
     * single-iteration digests above, the work factor makes offline cracking of a disclosed user database
     * expensive. The format is <code>{PBKDF2-SHA256}<var>iterations</var>$base64(salt)$base64(hash)</code>, so the
     * iteration count of stored entries can be raised in the future without breaking old entries.</p>
     *
     * @param password the password to be hashed
     * @return the password entry, prepended by <code>{PBKDF2-SHA256}</code>
     * @throws NoSuchAlgorithmException If your JVM does not supply the necessary algorithm. Should not happen.
     */
    public static String getPbkdf2SaltedPassword( final byte[] password ) throws NoSuchAlgorithmException {
        final byte[] salt = new byte[ PBKDF2_SALT_SIZE ];
        RANDOM.nextBytes( salt );
        return getPbkdf2SaltedPassword( password, salt, PBKDF2_ITERATIONS );
    }

    static String getPbkdf2SaltedPassword( final byte[] password, final byte[] salt, final int iterations ) throws NoSuchAlgorithmException {
        final byte[] hash = pbkdf2( password, salt, iterations );
        final Base64.Encoder encoder = Base64.getEncoder();
        return PBKDF2_PREFIX + iterations + "$" + encoder.encodeToString( salt ) + "$" + encoder.encodeToString( hash );
    }

    /**
     * Verifies a password against a <code>{PBKDF2-SHA256}</code> entry created by
     * {@link #getPbkdf2SaltedPassword(byte[])}. The comparison is constant-time.
     *
     * @param password the password to verify
     * @param entry the stored password entry
     * @return true if the password matches the entry
     * @throws NoSuchAlgorithmException If your JVM does not supply the necessary algorithm. Should not happen.
     */
    public static boolean verifyPbkdf2SaltedPassword( final byte[] password, final String entry ) throws NoSuchAlgorithmException {
        if( !entry.startsWith( PBKDF2_PREFIX ) ) {
            throw new IllegalArgumentException( "Hash not prefixed by expected algorithm; is it really a PBKDF2 hash?" );
        }
        final String[] fields = entry.substring( PBKDF2_PREFIX.length() ).split( "\\$" );
        if( fields.length != 3 ) {
            throw new IllegalArgumentException( "Malformed PBKDF2 password entry" );
        }
        final int iterations = Integer.parseInt( fields[ 0 ] );
        final byte[] salt = Base64.getDecoder().decode( fields[ 1 ] );
        final byte[] expected = Base64.getDecoder().decode( fields[ 2 ] );
        final byte[] hash = pbkdf2( password, salt, iterations );
        return MessageDigest.isEqual( expected, hash );
    }

    private static byte[] pbkdf2( final byte[] password, final byte[] salt, final int iterations ) throws NoSuchAlgorithmException {
        final char[] chars = new String( password, StandardCharsets.UTF_8 ).toCharArray();
        final PBEKeySpec spec = new PBEKeySpec( chars, salt, iterations, PBKDF2_KEY_LENGTH_BYTES * 8 );
        try {
            return SecretKeyFactory.getInstance( "PBKDF2WithHmacSHA256" ).generateSecret( spec ).getEncoded();
        } catch( final InvalidKeySpecException e ) {
            throw new NoSuchAlgorithmException( "Unable to compute PBKDF2 hash", e );
        } finally {
            spec.clearPassword();
        }
    }

    /**
     *  Compares a password to a given entry and returns true, if it matches.
     *
     *  @param password The password in bytes.
     *  @param entry The password entry, typically starting with {SSHA}.
     *  @return True, if the password matches.
     *  @throws NoSuchAlgorithmException If there is no SHA available.
     */
    public static boolean verifySaltedPassword( final byte[] password, final String entry ) throws NoSuchAlgorithmException {
        if( !entry.startsWith( SSHA ) && !entry.startsWith( SHA256 ) && !entry.startsWith( CryptoUtil.PBKDF2_PREFIX )  ) {
            throw new IllegalArgumentException( "Hash not prefixed by expected algorithm; is it really a salted hash?" );
        }
        final String algorithm;
        if (entry.startsWith(PBKDF2_PREFIX)) {
            return verifyPbkdf2SaltedPassword(password, entry);
        } else if (entry.startsWith(SSHA)) {
            algorithm = SSHA;
        } else if (entry.startsWith(SHA256)) {
            algorithm = SHA256;
        } else {
            throw new NoSuchAlgorithmException("unknown hash algorithm prefix");
        }

        String hash2 = entry.substring( algorithm.length() );
        byte[] bits = hash2.getBytes( StandardCharsets.UTF_8 );
        final byte[] challenge = Base64.getDecoder().decode(bits);

        // Extract the password hash and salt
        final byte[] passwordHash = extractPasswordHash( challenge, algorithm.equals( SSHA ) ? 20 : 32 );
        final byte[] salt = extractSalt( challenge, algorithm.equals( SSHA ) ? 20 : 32  );

        // Re-create the hash using the password and the extracted salt
        // The term SSHA is used as a password prefix for backwards compatibility, but we use SHA-1 when fetching an instance
        // of MessageDigest, as it is the guaranteed option. We also need to remove curly braces surrounding the string for
        // backwards compatibility.
        final String algorithmToUse = algorithm.equals( SSHA ) ? SHA1 : algorithm;
        final MessageDigest digest = MessageDigest.getInstance( algorithmToUse.substring( 1, algorithmToUse.length() -1 ) );
        digest.update( password );
        final byte[] hash = digest.digest( salt );

        // See if our extracted hash matches what we just re-created
        return MessageDigest.isEqual( passwordHash, hash );
    }

    /**
     * Helper method that extracts the hashed password fragment from a supplied salted SHA-1 or SHA-256 digest
     * by taking all the characters before position 20 or 32 depending on algorithm.
     * 
     * @param digest the salted digest, which is assumed to have been previously decoded from Base64.
     * @return the password hash
     * @throws IllegalArgumentException if the length of the supplied digest is less than or equal to 20 bytes
     */
    static byte[] extractPasswordHash( final byte[] digest, final int hashLength ) throws IllegalArgumentException {
        if( digest.length < hashLength ) {
            throw new IllegalArgumentException( "Hash was shorter than expected; could not extract password hash!" );
        }

        // Extract the password hash
        final byte[] hash = new byte[ hashLength ];
        System.arraycopy( digest, 0, hash, 0, hashLength );

        return hash;
    }

    /**
     * Helper method that extracts the salt from supplied salted digest by taking all the characters after a given index.
     * 
     * @param digest the salted digest, which is assumed to have been previously decoded from Base64.
     * @return the salt
     * @throws IllegalArgumentException if the length of the supplied digest is less than given length.
     */
    static byte[] extractSalt( final byte[] digest, final int hashLength ) throws IllegalArgumentException {
        if( digest.length <= hashLength ) {
            throw new IllegalArgumentException( "Hash was shorter than expected; we found no salt!" );
        }

        // Extract the salt
        final byte[] salt = new byte[digest.length - hashLength];
        System.arraycopy(digest, hashLength, salt, 0, digest.length - hashLength);

        return salt;
    }

}
