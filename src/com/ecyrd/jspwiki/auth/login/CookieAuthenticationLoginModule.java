/*
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2007 Janne Jalkanen (Janne.Jalkanen@iki.fi)

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation; either version 2.1 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.ecyrd.jspwiki.auth.login;

import java.io.*;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.safehaus.uuid.UUID;
import org.safehaus.uuid.UUIDGenerator;

import com.ecyrd.jspwiki.FileUtil;
import com.ecyrd.jspwiki.TextUtil;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.auth.WikiPrincipal;
import com.ecyrd.jspwiki.auth.authorize.Role;
import com.ecyrd.jspwiki.util.HttpUtil;

/**
 *  Logs in an user based on a cookie stored in the user's computer.  The cookie
 *  information is stored in the <code>jspwiki.workDir</code>, under the directory
 *  "{@value COOKIE_DIR}".  For security purposes it is a very, very good idea
 *  to prevent access to this directory by everyone except the web server process;
 *  otherwise people having read access to this directory may be able to spoof
 *  other users.
 *  <p>
 *  The cookie directory is scrubbed of old entries every {@value SCRUB_PERIOD} milliseconds.
 *  <p>
 *   This module must be used with a CallbackHandler (such as
 *   {@link WebContainerCallbackHandler}) that supports the following Callback
 *   types:
 *   </p>
 *   <ol>
 *   <li>{@link HttpRequestCallback}- supplies the cookie, which should contain
 *       an unique id for fetching the UID.</li>
 *   <li>{@link WikiEngineCallBack} - allows access to the WikiEngine itself.
 *   </ol>
 *  <p>
 *  After authentication, a generic WikiPrincipal based on the username will be
 *  created and associated with the Subject. Principals
 *  {@link com.ecyrd.jspwiki.auth.authorize.Role#ALL} and
 *  {@link com.ecyrd.jspwiki.auth.authorize.Role#AUTHORIZED} will be added.
 *  @see javax.security.auth.spi.LoginModule#commit()
 *      </p>
 *  @author Janne Jalkanen
 *  @since  2.5.62
 */
public class CookieAuthenticationLoginModule extends AbstractLoginModule
{

    private static final Logger log = Logger.getLogger( CookieAuthenticationLoginModule.class );
    private static final String LOGIN_COOKIE_NAME = "JSPWikiUID";
    private static final String COOKIE_DIR        = "logincookies";

    /**
     *  User property for setting how long the cookie is stored on the user's computer.
     */
    public static final  String PROP_LOGIN_EXPIRY_DAYS  = "jspwiki.cookieAuthorization.expiry";

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
                        in = new FileReader( cookieFile );
                        String username = FileUtil.readContents( in );

                        if ( log.isDebugEnabled() )
                        {
                            log.debug( "Logged in loginName=" + username );
                            log.debug( "Added Principals Role.AUTHENTICATED,Role.ALL" );
                        }

                        // If login succeeds, commit these principals/roles
                        m_principals.add( new PrincipalWrapper( new WikiPrincipal( username,  WikiPrincipal.LOGIN_NAME ) ) );
                        m_principals.add( Role.AUTHENTICATED );
                        m_principals.add( Role.ALL );

                        // If login succeeds, overwrite these principals/roles
                        m_principalsToOverwrite.add( WikiPrincipal.GUEST );
                        m_principalsToOverwrite.add( Role.ANONYMOUS );
                        m_principalsToOverwrite.add( Role.ASSERTED );

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
    // FIXME3.0: For 3.0, switch to using java.util.UUID so that we can
    //           get rid of jug.jar
    public static void setLoginCookie( WikiEngine engine, HttpServletResponse response, String username )
    {
        UUID uid = UUIDGenerator.getInstance().generateRandomBasedUUID();

        int days = TextUtil.getIntegerProperty( engine.getWikiProperties(),
                                                PROP_LOGIN_EXPIRY_DAYS,
                                                DEFAULT_EXPIRY_DAYS );

        Cookie userId = new Cookie( LOGIN_COOKIE_NAME, uid.toString() );
        userId.setMaxAge( days * 24 * 60 * 60 );
        response.addCookie( userId );

        File cf = getCookieFile( engine, uid.toString() );
        Writer out = null;

        try
        {
            out = new FileWriter(cf);
            FileUtil.copyContents( new StringReader(username), out );

            if( log.isDebugEnabled() )
            {
                log.debug( "Created login cookie for user "+username+" for "+days+" days" );
            }
        }
        catch( IOException ex )
        {
            log.error("Unable to create cookie file to store user id: "+uid);
        }
        finally
        {
            if( out != null )
            {
                try
                {
                    out.close();
                }
                catch( IOException ex )
                {
                    log.error("Unable to close stream");
                }
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
     *
     *  @param days
     *  @param cookieDir
     */
    private static synchronized void scrub( int days, File cookieDir )
    {
        log.debug("Scrubbing cookieDir...");

        File[] files = cookieDir.listFiles();

        //long obsoleteDateLimit = System.currentTimeMillis() - (days+1) * 24 * 60 * 60 * 1000L;

        long obsoleteDateLimit = System.currentTimeMillis() - 30 * 1000L;

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
