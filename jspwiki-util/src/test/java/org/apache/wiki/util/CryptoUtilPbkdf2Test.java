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

}
