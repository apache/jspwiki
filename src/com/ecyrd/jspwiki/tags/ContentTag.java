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
