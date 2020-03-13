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

import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.exceptions.FilterException;

import java.lang.reflect.Method;
import java.util.Properties;

import static org.apache.wiki.api.filters.FilterSupportOperations.executePageFilterPhase;
import static org.apache.wiki.api.filters.FilterSupportOperations.methodOfNonPublicAPI;


/**
 *  <p>Provides a definition for a page filter. A page filter is a class that can be used to transform the WikiPage content being saved or
 *  being loaded at any given time.</p>
 *  <p>Note that the Context#getPage() method always returns the context in which text is rendered, i.e. the original request. Thus the
 *  content may actually be different content than what what the Context#getPage() implies! This happens often if you are for example
 *  including multiple pages on the same page.</p>
 *  <p>PageFilters must be thread-safe! There is only one instance of each PageFilter per each Engine invocation. If you need to store data
 *  persistently, use VariableManager, or WikiContext.</p>
 *  <p><strong>Design notes</strong></p>
 *  <p>As of 2.5.30, initialize() gains access to the Engine.</p>
 *  <p>As of 2.11.0.M7, almost all methods from BasicPageFilter end up here as default methods.</p>
 *  <p>In order to preserve backwards compatibility with filters not using the public API, these default methods checks if a given filter
 *  is using the old, non public API and, if that's the case attempt to execute the old, non public api corresponding method. If the filter
 *  uses the public API, then the default callback is used. None of the default callbacks do anything, so it is a good idea for you to
 *  implement only methods that you need.</p>
 */
public interface PageFilter {

    /**
     *  Is called whenever the a new PageFilter is instantiated and reset.
     *  
     *  @param engine The Engine which owns this PageFilter
     *  @param properties The properties ripped from filters.xml.
     *  @throws FilterException If the filter could not be initialized. If this is thrown, the filter is not added to the internal queues.
     */
    void initialize( final Engine engine, final Properties properties ) throws FilterException;

    /**
     *  This method is called whenever a page has been loaded from the provider, but not yet been sent through the markup-translation
     *  process.  Note that you cannot do HTML translation here, because it will be escaped.
     *
     *  @param context The current context.
     *  @param content WikiMarkup.
     *  @return The modified wikimarkup content. Default implementation returns the markup as received.
     *  @throws FilterException If something goes wrong.  Throwing this causes the entire page processing to be abandoned.
     */
    default String preTranslate( final Context context, final String content ) throws FilterException {
        final Method m = methodOfNonPublicAPI( this, "preTranslate", "org.apache.wiki.WikiContext", "java.lang.String" );
        return executePageFilterPhase( () -> content, m, this, context, content );
        // return content;
    }

    /**
     *  This method is called after a page has been fed through the translation process, so anything you are seeing here is translated
     *  content.  If you want to do any of your own WikiMarkup2HTML translation, do it here.
     *  
     *  @param context The WikiContext.
     *  @param htmlContent The translated HTML.
     *  @return The modified HTML. Default implementation returns the translated html as received.
     *  @throws FilterException If something goes wrong.  Throwing this causes the entire page processing to be abandoned.
     */
    default String postTranslate( final Context context, final String htmlContent ) throws FilterException {
        final Method m = methodOfNonPublicAPI( this, "postTranslate", "org.apache.wiki.WikiContext", "java.lang.String" );
        return executePageFilterPhase( () -> htmlContent, m, this, context, htmlContent );
        // return htmlContent;
    }

    /**
     *  This method is called before the page has been saved to the PageProvider.
     *  
     *  @param context The WikiContext
     *  @param content The wikimarkup that the user just wanted to save.
     *  @return The modified wikimarkup. Default implementation returns the markup as received.
     *  @throws FilterException If something goes wrong.  Throwing this causes the entire page processing to be abandoned.
     */
    default String preSave( final Context context, final String content ) throws FilterException {
        final Method m = methodOfNonPublicAPI( this, "preSave", "org.apache.wiki.WikiContext", "java.lang.String" );
        return executePageFilterPhase( () -> content, m, this, context, content );
        // return content;
    }

    /**
     *  This method is called after the page has been successfully saved. If the saving fails for any reason, then this method will not
     *  be called.
     *  <p>
     *  Since the result is discarded from this method, this is only useful for things like counters, etc.
     *  
     *  @param context The WikiContext
     *  @param content The content which was just stored.
     *  @throws FilterException If something goes wrong.  As the page is already saved, This is just logged.
     */
    default void postSave( final Context context, final String content ) throws FilterException {
        final Method m = methodOfNonPublicAPI( this, "postSave", "org.apache.wiki.WikiContext", "java.lang.String" );
        executePageFilterPhase( () -> null, m, this, content );
        // empty method
    }

    /**
     *  Called for every filter, e.g. on wiki engine shutdown. Use this if you have to 
     *  clean up or close global resources you allocated in the initialize() method.
     * 
     *  @param engine The Engine which owns this filter.
     *  @since 2.5.36
     */
    default void destroy( final Engine engine ) {
        final Method m = methodOfNonPublicAPI( this, "destroy", "org.apache.wiki.WikiEngine" );
        executePageFilterPhase( () -> null, m, this, engine );
        // empty method
    }

}
