/* 
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
package org.apache.wiki.api.filters;

import java.util.Properties;

import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.api.exceptions.FilterException;

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
 *  As of 2.5.30, initialize() gains access to the WikiEngine.
 *
 */
public interface PageFilter
{
    /**
     *  Is called whenever the a new PageFilter is instantiated and
     *  reset.
     *  
     *  @param engine The WikiEngine whic owns this PageFilter
     *  @param properties The properties ripped from filters.xml.
     *  @throws FilterException If the filter could not be initialized. If this is thrown,
     *                          the filter is not added to the internal queues.
     */
    void initialize( WikiEngine engine, Properties properties )
        throws FilterException;

    /**
     *  This method is called whenever a page has been loaded from the provider,
     *  but not yet been sent through the markup-translation process.  Note that you cannot
     *  do HTML translation here, because it will be escaped.
     *
     *  @param wikiContext The current wikicontext.
     *  @param content     WikiMarkup.
     *  @return The modified wikimarkup content.
     *  @throws FilterException If something goes wrong.  Throwing this causes the entire page
     *                          processing to be abandoned.
     */
    String preTranslate( WikiContext wikiContext, String content )
        throws FilterException;

    /**
     *  This method is called after a page has been fed through the translation process,
     *  so anything you are seeing here is translated content.  If you want to
     *  do any of your own WikiMarkup2HTML translation, do it here.
     *  
     *  @param wikiContext The WikiContext.
     *  @param htmlContent The translated HTML
     *  @return The modified HTML
     *  
     *  @throws FilterException If something goes wrong.  Throwing this causes the entire page
     *                          processing to be abandoned.
     */
    String postTranslate( WikiContext wikiContext, String htmlContent )
        throws FilterException;

    /**
     *  This method is called before the page has been saved to the PageProvider.
     *  
     *  @param wikiContext The WikiContext
     *  @param content The wikimarkup that the user just wanted to save.
     *  @return The modified wikimarkup
     *  @throws FilterException If something goes wrong.  Throwing this causes the entire page
     *                          processing to be abandoned.
     */
    String preSave( WikiContext wikiContext, String content )
        throws FilterException;

    /**
     *  This method is called after the page has been successfully saved.
     *  If the saving fails for any reason, then this method will not
     *  be called.
     *  <p>
     *  Since the result is discarded from this method, this is only useful
     *  for things like counters, etc.
     *  
     *  @param wikiContext The WikiContext
     *  @param content The content which was just stored.
     *  @throws FilterException If something goes wrong.  As the page is already saved,
     *                          This is just logged.
     */
    void postSave( WikiContext wikiContext, String content )
        throws FilterException;

    /**
     *  Called for every filter, e.g. on wiki engine shutdown. Use this if you have to 
     *  clean up or close global resources you allocated in the initialize() method.
     * 
     *  @param engine The WikiEngine which owns this filter.
     *  @since 2.5.36
     */
    void destroy( WikiEngine engine );

}
