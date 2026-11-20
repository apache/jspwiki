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

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.wiki.api.core.Attachment;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.ContextEnum;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.core.Page;
import org.apache.wiki.api.core.Session;
import org.apache.wiki.api.exceptions.ProviderException;
import org.apache.wiki.api.plugin.Plugin;
import org.apache.wiki.api.providers.WikiProvider;
import org.apache.wiki.api.search.SearchResult;
import org.apache.wiki.api.spi.Wiki;
import org.apache.wiki.auth.AuthorizationManager;
import org.apache.wiki.attachment.AttachmentManager;
import org.apache.wiki.auth.permissions.PagePermission;
import org.apache.wiki.auth.permissions.PermissionFactory;
import org.apache.wiki.pages.PageManager;
import org.apache.wiki.plugin.PluginManager;
import org.apache.wiki.references.ReferenceManager;
import org.apache.wiki.render.RenderingManager;
import org.apache.wiki.search.SearchManager;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Version 1 of the REST API of the wiki, described by {@code openapi.yaml} in this package. It answers with JSON, also when it fails.
 * A caller that is not allowed to read gets 401 if it is anonymous and 403 if it is logged in.
 *
 * @since 3.0.1
 */
public class ApiV1Servlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LogManager.getLogger( ApiV1Servlet.class );
    /** Leaves out the fields without a value, as the page that has no author. */
    private static final JsonMapper JSON = JsonMapper.builder()
        .changeDefaultPropertyInclusion( value -> value.withValueInclusion( JsonInclude.Include.NON_NULL ) )
        .build();

    /** Number of entries returned when the request does not say how many it wants. */
    static final int DEFAULT_MAX = 20;

    private static final String PAGES = "/pages/";
    private static final String HISTORY = "/history";
    private static final String ATTACHMENTS = "/attachments";
    private static final String REFERENCES = "/references";
    private static final String HTML = "/html";

    private transient Engine engine;

    /** {@inheritDoc} */
    @Override
    public void init( final ServletConfig config ) throws ServletException {
        super.init( config );
        engine = Wiki.engine().find( config );
    }

    /** {@inheritDoc} */
    @Override
    protected void doGet( final HttpServletRequest req, final HttpServletResponse resp ) throws IOException {
        final String path = req.getPathInfo();
        if( "/search".equals( path ) ) {
            search( req, resp );
        } else if( "/search/suggestions".equals( path ) ) {
            suggestions( req, resp );
        } else if( "/plugins".equals( path ) ) {
            plugins( req, resp );
        } else if( "/pages".equals( path ) ) {
            list( req, resp );
        } else if( path != null && path.startsWith( PAGES ) ) {
            pages( req, resp, path.substring( PAGES.length() ) );
        } else {
            error( resp, HttpServletResponse.SC_NOT_FOUND, "No such resource." );
        }
    }

    private void search( final HttpServletRequest req, final HttpServletResponse resp ) throws IOException {
        final Session session = authorized( req, resp );
        final String query = session == null ? null : query( req, resp );
        final int max = query == null ? 0 : max( req, resp );
        if( max < 1 ) {
            return;
        }

        final Context context = Wiki.context().create( engine, req, ContextEnum.PAGE_VIEW.getRequestContext() );
        try {
            final Collection< SearchResult > found = engine.getManager( SearchManager.class ).findPages( query, context );
            if( found == null ) {
                // The Lucene provider logs a failure of its index, for instance before it is first built, and returns null
                error( resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "The search index is not available." );
                return;
            }
            final List< Hit > hits = new ArrayList<>();
            for( final SearchResult result : found ) {
                if( hits.size() == max ) {
                    break;
                }
                hits.add( new Hit( result.getPage().getName(), result.getScore() ) );
            }
            json( resp, HttpServletResponse.SC_OK, hits );
        } catch( final ProviderException e ) {
            if( e.getCause() instanceof ParseException ) {
                error( resp, HttpServletResponse.SC_BAD_REQUEST, "The parameter query is not a valid query." );
            } else {
                LOG.error( "The search for the REST API failed", e );
                error( resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "The search failed." );
            }
        } catch( final IOException e ) {
            LOG.error( "The search for the REST API failed", e );
            error( resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "The search failed." );
        }
    }

    private void suggestions( final HttpServletRequest req, final HttpServletResponse resp ) throws IOException {
        final Session session = authorized( req, resp );
        final String query = session == null ? null : query( req, resp );
        final int max = query == null ? 0 : max( req, resp );
        if( max > 0 ) {
            json( resp, HttpServletResponse.SC_OK, engine.getManager( SearchManager.class ).suggestPages( query, max, session ) );
        }
    }

    private void plugins( final HttpServletRequest req, final HttpServletResponse resp ) throws IOException {
        if( authorized( req, resp ) == null ) {
            return;
        }
        final Locale locale = req.getLocale() != null ? req.getLocale() : Locale.getDefault();
        final List< Plugin > discovered = new ArrayList<>( engine.getManager( PluginManager.class ).getDiscoveredPlugins() );
        discovered.sort( Comparator.comparing( plugin -> plugin.getDisplayName( locale ) ) );
        final List< PluginEntry > entries = new ArrayList<>();
        for( final Plugin plugin : discovered ) {
            entries.add( new PluginEntry( plugin.getDisplayName( locale ), plugin.getSnipExample() ) );
        }
        json( resp, HttpServletResponse.SC_OK, entries );
    }

    /** Serves a page, its history or its attachments. The part of the path before the optional suffix is the page name. */
    private void pages( final HttpServletRequest req, final HttpServletResponse resp, final String rest ) throws IOException {
        final String suffix = rest.endsWith( HISTORY ) ? HISTORY : rest.endsWith( ATTACHMENTS ) ? ATTACHMENTS
                            : rest.endsWith( REFERENCES ) ? REFERENCES : rest.endsWith( HTML ) ? HTML : "";
        final String name = rest.substring( 0, rest.length() - suffix.length() );
        if( name.isEmpty() ) {
            error( resp, HttpServletResponse.SC_NOT_FOUND, "No such resource." );
            return;
        }
        if( !plausible( name ) ) {
            error( resp, HttpServletResponse.SC_BAD_REQUEST, "The name of the page is not valid." );
            return;
        }
        final Session session = authorized( req, resp );
        if( session == null ) {
            return;
        }
        // The permission is checked before the existence of the page, so that the answer does not depend on whether a page is there
        if( !engine.getManager( AuthorizationManager.class ).checkPermission( session, PermissionFactory.getPagePermission( Wiki.contents().page( engine, name ), PagePermission.VIEW_ACTION ) ) ) {
            denied( resp, session );
            return;
        }
        final int version = suffix.isEmpty() || HTML.equals( suffix ) ? version( req, resp ) : WikiProvider.LATEST_VERSION;
        if( version == 0 ) {
            return;
        }

        try {
            final Page page = engine.getManager( PageManager.class ).getPageInfo( name, version );
            if( page == null ) {
                error( resp, HttpServletResponse.SC_NOT_FOUND, "The page does not exist." );
            } else if( HISTORY.equals( suffix ) ) {
                history( resp, page );
            } else if( ATTACHMENTS.equals( suffix ) ) {
                attachments( resp, page );
            } else if( REFERENCES.equals( suffix ) ) {
                references( req, resp, page );
            } else if( HTML.equals( suffix ) ) {
                final Context context = Wiki.context().create( engine, req, page );
                json( resp, HttpServletResponse.SC_OK, new Rendered( page, engine.getManager( RenderingManager.class ).getHTML( context, page ) ) );
            } else {
                final String text = engine.getManager( PageManager.class ).getPageText( name, page.getVersion() );
                json( resp, HttpServletResponse.SC_OK, new PageContent( page, text ) );
            }
        } catch( final ProviderException e ) {
            LOG.error( "The request for the page {} of the REST API failed", name, e );
            error( resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "The page could not be read." );
        }
    }

    /** Lists the names of the pages the caller may view, sorted by name, from the position given by offset. */
    private void list( final HttpServletRequest req, final HttpServletResponse resp ) throws IOException {
        final Session session = authorized( req, resp );
        final int max = session == null ? 0 : max( req, resp );
        final int offset = max < 1 ? -1 : offset( req, resp );
        if( offset < 0 ) {
            return;
        }
        try {
            final AuthorizationManager mgr = engine.getManager( AuthorizationManager.class );
            final List< String > names = new ArrayList<>();
            for( final Page page : engine.getManager( PageManager.class ).getAllPages() ) {
                if( mgr.checkPermission( session, PermissionFactory.getPagePermission( page, PagePermission.VIEW_ACTION ) ) ) {
                    names.add( page.getName() );
                }
            }
            names.sort( engine.getManager( PageManager.class ).getPageSorter() );
            json( resp, HttpServletResponse.SC_OK, names.subList( Math.min( offset, names.size() ), ( int )Math.min( offset + ( long )max, names.size() ) ) );
        } catch( final ProviderException e ) {
            LOG.error( "The list of pages for the REST API failed", e );
            error( resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "The pages could not be listed." );
        }
    }

    /** Answers with the pages that link to the page and the pages it links to, leaving out the ones the caller may not view. */
    private void references( final HttpServletRequest req, final HttpServletResponse resp, final Page page ) throws IOException {
        final Session session = Wiki.session().find( engine, req );
        final ReferenceManager references = engine.getManager( ReferenceManager.class );
        json( resp, HttpServletResponse.SC_OK, new References( viewable( session, references.findReferrers( page.getName() ) ),
                                                               viewable( session, references.findRefersTo( page.getName() ) ) ) );
    }

    /** Returns the names the session may view, sorted. A name without a page, such as a link to a page not yet written, is kept. */
    private List< String > viewable( final Session session, final Collection< String > names ) {
        final List< String > viewable = new ArrayList<>();
        if( names != null ) {
            final AuthorizationManager mgr = engine.getManager( AuthorizationManager.class );
            for( final String name : names ) {
                if( mgr.checkPermission( session, PermissionFactory.getPagePermission( Wiki.contents().page( engine, name ), PagePermission.VIEW_ACTION ) ) ) {
                    viewable.add( name );
                }
            }
        }
        viewable.sort( engine.getManager( PageManager.class ).getPageSorter() );
        return viewable;
    }

    /** Returns the offset parameter, zero when it is absent, and -1 after answering 400 when it is not an integer of zero or more. */
    private static int offset( final HttpServletRequest req, final HttpServletResponse resp ) throws IOException {
        final String value = req.getParameter( "offset" );
        if( value == null ) {
            return 0;
        }
        try {
            final int offset = Integer.parseInt( value.trim() );
            if( offset >= 0 ) {
                return offset;
            }
        } catch( final NumberFormatException e ) {
            // answered below
        }
        error( resp, HttpServletResponse.SC_BAD_REQUEST, "The parameter offset must be an integer of zero or more." );
        return -1;
    }

    private void history( final HttpServletResponse resp, final Page page ) throws IOException {
        final List< Page > versions = engine.getManager( PageManager.class ).getVersionHistory( page.getName() );
        if( versions == null ) {
            error( resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "The history of the page could not be read." );
            return;
        }
        final List< Revision > revisions = new ArrayList<>();
        for( final Page version : versions ) {
            revisions.add( new Revision( version ) );
        }
        if( revisions.isEmpty() ) {
            // A page that was put in the page directory by hand has no history, but it still has its current version
            revisions.add( new Revision( page ) );
        }
        revisions.sort( Comparator.comparingInt( ( Revision revision ) -> revision.version() ).reversed() );
        json( resp, HttpServletResponse.SC_OK, revisions );
    }

    private void attachments( final HttpServletResponse resp, final Page page ) throws ProviderException, IOException {
        final List< AttachmentEntry > entries = new ArrayList<>();
        for( final Attachment attachment : engine.getManager( AttachmentManager.class ).listAttachments( page ) ) {
            entries.add( new AttachmentEntry( attachment ) );
        }
        json( resp, HttpServletResponse.SC_OK, entries );
    }

    /**
     * Tells whether the text can be the name of a page. The providers encode the names they turn into files, so this is only an extra
     * barrier before a name that comes from the caller reaches them: no parent directory, no backslash and no control character.
     */
    private static boolean plausible( final String name ) {
        return !name.contains( ".." ) && name.indexOf( '\\' ) < 0 && name.chars().noneMatch( Character::isISOControl );
    }

    /** Returns the version parameter, the latest version when it is absent, and 0 after answering 400 when it is not a positive integer. */
    private static int version( final HttpServletRequest req, final HttpServletResponse resp ) throws IOException {
        final String value = req.getParameter( "version" );
        if( value == null ) {
            return WikiProvider.LATEST_VERSION;
        }
        try {
            final int version = Integer.parseInt( value.trim() );
            if( version > 0 ) {
                return version;
            }
        } catch( final NumberFormatException e ) {
            // answered below
        }
        error( resp, HttpServletResponse.SC_BAD_REQUEST, "The parameter version must be an integer greater than zero." );
        return 0;
    }

    /** A provider that keeps no old versions numbers its only version -1, which the API reports as 1. */
    private static int number( final int version ) {
        return Math.max( 1, version );
    }

    private static String instant( final Date date ) {
        return date == null ? null : date.toInstant().toString();
    }

    /** Returns the session of the caller, or null after answering 401 or 403 when the caller may not read. */
    private Session authorized( final HttpServletRequest req, final HttpServletResponse resp ) throws IOException {
        final Session session = Wiki.session().find( engine, req );
        if( engine.getManager( AuthorizationManager.class ).checkPermission( session, PagePermission.VIEW ) ) {
            return session;
        }
        denied( resp, session );
        return null;
    }

    /** Returns the query parameter, or null after answering 400 when it is missing. */
    private static String query( final HttpServletRequest req, final HttpServletResponse resp ) throws IOException {
        final String query = req.getParameter( "query" );
        if( StringUtils.isBlank( query ) ) {
            error( resp, HttpServletResponse.SC_BAD_REQUEST, "The parameter query is required." );
            return null;
        }
        return query;
    }

    /** Returns the max parameter, the default when it is absent, and 0 after answering 400 when it is not a positive integer. */
    private static int max( final HttpServletRequest req, final HttpServletResponse resp ) throws IOException {
        final String value = req.getParameter( "max" );
        if( value == null ) {
            return DEFAULT_MAX;
        }
        try {
            final int max = Integer.parseInt( value.trim() );
            if( max > 0 ) {
                return max;
            }
        } catch( final NumberFormatException e ) {
            // answered below
        }
        error( resp, HttpServletResponse.SC_BAD_REQUEST, "The parameter max must be an integer greater than zero." );
        return 0;
    }

    private static void denied( final HttpServletResponse resp, final Session session ) throws IOException {
        if( session.isAuthenticated() ) {
            error( resp, HttpServletResponse.SC_FORBIDDEN, "The permission to read is missing." );
        } else {
            error( resp, HttpServletResponse.SC_UNAUTHORIZED, "Authentication is required." );
        }
    }

    private static void error( final HttpServletResponse resp, final int status, final String message ) throws IOException {
        json( resp, status, new Failure( status, message ) );
    }

    private static void json( final HttpServletResponse resp, final int status, final Object body ) throws IOException {
        resp.setStatus( status );
        resp.setContentType( "application/json" );
        resp.setCharacterEncoding( StandardCharsets.UTF_8.name() );
        try {
            resp.getWriter().write( JSON.writeValueAsString( body ) );
        } catch( final JacksonException e ) {
            // the bodies are built here, so this is a bug and not a failure of the caller
            throw new IOException( e );
        }
    }

    /** An entry of the answer to a search. */
    private record Hit( String page, int score ) {
    }

    /** An entry of the list of plugins. */
    private record PluginEntry( String displayName, String snip ) {
    }

    /** The content of a page. */
    private record PageContent( String name, int version, String author, String lastModified, String text ) {

        PageContent( final Page page, final String text ) {
            this( page.getName(), number( page.getVersion() ), page.getAuthor(), instant( page.getLastModified() ), text );
        }
    }

    /** The page rendered to HTML. */
    private record Rendered( String name, int version, String html ) {

        Rendered( final Page page, final String html ) {
            this( page.getName(), number( page.getVersion() ), html );
        }
    }

    /** The links of a page. */
    private record References( List< String > referredBy, List< String > refersTo ) {
    }

    /** An entry of the history of a page. */
    private record Revision( int version, String author, String lastModified, String changeNote ) {

        Revision( final Page page ) {
            this( number( page.getVersion() ), page.getAuthor(), instant( page.getLastModified() ), page.getAttribute( Page.CHANGENOTE ) );
        }
    }

    /** An entry of the list of attachments of a page. */
    private record AttachmentEntry( String name, int version, long size, String author, String lastModified ) {

        AttachmentEntry( final Attachment attachment ) {
            this( attachment.getFileName(), number( attachment.getVersion() ), attachment.getSize(), attachment.getAuthor(),
                  instant( attachment.getLastModified() ) );
        }
    }

    /** The body of every answer that is not a success. */
    private record Failure( int status, String message ) {
    }

}
