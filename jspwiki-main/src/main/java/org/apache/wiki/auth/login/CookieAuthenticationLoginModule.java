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
import org.apache.wiki.WikiEngine;
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
 *  Logs in an user based on a cookie stored in the user's computer.  The cookie
 *  information is stored in the <code>jspwiki.workDir</code>, under the directory
 *  {@value #COOKIE_DIR}.  For security purposes it is a very, very good idea
 *  to prevent access to this directory by everyone except the web server process;
 *  otherwise people having read access to this directory may be able to spoof
 *  other users.
 *  <p>
 *  The cookie directory is scrubbed of old entries at regular intervals.
 *  <p>
 *   This module must be used with a CallbackHandler (such as
 *   {@link WebContainerCallbackHandler}) that supports the following Callback
 *   types:
 *   </p>
 *   <ol>
 *   <li>{@link HttpRequestCallback}- supplies the cookie, which should contain
 *       an unique id for fetching the UID.</li>
 *   <li>{@link WikiEngineCallback} - allows access to the WikiEngine itself.
 *   </ol>
 *  <p>
 *  After authentication, a generic WikiPrincipal based on the username will be
 *  created and associated with the Subject.
 *  </p>
 *  @see javax.security.auth.spi.LoginModule#commit()
 *  @see CookieAssertionLoginModule
 *  @since  2.5.62
 */
public class CookieAuthenticationLoginModule extends AbstractLoginModule
{

    private static final Logger log = Logger.getLogger( CookieAuthenticationLoginModule.class );
    private static final String LOGIN_COOKIE_NAME = "JSPWikiUID";

    /** The directory name under which the cookies are stored.  The value is {@value}. */
    protected static final String COOKIE_DIR        = "logincookies";

    /**
     *  User property for setting how long the cookie is stored on the user's computer.
     *  The value is {@value}.  The default expiry time is 14 days.
     */
    public static final  String PROP_LOGIN_EXPIRY_DAYS  = "jspwiki.cookieAuthentication.expiry";

    /**
     *  Built-in value for storing the cookie.
     */
    private static final int    DEFAULT_EXPIRY_DAYS = 14;

    private static       long   c_lastScrubTime   = 0L;

    /** Describes how often we scrub the cookieDir directory.
     */
    private static final long   SCRUB_PERIOD      = 60*60*1000L; // In milliseconds

    /**
     * @see javax.security.auth.spi.LoginModule#login()
     * 
     * {@inheritDoc}
     */
    public boolean login() throws LoginException
    {
        // Otherwise, let's go and look for the cookie!
        HttpRequestCallback hcb = new HttpRequestCallback();
        //UserDatabaseCallback ucb = new UserDatabaseCallback();
        WikiEngineCallback wcb  = new WikiEngineCallback();

        Callback[] callbacks = new Callback[]
        { hcb, wcb };

        try
        {
            m_handler.handle( callbacks );

            HttpServletRequest request = hcb.getRequest();
            String uid = getLoginCookie( request );

            if( uid != null )
            {
                WikiEngine engine = wcb.getEngine();
                File cookieFile = getCookieFile(engine, uid);

                if( cookieFile != null && cookieFile.exists() && cookieFile.canRead() )
                {
                    Reader in = null;

                    try
                    {
                        in = new BufferedReader( new InputStreamReader( new FileInputStream( cookieFile ), "UTF-8" ) );
                        String username = FileUtil.readContents( in );

                        if ( log.isDebugEnabled() )
                        {
                            log.debug( "Logged in cookie authenticated name=" + username );
                        }

                        // If login succeeds, commit these principals/roles
                        m_principals.add( new WikiPrincipal( username,  WikiPrincipal.LOGIN_NAME ) );

                        //
                        //  Tag the file so that we know that it has been accessed recently.
                        //
                        cookieFile.setLastModified( System.currentTimeMillis() );

                        return true;
                    }
                    catch( IOException e )
                    {
                        return false;
                    }
                    finally
                    {
                        if( in != null ) in.close();
                    }
                }
            }
        }
        catch( IOException e )
        {
            String message = "IO exception; disallowing login.";
            log.error( message, e );
            throw new LoginException( message );
        }
        catch( UnsupportedCallbackException e )
        {
            String message = "Unable to handle callback; disallowing login.";
            log.error( message, e );
            throw new LoginException( message );
        }

        return false;
    }

    /**
     *  Attempts to locate the cookie file.
     *  @param engine WikiEngine
     *  @param uid An unique ID fetched from the user cookie
     *  @return A File handle, or null, if there was a problem.
     */
    private static File getCookieFile(WikiEngine engine, String uid)
    {
        File cookieDir = new File( engine.getWorkDir(), COOKIE_DIR );

        if( !cookieDir.exists() )
        {
            cookieDir.mkdirs();
        }

        if( !cookieDir.canRead() )
        {
            log.error("Cannot read from cookie directory!"+cookieDir.getAbsolutePath());
            return null;
        }

        if( !cookieDir.canWrite() )
        {
            log.error("Cannot write to cookie directory!"+cookieDir.getAbsolutePath());
            return null;
        }

        //
        //  Scrub away old files
        //
        long now = System.currentTimeMillis();

        if( now > (c_lastScrubTime+SCRUB_PERIOD ) )
        {
            scrub( TextUtil.getIntegerProperty( engine.getWikiProperties(),
                                                PROP_LOGIN_EXPIRY_DAYS,
                                                DEFAULT_EXPIRY_DAYS ),
                                                cookieDir );
            c_lastScrubTime = now;
        }

        //
        //  Find the cookie file
        //
        File cookieFile = new File( cookieDir, uid );
        return cookieFile;
    }

    /**
     *  Extracts the login cookie UID from the servlet request.
     *
     *  @param request The HttpServletRequest
     *  @return The UID value from the cookie, or null, if no such cookie exists.
     */
    private static String getLoginCookie(HttpServletRequest request)
    {
        String cookie = HttpUtil.retrieveCookieValue( request, LOGIN_COOKIE_NAME );

        return cookie;
    }

    /**
     *  Sets a login cookie based on properties set by the user.  This method also
     *  creates the cookie uid-username mapping in the work directory.
     *
     *  @param engine The WikiEngine
     *  @param response The HttpServletResponse
     *  @param username The username for whom to create the cookie.
     */
    public static void setLoginCookie( final WikiEngine engine, final HttpServletResponse response, final String username ) {
        final UUID uid = UUID.randomUUID();
        final int days = TextUtil.getIntegerProperty( engine.getWikiProperties(), PROP_LOGIN_EXPIRY_DAYS, DEFAULT_EXPIRY_DAYS );
        final Cookie userId = new Cookie( LOGIN_COOKIE_NAME, uid.toString() );
        userId.setMaxAge( days * 24 * 60 * 60 );
        response.addCookie( userId );

        final File cf = getCookieFile( engine, uid.toString() );
        if( cf != null ) {
            //  Write the cookie content to the cookie store file.
            try( final Writer out = new BufferedWriter( new OutputStreamWriter( new FileOutputStream(cf), StandardCharsets.UTF_8 ) ) ) {
                FileUtil.copyContents( new StringReader(username), out );

                if( log.isDebugEnabled() ) {
                    log.debug( "Created login cookie for user "+username+" for "+days+" days" );
                }
            } catch( final IOException ex ) {
                log.error( "Unable to create cookie file to store user id: " + uid );
            }
        }
    }

    /**
     *  Clears away the login cookie, and removes the uid-username mapping file as well.
     *
     *  @param engine   WikiEngine
     *  @param request  Servlet request
     *  @param response Servlet response
     */
    public static void clearLoginCookie( WikiEngine engine, HttpServletRequest request, HttpServletResponse response )
    {
        Cookie userId = new Cookie( LOGIN_COOKIE_NAME, "" );
        userId.setMaxAge( 0 );
        response.addCookie( userId );

        String uid = getLoginCookie( request );

        if( uid != null )
        {
            File cf = getCookieFile( engine, uid );

            if( cf != null )
            {
                cf.delete();
            }
        }
    }

    /**
     *  Goes through the cookie directory and removes any obsolete files.
     *  The scrubbing takes place one day after the cookie was supposed to expire.
     *  However, if the user has logged in during the expiry period, the expiry is
     *  reset, and the cookie file left here.
     *
     *  @param days
     *  @param cookieDir
     */
    private static synchronized void scrub( int days, File cookieDir )
    {
        log.debug("Scrubbing cookieDir...");

        File[] files = cookieDir.listFiles();

        long obsoleteDateLimit = System.currentTimeMillis() - ((long)days+1) * 24 * 60 * 60 * 1000L;

        int  deleteCount = 0;

        for( int i = 0; i < files.length; i++ )
        {
            File f = files[i];

            long lastModified = f.lastModified();

            if( lastModified < obsoleteDateLimit )
            {
                f.delete();
                deleteCount++;
            }
        }

        log.debug("Removed "+deleteCount+" obsolete cookie logins");
    }
}
