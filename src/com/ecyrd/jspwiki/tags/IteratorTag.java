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
package com.ecyrd.jspwiki.tags;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Collection;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyTagSupport;
import javax.servlet.jsp.tagext.TryCatchFinally;
import javax.servlet.jsp.PageContext;

import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiPage;

/**
 *  Iterates through tags.
 *
 *  <P><B>Attributes</B></P>
 *  <UL>
 *    <LI>list - a collection.
 *  </UL>
 *
 *  @author Janne Jalkanen
 *  @since 2.0
 */
public abstract class IteratorTag
    extends BodyTagSupport
    implements TryCatchFinally
{

    protected String      m_pageName;
    protected Iterator    m_iterator;
    protected WikiContext m_wikiContext;

    private static Logger log = Logger.getLogger( IteratorTag.class );

    /**
     *  Sets the collection that is used to form the iteration.
     *  
     *  @param arg A Collection which will be iterated.
     */
    public void setList( Collection arg )
    {
        if( arg != null )
            m_iterator = arg.iterator();
    }

    /**
     *  Sets the collection list, but using an array.
     *  @param arg An array of objects which will be iterated.
     */
    public void setList( Object[] arg )
    {
        if( arg != null )
        {
            m_iterator = Arrays.asList(arg).iterator();
        }
    }
    
    /**
     *  Sets the iterator directly that is used to form the iteration.
     */
    /*
    public void setList( Iterator arg )
    {
        m_iterator = arg;
    }
    */

    /**
     *  Clears the iterator away.  After calling this method doStartTag()
     *  will always return SKIP_BODY
     */
    public void clearList()
    {
        m_iterator = null;
    }
    
    /**
     *  Override this method to reset your own iterator.
     */
    public void resetIterator()
    {
        // No operation here
    }
    
    /**
     *  {@inheritDoc}
     */
    public int doStartTag()
    {
        m_wikiContext = WikiContext.findContext(pageContext);
        
        resetIterator();
        
        if( m_iterator == null ) return SKIP_BODY;

        if( m_iterator.hasNext() )
        {
            buildContext();
        }

        return EVAL_BODY_BUFFERED;
    }

    /**
     *  Arg, I hate globals.
     */
    private void buildContext()
    {
        //
        //  Build a clone of the current context
        //
        WikiContext context = (WikiContext)m_wikiContext.clone();
        
        Object o = m_iterator.next();
        
        if( o instanceof WikiPage )
            context.setPage( (WikiPage)o );

        //
        //  Push it to the iterator stack, and set the id.
        //
        pageContext.setAttribute( WikiTagBase.ATTR_CONTEXT,
                                  context,
                                  PageContext.REQUEST_SCOPE );
        pageContext.setAttribute( getId(),
                                  o );
    }

    /**
     *  {@inheritDoc}
     */
    public int doEndTag()
    {
        // Return back to the original.
        pageContext.setAttribute( WikiTagBase.ATTR_CONTEXT,
                                  m_wikiContext,
                                  PageContext.REQUEST_SCOPE );

        return EVAL_PAGE;
    }

    /**
     *  {@inheritDoc}
     */
    public int doAfterBody()
    {
        if( bodyContent != null )
        {
            try
            {
                JspWriter out = getPreviousOut();
                out.print(bodyContent.getString());
                bodyContent.clearBody();
            }
            catch( IOException e )
            {
                log.error("Unable to get inner tag text", e);
                // FIXME: throw something?
            }
        }

        if( m_iterator != null && m_iterator.hasNext() )
        {
            buildContext();
            return EVAL_BODY_BUFFERED;
        }

        return SKIP_BODY;
    }
    
    /**
     *  In case your tag throws an exception at any point, you can
     *  override this method and implement a custom exception handler.
     *  <p>
     *  By default, this handler does nothing.
     *  
     *  @param arg0 The Throwable that the tag threw
     *  
     *  @throws Throwable I have no idea why this would throw anything
     */
    public void doCatch(Throwable arg0) throws Throwable
    {
    }

    /**
     *  Executed after the tag has been finished.  This is a great place
     *  to put any cleanup code.  However you <b>must</b> call super.doFinally()
     *  if you override this method, or else some of the things may not
     *  work as expected.
     */
    public void doFinally()
    {
        resetIterator();
        m_iterator = null;
        m_pageName = null;
        m_wikiContext = null;        
    }

}
