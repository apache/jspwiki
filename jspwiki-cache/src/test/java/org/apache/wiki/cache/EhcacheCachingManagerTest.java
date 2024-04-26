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
package org.apache.wiki.cache;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Properties;


public class EhcacheCachingManagerTest {

    static EhcacheCachingManager ecm = new EhcacheCachingManager();

    @BeforeAll
    static void beforeAll() throws Exception {
        ecm.initialize( null, new Properties() );
    }

    @Test
    void testInitAndShutdown() throws Exception {
        final Properties props = new Properties();
        props.setProperty( CachingManager.PROP_CACHE_CONF_FILE, "ehcache-jspwiki-test.xml" );
        EhcacheCachingManager ecm = new EhcacheCachingManager();
        ecm.initialize( null, props );
        Assertions.assertEquals( 7, ecm.cacheMap.size() );

        ecm.registerCache( "anotherCache" );
        Assertions.assertEquals( 8, ecm.cacheMap.size() );

        ecm.shutdown();
        ecm.shutdown(); // does nothing if already shutdown
        Assertions.assertEquals( 0, ecm.cacheMap.size() );

        props.setProperty( CachingManager.PROP_CACHE_ENABLE, "false" ); ecm = new EhcacheCachingManager();
        ecm.initialize( null, props );
        Assertions.assertEquals( 0, ecm.cacheMap.size() );
    }

    @Test
    void testEnabled() {
        Assertions.assertTrue( ecm.enabled( CachingManager.CACHE_PAGES ) );
        Assertions.assertFalse( ecm.enabled( "Trucutru" ) );
    }

    @Test
    void testInfo() {
        Assertions.assertNotNull( ecm.info( CachingManager.CACHE_PAGES ) );
        Assertions.assertNull( ecm.info( "Trucutru" ) );
    }

    @Test
    void testPutGetRemoveAndKeys() {
        final String retrieveFromBackend = "item";
        ecm.put( CachingManager.CACHE_PAGES, "key", "test" );
        ecm.put( "trucutru", "key", "test" );
        Assertions.assertEquals( "test", ecm.get( CachingManager.CACHE_PAGES, "key", () -> retrieveFromBackend ) );
        ecm.remove( CachingManager.CACHE_PAGES, "key" );
        ecm.remove( CachingManager.CACHE_PAGES, null );
        Assertions.assertEquals( "item", ecm.get( CachingManager.CACHE_PAGES, "key", () -> retrieveFromBackend ) );
        Assertions.assertEquals( 1, ecm.keys( CachingManager.CACHE_PAGES ).size() );
        Assertions.assertEquals( 0, ecm.keys( "trucutru" ).size() );

        Assertions.assertNull( ecm.get( CachingManager.CACHE_PAGES, null,  () -> retrieveFromBackend ) );
        Assertions.assertNull( ecm.get( "trucutru", "key",  () -> retrieveFromBackend ) );
    }

}
