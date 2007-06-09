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
package com.ecyrd.jspwiki.filters;

import java.util.Properties;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;

/**
 *  Provides a definition for a page filter.  A page filter is a class
 *  that can be used to transform the WikiPage content being saved or
 *  being loaded at any given time.
 *  <p>
 *  Note that the WikiContext.getPage() method always returns the context
 *  in which text is rendered, i.e. the original request.  Thus the content
 *  may actually be different content than what what the wikiContext.getPage()
 *  implies!  This happens often if you are for example including multiple
 *  pages on the same page.
 *  <p>
 *  PageFilters must be thread-safe!  There is only one instance of each PageFilter 
 *  per each WikiEngine invocation.  If you need to store data persistently, use
 *  VariableManager, or WikiContext.
 *  <p>
 *  As of 2.5.30, initialize() gains accesso to the WikiEngine.
 *
 *  @author Janne Jalkanen
 */
public interface PageFilter
{
    /**
     *  Is called whenever the a new PageFilter is instantiated and
     *  reset.
     */
    public void initialize( WikiEngine engine, Properties properties )
        throws FilterException;

    /**
     *  This method is called whenever a page has been loaded from the provider,
     *  but not yet been sent through the TranslatorReader.  Note that you cannot
     *  do HTML translation here, because TranslatorReader is likely to escape it.
     *
     *  @param wikiContext The current wikicontext.
     *  @param content     WikiMarkup.
     */
    public String preTranslate( WikiContext wikiContext, String content )
        throws FilterException;

    /**
     *  This method is called after a page has been fed through the TranslatorReader,
     *  so anything you are seeing here is translated content.  If you want to
     *  do any of your own WikiMarkup2HTML translation, do it here.
     */
    public String postTranslate( WikiContext wikiContext, String htmlContent )
        throws FilterException;

    /**
     *  This method is called before the page has been saved to the PageProvider.
     */
    public String preSave( WikiContext wikiContext, String content )
        throws FilterException;

    /**
     *  This method is called after the page has been successfully saved.
     *  If the saving fails for any reason, then this method will not
     *  be called.
     *  <p>
     *  Since the result is discarded from this method, this is only useful
     *  for things like counters, etc.
     */
    public void postSave( WikiContext wikiContext, String content )
        throws FilterException;

    /**
     * Called for every filter, e.g. on wiki eingine shutdown. Use this if you have to 
     * clean up or close global resources you allocated in the initialize() method.
     * 
     * @since 2.5.36
     */
    public void destroy( WikiEngine engine );


}