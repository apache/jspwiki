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
package org.apache.jspwiki.api;

import java.security.Principal;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.servlet.http.HttpServletRequest;

import com.ecyrd.jspwiki.action.*;
import com.ecyrd.jspwiki.ui.stripes.HandlerInfo;

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
 *  <p>Do not cache the page object that you get from the WikiContext; always
 *  use getPage()!</p>
 *
 *  @see com.ecyrd.jspwiki.plugin.Counter
 *
 *  @author Andrew R. Jaquith
 */
public interface WikiContext
    extends Cloneable
{
    /** User is administering JSPWiki (Install, SecurityConfig). */
    public static final String    INSTALL  = HandlerInfo.getHandlerInfo( InstallActionBean.class, "install" ).getRequestContext();

    /** The VIEW context - the user just wants to view the page
        contents. */
    public static final String    VIEW     = HandlerInfo.getHandlerInfo( ViewActionBean.class, "view" ).getRequestContext();

    /** User wants to view or administer workflows. */
    public static final String    WORKFLOW = HandlerInfo.getHandlerInfo( WorkflowActionBean.class, "view" ).getRequestContext();

    /** The EDIT context - the user is editing the page. */
    public static final String    EDIT     = HandlerInfo.getHandlerInfo( EditActionBean.class, "edit" ).getRequestContext();

    /** User is preparing for a login/authentication. */
    public static final String    LOGIN    = HandlerInfo.getHandlerInfo( LoginActionBean.class, "login" ).getRequestContext();

    /** User is preparing to log out. */
    public static final String    LOGOUT   = HandlerInfo.getHandlerInfo( LoginActionBean.class, "logout" ).getRequestContext();

    /** JSPWiki wants to display a message. */
    public static final String    MESSAGE  = HandlerInfo.getHandlerInfo( MessageActionBean.class, "message" ).getRequestContext();

    /** User is viewing a DIFF between the two versions of the page. */
    public static final String    DIFF     = HandlerInfo.getHandlerInfo( EditActionBean.class, "diff" ).getRequestContext();

    /** User is viewing page history. */
    public static final String    INFO     = HandlerInfo.getHandlerInfo( ViewActionBean.class, "info" ).getRequestContext();

    /** User is previewing the changes he just made. */
    public static final String    PREVIEW  = HandlerInfo.getHandlerInfo( EditActionBean.class, "preview" ).getRequestContext();

    /** User has an internal conflict, and does quite not know what to
        do. Please provide some counseling. */
    public static final String    CONFLICT = HandlerInfo.getHandlerInfo( PageModifiedActionBean.class, "conflict" ).getRequestContext();

    /** An error has been encountered and the user needs to be informed. */
    public static final String    ERROR    = HandlerInfo.getHandlerInfo( ErrorActionBean.class, "error" ).getRequestContext();

    /** User is uploading something. */
    public static final String    UPLOAD   = HandlerInfo.getHandlerInfo( UploadActionBean.class, "upload" ).getRequestContext();

    /** User is commenting something. */
    public static final String    COMMENT  = HandlerInfo.getHandlerInfo( CommentActionBean.class, "comment" ).getRequestContext();

    /** User is searching for content. */
    public static final String    FIND     = HandlerInfo.getHandlerInfo( SearchActionBean.class, "find" ).getRequestContext();

    /** User wishes to create a new group */
    public static final String    CREATE_GROUP = HandlerInfo.getHandlerInfo( GroupActionBean.class, "create" ).getRequestContext();

    /** User is deleting an existing group. */
    public static final String    DELETE_GROUP = HandlerInfo.getHandlerInfo( GroupActionBean.class, "delete" ).getRequestContext();

    /** User is editing an existing group. */
    public static final String    EDIT_GROUP = HandlerInfo.getHandlerInfo( GroupActionBean.class, "save" ).getRequestContext();

    /** User is viewing an existing group */
    public static final String    VIEW_GROUP = HandlerInfo.getHandlerInfo( GroupActionBean.class, "view" ).getRequestContext();

    /** User is editing preferences */
    public static final String    PREFS    = HandlerInfo.getHandlerInfo( UserPreferencesActionBean.class, "createAssertedName" ).getRequestContext();

    /** User is renaming a page. */
    public static final String    RENAME   = HandlerInfo.getHandlerInfo( RenameActionBean.class, "rename" ).getRequestContext();

    /** User is deleting a page or an attachment. */
    public static final String    DELETE   = HandlerInfo.getHandlerInfo( DeleteActionBean.class, "delete" ).getRequestContext();

    /** User is downloading an attachment. */
    public static final String    ATTACH   = HandlerInfo.getHandlerInfo( AttachActionBean.class, "upload" ).getRequestContext();

    /** RSS feed is being generated. */
    public static final String    RSS      = HandlerInfo.getHandlerInfo( RSSActionBean.class, "rss" ).getRequestContext();

    /** This is not a JSPWiki context, use it to access static files. */
    public static final String    NONE     = "none";  

    /** Same as NONE; this is just a clarification. */
    public static final String    OTHER    = "other";

    /** User is doing administrative things. */
    public static final String    ADMIN    = "admin";
 
    /**
     *  Sets a reference to the real page whose content is currently being
     *  rendered.
     *  <p>
     *  Sometimes you may want to render the page using some other page's context.
     *  In those cases, it is highly recommended that you set the setRealPage()
     *  to point at the real page you are rendering.  Please see InsertPageTag
     *  for an example.
     *  <p>
     *  Also, if your plugin e.g. does some variable setting, be aware that if it
     *  is embedded in the LeftMenu or some other page added with InsertPageTag,
     *  you should consider what you want to do - do you wish to really reference
     *  the "master" page or the included page.
     *
     *  @param page  The real page which is being rendered.
     *  @return The previous real page
     *  @since 2.3.14
     *  @see com.ecyrd.jspwiki.tags.InsertPageTag
     */
    public WikiPage setRealPage( WikiPage page );

    /**
     *  Gets a reference to the real page whose content is currently being rendered.
     *  If your plugin e.g. does some variable setting, be aware that if it
     *  is embedded in the LeftMenu or some other page added with InsertPageTag,
     *  you should consider what you want to do - do you wish to really reference
     *  the "master" page or the included page.
     *  <p>
     *  For example, in the default template, there is a page called "LeftMenu".
     *  Whenever you access a page, e.g. "Main", the master page will be Main, and
     *  that's what the getPage() will return - regardless of whether your plugin
     *  resides on the LeftMenu or on the Main page.  However, getRealPage()
     *  will return "LeftMenu".
     *
     *  @return A reference to the real page.
     *  @see com.ecyrd.jspwiki.tags.InsertPageTag
     *  @see com.ecyrd.jspwiki.parser.JSPWikiMarkupParser
     */
    public WikiPage getRealPage();

    /**
     *  Returns the handling engine.
     *
     *  @return The wikiengine owning this context.
     */
    public WikiEngine getEngine();

    /**
     *  Returns the page that is being handled.
     *
     *  @return the page which was fetched.
     */
    public WikiPage getPage();

    /**
     *  Sets the page that is being handled.
     *
     *  @param page The wikipage
     *  @since 2.1.37.
     */
    public void setPage( WikiPage page );

    /**
     *  Returns the request context.
     *  @return The name of the request context (e.g. VIEW).
     */
    public String getRequestContext();

    /**
     *  Sets the request context.  See above for the different
     *  request contexts (VIEW, EDIT, etc.)
     *
     *  @param arg The request context (one of the predefined contexts.)
     */
    public void setRequestContext( String arg );

    /**
     *  Gets a previously set variable.
     *
     *  @param key The variable name.
     *  @return The variable contents.
     */
    public Object getVariable( String key );

    /**
     *  Sets a variable.  The variable is valid while the WikiContext is valid,
     *  i.e. while page processing continues.  The variable data is discarded
     *  once the page processing is finished.
     *
     *  @param key The variable name.
     *  @param data The variable value.
     */
    public void setVariable( String key, Object data );

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
    public String getHttpParameter( String paramName );

    /**
     *  If the request did originate from a HTTP request,
     *  then the HTTP request can be fetched here.  However, it the request
     *  did NOT originate from a HTTP request, then this method will
     *  return null, and YOU SHOULD CHECK FOR IT!
     *
     *  @return Null, if no HTTP request was done.
     *  @since 2.0.13.
     */
    public HttpServletRequest getHttpRequest();

    /**
     *  Sets the template to be used for this request.
     *
     *  @param dir The template name
     *  @since 2.1.15.
     */
    public void setTemplate( String dir );

    /**
     *  Gets the template that is to be used throughout this request.
     *  @since 2.1.15.
     *  @return template name
     */
    public String getTemplate();

    /**
     *  Convenience method that gets the current user. Delegates the
     *  lookup to the WikiSession associated with this WikiContect.
     *  May return null, in case the current
     *  user has not yet been determined; or this is an internal system.
     *  If the WikiSession has not been set, <em>always</em> returns null.
     *
     *  @return The current user; or maybe null in case of internal calls.
     */
    public Principal getCurrentUser();

    /**
     *  A shortcut to generate a VIEW url.
     *
     *  @param page The page to which to link.
     *  @return An URL to the page.  This honours the current absolute/relative setting.
     */
    public String getViewURL( String page );

    /**
     *  Creates an URL for the given request context.
     *
     *  @param context e.g. WikiContext.EDIT
     *  @param page The page to which to link
     *  @return An URL to the page, honours the absolute/relative setting in jspwiki.properties
     */
    public String getURL( String context,
                          String page );

    /**
     *  Returns an URL from a page. It this WikiContext instance was constructed
     *  with an actual HttpServletRequest, we will attempt to construct the
     *  URL using HttpUtil, which preserves the HTTPS portion if it was used.
     *
     *  @param context The request context (e.g. WikiContext.UPLOAD)
     *  @param page    The page to which to link
     *  @param params  A list of parameters, separated with "&amp;"
     *
     *  @return An URL to the given context and page.
     */
    public String getURL( String context,
                          String page,
                          String params );
    
    /**
     *  Returns a shallow clone of the WikiContext.
     *
     *  @since 2.1.37.
     *  @return A shallow clone of the WikiContext
     */
    public Object clone();

    /**
     *  Creates a deep clone of the WikiContext.  This is useful when you want
     *  to be sure that you don't accidentally mess with page attributes, etc.
     *  
     *  @since  2.8.0
     *  @return A deep clone of the WikiContext.
     */
    public WikiContext deepClone();
    
    /**
     *  Returns the WikiSession associated with the context.
     *  This method is guaranteed to always return a valid WikiSession.
     *  If this context was constructed without an associated
     *  HttpServletRequest, it will return {@link com.ecyrd.jspwiki.WikiSession#guestSession(com.ecyrd.jspwiki.WikiEngine)}.
     *
     *  @return the WikiSession associate with this context.
     */
    public WikiSession getWikiSession();

    /**
     *  Returns true, if the current user has administrative permissions (i.e. the omnipotent
     *  AllPermission).
     *
     *  @since 2.4.46
     *  @return true, if the user has all permissions.
     */
    public boolean hasAdminPermissions();

    /**
     *  Locates the i18n ResourceBundle given.  This method interprets
     *  the request locale, and uses that to figure out which language the
     *  user wants.
     *  @see com.ecyrd.jspwiki.i18n.InternationalizationManager
     *  @param bundle The name of the bundle you are looking for.
     *  @return A resource bundle object
     *  @throws MissingResourceException If the bundle cannot be found
     */
    // FIXME: This method should really cache the ResourceBundles or something...
    public ResourceBundle getBundle( String bundle ) throws MissingResourceException;

}
