/*
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2006 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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

package com.ecyrd.jspwiki.event;

import com.ecyrd.jspwiki.filters.BasicPageFilter;
import com.ecyrd.jspwiki.filters.FilterException;

import com.ecyrd.jspwiki.WikiContext;
import java.util.Properties;

/**
  * Fires WikiPageEvents for page events.
  * <p>
  * Adding a PageEventFilter to the FilterManager will automatically
  * attach an event delegate with the WikiEventManager to provide for
  * firing and listener management. All that remains is then adding
  * the listener to the filter via the WikiEventManager. This is quite
  * simple:
  * </p>
  * <pre>
  *    PageEventFilter filter = new PageEventFilter();
  *    engine.getFilterManager().addPageFilter(filter,5000);
  *    // attach listener to filter
  *    WikiEventManager.addWikiEventListener(filter,listener);
  * </pre>
  * <p>
  * This class provides convenience methods for adding and removing
  * WikiEventListeners.
  * </p>
  *
  * @see com.ecyrd.jspwiki.event.WikiEventManager
  * @author Murray Altheim
  */
public class PageEventFilter extends BasicPageFilter
{

    /**
      * Called whenever a new PageFilter is instantiated and reset.
      */
    public void initialize( Properties properties )
            throws FilterException
    {
        //
    }

    /**
      * This method is called whenever a page has been loaded from the provider,
      * but not yet been sent through the TranslatorReader.  Note that you cannot
      * do HTML translation here, because TranslatorReader is likely to escape it.
      *
      * @param wikiContext The current wikicontext.
      * @param content     WikiMarkup.
      */
    public String preTranslate( WikiContext wikiContext, String content )
            throws FilterException
    {
        fireEvent( WikiPageEvent.PRE_TRANSLATE, wikiContext );
        return content;
    }


    /**
      * This method is called after a page has been fed through the TranslatorReader,
      * so anything you are seeing here is translated content.  If you want to
      * do any of your own WikiMarkup2HTML translation, do it here.
      */
    public String postTranslate( WikiContext wikiContext, String htmlContent )
            throws FilterException
    {
        fireEvent( WikiPageEvent.POST_TRANSLATE, wikiContext );
        return htmlContent;
    }


    /**
      * This method is called before the page has been saved to the PageProvider.
      */
    public String preSave( WikiContext wikiContext, String content )
            throws FilterException
    {
        fireEvent( WikiPageEvent.PRE_SAVE, wikiContext );
        return content;
    }


    /**
      * This method is called after the page has been successfully saved.
      * If the saving fails for any reason, then this method will not
      * be called.
      * <p>
      * Since the result is discarded from this method, this is only useful
      * for things like counters, etc.
      */
    public void postSave( WikiContext wikiContext, String content )
            throws FilterException
    {
        fireEvent( WikiPageEvent.POST_SAVE, wikiContext );
    }


    // events processing .......................................................


    /**
     *  Registers a WikiEventListener with this instance.
     *  This is a convenience method.
     *
     * @param listener the event listener
     */
    public final synchronized void addWikiEventListener( WikiEventListener listener )
    {
        WikiEventManager.addWikiEventListener( this, listener );
    }

    /**
     *  Un-registers a WikiEventListener with this instance.
     *  This is a convenience method.
     *
     * @param listener the event listener
     */
    public final synchronized void removeWikiEventListener( WikiEventListener listener )
    {
        WikiEventManager.removeWikiEventListener( this, listener );
    }

    /**
     *  Fires a WikiPageEvent of the provided type and page name
     *  to all registered listeners. Only <tt>PAGE_LOCK</tt> and
     *  <tt>PAGE_UNLOCK</tt> event types will fire an event; other
     *  event types are ignored.
     *
     * @see com.ecyrd.jspwiki.event.WikiPageEvent
     * @param type      the WikiPageEvent type to be fired.
     * @param context   the WikiContext of the event.
     */
    protected final void fireEvent( int type, WikiContext context )
    {
        if ( WikiEventManager.isListening(this) && WikiPageEvent.isValidType(type) )
        {
            WikiPageEvent event = new WikiPageEvent(
                    context.getEngine(),
                    type,
                    context.getPage().getName());
            WikiEventManager.fireEvent(this,event);
        }
    }

} // end com.ecyrd.jspwiki.event.PageEventFilter
