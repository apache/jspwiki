/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2003 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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
package com.ecyrd.jspwiki.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.WikiPage;

/**
 *  Contains useful utilities for some common HTTP tasks.
 *
 *  @author Janne Jalkanen
 *  @since 2.1.61.
 */
public class HttpUtil
{
    static Logger log = Logger.getLogger( HttpUtil.class );
    private static String cachedBaseURL = null;
    private static String cachedBaseURLNoContext = null;
    private static String cachedSecureBaseURL = null;
    private static String cachedSecureBaseURLNoContext = null;
    private static final String LOOPBACK = "127.0.0.1";
    private static final String LOCALHOST = "localhost";

    /**
     *  Attempts to retrieve the given cookie value from the request.
     *  Returns the string value (which may or may not be decoded
     *  correctly, depending on browser!), or null if the cookie is
     *  not found. The algorithm will automatically trim leading
     *  and trailing double quotes, if found.
     *
     *  @param request The current request
     *  @param cookieName The name of the cookie to fetch.
     *  @return Value of the cookie, or null, if there is no such cookie.
     */

    public static String retrieveCookieValue( HttpServletRequest request, String cookieName )
    {
        Cookie[] cookies = request.getCookies();

        if( cookies != null )
        {
            for( int i = 0; i < cookies.length; i++ )
            {
                if( cookies[i].getName().equals( cookieName ) )
                {
                    String value = cookies[i].getValue();
                    if ( value.charAt( 0 ) == '"' && value.charAt( value.length() - 1 ) == '"')
                    {
                        value = value.substring( 1, value.length() - 1 );
                    }
                    return value;
                }
            }
        }

        return( null );
    }

    public static String createETag( WikiPage p )
    {
        return Long.toString(p.getLastModified().getTime());
    }
    
    /**
     *  If returns true, then should return a 304 (HTTP_NOT_MODIFIED)
     */
    public static boolean checkFor304( HttpServletRequest req,
                                       WikiPage page )
    {
        //
        //  We'll do some handling for CONDITIONAL GET (and return a 304)
        //  If the client has set the following headers, do not try for a 304.
        //
        //    pragma: no-cache
        //    cache-control: no-cache
        //

        if( "no-cache".equalsIgnoreCase(req.getHeader("Pragma"))
            || "no-cache".equalsIgnoreCase(req.getHeader("cache-control"))) 
        {
            // Wants specifically a fresh copy
        } 
        else 
        {
            //
            //  HTTP 1.1 ETags go first
            //
            String thisTag = createETag( page );
                        
            String eTag = req.getHeader( "If-None-Match" );
            
            if( eTag != null )
            {
                if( eTag.equals(thisTag) )
                {
                    return true;
                }
            }
            
            //
            //  Next, try if-modified-since
            //
            DateFormat rfcDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
            Date lastModified = page.getLastModified();

            try
            {
                long ifModifiedSince = req.getDateHeader("If-Modified-Since");

                //log.info("ifModifiedSince:"+ifModifiedSince);
                if( ifModifiedSince != -1 )
                {
                    long lastModifiedTime = lastModified.getTime();

                    //log.info("lastModifiedTime:" + lastModifiedTime);
                    if( lastModifiedTime <= ifModifiedSince )
                    {
                        return true;
                    }
                } 
                else
                {
                    try 
                    {
                        String s = req.getHeader("If-Modified-Since");

                        if( s != null ) 
                        {
                            Date ifModifiedSinceDate = rfcDateFormat.parse(s);
                            //log.info("ifModifiedSinceDate:" + ifModifiedSinceDate);
                            if( lastModified.before(ifModifiedSinceDate) ) 
                            {
                                return true;
                            }
                        }
                    } 
                    catch (ParseException e) 
                    {
                        log.warn(e.getLocalizedMessage(), e);
                    }
                }
            }
            catch( IllegalArgumentException e )
            {
                // Illegal date/time header format.
                // We fail quietly, and return false.
                // FIXME: Should really move to ETags.
            }
        }
         
        return false;
    }

    /**
     *  Attempts to form a valid URI based on the string given.  Currently
     *  it can guess email addresses (mailto:).  If nothing else is given,
     *  it assumes it to be a http:// url.
     * 
     *  @param uri  URI to take a poke at
     *  @return Possibly a valid URI
     *  @since 2.2.8
     */
    public static String guessValidURI( String uri )
    {
        if( uri.indexOf('@') != -1 )
        {
            if( !uri.startsWith("mailto:") )
            {
                // Assume this is an email address
            
                uri = "mailto:"+uri;
            }
        }
        else if( uri.length() > 0 && !((uri.startsWith("http://") || uri.startsWith("https://")) ))
        {
            uri = "http://"+uri;
        }
        
        return uri;
    }

    /**
     * Calculates the base URL for wiki web pages. We could certainly do this by
     * consulting {@link WikiEngine#getBaseURL()}, but when requests are sent
     * over HTTPS, we need to send back something different. Thus, we build a
     * base URL by inspecting the incoming HTTP request. This method appends a
     * trailing slash to the returned URL.
     * @param request
     * @return the base URL
     */
//    public static String makeBaseURL( HttpServletRequest request )
//    {
//        return constructBaseURL( request, true );
//    }
//    
    /**
     * Calculates the base URL for wiki web pages, but does not append the
     * application context or trailing slash.
     * @param request may not be null
     * @return the base URL
     */
//    public static String makeBaseURLNoContext( HttpServletRequest request )
//    {
//        return constructBaseURL( request, false );
//    }

    private static String constructBaseURL( HttpServletRequest request, boolean appendContext )
    {
        String server = request.getServerName();
        boolean isLocalhost = ( server.startsWith( LOCALHOST ) || server.startsWith( LOOPBACK ) );
        boolean getFromCache = !isLocalhost;
 
        // If we are supposed to get this from the cache, retrieve value if we can
        if ( getFromCache )
        {
            if ( request.isSecure() )
            {
                if ( cachedSecureBaseURL != null ) 
                {
                    return appendContext ? cachedSecureBaseURL : cachedSecureBaseURLNoContext;
                }
            }
            else
            {
                if ( cachedBaseURL != null ) 
                {
                    return appendContext ? cachedBaseURL : cachedBaseURLNoContext;
                }
            }
        }
        
        // Build the base URL from scratch
        // If not localhost, get the proper server name
        if ( !isLocalhost ) {
            server = getServerName( request ); 
        }

        // Build the base URLs
        int defaultPort = request.isSecure() ? 443 : 80;
        String portSuffix = request.getServerPort() == defaultPort ? "" : ( ":" + String.valueOf( request.getServerPort() ) ); 
        String baseURLNoContext = request.getScheme() + "://" + server + portSuffix;
        String baseURL = baseURLNoContext + request.getContextPath() + "/";
        
        // Store new values in cache
        if ( getFromCache )
        {
            if ( request.isSecure() )
            {
                cachedSecureBaseURL = baseURL;
                cachedSecureBaseURLNoContext = baseURLNoContext;
            }
            else
            {
                cachedBaseURL = baseURL;
                cachedBaseURLNoContext = baseURLNoContext;
            }
        }
        
        // Return the newly constructed value
        return appendContext ? baseURL : baseURLNoContext;
    }
    
    /**
     * Static method that determines a fully-qualified host name by examining an
     * HTTP request's <code>Host</code> header. If the browser did not supply
     * a <code>Host</code> header (a highly unusual and possibly suspicious
     * event) or the <code>Host</code> header returns the localhost, the
     * method will calculate the host name using by resolving the name of the
     * server using the local naming service. The latter method may be less
     * reliable if the server's host resolves to multiple names, but it
     * should be sufficient in most cases.
     * @param request the HTTP request
     * @return the server name associated with the request, without trailing port number
     */
    private static String getServerName( HttpServletRequest request )
    {
        String host = request.getHeader( "Host" );
        if ( host != null && host.length() > 0 && 
           !( host.startsWith( LOCALHOST ) || host.startsWith( LOOPBACK ) ) )
        {
            // Strip off any port numbers at the end
            int colon = host.indexOf(':');
            if ( colon != -1 )
            {
                host = host.substring( 0, colon );
            }
        }
        else
        {
            try {
                InetAddress addr = InetAddress.getLocalHost();
                host = addr.getHostName();
                }
                catch (UnknownHostException e) 
                {
                    request.getServerName(); 
                }
        }
        return host;
    }
        
}
