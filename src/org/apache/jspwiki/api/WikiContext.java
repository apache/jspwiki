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


/**
 *  The WikiContext represents a request which targets a WikiPage
 *  in some way or the other.
 *  <p>
 *  This class would be better named as "PageRequestContext" or something,
 *  but we need to keep at least some similarity with the 2.x API...
 */
public interface WikiContext extends AbstractContext
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
     *  Figure out to which page we are really going to.  Considers
     *  special page names from the jspwiki.properties, and possible aliases.
     *  This method forwards requests to
     *  {@link com.ecyrd.jspwiki.ui.CommandResolver#getSpecialPageReference(String)}.
     *  @return A complete URL to the new page to redirect to
     *  @since 2.2
     */

    // FIXME: Probably not needed for 3.0?
    public String getRedirectURL();


    /**
     *  Returns the page that is being handled.
     *
     *  @return the page which was fetched.
     */
    public WikiPage getPage();


}
