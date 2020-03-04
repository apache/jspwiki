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


public class HttpUtilTest {

    @Test
    public void testIsIPV4Address() {
        Assertions.assertFalse( HttpUtil.isIPV4Address( null ) );
        Assertions.assertFalse( HttpUtil.isIPV4Address( "" ) );
        Assertions.assertFalse( HttpUtil.isIPV4Address( ".123.123.123.123" ) );
        Assertions.assertFalse( HttpUtil.isIPV4Address( "123.123.123.123." ) );
        Assertions.assertFalse( HttpUtil.isIPV4Address( "123.123.123" ) );
        Assertions.assertFalse( HttpUtil.isIPV4Address( "123.123.123.123.123" ) );
        Assertions.assertFalse( HttpUtil.isIPV4Address( "abc.123.123.123" ) );
        Assertions.assertFalse( HttpUtil.isIPV4Address( "Me" ) );
        Assertions.assertFalse( HttpUtil.isIPV4Address( "Guest" ) );
        Assertions.assertFalse( HttpUtil.isIPV4Address( "1207.0.0.1" ) );
        Assertions.assertFalse( HttpUtil.isIPV4Address( "127..0.1" ) );
        Assertions.assertFalse( HttpUtil.isIPV4Address( "1207.0.0." ) );
        Assertions.assertFalse( HttpUtil.isIPV4Address( ".0.0.1" ) );
        Assertions.assertFalse( HttpUtil.isIPV4Address( "..." ) );

        Assertions.assertTrue( HttpUtil.isIPV4Address( "127.0.0.1" ) );
        Assertions.assertTrue( HttpUtil.isIPV4Address( "12.123.123.123" ) );
        Assertions.assertTrue( HttpUtil.isIPV4Address( "012.123.123.123" ) );
        Assertions.assertTrue( HttpUtil.isIPV4Address( "123.123.123.123" ) );
    }

}
