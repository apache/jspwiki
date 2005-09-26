/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2002 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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
package com.ecyrd.jspwiki.tags;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.log4j.Logger;


/**
 * Sets or gets Cookie values. This implementation makes the following 
 * assumptions:
 * <ul>
 * <li>The cookie contains any number of name-value pairs
 * <li>Name-value pairs are separated by "&" in the encoded cookie value string
 * <li>An encoded name-value pair is compatible with JavaScript's 
 * encodeURIComponent(). Notably, spaces are encoded as "%20".
 * <li>A decoded name-value pair separates the name and value with a "="
 * </ul>
 * 
 * <p>The value of a cookie carrying values n1="v1" and n2="v2 with space" 
 * would thus be
 * <pre>
 *   n1%3Dv1&n2%3Dv2%20with%20space
 * </pre>
 * 
 * <p>Usage:
 * 
 * <pre>
 * &lt;wiki:cookie name="cookiename" var="contextvariable" scope="page" /&gt;
 * </pre>
 * - Returns the value of the named cookie, or an empty string if not set.
 * If 'var' is specified, the value is set into a context variable of this name.
 * The 'scope' parameter may be added to specify the context: "session", 
 * "page", "request". If var is omitted, the output is placed directly into
 * the JSP page.
 * 
 * <pre>
 * &lt;wiki:cookie name="cookiename" value="encoded_value" /&gt;
 * </pre>
 * - Sets the named cookie to the given value. If the value string is empty,
 * the cookie value is set to empty; otherwise the cookie encoding rules of
 * this class must be followed for the value.
 * 
 * <pre>
 * &lt;wiki:cookie name="cookiename" item="parameter_name" /&gt;
 * </pre>
 * - Assumes that the cookie contains URLEncoded name-value pairs,
 * with name and value separated by an equals sign, and returns the value
 * of the specified item. 
 * 
 * &lt;wiki:cookie name="cookiename" item="parameter_name" value="value" /&gt;
 * </pre>
 * - Sets the value of 'parameter_name' in the named cookie to 'value'.
 * 
 * <pre>
 * &lt;wiki:cookie name="cookiename" clear="parameter_name" /&gt;
 * </pre>
 * - Removes the named parameter from the cookie.
 * 
 * <pre>
 * &lt;wiki:cookie clear="cookiename" /&gt;
 * </pre>
 * - Removes the named cookie. Clear may be used at the same time as a value
 * is retrieved (or set, despite the dubious usefulness of that operation).
 */
public class CookieTag 
    extends TagSupport
{
    private static Logger log = Logger.getLogger( CookieTag.class );

    /** Name of the cookie value. Required. */
    private String m_name;
    /** Name of the cookie nvp item. Optional. */
    private String m_item;
    /** A value to echo or set. Optional. */
    private String m_value;
    /** Name of a context variable to set result in. Optional, defaults to out.*/
    private String m_var;
    /** Scope of m_var: request, session, page. */
    private String m_scope;
    /** Name of a cookie or a cookie nvp to clear. */
    private String m_clear;
    
    
    public void setName( String s )  { m_name = s; }
    public void setItem( String s )  { m_item = s; }
    public void setValue( String s ) { m_value = s; }
    public void setVar( String s )   { m_scope = s; }
    public void setClear( String s ) { m_clear = s; }
    public void setScope( String s ) { m_scope = s; }


    public void release()
    {
        m_name = m_item = m_var = m_value = m_clear = m_scope = null;
        super.release();
    }
    
    /**
     * Examines the parameter and returns the corresponding scope identifier:
     * "request" maps to PageContext.REQUEST_SCOPE, and so on.
     * Possible values are "page", "session", "application", and "request",
     * which is the default return value.
     */
    private int getScope( String s ) {
        if( s == null ) {
            return PageContext.REQUEST_SCOPE;
        }
        if( "page".equals( m_scope ) ) {
            return PageContext.PAGE_SCOPE;
        } 
        if( "session".equals( m_scope ) ) {
            return PageContext.SESSION_SCOPE;
        } 
        if( "application".equals( m_scope ) ) {
            return PageContext.APPLICATION_SCOPE;
        } 
            
        return PageContext.REQUEST_SCOPE;
    }
    
    
    public int doEndTag() 
    {
        String out = null;
        Cookie cookie = findCookie( m_name );
        boolean changed = false;
        
        if( m_value != null ) {
            if( m_item != null ) {
                setItemValue( cookie, m_item, m_value );
            } else {
                cookie.setValue( m_value );
            }
            changed = true;
        } else {
            if( m_item != null ) {
                out = getItemValue( cookie, m_item );
            } else {
                out = cookie.getValue();
            }
        }

        if( out != null ) {
            if( m_var != null ) {
                int scope = getScope( m_scope );
                pageContext.setAttribute( m_var, out,  scope );
            } else {
                try {
                    pageContext.getOut().print( out );
                } catch( IOException ioe ) {
                    log.warn( "Failed to write to JSP page: " + ioe.getMessage(), ioe );
                }
            }
        }

        Cookie cleared = null;
        if( m_clear != null ) {
            cleared = findCookie( m_clear );
            if( m_item != null ) {
                setItemValue( cookie, m_item, null );
            } else {
                cleared.setValue( null );
            }
        }

        HttpServletResponse res = (HttpServletResponse)pageContext.getResponse();
        if( changed ) {
            res.addCookie( cookie );
        }
        if( cleared != null ) {
            res.addCookie( cleared );
        }

        return( EVAL_PAGE );
    }

    /**
     * Sets a single name-value pair in the given cookie.
     */
    private void setItemValue( Cookie c, String item, String value ) {
        if( c == null ) { 
            return;
        }
        String in = c.getValue();
        Map values = parseCookieValues( in );
        values.put( item, value );
        String cv = encodeValues( values );
        c.setValue( cv );
    }
    
    /**
     * Returns the value of the given item in the cookie.
     */
    private String getItemValue( Cookie c, String item ) {
        if( c == null || item == null ) {
            return null;
        }
        String in = c.getValue();
        Map values = parseCookieValues( in );
        return (String)values.get( item );
    }
    
    
    /**
     * Parses a cookie value, of format name1%3Fvalue1&name2%3Fvalue2...,
     * into a Map<String,String>.
     */
    private Map parseCookieValues( String s ) {
        Map rval = new HashMap();
        if( s == null ) {
            return rval;
        }
        String[] nvps = s.split( "&" );
        if( nvps == null ) {
            return rval;
        }
        for( int i = 0; i < nvps.length; i++ ) {
            String nvp = decode( nvps[i] );
            String[] nv = nvp.split( "=" );
            if( nv[0] != null && nv[0].trim().length() > 0 ) {
                rval.put( nv[0], nv[1] );
            }
        }
        
        return rval;
    }
    
    /**
     * Encodes name-value pairs in the map into a single string, in a format
     * understood by this class and JavaScript decodeURIComponent().
     */
    private String encodeValues( Map values ) {
        StringBuffer rval = new StringBuffer();
        if( values == null || values.size() == 0 ) {
            return rval.toString();
        }
        Iterator it = values.keySet().iterator();
        while( it.hasNext() ) {
            String n = (String)it.next();
            String v = (String)values.get( n );
            if( v != null ) {
                String nv = n + "=" + v;
                rval.append( encode( nv ) );
            }
        }
        
        return rval.toString();
    }
    
    /**
     * Converts a String to an encoding understood by JavaScript
     * decodeURIComponent.
     */
    private String encode( String nvp ) {
        String coded = "";
        try {
            coded = URLEncoder.encode( nvp, "UTF-8" );
        } catch( UnsupportedEncodingException e ) {
            /* never happens */
            log.info( "Failed to encode UTF-8", e );
        }
        return coded.replaceAll( "\\+", "%20" );
    }
    
    /**
     * Converts a cookie value (set by this class, or by a JavaScript
     * encodeURIComponent call) into a plain string.
     */
    private String decode( String envp ) {
        String rval;
        try {
            rval = URLDecoder.decode( envp , "UTF-8" );
            return( rval );
        } catch( UnsupportedEncodingException e ) {
            log.error( "Failed to decode cookie", e );
            return( envp );
        }
    }
    
    /**
     * Locates the named cookie in the request, or creates a new one if it
     * doesn't exist.
     */
    private Cookie findCookie( String cname )
    {
        HttpServletRequest req = (HttpServletRequest)pageContext.getRequest();
        if( req != null ) {
            Cookie[] cookies = req.getCookies();
            if( cookies != null ) {
                for( int i = 0; i < cookies.length; i++ ) {
                    if( cookies[i].getName().equals( cname ) ) {
                        return( cookies[i] );
                    }
                }
            }
        }
        
        return new Cookie( cname, null );
    }
    
}
