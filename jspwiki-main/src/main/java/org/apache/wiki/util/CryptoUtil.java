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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Random;


/**
 * Hashes and verifies salted SHA-1 passwords, which are compliant with RFC
 * 2307.
 */
public final class CryptoUtil
{
    private static final String SSHA = "{SSHA}";

    private static final Random RANDOM = new SecureRandom();

    private static final int DEFAULT_SALT_SIZE = 8;

    private static final Object HELP = "--help";

    private static final Object HASH = "--hash";

    private static final Object VERIFY = "--verify";

    /**
     * Private constructor to prevent direct instantiation.
     */
    private CryptoUtil()
    {
    }

    /**
     * <p>
     * Convenience method for hashing and verifying salted SHA-1 passwords from
     * the command line. This method requires <code>commons-codec-1.3.jar</code>
     * (or a newer version) to be on the classpath. Command line arguments are
     * as follows:
     * </p>
     * <ul>
     * <li><code>--hash <var>password</var></code> - hashes <var>password</var></code>
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
    public static void main( final String[] args ) throws Exception
    {
        // Print help if the user requested it, or if no arguments
        if( args.length == 0 || (args.length == 1 && HELP.equals( args[0] )) )
        {
            System.out.println( "Usage: CryptoUtil [options] " );
            System.out.println( "   --hash   password             create hash for password" );
            System.out.println( "   --verify password digest      verify password for digest" );
            System.exit( 0 );
        }

        // User wants to hash the password
        if( HASH.equals( args[0] ) )
        {
            if( args.length < 2 )
            {
                throw new IllegalArgumentException( "Error: --hash requires a 'password' argument." );
            }
            final String password = args[1].trim();
            System.out.println( CryptoUtil.getSaltedPassword( password.getBytes( StandardCharsets.UTF_8 ) ) );
        }

        // User wants to verify an existing password
        else if( VERIFY.equals( args[0] ) )
        {
            if( args.length < 3 )
            {
                throw new IllegalArgumentException( "Error: --hash requires 'password' and 'digest' arguments." );
            }
            final String password = args[1].trim();
            final String digest = args[2].trim();
            System.out.println( CryptoUtil.verifySaltedPassword( password.getBytes( StandardCharsets.UTF_8 ), digest ) );
        }

        else
        {
            System.out.println( "Wrong usage. Try --help." );
        }
    }

    /**
     * <p>
     * Creates an RFC 2307-compliant salted, hashed password with the SHA1
     * MessageDigest algorithm. After the password is digested, the first 20
     * bytes of the digest will be the actual password hash; the remaining bytes
     * will be a randomly generated salt of length {@link #DEFAULT_SALT_SIZE},
     * for example: <blockquote><code>{SSHA}3cGWem65NCEkF5Ew5AEk45ak8LHUWAwPVXAyyw==</code></blockquote>
     * </p>
     * <p>
     * In layman's terms, the formula is
     * <code>digest( secret + salt ) + salt</code>. The resulting digest is
     * Base64-encoded.
     * </p>
     * <p>
     * Note that successive invocations of this method with the same password
     * will result in different hashes! (This, of course, is exactly the point.)
     * </p>
     * 
     * @param password the password to be digested
     * @return the Base64-encoded password hash, prepended by
     *         <code>{SSHA}</code>.
     * @throws NoSuchAlgorithmException If your JVM is completely b0rked and does not have SHA.
     */
    public static String getSaltedPassword( byte[] password ) throws NoSuchAlgorithmException
    {
        byte[] salt = new byte[DEFAULT_SALT_SIZE];
        RANDOM.nextBytes( salt );
        return getSaltedPassword( password, salt );
    }

    /**
     * <p>
     * Helper method that creates an RFC 2307-compliant salted, hashed password with the SHA1
     * MessageDigest algorithm. After the password is digested, the first 20
     * bytes of the digest will be the actual password hash; the remaining bytes
     * will be the salt. Thus, supplying a password <code>testing123</code>
     * and a random salt <code>foo</code> produces the hash:
     * </p>
     * <blockquote><code>{SSHA}yfT8SRT/WoOuNuA6KbJeF10OznZmb28=</code></blockquote>
     * <p>
     * In layman's terms, the formula is
     * <code>digest( secret + salt ) + salt</code>. The resulting digest is Base64-encoded.</p>
     * 
     * @param password the password to be digested
     * @param salt the random salt
     * @return the Base64-encoded password hash, prepended by <code>{SSHA}</code>.
     * @throws NoSuchAlgorithmException If your JVM is totally b0rked and does not have SHA1.
     */
    protected static String getSaltedPassword( byte[] password, byte[] salt ) throws NoSuchAlgorithmException
    {
        MessageDigest digest = MessageDigest.getInstance( "SHA" );
        digest.update( password );
        byte[] hash = digest.digest( salt );

        // Create an array with the hash plus the salt
        byte[] all = new byte[hash.length + salt.length];
        for( int i = 0; i < hash.length; i++ )
        {
            all[i] = hash[i];
        }
        for( int i = 0; i < salt.length; i++ )
        {
            all[hash.length + i] = salt[i];
        }
        byte[] base64 = Base64.getEncoder().encode( all );
        
        return SSHA + new String( base64, StandardCharsets.UTF_8 );
    }

    /**
     *  Compares a password to a given entry and returns true, if it matches.
     *  
     *  @param password The password in bytes.
     *  @param entry The password entry, typically starting with {SSHA}.
     *  @return True, if the password matches.
     *  @throws NoSuchAlgorithmException If there is no SHA available.
     */
    public static boolean verifySaltedPassword( byte[] password, String entry ) throws NoSuchAlgorithmException
    {
        // First, extract everything after {SSHA} and decode from Base64
        if( !entry.startsWith( SSHA ) )
        {
            throw new IllegalArgumentException( "Hash not prefixed by {SSHA}; is it really a salted hash?" );
        }
        byte[] challenge = Base64.getDecoder().decode( entry.substring( 6 ).getBytes( StandardCharsets.UTF_8 ) );

        // Extract the password hash and salt
        byte[] passwordHash = extractPasswordHash( challenge );
        byte[] salt = extractSalt( challenge );

        // Re-create the hash using the password and the extracted salt
        MessageDigest digest = MessageDigest.getInstance( "SHA" );
        digest.update( password );
        byte[] hash = digest.digest( salt );

        // See if our extracted hash matches what we just re-created
        return Arrays.equals( passwordHash, hash );
    }

    /**
     * Helper method that extracts the hashed password fragment from a supplied salted SHA digest
     * by taking all of the characters before position 20.
     * 
     * @param digest the salted digest, which is assumed to have been
     *            previously decoded from Base64.
     * @return the password hash
     * @throws IllegalArgumentException if the length of the supplied digest is
     *             less than or equal to 20 bytes
     */
    protected static byte[] extractPasswordHash( byte[] digest ) throws IllegalArgumentException
    {
        if( digest.length < 20 )
        {
            throw new IllegalArgumentException( "Hash was less than 20 characters; could not extract password hash!" );
        }

        // Extract the password hash
        byte[] hash = new byte[20];
        for( int i = 0; i < 20; i++ )
        {
            hash[i] = digest[i];
        }

        return hash;
    }

    /**
     * Helper method that extracts the salt from supplied salted digest by taking all of the
     * characters at position 20 and higher.
     * 
     * @param digest the salted digest, which is assumed to have been previously
     *            decoded from Base64.
     * @return the salt
     * @throws IllegalArgumentException if the length of the supplied digest is
     *             less than or equal to 20 bytes
     */
    protected static byte[] extractSalt( byte[] digest ) throws IllegalArgumentException
    {
        if( digest.length <= 20 )
        {
            throw new IllegalArgumentException( "Hash was less than 21 characters; we found no salt!" );
        }

        // Extract the salt
        byte[] salt = new byte[digest.length - 20];
        for( int i = 20; i < digest.length; i++ )
        {
            salt[i - 20] = digest[i];
        }

        return salt;
    }
}
