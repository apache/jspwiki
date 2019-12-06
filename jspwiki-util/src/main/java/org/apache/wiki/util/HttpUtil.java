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

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 *  Contains useful utilities for some common HTTP tasks.
 *
 *  @since 2.1.61.
 */
public final class HttpUtil {

    static Logger log = Logger.getLogger( HttpUtil.class );
    
    /** Private constructor to prevent direct instantiation. */
    private HttpUtil() {
    }
    
    /**
     * returns the remote address by looking into {@code x-forwarded-for} header or, if unavailable, 
     * into {@link HttpServletRequest#getRemoteAddr()}.
     * 
     * @param req http request
     * @return remote address associated to the request.
     */
    public static String getRemoteAddress( HttpServletRequest req ) {
		return StringUtils.isNotEmpty ( req.getHeader( "X-Forwarded-For" ) ) ? req.getHeader( "X-Forwarded-For" ) : 
			                                                                   req.getRemoteAddr();
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
    public static String retrieveCookieValue( HttpServletRequest request, String cookieName ) {
        Cookie[] cookies = request.getCookies();

        if( cookies != null ) {
            for( int i = 0; i < cookies.length; i++ ) {
                if( cookies[i].getName().equals( cookieName ) ) {
                    String value = cookies[i].getValue();
                    if( value.length() == 0 ) {
                        return null;
                    }
                    if( value.charAt( 0 ) == '"' && value.charAt( value.length() - 1 ) == '"' ) {
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
     *  @param pageName  The page name for which the ETag should be created.
     *  @param lastModified  The page last modified date for which the ETag should be created.
     *  @return A String depiction of an ETag.
     */
    public static String createETag( String pageName, Date lastModified ) {
        return Long.toString( pageName.hashCode() ^ lastModified.getTime() );
    }
    
    /**
     *  If returns true, then should return a 304 (HTTP_NOT_MODIFIED)
     *  @param req the HTTP request
     *  @param pageName the wiki page name to check for
     *  @param lastModified the last modified date of the wiki page to check for
     *  @return the result of the check
     */
    public static boolean checkFor304( HttpServletRequest req, String pageName, Date lastModified ) {
        //
        //  We'll do some handling for CONDITIONAL GET (and return a 304)
        //  If the client has set the following headers, do not try for a 304.
        //
        //    pragma: no-cache
        //    cache-control: no-cache
        //

        if( "no-cache".equalsIgnoreCase( req.getHeader( "Pragma" ) )
            || "no-cache".equalsIgnoreCase( req.getHeader( "cache-control" ) ) ) {
            // Wants specifically a fresh copy
        } else {
            //
            //  HTTP 1.1 ETags go first
            //
            String thisTag = createETag( pageName, lastModified );
                        
            String eTag = req.getHeader( "If-None-Match" );
            
            if( eTag != null && eTag.equals(thisTag) ) {
                return true;
            }
            
            //
            //  Next, try if-modified-since
            //
            DateFormat rfcDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");

            try {
                long ifModifiedSince = req.getDateHeader( "If-Modified-Since" );

                //log.info("ifModifiedSince:"+ifModifiedSince);
                if( ifModifiedSince != -1 ) {
                    long lastModifiedTime = lastModified.getTime();

                    //log.info("lastModifiedTime:" + lastModifiedTime);
                    if( lastModifiedTime <= ifModifiedSince ) {
                        return true;
                    }
                } else {
                    try {
                        String s = req.getHeader("If-Modified-Since");

                        if( s != null ) {
                            Date ifModifiedSinceDate = rfcDateFormat.parse(s);
                            //log.info("ifModifiedSinceDate:" + ifModifiedSinceDate);
                            if( lastModified.before(ifModifiedSinceDate) ) {
                                return true;
                            }
                        }
                    } catch (ParseException e) {
                        log.warn(e.getLocalizedMessage(), e);
                    }
                }
            } catch( IllegalArgumentException e ) {
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
     *  it assumes it to be an http:// url.
     * 
     *  @param uri  URI to take a poke at
     *  @return Possibly a valid URI
     *  @since 2.2.8
     */
    public static String guessValidURI( String uri ) {
        if( uri.indexOf( '@' ) != -1 ) {
            if( !uri.startsWith( "mailto:" ) ) {
                // Assume this is an email address
                uri = "mailto:" + uri;
            }
        } else if( notBeginningWithHttpOrHttps( uri ) ) {
            uri = "http://" + uri;
        }
        
        return uri;
    }

	static boolean notBeginningWithHttpOrHttps( final String uri ) {
		return uri.length() > 0 && !( ( uri.startsWith("http://" ) || uri.startsWith( "https://" ) ) );
	}

    /**
     *  Returns the query string (the portion after the question mark).
     *
     *  @param request The HTTP request to parse.
     *  @return The query string. If the query string is null, returns an empty string.
     *
     *  @since 2.1.3 (method moved from WikiEngine on 2.11.0.M6)
     */
    public static String safeGetQueryString( final HttpServletRequest request, final Charset contentEncoding ) {
        if( request == null ) {
            return "";
        }

        String res = request.getQueryString();
        if( res != null ) {
            res = new String( res.getBytes( StandardCharsets.ISO_8859_1 ), contentEncoding );

            //
            // Ensure that the 'page=xyz' attribute is removed
            // FIXME: Is it really the mandate of this routine to do that?
            //
            final int pos1 = res.indexOf("page=");
            if( pos1 >= 0 ) {
                String tmpRes = res.substring( 0, pos1 );
                final int pos2 = res.indexOf( "&",pos1 ) + 1;
                if ( ( pos2 > 0 ) && ( pos2 < res.length() ) ) {
                    tmpRes = tmpRes + res.substring(pos2);
                }
                res = tmpRes;
            }
        }

        return res;
    }

}
