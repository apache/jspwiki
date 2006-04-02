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

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.TagSupport;
import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.WikiContext;

/**
 *  Base class for JSPWiki tags.  You do not necessarily have
 *  to derive from this class, since this does some initialization.
 *  <P>
 *  This tag is only useful if you're having an "empty" tag, with
 *  no body content.
 *
 *  @author Janne Jalkanen
 *  @since 2.0
 */
public abstract class WikiTagBase
    extends TagSupport
{
    public static final String ATTR_CONTEXT = "jspwiki.context";

    static    Logger    log = Logger.getLogger( WikiTagBase.class );

    protected WikiContext m_wikiContext;

    /**
     *   This method calls the parent setPageContext() but it also
     *   provides a way for a tag to initialize itself before
     *   any of the setXXX() methods are called.
     */
    public void setPageContext(PageContext arg0)
    {
        super.setPageContext(arg0);
        
        initTag();
    }

    /**
     *  This method is called when the tag is encountered within a new request,
     *  but before the setXXX() methods are called. 
     *  The default implementation does nothing.
     *
     */
    public void initTag()
    {
        return;
    }
    
    public int doStartTag()
        throws JspException
    {
        try
        {
            m_wikiContext = (WikiContext) pageContext.getAttribute( ATTR_CONTEXT,
                                                                    PageContext.REQUEST_SCOPE );

            if( m_wikiContext == null )
            {
                throw new JspException("WikiContext may not be NULL - serious internal problem!");
            }

            return doWikiStartTag();
        }
        catch( Exception e )
        {
            log.error( "Tag failed", e );
            throw new JspException( "Tag failed, check logs: "+e.getMessage() );
        }
    }

    /**
     *  This method is allowed to do pretty much whatever he wants.
     *  We then catch all mistakes.
     */
    public abstract int doWikiStartTag() throws Exception;

    public int doEndTag()
        throws JspException
    {
        return EVAL_PAGE;
    }
}
