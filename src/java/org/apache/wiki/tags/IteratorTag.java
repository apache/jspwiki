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
package org.apache.wiki.tags;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyTagSupport;
import javax.servlet.jsp.tagext.TryCatchFinally;

import org.apache.wiki.WikiContext;
import org.apache.wiki.action.WikiContextFactory;
import org.apache.wiki.api.WikiPage;
import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;

/**
 * <p>
 * Abstract base class for tags that iterate through collections.
 * </p>
 * <P>
 * <B>Attributes</B>
 * </P>
 * <UL>
 * <LI>list - a collection.
 * </UL>
 * 
 * @since 2.0
 */
public abstract class IteratorTag<T> extends BodyTagSupport implements TryCatchFinally
{
    private static Logger log = LoggerFactory.getLogger( IteratorTag.class );

    private Collection<T> m_items = null;

    /**
     * The WikiContext passed to this tag when initialized. It is stashed by
     * {@link #doStartTag()} and restored by {@link #doFinally()}.
     */
    private WikiContext m_originalContext = null;

    private   int         m_maxItems = Integer.MAX_VALUE;
    
    private   int         m_count = 0;

    private   int         m_start = 0;

    /**
     * Protected Iterator field associated by the collection set by
     * {@link #setList(Collection)} or {@link #setList(Object[])}.
     * <em>This variable should not be reassigned by subclasses.</em>
     */
    private Iterator<T> m_iterator = null;

    /**
     * Protected field that stores the current WikiContext.
     * <em>This variable should not be reassigned by subclasses.</em>
     */
    protected WikiContext m_wikiContext = null;
    
    /**
     * {@inheritDoc}
     */
    public final int doAfterBody()
    {
        if( bodyContent != null )
        {
            try
            {
                JspWriter out = getPreviousOut();
                out.print( bodyContent.getString() );
                bodyContent.clearBody();
            }
            catch( IOException e )
            {
                log.error( "Unable to get inner tag text", e );
                // FIXME: throw something?
            }
        }

        if( m_iterator.hasNext() && m_count++ < m_maxItems )
        {
            T item = m_iterator.next();
            pageContext.setAttribute( getId(), item );
            nextItem( item );
            return EVAL_BODY_BUFFERED;
        }
        return SKIP_BODY;
    }
    
    /**
     * <p>
     * Handles any exceptions thrown by the various body tag methods. Subclasses
     * can override this method if they require specialized handling. By
     * default, this method does nothing.
     * </p>
     * 
     * @param t The Throwable that the tag threw
     * @throws Throwable I have no idea why this would throw anything
     */
    public final void doCatch( Throwable t ) throws Throwable
    {
    }

    /**
     * {@inheritDoc}
     */
    public final int doEndTag()
    {
        return EVAL_PAGE;
    }
    
    /**
     * <p>
     * Cleans up tag state after the tag finishes executing. Subclasses can
     * override this method to include any cleanup code they need. However,
     * subclasses <em>must</em> call this superclass method in order for the
     * tag to work as expected. The default implementation does nothing.
     */
    public final void doFinally()
    {
        if ( !m_iterator.hasNext() )
        {
            // Return WikiContext back to the original.
            WikiContextFactory.saveContext( pageContext.getRequest(), m_originalContext );
            
            // Null out everything else
            m_maxItems = Integer.MAX_VALUE;
            m_start = 0;
            m_count = 0;
            m_items = null;
            m_iterator = null;
            m_wikiContext = null;
            m_originalContext = null;
        }
    }

    /**
     * <p>
     * Initialization method used by IteratorTag and its subclasses to
     * initialize tag state, before the first body tag evaluation. Subclasses
     * can override this method to do just about anything. This implementation
     * does the following:
     * </p>
     * <ul>
     * <li>sets the internal field {@link #m_wikiContext} to the value returned
     * by {@link WikiContextFactory#findContext(javax.servlet.jsp.PageContext)}</li>
     * <li>if {@link #setList(Collection)} or {@link #setList(Object[])} has
     * not been called, sets the internally cached list of items to an empty
     * collection of type <var>&lt;T&gt;</var></li>
     * <li>calls {@link #resetIterator()}, which by default sets the internal
     * field {@link #m_iterator} to the <code>m_item</code>'s Iterator</li>
     * </ul>
     * <p>
     * Subclasses should generally call this method via
     * <code>super.doStartTag()</code>, after executing their own
     * initialization code, to ensure that the iterator tag's internal state
     * initializes correctly. For subclasses that need to "pre-load" a default
     * collection of items, all that is needed is to have the overridden method
     * call {@link #setList(Collection) or {@link #setList(Object[])}, then
     * call <code>super.doInitBody()</code>.
     * </p>
     * <p>
     * Implementations should return
     * {@link javax.servlet.jsp.tagext.Tag#EVAL_BODY_INCLUDE},
     * {@link javax.servlet.jsp.tagext.Tag#SKIP_BODY} or
     * {@link javax.servlet.jsp.tagext.BodyTag#EVAL_BODY_BUFFERED}. Any
     * exceptions that are thrown can then be dealt with by
     * {@link #doCatch(Throwable)} or {@link #doFinally()}.
     * </p>
     * The default implementation simply returns
     * {@link javax.servlet.jsp.tagext.BodyTag#EVAL_BODY_BUFFERED} if
     * <code>m_items</code> contains one or more values, or
     * {@link javax.servlet.jsp.tagext.Tag#SKIP_BODY} if not.
     * 
     * @see javax.servlet.jsp.tagext.Tag#doStartTag()}
     *      </p>
     */
    public final int doStartTag()
    {
        // If first time through, stash the original WikiContext
        m_wikiContext = WikiContextFactory.findContext( pageContext );
        if ( m_originalContext == null )
        {
            m_originalContext = m_wikiContext;
            m_wikiContext = (WikiContext)m_wikiContext.clone();
            WikiContextFactory.saveContext( pageContext.getRequest(), m_wikiContext );
        }
        
        // Initialize the items for the iterator
        if ( m_items == null )
        {
            setList( initItems() );
        }
        resetIterator();
        if( m_items == null || m_items.size() == 0 )
        {
            return SKIP_BODY;
        }

        //  Skip the first few ones...
        int skip = 0;
        while( m_iterator.hasNext() && (skip++ < m_start) ) m_iterator.next();
        
        // Start with the first one after the skipped number
        m_count = 0;
        if( m_iterator.hasNext() && m_count++ < m_maxItems )
        {
            T item = m_iterator.next();
            pageContext.setAttribute( getId(), item );
            nextItem( item );
        }

        return EVAL_BODY_BUFFERED;
    }

    /**
     * {@inheritDoc}
     * calls
     * {@link #resetIterator(), nulls out the internal iterator and wiki context
     * references, and restores the WikiContext to its original state.     
     */
    public void release()
    {
        super.release();
    }

    /**
     * Resets the iterator to the first item in the list set by
     * {@link #setList(Collection)} or
     * {@link #setList(Object[]). The default implementation sets the current record
     * to the first one. Override this method to reset your own iterator.
     */
    public final void resetIterator()
    {
//        m_iterator = m_items.iterator();
    }

    /**
     * Sets the collection that is used to form the iteration.
     * 
     * @param items A Collection which will be iterated.
     */
    public final void setList( Collection<T> items )
    {
        if( items != null )
        {
            m_items = items;
            m_iterator = items.iterator();
        }
    }
    
    /**
     * Sets the collection list, but using an array.
     * 
     * @param items An array of objects which will be iterated.
     */
    public final void setList( T[] items )
    {
        if( items != null )
        {
            m_items = Arrays.asList( items );
            m_iterator = m_items.iterator();
        }
    }

    /**
     * Sets the maximum number of items the iterator should iterate over.
     * @param maxItems the maximum number
     */
    public void setMaxItems( int maxItems )
    {
        m_maxItems = maxItems;
    }

    /**
     * Sets the start position for the iterator, starting with position 0.
     * @param start the start position
     */
    public void setStart( int start )
    {
        m_start = start;
    }

    /**
     * Initializes the list of items that will be iterated over. This default implementation
     * returns an empty collection. Subclasses can override this method to provide
     * a list of default items to iterate over.
     * @return the collection of items
     */
    protected Collection<T> initItems()
    {
        return new ArrayList<T>();
    }

    /**
     * <p>Processes the next item in the iterator. This method is called by {@link #doStartTag()}
     * if the tag iterates over one or more items. It is also called by {@link #doAfterBody()}
     * following each iteration. Subclasses should override this class to perform custom
     * actions on the item.</p>
     * <p>The default implementation of this method checks to see if the item
     * is a WikiPage, and if so, calls {@link WikiContext#setPage(WikiPage)} with it
     * for the current WikiContext.</p>
     * @param item the next item in the iteration to be processed
     */
    protected void nextItem( T item )
    {
        if( item instanceof WikiPage )
        {
            m_wikiContext.setPage( (WikiPage) item );
        }
    }

}
