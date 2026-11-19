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
package org.apache.wiki.references;

import org.apache.wiki.TestEngine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.apache.wiki.TestEngine.with;

/** A page that is inserted in another one with the InsertPage plugin is referenced by the page that inserts it. */
public class InsertPageReferencesTest {

    private static final String[] PAGES = { "IprChild", "IprHost", "IprLinker", "IprOther" };

    private TestEngine engine;

    @BeforeEach
    public void setUp() throws Exception {
        engine = TestEngine.build( with( "jspwiki.cache.enable", "false" ) );
        engine.saveText( "IprChild", "The page that is inserted." );
        engine.saveText( "IprHost", "[{InsertPage page=IprChild}]" );
        engine.saveText( "IprLinker", "A link to [IprChild]." );
        engine.saveText( "IprOther", "Nothing to do with it." );
    }

    @AfterEach
    public void tearDown() {
        for( final String page : PAGES ) {
            engine.deleteTestPage( page );
        }
        TestEngine.emptyWorkDir();
    }

    private Collection< String > referrers( final String page ) {
        return engine.getManager( ReferenceManager.class ).findReferrers( page );
    }

    @Test
    public void testThePageThatInsertsIsAReferrerOfTheInsertedPage() {
        final Collection< String > referrers = referrers( "IprChild" );

        Assertions.assertTrue( referrers.contains( "IprHost" ), "the page that inserts it is a referrer: " + referrers );
        Assertions.assertTrue( referrers.contains( "IprLinker" ), "the page that links to it still is a referrer: " + referrers );
        Assertions.assertFalse( referrers.contains( "IprOther" ), referrers.toString() );
    }

    @Test
    public void testTheReferenceFollowsTheChangesOfTheInsertingPage() throws Exception {
        engine.saveText( "IprHost", "No plugin any more." );

        Assertions.assertFalse( referrers( "IprChild" ).contains( "IprHost" ), "after removing the plugin: " + referrers( "IprChild" ) );
    }

    @Test
    public void testAPluginWithoutThePageParameterIsIgnored() throws Exception {
        engine.saveText( "IprHost", "[{InsertPage}]" );

        Assertions.assertFalse( referrers( "IprChild" ).contains( "IprHost" ) );
    }


    @Test
    public void testAPageWithoutTextHasNoLinks() throws Exception {
        final org.apache.wiki.api.core.Page page = engine.getManager( org.apache.wiki.pages.PageManager.class ).getPage( "IprHost" );

        final Collection< String > links = engine.getManager( ReferenceManager.class ).scanWikiLinks( page, null );

        Assertions.assertTrue( links.isEmpty(), links.toString() );
    }

}
