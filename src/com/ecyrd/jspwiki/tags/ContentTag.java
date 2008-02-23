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

import java.util.HashMap;
import java.util.Map;

import java.io.IOException;
import javax.servlet.jsp.JspException;
import javax.servlet.ServletException;

import com.ecyrd.jspwiki.providers.ProviderException;
import com.ecyrd.jspwiki.action.*;

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
    
    private Map<Class <? extends WikiActionBean>,String> m_mappings = new HashMap<Class <? extends WikiActionBean>,String>();

    public void setView( String s )
    {
        m_mappings.put( ViewActionBean.class, s );
    }

    public void setDiff( String s )
    {
        m_mappings.put( DiffActionBean.class, s );
    }

    public void setInfo( String s )
    {
        m_mappings.put( PageInfoActionBean.class, s );
    }

    public void setPreview( String s )
    {
        m_mappings.put( PreviewActionBean.class, s );
    }

    public void setConflict( String s )
    {
        m_mappings.put( DiffActionBean.class, s );
    }

    public void setFind( String s )
    {
        m_mappings.put( SearchActionBean.class, s );
    }

    public void setPrefs( String s )
    {
        m_mappings.put( UserPreferencesActionBean.class, s );
    }

    public void setError( String s )
    {
        m_mappings.put( ErrorActionBean.class, s );
    }

    public void setEdit( String s )
    {
        m_mappings.put( EditActionBean.class, s );
    }

    public void setComment( String s )
    {
        m_mappings.put( CommentActionBean.class, s );
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
            String contentTemplate = m_mappings.get( m_actionBean.getClass() );

            // If not found, use the default name (trim "ActionBean" from name, and append "Content"
            // e.g., EditActionBean yields "EditContent.jsp"
            if ( contentTemplate == null )
            {
                String beanName = m_actionBean.getClass().getName();
                if ( beanName.endsWith( "ActionBean" ) )
                {
                    beanName = beanName.substring( 0, beanName.lastIndexOf( "ActionBean") );
                }
                contentTemplate = beanName + "Content.jsp";
            }
            
            // If still no, something fishy is going on
            if( contentTemplate == null )
            {
                throw new JspException("This template uses <wiki:Content/> in an unsupported context: " + m_actionBean.getClass().getName() );
            }

            String page = m_actionBean.getEngine().getTemplateManager().findJSP( pageContext,
                                                                                  m_actionBean.getTemplate(),
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
