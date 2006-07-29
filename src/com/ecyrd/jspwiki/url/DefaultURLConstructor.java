/* 
  JSPWiki - a JSP-based WikiWiki clone.

  Copyright (C) 2001-2005 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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
package com.ecyrd.jspwiki.url;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;

import com.ecyrd.jspwiki.TextUtil;
import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.ui.CommandResolver;
import com.ecyrd.jspwiki.ui.Command;

/**
 *  Implements the default URL constructor using links directly to the
 *  JSP pages.  This is what JSPWiki by default is using.  For example,
 *  WikiContext.VIEW points at "Wiki.jsp", etc.
 *  
 *  @author Janne Jalkanen
 *  @since 2.2
 */
public class DefaultURLConstructor
    implements URLConstructor
{
    protected WikiEngine m_engine;

    /** Are URL styles relative or absolute? */
    protected boolean          m_useRelativeURLStyle = true;

    /**
     *  Contains the absolute path of the JSPWiki Web application without the
     *  actual servlet (which is the m_urlPrefix).
     */
    protected String m_pathPrefix = "";
    
    public void initialize( WikiEngine engine, 
                            Properties properties )
    {
        m_engine = engine;

        m_useRelativeURLStyle = "relative".equals( properties.getProperty( WikiEngine.PROP_REFSTYLE,
                                                                           "relative" ) );

        String baseurl = engine.getBaseURL();

        if( baseurl != null && baseurl.length() > 0 )
        {
            try
            {
                URL url = new URL( baseurl );
        
                String path = url.getPath();
        
                m_pathPrefix = path;
            }
            catch( MalformedURLException e )
            {
                m_pathPrefix = "/JSPWiki"; // Just a guess.
            }
        }
    }

    /**
     *  Does replacement of some particular variables.  The variables are:
     *  
     *  <ul>
     *  <li> "%u" - inserts either the base URL (when absolute is required), or the base path
     *       (which is an absolute path without the host name).
     *  <li> "%U" - always inserts the base URL
     *  <li> "%p" - always inserts the base path
     *  <li> "%n" - inserts the page name
     *  </ul>
     *  
     * @param baseptrn  The pattern to use
     * @param name The page name
     * @param absolute If true, %u is always the entire base URL, otherwise it depends on
     *                 the setting in jspwiki.properties.
     * @return A replacement.
     */
    protected final String doReplacement( String baseptrn, String name, boolean absolute )
    {
        String baseurl = m_pathPrefix;

        if( absolute ) baseurl = m_engine.getBaseURL();

        baseptrn = TextUtil.replaceString( baseptrn, "%u", baseurl );
        baseptrn = TextUtil.replaceString( baseptrn, "%U", m_engine.getBaseURL() );
        baseptrn = TextUtil.replaceString( baseptrn, "%n", encodeURI(name) );
        baseptrn = TextUtil.replaceString( baseptrn, "%p", m_pathPrefix );

        return baseptrn;
    }

    /**
     *  URLEncoder returns pluses, when we want to have the percent
     *  encoding.  See http://issues.apache.org/bugzilla/show_bug.cgi?id=39278
     *  for more info.
     *  
     *  We also convert any %2F's back to slashes to make nicer-looking URLs.
     */
    private final String encodeURI( String uri )
    {
        uri = m_engine.encodeName(uri);
        
        uri = StringUtils.replace( uri, "+", "%20" );
        uri = StringUtils.replace( uri, "%2F", "/" );
        
        return uri;
    }
    
    /**
     * Returns the URL pattern for a supplied wiki request context.
     * @param context the wiki context
     * @param name the wiki page
     * @return A pattern for replacement.
     * @throws IllegalArgumentException if the context cannot be found
     */
    public static String getURLPattern( String context, String name )
    {
        if( context.equals(WikiContext.VIEW) )
        {
            if( name == null ) return "%uWiki.jsp"; // FIXME
        }
        
        // Find the action matching our pattern (could throw exception)
        Command command = CommandResolver.findCommand( context );
        
        return command.getURLPattern();
    }
    
    /**
     *  Constructs the actual URL based on the context.
     */
    private String makeURL( String context,
                            String name,
                            boolean absolute )
    {
        return doReplacement( getURLPattern(context,name), name, absolute );
    }

    /**
     *  Constructs the URL with a bunch of parameters.
     *  @param parameters If null or empty, no parameters are added.
     */
    public String makeURL( String context,
                           String name,
                           boolean absolute,
                           String parameters )
    {
        if( parameters != null && parameters.length() > 0 )
        {            
            if( context.equals(WikiContext.ATTACH) )
            {
                parameters = "?"+parameters;
            }
            else if( context.equals(WikiContext.NONE) )
            {
                parameters = (name.indexOf('?') != -1 ) ? "&amp;" : "?" + parameters;
            }
            else
            {
                parameters = "&amp;"+parameters;
            }
        }
        else
        {
            parameters = "";
        }
        return makeURL( context, name, absolute )+parameters;
    }

    /**
     *  Should parse the "page" parameter from the actual
     *  request.
     */
    public String parsePage( String context,
                             HttpServletRequest request,
                             String encoding )
        throws UnsupportedEncodingException
    {
        request.setCharacterEncoding( encoding );
        String pagereq = request.getParameter( "page" );

        if( context.equals(WikiContext.ATTACH) )
        {
            pagereq = parsePageFromURL( request, encoding );
        }

        return pagereq;
    }

    /**
     *  There's a bug in Tomcat until 5.5.16 at least: The "+" sign is not
     *  properly decoded by the servlet container, and therefore request.getPathInfo()
     *  will return faulty results for paths which contains + signs to signify spaces.
     *  <p>
     *  This method provides a workaround by simply parsing the getRequestURI(), which
     *  is returned from the servlet container undedecoded.
     *  <p>
     *  Please see <a href="http://issues.apache.org/bugzilla/show_bug.cgi?id=39278">Tomcat Bug 39278</a>
     *  for more information.
     *  
     *  @param request A HTTP servlet request
     *  @param encoding The used encoding
     *  @return a String, decoded by JSPWiki, specifying extra path information that comes 
     *          after the servlet path but before the query string in the request URL; 
     *          or null if the URL does not have any extra path information
     *  @throws UnsupportedEncodingException
     */
    private static String getPathInfo( HttpServletRequest request, String encoding )
        throws UnsupportedEncodingException
    {
        String c = request.getContextPath(); // Undecoded
        String s = request.getServletPath(); // Decoded
        String u = request.getRequestURI();  // Undecoded
        
        c = URLDecoder.decode( c, encoding );
        u = URLDecoder.decode( u, encoding );
        
        String pi = u.substring( s.length()+c.length() );
        
        if( pi.length() == 0 ) pi = null;
        
        return pi;
    }
    
    /**
     *  Takes the name of the page from the request URI.
     *  The initial slash is also removed.  If there is no page,
     *  returns null.
     */
    public static String parsePageFromURL( HttpServletRequest request,
                                           String encoding )
        throws UnsupportedEncodingException
    {
        String name = request.getPathInfo();

        if( name == null || name.length() <= 1 )
        {
            return null;
        }
        else if( name.charAt(0) == '/' )
        {
            name = name.substring(1);
        }
       
        //
        //  This is required, because by default all URLs are handled
        //  as Latin1, even if they are really UTF-8.
        //
        
        // name = TextUtil.urlDecode( name, encoding );
        
        return name;
    }

    
    /**
     *  This method is not needed for the DefaultURLConstructor.
     *  
     *  @since
     */
    public String getForwardPage( HttpServletRequest request )
    {
        return request.getPathInfo();
    }
}