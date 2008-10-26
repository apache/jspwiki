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

import java.io.IOException;
import java.security.Permission;
import java.security.Principal;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *  The AbstractContext represents a request lifecycle.  It is valid throughout
 *  the request, and gives access to innards of JSPWiki.
 *  <p>
 *  This mostly replaces the old WikiContext class.  The reason is that WikiContext
 *  always needs a WikiPage, but since 2.4 we've had quite a few non-page contexts
 *  like group editing and so on.  For these pages, it is unnecessary to carry
 *  the WikiPage all around.
 *  <p>
 *  WikiContext still stays the main interface for third party developers (e.g. plugins
 *  or filters).
 */
// FIXME: This might be the wrong name.
public interface AbstractContext
{
    
    /**
     *  Returns the name of the current template which is in use.
     *  
     *  @return The template name
     */
    // FIXME: Would it be better to return an object (e.g. WikiTemplate?)
    public String getContentTemplate();
    
    /**
     *  Returns the name of the top-level JSP file that this one refers to
     *  @return The JSP file name
     */
    // FIXME: Shouldn't this be really just a pointer to the bean?
    public String getJSP();
    
    /**
     *  Returns the handling engine.
     *
     *  @return The wikiengine owning this context.
     */
    public WikiEngine getEngine();
    /**
     *  Returns the request context.
     *  @return The name of the request context (e.g. VIEW).
     */
    public String getRequestContext();

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
     * Returns the target of this wiki context: a page, group name or JSP. If
     * the associated Command is a PageCommand, this method returns the page's
     * name. Otherwise, this method delegates to the associated Command's
     * {@link com.ecyrd.jspwiki.ui.Command#getName()} method. Calling classes
     * can rely on the results of this method for looking up canonically-correct
     * page or group names. Because it does not automatically assume that the
     * wiki context is a PageCommand, calling this method is inherently safer
     * than calling <code>getPage().getName()</code>.
     * @return the name of the target of this wiki context
     * @see com.ecyrd.jspwiki.ui.PageCommand#getName()
     * @see com.ecyrd.jspwiki.ui.GroupCommand#getName()
     */
    public String getName();

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
    // FIXME: Better to create a new URL creation class, which is WikiContext-specific?
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
     *  Returns the WikiSession associated with the context.
     *  This method is guaranteed to always return a valid WikiSession.
     *  If this context was constructed without an associated
     *  HttpServletRequest, it will return {@link WikiSession#guestSession(WikiEngine)}.
     *
     *  @return The WikiSession associate with this context.
     */
    public WikiSession getWikiSession();

    /**
     * Returns the permission required to successfully execute this context.
     * For example, the a wiki context of VIEW for a certain page means that
     * the PagePermission "view" is required for the page. In some cases, no
     * particular permission is required, in which case a dummy permission will
     * be returned ({@link java.util.PropertyPermission}<code> "os.name",
     * "read"</code>). This method is guaranteed to always return a valid,
     * non-null permission.
     * @return the permission
     * @since 2.4
     */
    public Permission requiredPermission();


    /**
     * Checks whether the current user has access to this wiki context,
     * by obtaining the required Permission ({@link #requiredPermission()})
     * and delegating the access check to
     * {@link com.ecyrd.jspwiki.auth.AuthorizationManager#checkPermission(WikiSession, Permission)}.
     * If the user is allowed, this method returns <code>true</code>;
     * <code>false</code> otherwise. If access is allowed,
     * the wiki context will be added to the request as an attribute
     * with the key name {@link com.ecyrd.jspwiki.tags.WikiTagBase#ATTR_CONTEXT}.
     * Note that this method will automatically redirect the user to
     * a login or error page, as appropriate, if access fails. This is
     * NOT guaranteed to be default behavior in the future.
     * @param response the http response
     * @return the result of the access check
     * @throws IOException In case something goes wrong
     */
    // FIXME: Is this the correct place really for this?
    public boolean hasAccess( HttpServletResponse response ) throws IOException;

    /**
     * Checks whether the current user has access to this wiki context (and
     * optionally redirects if not), by obtaining the required Permission ({@link #requiredPermission()})
     * and delegating the access check to
     * {@link com.ecyrd.jspwiki.auth.AuthorizationManager#checkPermission(WikiSession, Permission)}.
     * If the user is allowed, this method returns <code>true</code>;
     * <code>false</code> otherwise. If access is allowed,
     * the wiki context will be added to the request as attribute
     * with the key name {@link com.ecyrd.jspwiki.tags.WikiTagBase#ATTR_CONTEXT}.
     * @return the result of the access check
     * @param response The servlet response object
     * @param redirect If true, makes an automatic redirect to the response
     * @throws IOException If something goes wrong
     */
    // FIXME: Is this the correct place really for this?
    public boolean hasAccess( HttpServletResponse response, boolean redirect ) throws IOException;

    /**
     *  Locates the i18n ResourceBundle given.  This method interprets
     *  the request locale, and uses that to figure out which language the
     *  user wants.
     *  @see com.ecyrd.jspwiki.i18n.InternationalizationManager
     *  @param bundle The name of the bundle you are looking for.
     *  @return A resource bundle object
     *  @throws MissingResourceException If the bundle cannot be found
     */
    public ResourceBundle getBundle( String bundle ) throws MissingResourceException;

    /**
     *  Returns the locale of the HTTP request if available,
     *  otherwise returns the default Locale of the server.
     *
     *  @return A valid locale object
     *  @param context The WikiContext
     */
    public Locale getLocale();

}
