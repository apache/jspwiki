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
package org.apache.wiki.plugin;

import org.apache.wiki.HttpMockFactory;
import org.apache.wiki.TestEngine;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.spi.Wiki;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.wiki.TestEngine.with;

class IndexPluginTest {

    private static final String INDEX = "{INSERT org.apache.wiki.plugin.IndexPlugin}";
    private static final Pattern BODY = Pattern.compile( "<div[^>]*class=\"body\"[^>]*>(.*?)</div>" );
    private static final Pattern ENTRY = Pattern.compile( "<a [^>]*>([^<]*)</a>" );

    private static final String[] PAGES = { "IdxAlpha", "IdxApple", "IdxBeta", "IdxMixed", "IDXMIXED", "idxMixed" };

    private TestEngine engine;
    private Context context;

    @BeforeEach
    void setUp() {
        engine = TestEngine.build( with( "jspwiki.cache.enable", "false" ) );
        context = Wiki.context().create( engine, HttpMockFactory.createHttpRequest(), Wiki.contents().page( engine, "IdxAlpha" ) );
    }

    @AfterEach
    void tearDown() {
        for( final String page : PAGES ) {
            engine.deleteTestPage( page );
        }
    }

    /** The names listed by the plugin, section by section, in the order they appear. */
    private List< List< String > > sections( final String html ) {
        final List< List< String > > sections = new ArrayList<>();
        final Matcher body = BODY.matcher( html );
        while( body.find() ) {
            final List< String > names = new ArrayList<>();
            final Matcher entry = ENTRY.matcher( body.group( 1 ) );
            while( entry.find() ) {
                names.add( entry.group( 1 ) );
            }
            sections.add( names );
        }
        return sections;
    }

    @Test
    void testPagesAreGroupedByTheirFirstLetter() throws Exception {
        engine.saveText( "IdxAlpha", "alpha" );
        engine.saveText( "IdxApple", "apple" );
        engine.saveText( "IdxBeta", "beta" );

        final List< List< String > > sections = sections( engine.getManager( PluginManager.class ).execute( context, INDEX ) );

        Assertions.assertEquals( 1, sections.size(), "all the names start with the same letter" );
        final List< String > names = sections.get( 0 );
        Assertions.assertTrue( names.containsAll( List.of( "IdxAlpha", "IdxApple", "IdxBeta" ) ), names.toString() );
        Assertions.assertTrue( names.indexOf( "IdxAlpha" ) < names.indexOf( "IdxApple" ), "sorted: " + names );
        Assertions.assertTrue( names.indexOf( "IdxApple" ) < names.indexOf( "IdxBeta" ), "sorted: " + names );
    }

    @Test
    void testThereAreNoEmptyEntries() throws Exception {
        engine.saveText( "IdxAlpha", "alpha" );
        engine.saveText( "IdxBeta", "beta" );

        final String html = engine.getManager( PluginManager.class ).execute( context, INDEX );

        sections( html ).forEach( names -> names.forEach( name -> Assertions.assertFalse( name.isBlank(), "empty entry in " + names ) ) );
        Assertions.assertFalse( html.contains( ", ," ), html );
        Assertions.assertFalse( html.contains( ", </div>" ), html );
    }

    /** JSPWIKI-572: the pages whose names differ only in the case of a letter are all listed. */
    @Test
    void testPagesWhoseNamesDifferOnlyInCaseAreAllListed() throws Exception {
        Assumptions.assumeTrue( fileSystemIsCaseSensitive(), "the page files of these names would be the same file" );
        engine.saveText( "IdxMixed", "one" );
        engine.saveText( "IDXMIXED", "two" );
        engine.saveText( "idxMixed", "three" );

        final List< String > listed = new ArrayList<>();
        sections( engine.getManager( PluginManager.class ).execute( context, INDEX ) ).forEach( listed::addAll );

        Assertions.assertTrue( listed.containsAll( List.of( "IdxMixed", "IDXMIXED", "idxMixed" ) ), listed.toString() );
    }

    private static boolean fileSystemIsCaseSensitive() throws IOException {
        final Path dir = Files.createTempDirectory( "case" );
        final Path lower = dir.resolve( "ab" );
        try {
            Files.createFile( lower );
            return !Files.exists( dir.resolve( "AB" ) );
        } finally {
            Files.deleteIfExists( lower );
            Files.deleteIfExists( dir );
        }
    }

}
