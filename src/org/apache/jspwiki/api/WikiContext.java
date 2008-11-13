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

import javax.servlet.http.HttpServletResponse;


/**
 *  The WikiContext represents a request which targets a WikiPage
 *  in some way or the other.
 *  <p>
 *  This class would be better named as "PageRequestContext" or something,
 *  but we need to keep at least some similarity with the 2.x API...
 */
public interface WikiContext extends ActionContext
{


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
     *  Returns the page that is being handled.
     *
     *  @return the page which was fetched.
     */
    public WikiPage getPage();

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
     *  A shortcut to generate a VIEW url.
     *
     *  @param page The page to which to link.
     *  @return An URL to the page.  This honours the current absolute/relative setting.
     */
    // FIXME: Better to create a new URL creation class, which is WikiContext-specific?
    public String getViewURL( String page );

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

}
