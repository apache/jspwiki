/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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
package com.ecyrd.jspwiki;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;


/**
 *  <p>Provides state information throughout the processing of a page.  A
 *  WikiContext is born when the JSP pages that are the main entry
 *  points, are invoked.  The JSPWiki engine creates the new
 *  WikiContext, which basically holds information about the page, the
 *  handling engine, and in which context (view, edit, etc) the
 *  call was done.</p>
 *  <p>A WikiContext also provides request-specific variables, which can
 *  be used to communicate between plugins on the same page, or
 *  between different instances of the same plugin.  A WikiContext
 *  variable is valid until the processing of the page has ended.  For
 *  an example, please see the Counter plugin.</p>
 *  <p>When a WikiContext is created, it automatically associates a 
 *  {@link WikiSession} object with the user's HttpSession. The
 *  WikiSession contains information about the user's authentication
 *  status, and is consulted by {@link #getCurrentUser()}.
 *  object</p>
 *
 *  @see com.ecyrd.jspwiki.plugin.Counter
 *  
 *  @author Janne Jalkanen
 *  @author Andrew R. Jaquith
 */
public class WikiContext
    implements Cloneable
{
    WikiPage   m_page;
    WikiEngine m_engine;
    String     m_requestContext = VIEW;
    String     m_template       = "default";

    Map        m_variableMap = new HashMap();

    HttpServletRequest m_request = null;

    WikiSession m_session = null;

    /** The VIEW context - the user just wants to view the page
        contents. */
    public static final String    VIEW     = "view";

    /** The EDIT context - the user is editing the page. */
    public static final String    EDIT     = "edit";

    /** User is preparing for a login/authentication. */
    public static final String    LOGIN    = "login";

    /** User is viewing a DIFF between the two versions of the page. */
    public static final String    DIFF     = "diff";

    /** User is viewing page history. */
    public static final String    INFO     = "info";

    /** User is previewing the changes he just made. */
    public static final String    PREVIEW  = "preview";

    /** User has an internal conflict, and does quite not know what to
        do. Please provide some counseling. */
    public static final String    CONFLICT = "conflict";

    /** An error has been encountered and the user needs to be informed. */
    public static final String    ERROR    = "error";

    public static final String    UPLOAD   = "upload";

    public static final String    COMMENT  = "comment";
    public static final String    FIND     = "find";

    public static final String    PREFS    = "prefs";
    public static final String    DELETE   = "del";
    public static final String    ATTACH   = "att";
    public static final String    RSS      = "rss";

    public static final String    NONE     = "";  // This is not a JSPWiki context, use it to access static files
    
    /**
     *  Create a new WikiContext for the given WikiPage. Delegates to
     * {@link #WikiContext(WikiEngine, HttpServletRequest, WikiPage)}.
     *  @param engine The WikiEngine that is handling the request.
     *  @param page   The WikiPage.  If you want to create a
     *  WikiContext for an older version of a page, you must use this
     *  constructor. 
     */
    public WikiContext( WikiEngine engine, WikiPage page )
    {
      this(engine, null, page);
    }
    
    /**
     * <p>
     * Creates a new WikiContext for the given WikiEngine, WikiPage and
     * HttpServletRequest. This constructor will also look up the HttpSession
     * associated with the request, and determine if a WikiSession object is
     * present. If not, a new one is created.
     * </p>
     * <p>
     * After the WikiSession object is obtained, the current authentication
     * status is checked. If not authenticated, or if the login status reported
     * by the container has changed, the constructor attempts to log in the user
     * with
     * {@link com.ecyrd.jspwiki.auth.AuthenticationManager#loginContainer(WikiContext)}.
     * </p>
     * @param engine The WikiEngine that is handling the request
     * @param request The HttpServletRequest that should be associated with this
     *            context
     * @param page The WikiPage. If you want to create a WikiContext for an
     *            older version of a page, you must supply this parameter
     */
    public WikiContext(WikiEngine engine, HttpServletRequest request, WikiPage page) {
      m_engine = engine;
      m_request = request;
      m_session = WikiSession.getWikiSession(request);
      m_page   = page;
      
      // Associate the wikiSession with this context
      // and associate a Subject with the session if it isn't there already
      if ( m_session.isUnknown() || m_session.isContainerStatusChanged( request ) )
      {
          engine.getAuthenticationManager().loginContainer( this );
      }
    }

    /**
     *  Returns the handling engine.
     */
    public WikiEngine getEngine()
    {
        return m_engine;
    }

    /**
     *  Returns the page that is being handled.
     */
    public WikiPage getPage()
    {
        return m_page;
    }

    /**
     *  Sets the page that is being handled.
     *
     *  @since 2.1.37.
     */
    public void setPage( WikiPage page )
    {
        m_page = page;
    }

    /**
     *  Returns the request context.
     */
    public String getRequestContext()
    {
        return m_requestContext;
    }

    /**
     *  Sets the request context.  See above for the different
     *  request contexts (VIEW, EDIT, etc.)
     *
     *  @param arg The request context (one of the predefined contexts.)
     */
    public void setRequestContext( String arg )
    {
        m_requestContext = arg;
    }

    /**
     *  Gets a previously set variable.
     *
     *  @param key The variable name.
     *  @return The variable contents.
     */
    public Object getVariable( String key )
    {
        return m_variableMap.get( key );
    }

    /**
     *  Sets a variable.  The variable is valid while the WikiContext is valid,
     *  i.e. while page processing continues.  The variable data is discarded
     *  once the page processing is finished.
     *
     *  @param key The variable name.
     *  @param data The variable value.
     */
    public void setVariable( String key, Object data )
    {
        m_variableMap.put( key, data );
    }

    /**
     *  This method will safely return any HTTP parameters that 
     *  might have been defined.  You should use this method instead
     *  of peeking directly into the result of getHttpRequest(), since 
     *  this method is smart enough to do all of the right things,
     *  figure out UTF-8 encoded parameters, etc.
     *
     *  @since 2.0.13.
     *  @param paramName Parameter name to look for.
     *  @return HTTP parameter, or null, if no such parameter existed.
     */
    public String getHttpParameter( String paramName )
    {
        String result = null;

        if( m_request != null )
        {
            result = m_engine.safeGetParameter( m_request, paramName );
        }

        return result;
    }

    /**
     *  If the request originated from a HTTP server,
     *  the HTTP request is stored here.
     *
     *  @param req The HTTP servlet request.
     *
     *  @since 2.0.13.
     */
    public void setHttpRequest( HttpServletRequest req )
    {
        m_request = req;
    }

    /**
     *  If the request did originate from a HTTP request,
     *  then the HTTP request can be fetched here.  However, it the request
     *  did NOT originate from a HTTP request, then this method will
     *  return null, and YOU SHOULD CHECK FOR IT!
     *
     *  @return Null, if no HTTP request was done.
     *  @since 2.0.13.
     */
    public HttpServletRequest getHttpRequest()
    {
        return m_request;
    }

    /**
     *  Sets the template to be used for this request.
     *  @since 2.1.15.
     */
    public void setTemplate( String dir )
    {
        m_template = dir;
    }

    /**
     *  Gets the template that is to be used throughout this request.
     *  @since 2.1.15.
     */
    public String getTemplate()
    {
        return m_template;
    }

    /**
     *  Convenience method that gets the current user. Delegates the
     *  lookup to the WikiSession associated with this WikiContect. 
     *  May return null, in case the current
     *  user has not yet been determined; or this is an internal system.
     *  If the WikiSession has not been set, <em>always</em> returns null.
     */
    public Principal getCurrentUser()
    {
        if (m_session == null) {
          return null;
        }
        return m_session.getUserPrincipal();
    }

    public String getViewURL( String page )
    {
        return getURL( VIEW, page, null );
    }

    public String getURL( String context,
                          String page )
    {
        return getURL( context, page, null );
    }

    /**
     *  Returns an URL from a page
     */
    public String getURL( String context,
                          String page,
                          String params )
    {
        // FIXME: is rather slow
        return m_engine.getURL( context,
                                page,
                                params,
                                "absolute".equals(m_engine.getVariable( this, WikiEngine.PROP_REFSTYLE )) );
    }

    /**
     *  Returns a shallow clone of the WikiContext.
     *
     *  @since 2.1.37.
     */
    public Object clone()
    {
        WikiContext copy = new WikiContext( m_engine, m_page );
        
        copy.m_requestContext = m_requestContext;
        copy.m_template       = m_template;
        copy.m_variableMap    = m_variableMap;
        copy.m_request        = m_request;
        copy.m_session        = m_session;
        return copy;
    }
    
    /**
     * Returns the WikiSession associated with the context.
     */  
    public WikiSession getWikiSession() {
      return m_session;
    }
    
    /**
     * Sets the WikiSession assocated with the context.
     * @param session
     */
    public void setWikiSession(WikiSession session) {
      m_session = session;
    }
        
}
