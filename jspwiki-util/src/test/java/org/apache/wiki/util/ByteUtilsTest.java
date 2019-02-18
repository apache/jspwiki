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

import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class ByteUtilsTest {

    final byte[] bytes = new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 63, 127 };
    final String EXPECTED_HEX_STRING = "000102030405060708090a0b0c0d0e0f3f7f";

    @Test
    public void testByteUtilsConvertBytes()
    {
        String hex = ByteUtils.bytes2hex(bytes);
        Assertions.assertEquals(EXPECTED_HEX_STRING, hex);
    }

    @Test
    public void testConvertHexToBytes()
    {
        byte[] reconstructedBytes = ByteUtils.parseHexBinary(EXPECTED_HEX_STRING);
        Assertions.assertEquals(bytes.length,reconstructedBytes.length);
        Assertions.assertTrue(Arrays.equals(bytes,reconstructedBytes));
    }

    @Test
    public void testByteUtilsConvertByte()
    {
        Assertions.assertEquals("0",  ByteUtils.byte2hex((byte)0));
        Assertions.assertEquals("f",  ByteUtils.byte2hex((byte)15));
        Assertions.assertEquals("10", ByteUtils.byte2hex((byte)16));
        Assertions.assertEquals("7f", ByteUtils.byte2hex((byte)127));
    }

}
