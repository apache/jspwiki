/* 
    JSPWiki - a JSP-based WikiWiki clone.

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
package com.ecyrd.jspwiki.url;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.util.TextUtil;

/**
 *  Implements the default URL constructor using links directly to the
 *  JSP pages.  This is what JSPWiki by default is using.  For example,
 *  WikiContext.VIEW points at "Wiki.jsp", etc.
 *  
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
    
    protected static final Map<String,String> c_urlPatterns = new HashMap<String,String>();
    
    static
    {
        c_urlPatterns.put( WikiContext.ATTACH, "%uattach/%n" );
        c_urlPatterns.put( WikiContext.COMMENT, "%uComment.jsp?page=%n" );
        c_urlPatterns.put( WikiContext.CONFLICT, "%uPageModified.jsp?page=%n" );
        c_urlPatterns.put( WikiContext.DELETE, "%uDelete.jsp?page=%n" );
        c_urlPatterns.put( WikiContext.DIFF, "%uDiff.jsp?page=%n" );
        c_urlPatterns.put( WikiContext.EDIT, "%uEdit.jsp?page=%n" );
        c_urlPatterns.put( WikiContext.INFO, "%uPageInfo.jsp?page=%n" );
        c_urlPatterns.put( WikiContext.PREVIEW, "%uPreview.jsp?page=%n" );
        c_urlPatterns.put( WikiContext.RENAME, "%uRename.jsp?page=%n" );
        c_urlPatterns.put( WikiContext.RSS, "%urss.jsp" );
        c_urlPatterns.put( WikiContext.UPLOAD, "%uUpload.jsp?page=%n" );
        c_urlPatterns.put( WikiContext.VIEW, "%uWiki.jsp?page=%n" );
        c_urlPatterns.put( WikiContext.NONE,"%u%n" );
        c_urlPatterns.put( WikiContext.OTHER, "%u%n" );
        c_urlPatterns.put( WikiContext.DELETE_GROUP, "%uDeleteGroup.jsp?group=%n" );
        c_urlPatterns.put( WikiContext.EDIT_GROUP, "%uEditGroup.jsp?group=%n" );
        c_urlPatterns.put( WikiContext.VIEW_GROUP, "%uGroup.jsp?group=%n" );
        c_urlPatterns.put( WikiContext.CREATE_GROUP, "%uNewGroup.jsp" );
        c_urlPatterns.put( WikiContext.ERROR, "%uError.jsp" );
        c_urlPatterns.put( WikiContext.FIND, "%uSearch.jsp" );
        c_urlPatterns.put( WikiContext.INSTALL, "%uInstall.jsp" );
        c_urlPatterns.put( WikiContext.LOGIN, "%uLogin.jsp?redirect=%n" );
        c_urlPatterns.put( WikiContext.LOGOUT, "%uLogout.jsp" );
        c_urlPatterns.put( WikiContext.MESSAGE, "%uMessage.jsp" );
        c_urlPatterns.put( WikiContext.PREFS, "%uUserPreferences.jsp" );
        c_urlPatterns.put( WikiContext.WORKFLOW, "%uWorkflow.jsp" );
        c_urlPatterns.put( WikiContext.ADMIN, "%uadmin/Admin.jsp" );
    };
    
    /**
     * 
     * {@inheritDoc}
     */
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
                m_pathPrefix = "/JSPWiki/"; // Just a guess.
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
        throws IllegalArgumentException
    {
        if( context.equals(WikiContext.VIEW) )
        {
            if( name == null ) return "%uWiki.jsp"; // FIXME
        }
        
        // Find the action matching our pattern (could throw exception)
        return c_urlPatterns.get( context );
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
     *  
     *  {@inheritDoc}
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
     *  
     *  {@inheritDoc}
     */
    public String parsePage( String context,
                             HttpServletRequest request,
                             String encoding )
        throws UnsupportedEncodingException
    {
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
    /*
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
    */
    /**
     *  Takes the name of the page from the request URI.
     *  The initial slash is also removed.  If there is no page,
     *  returns null.
     *  
     *  @param request The request to parse
     *  @param encoding The encoding to use
     *  
     *  @return a parsed page name, or null, if it cannot be found
     *  
     *  @throws UnsupportedEncodingException If the encoding is not recognized.
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
     * @param request The HTTP Request that was used to end up in this page.
     * @return "Wiki.jsp", "PageInfo.jsp", etc.  Just return the name,
     *         JSPWiki will figure out the page.
     */
    public String getForwardPage( HttpServletRequest request )
    {
        return request.getPathInfo();
    }
}
