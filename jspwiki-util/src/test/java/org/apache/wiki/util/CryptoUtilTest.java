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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class CryptoUtilTest {

    @Test
    public void testCommandLineHash() throws Exception {
        // Save old printstream
        final PrintStream oldOut = System.out;

        // Swallow System out and get command output
        final OutputStream out = new ByteArrayOutputStream();
        System.setOut( new PrintStream( out ) );
        CryptoUtil.main( new String[]{ "--hash", "password" } );
        final String output = out.toString();

        // Restore old printstream
        System.setOut( oldOut );

        // Run our tests
        Assertions.assertTrue( output.startsWith( "{SHA-256}" ) );
    }

    @Test
    public void testCommandLineNoVerify() throws Exception {
        // Save old printstream
        final PrintStream oldOut = System.out;

        // Swallow System out and get command output
        final OutputStream out = new ByteArrayOutputStream();
        System.setOut( new PrintStream( out ) );
        // Supply a bogus password
        CryptoUtil.main( new String[]{ "--verify", "password", "{SSHA}yfT8SRT/WoOuNuA6KbJeF10OznZmb28=" } );
        final String output = out.toString();

        // Restore old printstream
        System.setOut( oldOut );

        // Run our tests
        Assertions.assertTrue( output.startsWith( "false" ) );
    }

    @Test
    public void testCommandLineSyntaxError1() {
        // Try verifying password without the {SSHA} prefix
        Assertions.assertThrows( IllegalArgumentException.class, () -> CryptoUtil.main( new String[]{ "--verify", "password", "yfT8SRT/WoOuNuA6KbJeF10OznZmb28=", "{SSHA}" } ) );
    }

    @Test
    public void testCommandLineVerify() throws Exception {
        // Save old printstream
        final PrintStream oldOut = System.out;

        // Swallow System out and get command output
        final OutputStream out = new ByteArrayOutputStream();
        System.setOut( new PrintStream( out ) );
        CryptoUtil.main( new String[]{ "--verify", "testing123", "{SSHA}yfT8SRT/WoOuNuA6KbJeF10OznZmb28=" } );
        final String output = out.toString();

        // Restore old printstream
        System.setOut( oldOut );

        // Run our tests
        Assertions.assertTrue( output.startsWith( "true" ) );
    }

    @Test
    public void testExtractHash() {
        byte[] digest;

        digest = Base64.getDecoder().decode( "yfT8SRT/WoOuNuA6KbJeF10OznZmb28=".getBytes() );
        Assertions.assertEquals( "foo", new String( CryptoUtil.extractSalt( digest, 20 ) ) );

        digest = Base64.getDecoder().decode( "tAVisOOQGAeVyP8UMFQY9qi83lxsb09e".getBytes() );
        Assertions.assertEquals( "loO^", new String( CryptoUtil.extractSalt( digest, 20 ) ) );

        digest = Base64.getDecoder().decode( "BZaDYvB8czmNW3MjR2j7/mklODV0ZXN0eQ==".getBytes() );
        Assertions.assertEquals( "testy", new String( CryptoUtil.extractSalt( digest, 20 ) ) );
    }

    @Test
    public void testGetSaltedPassword() throws Exception {
        byte[] password;

        // Generate a hash with a known password and salt
        password = "testing123".getBytes();
        Assertions.assertEquals( "{SSHA}yfT8SRT/WoOuNuA6KbJeF10OznZmb28=", CryptoUtil.getSaltedPassword( password, "foo".getBytes(), "{SSHA}" ) );

        // Generate two hashes with a known password and 2 different salts
        password = "password".getBytes();
        Assertions.assertEquals( "{SSHA}tAVisOOQGAeVyP8UMFQY9qi83lxsb09e", CryptoUtil.getSaltedPassword( password, "loO^".getBytes(), "{SSHA}" ) );
        Assertions.assertEquals( "{SSHA}BZaDYvB8czmNW3MjR2j7/mklODV0ZXN0eQ==", CryptoUtil.getSaltedPassword( password, "testy".getBytes(), "{SSHA}" ) );
    }

    @Test
    public void testMultipleHashes() throws Exception {
        final String p1 = CryptoUtil.getSaltedPassword( "password".getBytes(), "{SSHA}" );
        final String p2 = CryptoUtil.getSaltedPassword( "password".getBytes(), "{SSHA}" );
        final String p3 = CryptoUtil.getSaltedPassword( "password".getBytes(), "{SSHA}" );
        Assertions.assertNotSame( p1, p2 );
        Assertions.assertNotSame( p2, p3 );
        Assertions.assertNotSame( p1, p3 );
    }

    @Test
    public void testSaltedPasswordLength() throws Exception {
        // Generate a hash with a known password and salt
        final byte[] password = "mySooperRandomPassword".getBytes();
        final String hash = CryptoUtil.getSaltedPassword( password, "salt".getBytes(), "{SSHA}" );

        // slappasswd says that a 4-byte salt should give us 6 chars for prefix
        // + 20 chars for the hash + 12 for salt (38 total)
        Assertions.assertEquals( 38, hash.length() );
    }

    @Test
    public void verifySaltedPassword() throws Exception {
        // Verify with a known digest
        byte[] password = "testing123".getBytes( StandardCharsets.UTF_8 );
        Assertions.assertTrue( CryptoUtil.verifySaltedPassword( password, "{SSHA}yfT8SRT/WoOuNuA6KbJeF10OznZmb28=" ) );

        // Verify with two more known digests
        password = "password".getBytes();
        Assertions.assertTrue( CryptoUtil.verifySaltedPassword( password, "{SSHA}tAVisOOQGAeVyP8UMFQY9qi83lxsb09e" ) );
        Assertions.assertTrue( CryptoUtil.verifySaltedPassword( password, "{SSHA}BZaDYvB8czmNW3MjR2j7/mklODV0ZXN0eQ==" ) );

        // Verify with three consecutive random generations (based on slappasswd)
        password = "testPassword".getBytes();
        Assertions.assertTrue( CryptoUtil.verifySaltedPassword( password, "{SSHA}t2tfJHm/QZYUh0OZ8tkm05l2LLbuc3ZF" ) );
        Assertions.assertTrue( CryptoUtil.verifySaltedPassword( password, "{SSHA}0FKV9iM2cA5bAMws7mSgwg+zik/GT+wy" ) );
        Assertions.assertTrue( CryptoUtil.verifySaltedPassword( password, "{SSHA}/0Dzvh+8+w0YO673Qr7vqEOmdeMSrbGG" ) );
    }

}
