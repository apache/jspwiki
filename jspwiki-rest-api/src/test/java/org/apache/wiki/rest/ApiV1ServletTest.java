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
package org.apache.wiki.rest;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.wiki.HttpMockFactory;
import org.apache.wiki.TestEngine;
import org.apache.wiki.api.core.Attachment;
import org.apache.wiki.api.core.Session;
import org.apache.wiki.api.spi.Wiki;
import org.apache.wiki.attachment.AttachmentManager;
import org.apache.wiki.auth.AuthenticationManager;
import org.apache.wiki.auth.AuthorizationManager;
import org.apache.wiki.auth.Users;
import org.apache.wiki.pages.PageManager;
import org.apache.wiki.search.SearchManager;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.yaml.snakeyaml.Yaml;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

class ApiV1ServletTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final Map< String, Object > CONTRACT = contract();

    private TestEngine engine;

    @AfterEach
    void tearDown() {
        if( engine != null ) {
            engine.stop();
        }
    }

    @Test
    void searchAnswersWithTheContract() throws Exception {
        startEngine( null );
        engine.saveText( "ApiSearchPage", "The dawn of the third age of mankind." );
        engine.saveText( "ApiSearchOther", "Another page about mankind." );

        final Reply[] reply = new Reply[ 1 ];
        Awaitility.await( "both pages are indexed" ).ignoreExceptions().until( () -> {
            reply[ 0 ] = search( null, "query", "mankind" );
            return reply[ 0 ].json().size() == 2;
        } );

        assertMatchesContract( "/search", reply[ 0 ] );
        Assertions.assertEquals( 200, reply[ 0 ].status );
        final JsonNode first = reply[ 0 ].json().get( 0 );
        Assertions.assertTrue( first.get( "page" ).asString().startsWith( "ApiSearch" ) );
    }

    @Test
    void searchWithoutMatchesAnswersWithAnEmptyList() throws Exception {
        startEngine( null );

        final Reply[] answer = new Reply[ 1 ];
        Awaitility.await( "the index is built" ).until( () -> {
            answer[ 0 ] = search( null, "query", "nothingwillmatchthis" );
            return answer[ 0 ].status != 503;
        } );
        final Reply reply = answer[ 0 ];

        assertMatchesContract( "/search", reply );
        Assertions.assertEquals( 200, reply.status );
        Assertions.assertEquals( 0, reply.json().size() );
    }

    @Test
    void searchReturnsAtMostMaxEntries() throws Exception {
        startEngine( null );
        engine.saveText( "ApiMaxOne", "common words here" );
        engine.saveText( "ApiMaxTwo", "common words there" );
        Awaitility.await( "both pages are indexed" ).ignoreExceptions().until( () -> search( null, "query", "common" ).json().size() == 2 );

        final Reply reply = search( null, "query", "common", "max", "1" );

        assertMatchesContract( "/search", reply );
        Assertions.assertEquals( 1, reply.json().size() );
    }

    @Test
    void searchWithoutQueryIsABadRequest() throws Exception {
        startEngine( null );

        final Reply reply = search( null );

        assertMatchesContract( "/search", reply );
        Assertions.assertEquals( 400, reply.status );
    }

    @Test
    void searchWithAQueryThatCannotBeParsedIsABadRequest() throws Exception {
        startEngine( null );

        final Reply[] answer = new Reply[ 1 ];
        Awaitility.await( "the index is built" ).until( () -> {
            answer[ 0 ] = search( null, "query", "\"unbalanced" );
            return answer[ 0 ].status != 503;
        } );
        final Reply reply = answer[ 0 ];

        assertMatchesContract( "/search", reply );
        Assertions.assertEquals( 400, reply.status );
    }

    @Test
    void searchWithAMaxThatIsNotAPositiveIntegerIsABadRequest() throws Exception {
        startEngine( null );

        for( final String max : new String[] { "0", "-3", "many", "" } ) {
            final Reply reply = search( null, "query", "mankind", "max", max );

            assertMatchesContract( "/search", reply );
            Assertions.assertEquals( 400, reply.status, "max=" + max );
        }
    }

    @Test
    void searchByAnAnonymousCallerWithoutPermissionIs401( @TempDir final Path dir ) throws Exception {
        startEngine( policy( dir, "Authenticated" ) );

        final Reply reply = search( null, "query", "mankind" );

        assertMatchesContract( "/search", reply );
        Assertions.assertEquals( 401, reply.status );
    }

    @Test
    void searchByALoggedInCallerWithoutPermissionIs403( @TempDir final Path dir ) throws Exception {
        startEngine( policy( dir, "Admin" ) );
        final HttpServletRequest request = request( "query", "mankind" );
        final Session session = Wiki.session().find( engine, request );
        engine.getManager( AuthenticationManager.class ).login( session, request, Users.JANNE, Users.JANNE_PASS );
        Assertions.assertTrue( session.isAuthenticated() );

        final Reply reply = call( request );

        assertMatchesContract( "/search", reply );
        Assertions.assertEquals( 403, reply.status );
    }

    @Test
    void searchBeforeTheIndexIsBuiltIs503() throws Exception {
        startEngine( null, "600" );

        final Reply reply = search( null, "query", "mankind" );

        assertMatchesContract( "/search", reply );
        Assertions.assertEquals( 503, reply.status );
    }

    @Test
    void suggestionsAnswerWithTheContract() throws Exception {
        startEngine( null );
        engine.saveText( "ApiSugOne", "one" );
        engine.saveText( "ApiSugTwo", "two" );
        engine.saveText( "ApiOther", "other" );

        final Reply reply = call( requestTo( "/search/suggestions", "query", "ApiSug" ) );

        assertMatchesContract( "/search/suggestions", reply );
        Assertions.assertEquals( 200, reply.status );
        Assertions.assertEquals( Set.of( "ApiSugOne", "ApiSugTwo" ), names( reply ) );
    }

    @Test
    void suggestionsReturnAtMostMaxEntries() throws Exception {
        startEngine( null );
        engine.saveText( "ApiSugOne", "one" );
        engine.saveText( "ApiSugTwo", "two" );

        final Reply reply = call( requestTo( "/search/suggestions", "query", "ApiSug", "max", "1" ) );

        assertMatchesContract( "/search/suggestions", reply );
        Assertions.assertEquals( 1, reply.json().size() );
    }

    @Test
    void suggestionsLeaveOutThePagesTheCallerMayNotView() throws Exception {
        startEngine( null );
        engine.saveText( "ApiSugOpen", "open" );
        engine.saveText( "ApiSugSecret", "[{ALLOW view Admin}] secret" );

        final Reply reply = call( requestTo( "/search/suggestions", "query", "ApiSug" ) );

        assertMatchesContract( "/search/suggestions", reply );
        Assertions.assertEquals( Set.of( "ApiSugOpen" ), names( reply ) );
    }

    @Test
    void suggestionsWithoutQueryAreABadRequest() throws Exception {
        startEngine( null );

        final Reply reply = call( requestTo( "/search/suggestions" ) );

        assertMatchesContract( "/search/suggestions", reply );
        Assertions.assertEquals( 400, reply.status );
    }

    @Test
    void suggestionsByAnAnonymousCallerWithoutPermissionAre401( @TempDir final Path dir ) throws Exception {
        startEngine( policy( dir, "Authenticated" ) );

        final Reply reply = call( requestTo( "/search/suggestions", "query", "Api" ) );

        assertMatchesContract( "/search/suggestions", reply );
        Assertions.assertEquals( 401, reply.status );
    }

    @Test
    void pluginsAnswerWithTheContractSortedByDisplayName() throws Exception {
        startEngine( null );

        final Reply reply = call( requestTo( "/plugins" ) );

        assertMatchesContract( "/plugins", reply );
        Assertions.assertEquals( 200, reply.status );
        final List< String > displayNames = new ArrayList<>();
        reply.json().forEach( entry -> displayNames.add( entry.get( "displayName" ).asString() ) );
        Assertions.assertFalse( displayNames.isEmpty(), "the wiki has plugins" );
        final List< String > sorted = new ArrayList<>( displayNames );
        sorted.sort( null );
        Assertions.assertEquals( sorted, displayNames );
    }

    @Test
    void pluginsByALoggedInCallerWithoutPermissionAre403( @TempDir final Path dir ) throws Exception {
        startEngine( policy( dir, "Admin" ) );
        final HttpServletRequest request = requestTo( "/plugins" );
        final Session session = Wiki.session().find( engine, request );
        engine.getManager( AuthenticationManager.class ).login( session, request, Users.JANNE, Users.JANNE_PASS );

        final Reply reply = call( request );

        assertMatchesContract( "/plugins", reply );
        Assertions.assertEquals( 403, reply.status );
    }

    @Test
    void anUnknownResourceIs404() throws Exception {
        startEngine( null );
        final HttpServletRequest request = request();
        Mockito.doReturn( "/nothing" ).when( request ).getPathInfo();

        final Reply reply = call( request );

        Assertions.assertEquals( 404, reply.status );
        Assertions.assertEquals( 404, reply.json().get( "status" ).asInt() );
    }

    @Test
    void pageAnswersWithTheContract() throws Exception {
        startEngine( null );
        engine.saveText( "ApiReadPage", "Some *markup* here." );

        final Reply reply = call( requestTo( "/pages/ApiReadPage" ) );

        assertMatchesContract( "/pages/{name}", reply );
        Assertions.assertEquals( 200, reply.status );
        final JsonNode page = reply.json();
        Assertions.assertEquals( "ApiReadPage", page.get( "name" ).asString() );
        Assertions.assertEquals( 1, page.get( "version" ).asInt() );
        Assertions.assertTrue( page.get( "text" ).asString().contains( "Some *markup* here." ) );
    }

    @Test
    void pageInAnOlderVersionAnswersWithThatText() throws Exception {
        startEngine( null, "0", "VersioningFileProvider" );
        engine.saveText( "ApiReadVersions", "first text" );
        engine.saveText( "ApiReadVersions", "second text" );

        final Reply latest = call( requestTo( "/pages/ApiReadVersions" ) );
        final Reply first = call( requestTo( "/pages/ApiReadVersions", "version", "1" ) );

        assertMatchesContract( "/pages/{name}", first );
        Assertions.assertEquals( 2, latest.json().get( "version" ).asInt() );
        Assertions.assertTrue( latest.json().get( "text" ).asString().contains( "second text" ) );
        Assertions.assertEquals( 1, first.json().get( "version" ).asInt() );
        Assertions.assertTrue( first.json().get( "text" ).asString().contains( "first text" ) );
    }

    @Test
    void pageThatDoesNotExistIs404() throws Exception {
        startEngine( null );

        final Reply reply = call( requestTo( "/pages/ApiNoSuchPage" ) );

        assertMatchesContract( "/pages/{name}", reply );
        Assertions.assertEquals( 404, reply.status );
    }

    @Test
    void pageWithAVersionThatIsNotAPositiveIntegerIsABadRequest() throws Exception {
        startEngine( null );
        engine.saveText( "ApiReadBadVersion", "text" );

        for( final String version : new String[] { "0", "-1", "last", "" } ) {
            final Reply reply = call( requestTo( "/pages/ApiReadBadVersion", "version", version ) );

            assertMatchesContract( "/pages/{name}", reply );
            Assertions.assertEquals( 400, reply.status, "version=" + version );
        }
    }

    @Test
    void pageTheCallerMayNotViewIs401ForAnAnonymousCallerAnd403ForALoggedInOne() throws Exception {
        startEngine( null );
        engine.saveText( "ApiReadSecret", "[{ALLOW view Admin}] secret" );

        final Reply anonymous = call( requestTo( "/pages/ApiReadSecret" ) );
        final HttpServletRequest request = requestTo( "/pages/ApiReadSecret" );
        final Session session = Wiki.session().find( engine, request );
        engine.getManager( AuthenticationManager.class ).login( session, request, Users.JANNE, Users.JANNE_PASS );
        final Reply loggedIn = call( request );

        assertMatchesContract( "/pages/{name}", anonymous );
        assertMatchesContract( "/pages/{name}", loggedIn );
        Assertions.assertEquals( 401, anonymous.status );
        Assertions.assertEquals( 403, loggedIn.status );
        Assertions.assertFalse( anonymous.body.toString().contains( "secret" ) );
        Assertions.assertFalse( loggedIn.body.toString().contains( "secret" ) );
    }

    @Test
    void historyAnswersWithTheVersionsLatestFirst() throws Exception {
        startEngine( null, "0", "VersioningFileProvider" );
        engine.saveText( "ApiReadHistory", "one" );
        engine.saveText( "ApiReadHistory", "two" );
        engine.saveText( "ApiReadHistory", "three" );

        final Reply reply = call( requestTo( "/pages/ApiReadHistory/history" ) );

        assertMatchesContract( "/pages/{name}/history", reply );
        Assertions.assertEquals( 200, reply.status );
        final List< Integer > versions = new ArrayList<>();
        reply.json().forEach( entry -> versions.add( entry.get( "version" ).asInt() ) );
        Assertions.assertEquals( List.of( 3, 2, 1 ), versions );
    }

    @Test
    void historyOfAPageWithoutOldVersionsListsTheCurrentOne() throws Exception {
        startEngine( null, "0", "VersioningFileProvider" );
        Files.write( Path.of( engine.getWikiProperties().getProperty( "jspwiki.fileSystemProvider.pageDir" ), "ApiReadByHand.txt" ),
                     "put here by hand".getBytes( StandardCharsets.UTF_8 ) );

        final Reply reply = call( requestTo( "/pages/ApiReadByHand/history" ) );

        assertMatchesContract( "/pages/{name}/history", reply );
        Assertions.assertEquals( 200, reply.status );
        Assertions.assertEquals( 1, reply.json().size() );
        Assertions.assertEquals( 1, reply.json().get( 0 ).get( "version" ).asInt() );
    }

    @Test
    void historyOfAPageThatDoesNotExistIs404() throws Exception {
        startEngine( null );

        final Reply reply = call( requestTo( "/pages/ApiNoSuchPage/history" ) );

        assertMatchesContract( "/pages/{name}/history", reply );
        Assertions.assertEquals( 404, reply.status );
    }

    @Test
    void historyOfAPageTheCallerMayNotViewIs401() throws Exception {
        startEngine( null );
        engine.saveText( "ApiReadSecretHistory", "[{ALLOW view Admin}] secret" );

        final Reply reply = call( requestTo( "/pages/ApiReadSecretHistory/history" ) );

        assertMatchesContract( "/pages/{name}/history", reply );
        Assertions.assertEquals( 401, reply.status );
    }

    @Test
    void attachmentsAnswerWithTheContract() throws Exception {
        startEngine( null );
        engine.saveText( "ApiReadAttachments", "text" );
        final Attachment attachment = Wiki.contents().attachment( engine, "ApiReadAttachments", "b.txt" );
        engine.getManager( AttachmentManager.class ).storeAttachment( attachment, new ByteArrayInputStream( "hello".getBytes( StandardCharsets.UTF_8 ) ) );
        final Attachment other = Wiki.contents().attachment( engine, "ApiReadAttachments", "a.txt" );
        engine.getManager( AttachmentManager.class ).storeAttachment( other, new ByteArrayInputStream( "hi".getBytes( StandardCharsets.UTF_8 ) ) );

        final Reply reply = call( requestTo( "/pages/ApiReadAttachments/attachments" ) );

        assertMatchesContract( "/pages/{name}/attachments", reply );
        Assertions.assertEquals( 200, reply.status );
        final JsonNode entries = reply.json();
        Assertions.assertEquals( 2, entries.size() );
        Assertions.assertEquals( "a.txt", entries.get( 0 ).get( "name" ).asString() );
        Assertions.assertEquals( 2, entries.get( 0 ).get( "size" ).asInt() );
        Assertions.assertEquals( "b.txt", entries.get( 1 ).get( "name" ).asString() );
    }

    @Test
    void attachmentsOfAPageWithoutAttachmentsAreAnEmptyList() throws Exception {
        startEngine( null );
        engine.saveText( "ApiReadNoAttachments", "text" );

        final Reply reply = call( requestTo( "/pages/ApiReadNoAttachments/attachments" ) );

        assertMatchesContract( "/pages/{name}/attachments", reply );
        Assertions.assertEquals( 200, reply.status );
        Assertions.assertEquals( 0, reply.json().size() );
    }

    @Test
    void attachmentsOfAPageTheCallerMayNotViewAre401() throws Exception {
        startEngine( null );
        engine.saveText( "ApiReadSecretAttachments", "[{ALLOW view Admin}] secret" );

        final Reply reply = call( requestTo( "/pages/ApiReadSecretAttachments/attachments" ) );

        assertMatchesContract( "/pages/{name}/attachments", reply );
        Assertions.assertEquals( 401, reply.status );
    }

    @Test
    void attachmentsOfAPageThatDoesNotExistAre404() throws Exception {
        startEngine( null );

        final Reply reply = call( requestTo( "/pages/ApiNoSuchPage/attachments" ) );

        assertMatchesContract( "/pages/{name}/attachments", reply );
        Assertions.assertEquals( 404, reply.status );
    }

    private void store( final String page, final String file, final String content ) throws Exception {
        final Attachment attachment = Wiki.contents().attachment( engine, page, file );
        engine.getManager( AttachmentManager.class ).storeAttachment( attachment, new ByteArrayInputStream( content.getBytes( StandardCharsets.UTF_8 ) ) );
    }

    @Test
    void anAttachmentIsDownloadedWithItsContentAndTypeAsADownload() throws Exception {
        startEngine( null );
        engine.saveText( "ApiDownload", "text" );
        store( "ApiDownload", "b.txt", "hello" );
        Mockito.doReturn( "text/plain" ).when( engine.getServletContext() ).getMimeType( "b.txt" );

        final Reply reply = call( requestTo( "/pages/ApiDownload/attachments/b.txt" ) );

        Assertions.assertEquals( 200, reply.status );
        Assertions.assertEquals( "text/plain", reply.contentType );
        Assertions.assertEquals( "hello", reply.bytes.toString( StandardCharsets.UTF_8 ) );
        Assertions.assertEquals( 5, reply.length );
        Assertions.assertEquals( "attachment; filename=\"b.txt\";", reply.headers.get( "Content-Disposition" ) );
        Assertions.assertEquals( "nosniff", reply.headers.get( "X-Content-Type-Options" ) );
    }

    @Test
    void anAttachmentOfAnUnknownTypeIsSentAsOctetStream() throws Exception {
        startEngine( null );
        engine.saveText( "ApiDownloadType", "text" );
        store( "ApiDownloadType", "data.zzzz", "1234" );

        final Reply reply = call( requestTo( "/pages/ApiDownloadType/attachments/data.zzzz" ) );

        Assertions.assertEquals( 200, reply.status );
        Assertions.assertEquals( "application/octet-stream", reply.contentType );
    }

    @Test
    void aWebPageAsAttachmentIsNeverSentInline() throws Exception {
        startEngine( null );
        engine.saveText( "ApiDownloadHtml", "text" );
        store( "ApiDownloadHtml", "x.html", "<script>alert(1)</script>" );
        Mockito.doReturn( "text/html" ).when( engine.getServletContext() ).getMimeType( "x.html" );

        final Reply reply = call( requestTo( "/pages/ApiDownloadHtml/attachments/x.html" ) );

        Assertions.assertEquals( 200, reply.status );
        Assertions.assertTrue( reply.headers.get( "Content-Disposition" ).startsWith( "attachment;" ), reply.headers.toString() );
        Assertions.assertEquals( "nosniff", reply.headers.get( "X-Content-Type-Options" ) );
    }

    @Test
    void aVersionOfAnAttachmentCanBeDownloaded() throws Exception {
        startEngine( null );
        engine.saveText( "ApiDownloadVersions", "text" );
        store( "ApiDownloadVersions", "v.txt", "first" );
        store( "ApiDownloadVersions", "v.txt", "second" );

        final Reply latest = call( requestTo( "/pages/ApiDownloadVersions/attachments/v.txt" ) );
        final Reply first = call( requestTo( "/pages/ApiDownloadVersions/attachments/v.txt", "version", "1" ) );

        Assertions.assertEquals( "second", latest.bytes.toString( StandardCharsets.UTF_8 ) );
        Assertions.assertEquals( "first", first.bytes.toString( StandardCharsets.UTF_8 ) );
    }

    @Test
    void anAttachmentThatDoesNotExistIs404() throws Exception {
        startEngine( null );
        engine.saveText( "ApiDownloadMissing", "text" );

        final Reply reply = call( requestTo( "/pages/ApiDownloadMissing/attachments/none.txt" ) );

        assertMatchesContract( "/pages/{name}/attachments/{file}", reply );
        Assertions.assertEquals( 404, reply.status );
    }

    @Test
    void anAttachmentOfAPageThatDoesNotExistIs404() throws Exception {
        startEngine( null );

        final Reply reply = call( requestTo( "/pages/ApiDownloadNoPage/attachments/a.txt" ) );

        assertMatchesContract( "/pages/{name}/attachments/{file}", reply );
        Assertions.assertEquals( 404, reply.status );
    }

    @Test
    void anAttachmentOfAPageTheCallerMayNotViewIs401AndItsContentIsNotSent() throws Exception {
        startEngine( null );
        engine.saveText( "ApiDownloadSecret", "[{ALLOW view Admin}] secret" );
        store( "ApiDownloadSecret", "s.txt", "secret content" );

        final Reply reply = call( requestTo( "/pages/ApiDownloadSecret/attachments/s.txt" ) );

        assertMatchesContract( "/pages/{name}/attachments/{file}", reply );
        Assertions.assertEquals( 401, reply.status );
        Assertions.assertEquals( 0, reply.bytes.size() );
    }

    @Test
    void aFileNameThatLeavesTheDirectoryIs400() throws Exception {
        startEngine( null );
        engine.saveText( "ApiDownloadDots", "text" );

        final Reply reply = call( requestTo( "/pages/ApiDownloadDots/attachments/..", "version", "1" ) );

        assertMatchesContract( "/pages/{name}/attachments/{file}", reply );
        Assertions.assertEquals( 400, reply.status );
    }

    @Test
    void aVersionThatIsNotANumberIs400() throws Exception {
        startEngine( null );
        engine.saveText( "ApiDownloadBadVersion", "text" );
        store( "ApiDownloadBadVersion", "a.txt", "a" );

        final Reply reply = call( requestTo( "/pages/ApiDownloadBadVersion/attachments/a.txt", "version", "x" ) );

        assertMatchesContract( "/pages/{name}/attachments/{file}", reply );
        Assertions.assertEquals( 400, reply.status );
    }

    @Test
    void theDifferenceBetweenTwoVersionsAnswersWithTheContract() throws Exception {
        startEngine( null, "0", "VersioningFileProvider" );
        engine.saveText( "ApiDiff", "first line" );
        engine.saveText( "ApiDiff", "first line\nsecond added line" );

        final Reply reply = call( requestTo( "/pages/ApiDiff/diff", "from", "1", "to", "2" ) );

        assertMatchesContract( "/pages/{name}/diff", reply );
        Assertions.assertEquals( 200, reply.status );
        Assertions.assertEquals( "ApiDiff", reply.json().get( "name" ).asString() );
        Assertions.assertEquals( 1, reply.json().get( "from" ).asInt() );
        Assertions.assertEquals( 2, reply.json().get( "to" ).asInt() );
        Assertions.assertTrue( reply.json().get( "html" ).asString().contains( "second added line" ), reply.body.toString() );
        Assertions.assertTrue( reply.json().get( "html" ).asString().contains( "diffadd" ), "an added line is marked: " + reply.body );
    }

    @Test
    void theDifferenceGoesToTheLatestVersionWhenToIsAbsent() throws Exception {
        startEngine( null, "0", "VersioningFileProvider" );
        engine.saveText( "ApiDiffLatest", "one" );
        engine.saveText( "ApiDiffLatest", "one two" );
        engine.saveText( "ApiDiffLatest", "one two three" );

        final Reply reply = call( requestTo( "/pages/ApiDiffLatest/diff", "from", "1" ) );

        Assertions.assertEquals( 200, reply.status );
        Assertions.assertEquals( 3, reply.json().get( "to" ).asInt() );
    }

    @Test
    void theDifferenceOfAVersionWithItselfHasNoMarkedChange() throws Exception {
        startEngine( null, "0", "VersioningFileProvider" );
        engine.saveText( "ApiDiffSame", "one" );
        engine.saveText( "ApiDiffSame", "one two" );

        final Reply reply = call( requestTo( "/pages/ApiDiffSame/diff", "from", "2", "to", "2" ) );

        assertMatchesContract( "/pages/{name}/diff", reply );
        Assertions.assertEquals( 200, reply.status );
        final String html = reply.json().get( "html" ).asString();
        Assertions.assertFalse( html.contains( "diffadd" ) || html.contains( "diffrem" ) || html.contains( "diffchange" ), html );
    }

    @Test
    void theDifferenceNeedsTheParameterFrom() throws Exception {
        startEngine( null, "0", "VersioningFileProvider" );
        engine.saveText( "ApiDiffNoFrom", "one" );

        final Reply reply = call( requestTo( "/pages/ApiDiffNoFrom/diff" ) );

        assertMatchesContract( "/pages/{name}/diff", reply );
        Assertions.assertEquals( 400, reply.status );
    }

    @Test
    void aVersionThatIsNotANumberInTheDifferenceIs400() throws Exception {
        startEngine( null, "0", "VersioningFileProvider" );
        engine.saveText( "ApiDiffBad", "one" );

        final Reply reply = call( requestTo( "/pages/ApiDiffBad/diff", "from", "1", "to", "x" ) );

        assertMatchesContract( "/pages/{name}/diff", reply );
        Assertions.assertEquals( 400, reply.status );
    }

    @Test
    void theDifferenceWithAVersionThatDoesNotExistIs404() throws Exception {
        startEngine( null, "0", "VersioningFileProvider" );
        engine.saveText( "ApiDiffMissing", "one" );
        engine.saveText( "ApiDiffMissing", "one two" );

        final Reply reply = call( requestTo( "/pages/ApiDiffMissing/diff", "from", "1", "to", "9" ) );

        assertMatchesContract( "/pages/{name}/diff", reply );
        Assertions.assertEquals( 404, reply.status );
    }

    @Test
    void theDifferenceOfAPageThatDoesNotExistIs404() throws Exception {
        startEngine( null, "0", "VersioningFileProvider" );

        final Reply reply = call( requestTo( "/pages/ApiDiffNoPage/diff", "from", "1" ) );

        assertMatchesContract( "/pages/{name}/diff", reply );
        Assertions.assertEquals( 404, reply.status );
    }

    @Test
    void theDifferenceOfAPageTheCallerMayNotViewIs401() throws Exception {
        startEngine( null, "0", "VersioningFileProvider" );
        engine.saveText( "ApiDiffSecret", "[{ALLOW view Admin}] secret" );
        engine.saveText( "ApiDiffSecret", "[{ALLOW view Admin}] secret changed" );

        final Reply reply = call( requestTo( "/pages/ApiDiffSecret/diff", "from", "1", "to", "2" ) );

        assertMatchesContract( "/pages/{name}/diff", reply );
        Assertions.assertEquals( 401, reply.status );
        Assertions.assertFalse( reply.body.toString().contains( "changed" ), reply.body.toString() );
    }

    @Test
    void theDifferenceOnAProviderWithoutVersionsKnowsOnlyVersionOne() throws Exception {
        startEngine( null );
        engine.saveText( "ApiDiffNoVersions", "one" );

        final Reply known = call( requestTo( "/pages/ApiDiffNoVersions/diff", "from", "1" ) );
        final Reply unknown = call( requestTo( "/pages/ApiDiffNoVersions/diff", "from", "2" ) );

        Assertions.assertEquals( 200, known.status );
        Assertions.assertEquals( 404, unknown.status );
    }

    @Test
    void pagesListTheNamesSortedAndLeaveOutTheOnesTheCallerMayNotView() throws Exception {
        startEngine( null );
        engine.saveText( "ApiListB", "b" );
        engine.saveText( "ApiListA", "a" );
        engine.saveText( "ApiListSecret", "[{ALLOW view Admin}] secret" );

        final Reply reply = call( requestTo( "/pages", "max", "1000" ) );

        assertMatchesContract( "/pages", reply );
        Assertions.assertEquals( 200, reply.status );
        final List< String > names = new ArrayList<>();
        reply.json().forEach( name -> names.add( name.asString() ) );
        Assertions.assertTrue( names.indexOf( "ApiListA" ) >= 0 && names.indexOf( "ApiListA" ) < names.indexOf( "ApiListB" ) );
        Assertions.assertFalse( names.contains( "ApiListSecret" ) );
    }

    @Test
    void pagesListInChunksWithMaxAndOffset() throws Exception {
        startEngine( null );
        engine.saveText( "ApiChunkA", "a" );
        engine.saveText( "ApiChunkB", "b" );
        engine.saveText( "ApiChunkC", "c" );
        final List< String > all = new ArrayList<>();
        call( requestTo( "/pages", "max", "1000" ) ).json().forEach( name -> all.add( name.asString() ) );

        final Reply second = call( requestTo( "/pages", "max", "2", "offset", "1" ) );

        assertMatchesContract( "/pages", second );
        final List< String > chunk = new ArrayList<>();
        second.json().forEach( name -> chunk.add( name.asString() ) );
        Assertions.assertEquals( all.subList( 1, 3 ), chunk );
        final Reply beyond = call( requestTo( "/pages", "offset", "100000" ) );
        Assertions.assertEquals( 0, beyond.json().size() );
    }

    @Test
    void pagesWithAnOffsetThatIsNotAnIntegerOfZeroOrMoreIsABadRequest() throws Exception {
        startEngine( null );

        for( final String offset : new String[] { "-1", "x", "" } ) {
            final Reply reply = call( requestTo( "/pages", "offset", offset ) );

            assertMatchesContract( "/pages", reply );
            Assertions.assertEquals( 400, reply.status, "offset=" + offset );
        }
    }

    @Test
    void pagesListByAnAnonymousCallerWithoutPermissionIs401( @TempDir final Path dir ) throws Exception {
        startEngine( policy( dir, "Authenticated" ) );

        final Reply reply = call( requestTo( "/pages" ) );

        assertMatchesContract( "/pages", reply );
        Assertions.assertEquals( 401, reply.status );
    }

    @Test
    void referencesListTheLinksInBothDirectionsWithoutTheSecretOnes() throws Exception {
        startEngine( null );
        engine.saveText( "ApiRefTarget", "the target" );
        engine.saveText( "ApiRefSource", "links to [ApiRefTarget] and to [ApiRefNotWritten]" );
        engine.saveText( "ApiRefSecret", "[{ALLOW view Admin}] links to [ApiRefTarget]" );

        final Reply target = call( requestTo( "/pages/ApiRefTarget/references" ) );
        final Reply source = call( requestTo( "/pages/ApiRefSource/references" ) );

        assertMatchesContract( "/pages/{name}/references", target );
        assertMatchesContract( "/pages/{name}/references", source );
        final List< String > referredBy = new ArrayList<>();
        target.json().get( "referredBy" ).forEach( name -> referredBy.add( name.asString() ) );
        Assertions.assertEquals( List.of( "ApiRefSource" ), referredBy );
        final List< String > refersTo = new ArrayList<>();
        source.json().get( "refersTo" ).forEach( name -> refersTo.add( name.asString() ) );
        Assertions.assertEquals( List.of( "ApiRefNotWritten", "ApiRefTarget" ), refersTo );
    }

    @Test
    void referencesOfAPageThatDoesNotExistAre404AndOfASecretPageAre401() throws Exception {
        startEngine( null );
        engine.saveText( "ApiRefHidden", "[{ALLOW view Admin}] hidden" );

        final Reply missing = call( requestTo( "/pages/ApiRefNone/references" ) );
        final Reply hidden = call( requestTo( "/pages/ApiRefHidden/references" ) );

        assertMatchesContract( "/pages/{name}/references", missing );
        assertMatchesContract( "/pages/{name}/references", hidden );
        Assertions.assertEquals( 404, missing.status );
        Assertions.assertEquals( 401, hidden.status );
    }

    @Test
    void htmlAnswersWithThePageRendered() throws Exception {
        startEngine( null );
        engine.saveText( "ApiHtmlPage", "Some __bold__ text." );

        final Reply reply = call( requestTo( "/pages/ApiHtmlPage/html" ) );

        assertMatchesContract( "/pages/{name}/html", reply );
        Assertions.assertEquals( 200, reply.status );
        final JsonNode page = reply.json();
        Assertions.assertEquals( "ApiHtmlPage", page.get( "name" ).asString() );
        Assertions.assertTrue( page.get( "html" ).asString().contains( "<b>bold</b>" ), page.get( "html" ).asString() );
    }

    @Test
    void htmlOfAPageThatDoesNotExistIs404AndOfASecretPageIs401() throws Exception {
        startEngine( null );
        engine.saveText( "ApiHtmlHidden", "[{ALLOW view Admin}] hidden" );

        final Reply missing = call( requestTo( "/pages/ApiHtmlNone/html" ) );
        final Reply hidden = call( requestTo( "/pages/ApiHtmlHidden/html" ) );

        assertMatchesContract( "/pages/{name}/html", missing );
        assertMatchesContract( "/pages/{name}/html", hidden );
        Assertions.assertEquals( 404, missing.status );
        Assertions.assertEquals( 401, hidden.status );
        Assertions.assertFalse( hidden.body.toString().contains( "hidden" ) );
    }

    @Test
    void aNameThatCouldLeaveTheDirectoryOfThePagesIsABadRequest() throws Exception {
        startEngine( null );

        for( final String name : new String[] { "..", "a..b", "a\\b", "a\u0000b", "a\nb" } ) {
            for( final String suffix : new String[] { "", "/history", "/attachments" } ) {
                final Reply reply = call( requestTo( "/pages/" + name + suffix ) );

                assertMatchesContract( "/pages/{name}" + suffix, reply );
                Assertions.assertEquals( 400, reply.status, name + suffix );
            }
        }
    }

    @Test
    void pagesWithoutANameIs404() throws Exception {
        startEngine( null );

        final Reply reply = call( requestTo( "/pages/" ) );

        Assertions.assertEquals( 404, reply.status );
    }

    @Test
    void contractDeclaresTheParametersTheServletEnforces() {
        final Map< String, Object > get = map( map( map( CONTRACT, "paths" ), "/search" ), "get" );
        final List< Object > parameters = list( get, "parameters" );

        final Map< String, Object > query = map( parameters.get( 0 ), null );
        Assertions.assertEquals( "query", query.get( "name" ) );
        Assertions.assertEquals( Boolean.TRUE, query.get( "required" ) );

        final Map< String, Object > max = resolve( parameters.get( 1 ) );
        Assertions.assertEquals( "max", max.get( "name" ) );
        Assertions.assertEquals( ApiV1Servlet.DEFAULT_MAX, map( max, "schema" ).get( "default" ) );
        Assertions.assertEquals( 1, map( max, "schema" ).get( "minimum" ) );
    }

    private void startEngine( final String policy ) {
        startEngine( policy, "0" );
    }

    private void startEngine( final String policy, final String initialIndexDelay ) {
        startEngine( policy, initialIndexDelay, null );
    }

    private void startEngine( final String policy, final String initialIndexDelay, final String pageProvider ) {
        final Properties props = TestEngine.getTestProperties();
        props.setProperty( SearchManager.PROP_SEARCHPROVIDER, "LuceneSearchProvider" );
        props.setProperty( "jspwiki.lucene.indexdelay", "0" );
        props.setProperty( "jspwiki.lucene.initialdelay", initialIndexDelay );
        props.setProperty( "jspwiki.workDir", props.getProperty( "jspwiki.workDir" ) + System.currentTimeMillis() );
        props.setProperty( "jspwiki.fileSystemProvider.pageDir", props.getProperty( "jspwiki.fileSystemProvider.pageDir" ) + System.currentTimeMillis() );
        if( pageProvider != null ) {
            props.setProperty( PageManager.PROP_PAGEPROVIDER, pageProvider );
        }
        if( policy != null ) {
            props.setProperty( AuthorizationManager.POLICY, policy );
        }
        engine = TestEngine.build( props );
    }

    /** Writes a policy file that lets only the given role view the pages. */
    private static String policy( final Path dir, final String role ) throws Exception {
        final Path file = dir.resolve( "api.policy" );
        Files.write( file, ( "grant principal org.apache.wiki.auth.authorize.Role \"" + role + "\" {\n"
                           + "    permission org.apache.wiki.auth.permissions.PagePermission \"*:*\", \"view\";\n"
                           + "};\n" ).getBytes( StandardCharsets.UTF_8 ) );
        return file.toAbsolutePath().toString();
    }

    private HttpServletRequest request( final String... parameters ) {
        return requestTo( "/search", parameters );
    }

    private HttpServletRequest requestTo( final String pathInfo, final String... parameters ) {
        final HttpServletRequest request = HttpMockFactory.createHttpRequest( "/JSPWiki", "/api/v1" + pathInfo );
        Mockito.doReturn( "GET" ).when( request ).getMethod();
        Mockito.doReturn( pathInfo ).when( request ).getPathInfo();
        for( int i = 0; i < parameters.length; i += 2 ) {
            Mockito.doReturn( parameters[ i + 1 ] ).when( request ).getParameter( parameters[ i ] );
        }
        return request;
    }

    private Reply search( final HttpServletRequest request, final String... parameters ) throws Exception {
        return call( request != null ? request : request( parameters ) );
    }

    private Reply call( final HttpServletRequest request ) throws Exception {
        // The context is a mock, so the engine that TestEngine stashed in it has to be handed over again
        final ServletContext context = engine.getServletContext();
        Mockito.doReturn( engine ).when( context ).getAttribute( "org.apache.wiki.WikiEngine" );
        final ServletConfig config = Mockito.mock( ServletConfig.class );
        Mockito.doReturn( context ).when( config ).getServletContext();
        final ApiV1Servlet servlet = new ApiV1Servlet();
        servlet.init( config );

        final Reply reply = new Reply();
        final HttpServletResponse response = Mockito.mock( HttpServletResponse.class );
        Mockito.doAnswer( invocation -> reply.status = invocation.getArgument( 0 ) ).when( response ).setStatus( Mockito.anyInt() );
        Mockito.doAnswer( invocation -> reply.contentType = invocation.getArgument( 0 ) ).when( response ).setContentType( Mockito.anyString() );
        Mockito.doReturn( new PrintWriter( reply.body ) ).when( response ).getWriter();
        Mockito.doReturn( new ServletOutputStream() {
            @Override
            public void write( final int b ) {
                reply.bytes.write( b );
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setWriteListener( final WriteListener listener ) {
                // writes never block here
            }
        } ).when( response ).getOutputStream();
        Mockito.doAnswer( invocation -> reply.headers.put( invocation.getArgument( 0 ), invocation.getArgument( 1 ) ) )
               .when( response ).setHeader( Mockito.anyString(), Mockito.anyString() );
        Mockito.doAnswer( invocation -> reply.length = invocation.getArgument( 0 ) ).when( response ).setContentLengthLong( Mockito.anyLong() );
        servlet.service( request, response );
        return reply;
    }

    /** Checks the status, the content type and the shape of the body against what the contract declares for the operation. */
    private static void assertMatchesContract( final String path, final Reply reply ) {
        final Map< String, Object > responses = map( map( map( map( CONTRACT, "paths" ), path ), "get" ), "responses" );
        final Object declared = responses.get( String.valueOf( reply.status ) );
        Assertions.assertNotNull( declared, "the contract has no response " + reply.status + " for " + path );

        final Map< String, Object > content = map( resolve( declared ), "content" );
        Assertions.assertTrue( content.containsKey( reply.contentType ), "content type " + reply.contentType + " is not declared" );
        assertMatchesSchema( resolve( map( content, reply.contentType ).get( "schema" ) ), reply.json(), "body" );
    }

    private static void assertMatchesSchema( final Map< String, Object > schema, final JsonNode value, final String where ) {
        switch( String.valueOf( schema.get( "type" ) ) ) {
            case "array":
                Assertions.assertTrue( value.isArray(), where + " must be an array" );
                value.forEach( item -> assertMatchesSchema( resolve( schema.get( "items" ) ), item, where + "[]" ) );
                break;
            case "object":
                Assertions.assertTrue( value.isObject(), where + " must be an object" );
                final JsonNode object = value;
                for( final Object required : list( schema, "required" ) ) {
                    Assertions.assertTrue( object.has( required.toString() ), where + " lacks " + required );
                }
                final Map< String, Object > properties = map( schema, "properties" );
                for( final String name : object.propertyNames() ) {
                    Assertions.assertTrue( properties.containsKey( name ), where + "." + name + " is not in the contract" );
                    assertMatchesSchema( resolve( properties.get( name ) ), object.get( name ), where + "." + name );
                }
                break;
            case "string":
                Assertions.assertTrue( value.isString(), where + " must be a string" );
                break;
            case "integer":
                Assertions.assertTrue( value.isNumber()
                                       && value.asDouble() == Math.rint( value.asDouble() ), where + " must be an integer" );
                break;
            default:
                Assertions.fail( "the test does not know the type of " + where );
        }
    }

    private static Map< String, Object > contract() {
        try( InputStream in = ApiV1Servlet.class.getResourceAsStream( "openapi.yaml" ) ) {
            return new Yaml().load( in );
        } catch( final java.io.IOException e ) {
            throw new IllegalStateException( e );
        }
    }

    /** Follows a local reference such as #/components/schemas/Error. */
    private static Map< String, Object > resolve( final Object node ) {
        Map< String, Object > current = map( node, null );
        while( current.containsKey( "$ref" ) ) {
            Object target = CONTRACT;
            for( final String part : current.get( "$ref" ).toString().substring( 2 ).split( "/" ) ) {
                target = map( target, null ).get( part );
            }
            current = map( target, null );
        }
        return current;
    }

    @SuppressWarnings( "unchecked" )
    private static Map< String, Object > map( final Object node, final String key ) {
        return ( Map< String, Object > )( key == null ? node : ( ( Map< String, Object > )node ).get( key ) );
    }

    @SuppressWarnings( "unchecked" )
    private static List< Object > list( final Map< String, Object > node, final String key ) {
        return ( List< Object > )node.get( key );
    }

    private static Set< String > names( final Reply reply ) {
        final Set< String > names = new HashSet<>();
        reply.json().forEach( name -> names.add( name.asString() ) );
        return names;
    }

    /** What the servlet wrote to a mock response. */
    private static final class Reply {
        int status;
        String contentType;
        long length = -1;
        final StringWriter body = new StringWriter();
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        final Map< String, String > headers = new HashMap<>();

        JsonNode json() {
            return JSON.readTree( body.toString() );
        }
    }

}
