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
package org.apache.wiki.auth.login;

import org.apache.log4j.Logger;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.auth.WikiPrincipal;
import org.apache.wiki.util.FileUtil;
import org.apache.wiki.util.HttpUtil;
import org.apache.wiki.util.TextUtil;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;


/**
 * Logs in an user based on a cookie stored in the user's computer.  The cookie
 * information is stored in the <code>jspwiki.workDir</code>, under the directory
 * {@value #COOKIE_DIR}.  For security purposes it is a very, very good idea
 * to prevent access to this directory by everyone except the web server process;
 * otherwise people having read access to this directory may be able to spoof
 * other users.
 * <p>
 * The cookie directory is scrubbed of old entries at regular intervals.
 * <p>
 * This module must be used with a CallbackHandler (such as
 * {@link WebContainerCallbackHandler}) that supports the following Callback
 * types:
 * </p>
 *  <ol>
 *  <li>{@link HttpRequestCallback}- supplies the cookie, which should contain
 *      an unique id for fetching the UID.</li>
 *  <li>{@link WikiEngineCallback} - allows access to the Engine itself.
 *  </ol>
 * <p>
 * After authentication, a generic WikiPrincipal based on the username will be
 * created and associated with the Subject.
 * </p>
 * @see javax.security.auth.spi.LoginModule#commit()
 * @see CookieAssertionLoginModule
 * @since 2.5.62
 */
public class CookieAuthenticationLoginModule extends AbstractLoginModule {

    private static final Logger log = Logger.getLogger( CookieAuthenticationLoginModule.class );
    private static final String LOGIN_COOKIE_NAME = "JSPWikiUID";

    /** The directory name under which the cookies are stored.  The value is {@value}. */
    protected static final String COOKIE_DIR = "logincookies";

    /**
     * User property for setting how long the cookie is stored on the user's computer.
     * The value is {@value}.  The default expiry time is 14 days.
     */
    public static final String PROP_LOGIN_EXPIRY_DAYS = "jspwiki.cookieAuthentication.expiry";

    /**
     * Built-in value for storing the cookie.
     */
    private static final int DEFAULT_EXPIRY_DAYS = 14;

    private static long c_lastScrubTime = 0L;

    /**
     * Describes how often we scrub the cookieDir directory.
     */
    private static final long SCRUB_PERIOD = 60 * 60 * 1000L; // In milliseconds

    /**
     * @see javax.security.auth.spi.LoginModule#login()
     * {@inheritDoc}
     */
    @Override
    public boolean login() throws LoginException {
        // Otherwise, let's go and look for the cookie!
        final HttpRequestCallback hcb = new HttpRequestCallback();
        final WikiEngineCallback wcb = new WikiEngineCallback();

        final Callback[] callbacks = new Callback[] { hcb, wcb };

        try {
            m_handler.handle( callbacks );

            final HttpServletRequest request = hcb.getRequest();
            final String uid = getLoginCookie( request );

            if( uid != null ) {
                final Engine engine = wcb.getEngine();
                final File cookieFile = getCookieFile( engine, uid );

                if( cookieFile != null && cookieFile.exists() && cookieFile.canRead() ) {

                    try( final Reader in = new BufferedReader( new InputStreamReader( new FileInputStream( cookieFile ), StandardCharsets.UTF_8 ) ) ) {
                        final String username = FileUtil.readContents( in );

                        if( log.isDebugEnabled() ) {
                            log.debug( "Logged in cookie authenticated name=" + username );
                        }

                        // If login succeeds, commit these principals/roles
                        m_principals.add( new WikiPrincipal( username, WikiPrincipal.LOGIN_NAME ) );

                        //
                        //  Tag the file so that we know that it has been accessed recently.
                        //
                        return cookieFile.setLastModified( System.currentTimeMillis() );

                    } catch( final IOException e ) {
                        return false;
                    }
                }
            }
        } catch( final IOException e ) {
            final String message = "IO exception; disallowing login.";
            log.error( message, e );
            throw new LoginException( message );
        } catch( final UnsupportedCallbackException e ) {
            final String message = "Unable to handle callback; disallowing login.";
            log.error( message, e );
            throw new LoginException( message );
        }

        return false;
    }

    /**
     * Attempts to locate the cookie file.
     *
     * @param engine Engine
     * @param uid    An unique ID fetched from the user cookie
     * @return A File handle, or null, if there was a problem.
     */
    private static File getCookieFile( final Engine engine, final String uid ) {
        final File cookieDir = new File( engine.getWorkDir(), COOKIE_DIR );

        if( !cookieDir.exists() ) {
            cookieDir.mkdirs();
        }

        if( !cookieDir.canRead() ) {
            log.error( "Cannot read from cookie directory!" + cookieDir.getAbsolutePath() );
            return null;
        }

        if( !cookieDir.canWrite() ) {
            log.error( "Cannot write to cookie directory!" + cookieDir.getAbsolutePath() );
            return null;
        }

        //
        //  Scrub away old files
        //
        final long now = System.currentTimeMillis();

        if( now > ( c_lastScrubTime + SCRUB_PERIOD ) ) {
            scrub( TextUtil.getIntegerProperty( engine.getWikiProperties(), PROP_LOGIN_EXPIRY_DAYS, DEFAULT_EXPIRY_DAYS ), cookieDir );
            c_lastScrubTime = now;
        }

        //
        //  Find the cookie file
        //
        return new File( cookieDir, uid );
    }

    /**
     * Extracts the login cookie UID from the servlet request.
     *
     * @param request The HttpServletRequest
     * @return The UID value from the cookie, or null, if no such cookie exists.
     */
    private static String getLoginCookie( final HttpServletRequest request ) {
        return HttpUtil.retrieveCookieValue( request, LOGIN_COOKIE_NAME );
    }

    /**
     * Sets a login cookie based on properties set by the user.  This method also
     * creates the cookie uid-username mapping in the work directory.
     *
     * @param engine   The Engine
     * @param response The HttpServletResponse
     * @param username The username for whom to create the cookie.
     */
    public static void setLoginCookie( final Engine engine, final HttpServletResponse response, final String username ) {
        final UUID uid = UUID.randomUUID();
        final int days = TextUtil.getIntegerProperty( engine.getWikiProperties(), PROP_LOGIN_EXPIRY_DAYS, DEFAULT_EXPIRY_DAYS );
        final Cookie userId = getLoginCookie( uid.toString() );
        userId.setMaxAge( days * 24 * 60 * 60 );
        response.addCookie( userId );

        final File cf = getCookieFile( engine, uid.toString() );
        if( cf != null ) {
            //  Write the cookie content to the cookie store file.
            try( final Writer out = new BufferedWriter( new OutputStreamWriter( new FileOutputStream( cf ), StandardCharsets.UTF_8 ) ) ) {
                FileUtil.copyContents( new StringReader( username ), out );

                if( log.isDebugEnabled() ) {
                    log.debug( "Created login cookie for user " + username + " for " + days + " days" );
                }

            } catch( final IOException ex ) {
                log.error( "Unable to create cookie file to store user id: " + uid );
            }
        }
    }

    /**
     * Clears away the login cookie, and removes the uid-username mapping file as well.
     *
     * @param engine   Engine
     * @param request  Servlet request
     * @param response Servlet response
     */
    public static void clearLoginCookie( final Engine engine, final HttpServletRequest request, final HttpServletResponse response ) {
        final Cookie userId = getLoginCookie( "" );
        userId.setMaxAge( 0 );
        response.addCookie( userId );

        final String uid = getLoginCookie( request );

        if( uid != null ) {
            final File cf = getCookieFile( engine, uid );

            if( cf != null ) {
                if( !cf.delete() ) {
                    log.debug( "Error deleting cookie login " + uid );
                }
            }
        }
    }

    /**
     * Helper function to get secure LOGIN cookie
     *
     * @param: value of the cookie
     */
    private static Cookie getLoginCookie( final String value ) {
        final Cookie c = new Cookie( LOGIN_COOKIE_NAME, value );
        c.setHttpOnly( true );  //no browser access
        c.setSecure( true ); //only access via encrypted https allowed
        return c;
    }

    /**
     * Goes through the cookie directory and removes any obsolete files.
     * The scrubbing takes place one day after the cookie was supposed to expire.
     * However, if the user has logged in during the expiry period, the expiry is
     * reset, and the cookie file left here.
     *
     * @param days
     * @param cookieDir
     */
    private static synchronized void scrub( final int days, final File cookieDir ) {
        log.debug( "Scrubbing cookieDir..." );
        final File[] files = cookieDir.listFiles();
        final long obsoleteDateLimit = System.currentTimeMillis() - ( ( long )days + 1 ) * 24 * 60 * 60 * 1000L;
        int deleteCount = 0;

        for( int i = 0; i < files.length; i++ ) {
            final File f = files[ i ];
            final long lastModified = f.lastModified();
            if( lastModified < obsoleteDateLimit ) {
                if( f.delete() ) {
                    deleteCount++;
                } else {
                    log.debug( "Error deleting cookie login with index " + i );
                }
            }
        }

        log.debug( "Removed " + deleteCount + " obsolete cookie logins" );
    }

}
