/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2005 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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

import java.util.*;

import java.io.IOException;
import javax.servlet.jsp.JspException;
import javax.servlet.ServletException;

import com.ecyrd.jspwiki.providers.ProviderException;
import com.ecyrd.jspwiki.*;

/**
 *  Is used as a "super include" tag, which can include the proper context
 *  based on the wikicontext.
 *
 *  @author Janne Jalkanen
 *  @since 2.2
 */
public class ContentTag
    extends WikiTagBase
{
    private static final long serialVersionUID = 0L;
    
    private Map m_mappings = new HashMap();

    public void setView( String s )
    {
        m_mappings.put( WikiContext.VIEW, s );
    }

    public void setDiff( String s )
    {
        m_mappings.put( WikiContext.DIFF, s );
    }

    public void setInfo( String s )
    {
        m_mappings.put( WikiContext.INFO, s );
    }

    public void setPreview( String s )
    {
        m_mappings.put( WikiContext.PREVIEW, s );
    }

    public void setConflict( String s )
    {
        m_mappings.put( WikiContext.CONFLICT, s );
    }

    public void setFind( String s )
    {
        m_mappings.put( WikiContext.FIND, s );
    }

    public void setPrefs( String s )
    {
        m_mappings.put( WikiContext.PREFS, s );
    }

    public void setError( String s )
    {
        m_mappings.put( WikiContext.ERROR, s );
    }

    public void setEdit( String s )
    {
        m_mappings.put( WikiContext.EDIT, s );
    }

    public void setComment( String s )
    {
        m_mappings.put( WikiContext.COMMENT, s );
    }

    public final int doWikiStartTag()
        throws IOException,
               ProviderException
    {
        return SKIP_BODY;
    }

    public final int doEndTag()
        throws JspException
    {
        try
        {
            // Check the overridden templates first
            String requestContext = m_wikiContext.getRequestContext();
            String contentTemplate = (String)m_mappings.get( requestContext );

            // If not found, use the defaults
            if ( contentTemplate == null )
            {
                contentTemplate = m_wikiContext.getContentTemplate();
            }
            
            // If still no, something fishy is going on
            if( contentTemplate == null )
            {
                throw new JspException("This template uses <wiki:Content/> in an unsupported context: " + requestContext );
            }

            String page = m_wikiContext.getEngine().getTemplateManager().findJSP( pageContext,
                                                                                  m_wikiContext.getTemplate(),
                                                                                  contentTemplate );
            pageContext.include( page );
        }
        catch( ServletException e )
        {
            log.warn( "Including failed, got a servlet exception from sub-page. "+
                      "Rethrowing the exception to the JSP engine.", e );
            throw new JspException( e.getMessage() );
        }
        catch( IOException e )
        {
            log.warn( "I/O exception - probably the connection was broken. "+
                      "Rethrowing the exception to the JSP engine.", e );
            throw new JspException( e.getMessage() );
        }

        return EVAL_PAGE;
    }
}
