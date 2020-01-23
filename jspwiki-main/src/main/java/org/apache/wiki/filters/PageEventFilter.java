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

package org.apache.wiki.filters;

import org.apache.wiki.api.filters.BasicPageFilter;
import org.apache.wiki.api.exceptions.FilterException;

import org.apache.wiki.WikiContext;
import org.apache.wiki.event.WikiEventListener;
import org.apache.wiki.event.WikiEventManager;
import org.apache.wiki.event.WikiPageEvent;

import java.util.Properties;

/**
  * Fires WikiPageEvents for page events.
  * <p>
  * Adding a PageEventFilter to the FilterManager will automatically attach an event delegate with the WikiEventManager to provide for
  * firing and listener management. All that remains is then adding the listener to the filter via the WikiEventManager. This is quite
  * simple:
  * </p>
  * <pre>
  *    PageEventFilter filter = new PageEventFilter();
  *    engine.getFilterManager().addPageFilter(filter,5000);
  *    // attach listener to filter
  *    WikiEventManager.addWikiEventListener(filter,listener);
  * </pre>
  * <p>
  * This class provides convenience methods for adding and removing WikiEventListeners.
  * </p>
  *
  * @see org.apache.wiki.event.WikiEventManager
  */
public class PageEventFilter extends BasicPageFilter {

    /**
     * Called whenever a new PageFilter is instantiated and reset.
     */
    public void initialize( final Properties properties ) throws FilterException {
        //
    }

    /**
     * This method is called whenever a page has been loaded from the provider, but not yet been sent through the TranslatorReader.
     * Note that you cannot do HTML translation here, because TranslatorReader is likely to escape it.
     *
     * @param wikiContext The current wikicontext.
     * @param content WikiMarkup.
     */
    public String preTranslate( final WikiContext wikiContext, final String content ) {
        fireEvent( WikiPageEvent.PRE_TRANSLATE, wikiContext );
        return content;
    }


    /**
     * This method is called after a page has been fed through the TranslatorReader, so anything you are seeing here is translated content.
     * If you want to do any of your own WikiMarkup2HTML translation, do it here.
     */
    public String postTranslate( final WikiContext wikiContext, final String htmlContent ) {
        fireEvent( WikiPageEvent.POST_TRANSLATE, wikiContext );
        return htmlContent;
    }


    /**
      * This method is called before the page has been saved to the PageProvider.
      */
    public String preSave( final WikiContext wikiContext, final String content ) {
        fireEvent( WikiPageEvent.PRE_SAVE, wikiContext );
        return content;
    }


    /**
      * This method is called after the page has been successfully saved. If the saving fails for any reason, then this method will not
      * be called.
      * <p>
      * Since the result is discarded from this method, this is only useful for things like counters, etc.
      */
    public void postSave( final WikiContext wikiContext, final String content ) {
        fireEvent( WikiPageEvent.POST_SAVE, wikiContext );
    }


    // events processing .......................................................

    /**
     *  Registers a WikiEventListener with this instance. This is a convenience method.
     *
     * @param listener the event listener
     */
    public final synchronized void addWikiEventListener( final WikiEventListener listener ) {
        WikiEventManager.addWikiEventListener( this, listener );
    }

    /**
     *  Un-registers a WikiEventListener with this instance. This is a convenience method.
     *
     * @param listener the event listener
     */
    public final synchronized void removeWikiEventListener( final WikiEventListener listener ) {
        WikiEventManager.removeWikiEventListener( this, listener );
    }

    /**
     *  Fires a WikiPageEvent of the provided type and page name to all registered listeners. Only <tt>PAGE_LOCK</tt> and
     *  <tt>PAGE_UNLOCK</tt> event types will fire an event; other event types are ignored.
     *
     * @see org.apache.wiki.event.WikiPageEvent
     * @param type      the WikiPageEvent type to be fired.
     * @param context   the WikiContext of the event.
     */
    protected final void fireEvent( final int type, final WikiContext context ) {
        if( WikiEventManager.isListening(this ) && WikiPageEvent.isValidType( type ) ) {
            final WikiPageEvent event = new WikiPageEvent( context.getEngine(), type, context.getPage().getName() );
            WikiEventManager.fireEvent(this, event );
        }
    }

}
