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
package org.apache.wiki.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

import org.apache.wiki.WikiPage;

/**
 *  Contains useful utilities for some common HTTP tasks.
 *
 *  @since 2.1.61.
 */
public final class HttpUtil
{
    static Logger log = Logger.getLogger( HttpUtil.class );
    
    /**
     * Private constructor to prevent direct instantiation.
     */
    private HttpUtil()
    {
    }

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
                    if ( value.length() == 0 )
                    {
                        return null;
                    }
                    if ( value.charAt( 0 ) == '"' && value.charAt( value.length() - 1 ) == '"')
                    {
                        value = value.substring( 1, value.length() - 1 );
                    }
                    return value;
                }
            }
        }

        return null;
    }

    /**
     *  Creates an ETag based on page information.  An ETag is unique to each page
     *  and version, so it can be used to check if the page has changed.  Do not
     *  assume that the ETag is in any particular format.
     *  
     *  @param p  The page for which the ETag should be created.
     *  @return A String depiction of an ETag.
     */
    public static String createETag( WikiPage p )
    {
        return Long.toString(p.getName().hashCode() ^ p.getLastModified().getTime());
    }
    
    /**
     *  If returns true, then should return a 304 (HTTP_NOT_MODIFIED)
     *  @param req the HTTP request
     *  @param page the wiki page to check for
     *  @return the result of the check
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
            
            if( eTag != null && eTag.equals(thisTag) )
            {
                return true;
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

}
