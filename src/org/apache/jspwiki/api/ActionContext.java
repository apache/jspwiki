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
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.servlet.http.HttpServletRequest;

/**
 *  The ActionContext represents a request lifecycle.  It is valid throughout
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
public interface ActionContext
{
    
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
