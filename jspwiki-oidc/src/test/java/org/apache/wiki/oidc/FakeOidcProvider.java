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
package org.apache.wiki.oidc;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * An OpenID Connect provider that runs in the test: discovery document, keys, authorization endpoint that signs the user in
 * without asking anything, token endpoint and end session endpoint. It is just enough for the authorization code flow.
 */
final class FakeOidcProvider {

    static final String CLIENT_ID = "jspwiki-test";
    static final String CLIENT_SECRET = "test-secret";

    /** Claim with the name of the user. A null value leaves the claim out of the tokens. */
    volatile String username = "kcuser";
    volatile String email = "kcuser@example.org";
    /** Roles in {@code realm_access.roles} of the access token. */
    volatile List< String > roles = List.of( "Admin", "viewer" );

    /** What the provider saw, for the assertions. */
    volatile String lastScope;
    volatile String lastIdTokenHint;
    volatile int logouts;

    private final RSAKey key;
    private final HttpServer server;
    private final Map< String, String > nonces = new ConcurrentHashMap<>();

    FakeOidcProvider() throws Exception {
        key = new RSAKeyGenerator( 2048 ).keyID( "test-key" ).generate();
        server = HttpServer.create( new InetSocketAddress( InetAddress.getLoopbackAddress(), 0 ), 0 );
        server.createContext( "/.well-known/openid-configuration", this::discovery );
        server.createContext( "/jwks", this::jwks );
        server.createContext( "/authorize", this::authorize );
        server.createContext( "/token", this::token );
        server.createContext( "/logout", this::logout );
        server.start();
    }

    String issuer() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    String discoveryUri() {
        return issuer() + "/.well-known/openid-configuration";
    }

    void stop() {
        server.stop( 0 );
    }

    private void discovery( final HttpExchange exchange ) throws IOException {
        final String iss = issuer();
        reply( exchange, 200, "application/json", "{"
            + "\"issuer\":\"" + iss + "\","
            + "\"authorization_endpoint\":\"" + iss + "/authorize\","
            + "\"token_endpoint\":\"" + iss + "/token\","
            + "\"jwks_uri\":\"" + iss + "/jwks\","
            + "\"end_session_endpoint\":\"" + iss + "/logout\","
            + "\"response_types_supported\":[\"code\"],"
            + "\"subject_types_supported\":[\"public\"],"
            + "\"id_token_signing_alg_values_supported\":[\"RS256\"],"
            + "\"token_endpoint_auth_methods_supported\":[\"client_secret_basic\"]}" );
    }

    private void jwks( final HttpExchange exchange ) throws IOException {
        reply( exchange, 200, "application/json", new JWKSet( key.toPublicJWK() ).toString() );
    }

    /** Signs the user in at once and sends the browser back to the application with a code. */
    private void authorize( final HttpExchange exchange ) throws IOException {
        final Map< String, String > query = parameters( exchange.getRequestURI().getRawQuery() );
        if( !CLIENT_ID.equals( query.get( "client_id" ) ) || !"code".equals( query.get( "response_type" ) ) ) {
            reply( exchange, 400, "text/plain", "bad authorization request" );
            return;
        }
        lastScope = query.get( "scope" );
        final String code = UUID.randomUUID().toString();
        nonces.put( code, query.getOrDefault( "nonce", "" ) );
        final String back = query.get( "redirect_uri" ) + ( query.get( "redirect_uri" ).contains( "?" ) ? "&" : "?" )
                            + "code=" + code + "&state=" + java.net.URLEncoder.encode( query.get( "state" ), StandardCharsets.UTF_8 );
        redirect( exchange, back );
    }

    private void token( final HttpExchange exchange ) throws IOException {
        final String expected = "Basic " + Base64.getEncoder().encodeToString( ( CLIENT_ID + ":" + CLIENT_SECRET ).getBytes( StandardCharsets.UTF_8 ) );
        if( !expected.equals( exchange.getRequestHeaders().getFirst( "Authorization" ) ) ) {
            reply( exchange, 401, "application/json", "{\"error\":\"invalid_client\"}" );
            return;
        }
        final Map< String, String > form = parameters( new String( exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8 ) );
        final String nonce = nonces.remove( form.get( "code" ) );
        if( nonce == null ) {
            reply( exchange, 400, "application/json", "{\"error\":\"invalid_grant\"}" );
            return;
        }
        try {
            final JWTClaimsSet.Builder id = base( CLIENT_ID ).claim( "nonce", nonce );
            if( username != null ) {
                id.claim( "preferred_username", username );
            }
            if( email != null ) {
                id.claim( "email", email );
            }
            final JWTClaimsSet.Builder access = base( "account" ).claim( "realm_access", Map.of( "roles", roles ) );
            reply( exchange, 200, "application/json", "{\"access_token\":\"" + sign( access ) + "\",\"token_type\":\"Bearer\","
                                                    + "\"expires_in\":300,\"id_token\":\"" + sign( id ) + "\"}" );
        } catch( final Exception e ) {
            throw new IOException( e );
        }
    }

    private void logout( final HttpExchange exchange ) throws IOException {
        final Map< String, String > query = parameters( exchange.getRequestURI().getRawQuery() );
        logouts++;
        lastIdTokenHint = query.get( "id_token_hint" );
        final String back = query.get( "post_logout_redirect_uri" );
        if( back == null ) {
            reply( exchange, 200, "text/plain", "signed out" );
        } else {
            redirect( exchange, back );
        }
    }

    private JWTClaimsSet.Builder base( final String audience ) {
        final Date now = new Date();
        return new JWTClaimsSet.Builder().issuer( issuer() ).subject( "user-1" ).audience( audience )
                                         .issueTime( now ).expirationTime( new Date( now.getTime() + 300_000 ) );
    }

    private String sign( final JWTClaimsSet.Builder claims ) throws Exception {
        final SignedJWT jwt = new SignedJWT( new JWSHeader.Builder( JWSAlgorithm.RS256 ).keyID( key.getKeyID() ).build(), claims.build() );
        jwt.sign( new RSASSASigner( key ) );
        return jwt.serialize();
    }

    private static Map< String, String > parameters( final String raw ) {
        final Map< String, String > map = new HashMap<>();
        if( raw != null ) {
            for( final String pair : raw.split( "&" ) ) {
                final int eq = pair.indexOf( '=' );
                if( eq > 0 ) {
                    map.put( pair.substring( 0, eq ), URLDecoder.decode( pair.substring( eq + 1 ), StandardCharsets.UTF_8 ) );
                }
            }
        }
        return map;
    }

    private static void redirect( final HttpExchange exchange, final String location ) throws IOException {
        exchange.getResponseHeaders().add( "Location", URI.create( location ).toString() );
        exchange.sendResponseHeaders( 302, -1 );
        exchange.close();
    }

    private static void reply( final HttpExchange exchange, final int status, final String type, final String body ) throws IOException {
        final byte[] bytes = body.getBytes( StandardCharsets.UTF_8 );
        exchange.getResponseHeaders().add( "Content-Type", type );
        exchange.sendResponseHeaders( status, bytes.length );
        try( OutputStream out = exchange.getResponseBody() ) {
            out.write( bytes );
        }
    }

}
