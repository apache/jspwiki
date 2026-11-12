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

import com.google.gson.Gson;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.ContextEnum;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.core.Session;
import org.apache.wiki.api.exceptions.ProviderException;
import org.apache.wiki.api.search.SearchResult;
import org.apache.wiki.api.spi.Wiki;
import org.apache.wiki.auth.AuthorizationManager;
import org.apache.wiki.auth.permissions.PagePermission;
import org.apache.wiki.search.SearchManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Version 1 of the REST API of the wiki, described by {@code openapi.yaml} in this package. It answers with JSON, also when it fails.
 * A caller that is not allowed to read gets 401 if it is anonymous and 403 if it is logged in.
 *
 * @since 3.0.1
 */
public class ApiV1Servlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LogManager.getLogger( ApiV1Servlet.class );
    private static final Gson GSON = new Gson();

    /** Number of entries returned when the request does not say how many it wants. */
    static final int DEFAULT_MAX = 20;

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
        if( "/search".equals( req.getPathInfo() ) ) {
            search( req, resp );
        } else {
            error( resp, HttpServletResponse.SC_NOT_FOUND, "No such resource." );
        }
    }

    private void search( final HttpServletRequest req, final HttpServletResponse resp ) throws IOException {
        final Session session = Wiki.session().find( engine, req );
        if( !engine.getManager( AuthorizationManager.class ).checkPermission( session, PagePermission.VIEW ) ) {
            denied( resp, session );
            return;
        }
        final String query = req.getParameter( "query" );
        if( StringUtils.isBlank( query ) ) {
            error( resp, HttpServletResponse.SC_BAD_REQUEST, "The parameter query is required." );
            return;
        }
        final int max = max( req.getParameter( "max" ) );
        if( max < 1 ) {
            error( resp, HttpServletResponse.SC_BAD_REQUEST, "The parameter max must be an integer greater than zero." );
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

    /** Returns the value of the max parameter, the default when it is absent, and 0 when it is not a positive integer. */
    private static int max( final String value ) {
        if( value == null ) {
            return DEFAULT_MAX;
        }
        try {
            return Math.max( 0, Integer.parseInt( value.trim() ) );
        } catch( final NumberFormatException e ) {
            return 0;
        }
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
        resp.getWriter().write( GSON.toJson( body ) );
    }

    /** An entry of the answer to a search. */
    private static final class Hit {
        private final String page;
        private final int score;

        Hit( final String page, final int score ) {
            this.page = page;
            this.score = score;
        }
    }

    /** The body of every answer that is not a success. */
    private static final class Failure {
        private final int status;
        private final String message;

        Failure( final int status, final String message ) {
            this.status = status;
            this.message = message;
        }
    }

}
