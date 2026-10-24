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
package org.apache.wiki;

import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.Page;
import org.apache.wiki.api.spi.Wiki;
import org.apache.wiki.cache.CachingManager;
import org.apache.wiki.pages.PageManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Properties;

class PageAliasTest {

    private static void assertAliasRedirects( final boolean cacheEnabled ) throws Exception {
        final Properties props = TestEngine.getTestProperties();
        props.setProperty( CachingManager.PROP_CACHE_ENABLE, Boolean.toString( cacheEnabled ) );
        final TestEngine engine = TestEngine.build( props );
        try {
            engine.saveText( "AliasSource", "[{SET alias='AliasTarget'}]" );
            final Page page = engine.getManager( PageManager.class ).getPage( "AliasSource" );
            final Context context = Wiki.context().create( engine, page );

            Assertions.assertEquals( context.getViewURL( "AliasTarget" ), context.getRedirectURL() );
        } finally {
            engine.stop();
        }
    }

    @Test
    void testAliasRedirectWithPageCache() throws Exception {
        assertAliasRedirects( true );
    }

    @Test
    void testAliasRedirectWithoutPageCache() throws Exception {
        assertAliasRedirects( false );
    }

}
