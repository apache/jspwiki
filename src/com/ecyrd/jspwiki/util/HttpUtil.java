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

import java.text.*;
import java.util.Date;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Cookie;

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

    /**
     *  Attempts to retrieve the given cookie value from the request.
     *  Returns the string value (which may or may not be decoded
     *  correctly, depending on browser!), or null if the cookie is
     *  not found.
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
                    return( cookies[i].getValue() );
                }
            }
        }

        return( null );
    }

    /**
     *  If returns true, then should return a 304 (HTTP_NOT_MODIFIED)
     */
    public static boolean checkFor304( HttpServletRequest req,
                                       WikiPage page )
    {
        DateFormat rfcDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
        Date lastModified = page.getLastModified();

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
         
        return false;
    }

}
