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

import java.nio.charset.StandardCharsets;


/**
 * Regression tests for the PBKDF2 password-storage format.
 */
public class CryptoUtilPbkdf2Test {

    @Test
    public void testRoundTrip() throws Exception {
        final String entry = CryptoUtil.getPbkdf2SaltedPassword( "test128".getBytes( StandardCharsets.UTF_8 ) );
        Assertions.assertTrue( entry.startsWith( CryptoUtil.PBKDF2_PREFIX ) );
        Assertions.assertTrue( CryptoUtil.verifyPbkdf2SaltedPassword( "test128".getBytes( StandardCharsets.UTF_8 ), entry ) );
    }

    @Test
    public void testWrongPasswordFails() throws Exception {
        final String entry = CryptoUtil.getPbkdf2SaltedPassword( "test128".getBytes( StandardCharsets.UTF_8 ) );
        Assertions.assertFalse( CryptoUtil.verifyPbkdf2SaltedPassword( "TEST128".getBytes( StandardCharsets.UTF_8 ), entry ) );
    }

    @Test
    public void testSaltsDiffer() throws Exception {
        final String one = CryptoUtil.getPbkdf2SaltedPassword( "test128".getBytes( StandardCharsets.UTF_8 ) );
        final String two = CryptoUtil.getPbkdf2SaltedPassword( "test128".getBytes( StandardCharsets.UTF_8 ) );
        Assertions.assertNotEquals( one, two );
    }

    @Test
    public void testNonPbkdf2EntryIsRejected() {
        Assertions.assertThrows( IllegalArgumentException.class,
                () -> CryptoUtil.verifyPbkdf2SaltedPassword( "x".getBytes( StandardCharsets.UTF_8 ), "{SSHA}abc" ) );
    }

    private static byte[] bytes( final String password ) {
        return password.getBytes( StandardCharsets.UTF_8 );
    }

    /** https://issues.apache.org/jira/browse/JSPWIKI-1231 */
    @Test
    public void testSha512RoundTrip() throws Exception {
        final String entry = CryptoUtil.getPbkdf2SaltedPassword( bytes( "test128" ), "PBKDF2-SHA512", 1_000 );
        Assertions.assertTrue( entry.startsWith( "{PBKDF2-SHA512}1000$" ), entry );
        Assertions.assertTrue( CryptoUtil.verifyPbkdf2SaltedPassword( bytes( "test128" ), entry ) );
        Assertions.assertFalse( CryptoUtil.verifyPbkdf2SaltedPassword( bytes( "TEST128" ), entry ) );
        Assertions.assertTrue( CryptoUtil.verifySaltedPassword( bytes( "test128" ), entry ) );
    }

    /** https://issues.apache.org/jira/browse/JSPWIKI-1231 */
    @Test
    public void testSha512HashIsLongerThanSha256() throws Exception {
        final String sha256 = CryptoUtil.getPbkdf2SaltedPassword( bytes( "test128" ), "PBKDF2-SHA256", 1_000 );
        final String sha512 = CryptoUtil.getPbkdf2SaltedPassword( bytes( "test128" ), "PBKDF2-SHA512", 1_000 );
        Assertions.assertEquals( 32, java.util.Base64.getDecoder().decode( sha256.substring( sha256.lastIndexOf( '$' ) + 1 ) ).length );
        Assertions.assertEquals( 64, java.util.Base64.getDecoder().decode( sha512.substring( sha512.lastIndexOf( '$' ) + 1 ) ).length );
    }

    /** https://issues.apache.org/jira/browse/JSPWIKI-1231 */
    @Test
    public void testEntryOfAnotherAlgorithmDoesNotVerifyWithTheSamePassword() throws Exception {
        final byte[] salt = "0123456789abcdef".getBytes( StandardCharsets.UTF_8 );
        final String sha256 = CryptoUtil.getPbkdf2SaltedPassword( bytes( "test128" ), salt, "PBKDF2-SHA256", 1_000 );
        final String sha512 = CryptoUtil.getPbkdf2SaltedPassword( bytes( "test128" ), salt, "PBKDF2-SHA512", 1_000 );
        Assertions.assertNotEquals( sha256.substring( sha256.lastIndexOf( '$' ) ), sha512.substring( sha512.lastIndexOf( '$' ) ) );
    }

    /** https://issues.apache.org/jira/browse/JSPWIKI-1231 */
    @Test
    public void testDefaultIterationsDependOnTheAlgorithm() {
        Assertions.assertEquals( 600_000, CryptoUtil.defaultPbkdf2Iterations( "PBKDF2-SHA256" ) );
        Assertions.assertEquals( 210_000, CryptoUtil.defaultPbkdf2Iterations( "PBKDF2-SHA512" ) );
        Assertions.assertThrows( IllegalArgumentException.class, () -> CryptoUtil.defaultPbkdf2Iterations( "PBKDF2-MD5" ) );
    }

    /** https://issues.apache.org/jira/browse/JSPWIKI-1231 */
    @Test
    public void testSupportedAlgorithms() {
        Assertions.assertTrue( CryptoUtil.isSupportedPbkdf2Algorithm( "PBKDF2-SHA256" ) );
        Assertions.assertTrue( CryptoUtil.isSupportedPbkdf2Algorithm( "PBKDF2-SHA512" ) );
        Assertions.assertFalse( CryptoUtil.isSupportedPbkdf2Algorithm( "PBKDF2-SHA1" ) );
        Assertions.assertFalse( CryptoUtil.isSupportedPbkdf2Algorithm( "SHA-256" ) );
        Assertions.assertFalse( CryptoUtil.isSupportedPbkdf2Algorithm( null ) );
    }

    /** https://issues.apache.org/jira/browse/JSPWIKI-1231 */
    @Test
    public void testUnsupportedAlgorithmIsRejected() {
        Assertions.assertThrows( IllegalArgumentException.class,
                () -> CryptoUtil.getPbkdf2SaltedPassword( bytes( "test128" ), "PBKDF2-SHA1", 1_000 ) );
    }

    /** https://issues.apache.org/jira/browse/JSPWIKI-1231 */
    @Test
    public void testNonPositiveIterationsAreRejected() {
        Assertions.assertThrows( IllegalArgumentException.class,
                () -> CryptoUtil.getPbkdf2SaltedPassword( bytes( "test128" ), "PBKDF2-SHA256", 0 ) );
    }

    /** https://issues.apache.org/jira/browse/JSPWIKI-1231 */
    @Test
    public void testIsPbkdf2Entry() throws Exception {
        Assertions.assertTrue( CryptoUtil.isPbkdf2Entry( CryptoUtil.getPbkdf2SaltedPassword( bytes( "x" ), "PBKDF2-SHA256", 1_000 ) ) );
        Assertions.assertTrue( CryptoUtil.isPbkdf2Entry( CryptoUtil.getPbkdf2SaltedPassword( bytes( "x" ), "PBKDF2-SHA512", 1_000 ) ) );
        Assertions.assertFalse( CryptoUtil.isPbkdf2Entry( "{SSHA}abc" ) );
        Assertions.assertFalse( CryptoUtil.isPbkdf2Entry( "{PBKDF2-SHA1}1000$a$b" ) );
        Assertions.assertFalse( CryptoUtil.isPbkdf2Entry( null ) );
    }

    /** https://issues.apache.org/jira/browse/JSPWIKI-1231 */
    @Test
    public void testHasPbkdf2Parameters() throws Exception {
        final String entry = CryptoUtil.getPbkdf2SaltedPassword( bytes( "x" ), "PBKDF2-SHA512", 5_000 );
        Assertions.assertTrue( CryptoUtil.hasPbkdf2Parameters( entry, "PBKDF2-SHA512", 5_000 ) );
        Assertions.assertFalse( CryptoUtil.hasPbkdf2Parameters( entry, "PBKDF2-SHA512", 5_001 ) );
        Assertions.assertFalse( CryptoUtil.hasPbkdf2Parameters( entry, "PBKDF2-SHA256", 5_000 ) );
        Assertions.assertFalse( CryptoUtil.hasPbkdf2Parameters( "{SSHA}abc", "PBKDF2-SHA512", 5_000 ) );
        Assertions.assertFalse( CryptoUtil.hasPbkdf2Parameters( "{PBKDF2-SHA512}garbage", "PBKDF2-SHA512", 5_000 ) );
    }

    /** The format written by earlier releases keeps verifying. */
    @Test
    public void testEntryWithTheDefaultAlgorithmKeepsTheLegacyPrefix() throws Exception {
        final String entry = CryptoUtil.getPbkdf2SaltedPassword( bytes( "x" ), "PBKDF2-SHA256", 1_000 );
        Assertions.assertTrue( entry.startsWith( CryptoUtil.PBKDF2_PREFIX + "1000$" ), entry );
    }

}
