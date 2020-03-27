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
package org.apache.wiki.tags;

import org.apache.log4j.Logger;
import org.apache.wiki.api.core.ContextEnum;
import org.apache.wiki.api.exceptions.ProviderException;
import org.apache.wiki.ui.TemplateManager;

import javax.servlet.ServletException;
import javax.servlet.jsp.JspException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


/**
 *  Is used as a "super include" tag, which can include the proper context
 *  based on the wikicontext.
 *
 *  @since 2.2
 */
public class ContentTag extends WikiTagBase {

    private static final long serialVersionUID = 0L;
    private static final Logger log = Logger.getLogger( ContentTag.class );
    
    private Map<String, String> m_mappings = new HashMap<>();

    /**
     *  Set the template for the VIEW context.
     *  
     *  @param s The template name.
     */
    public void setView( final String s )
    {
        m_mappings.put( ContextEnum.PAGE_VIEW.getRequestContext(), s );
    }

    /**
     *  Set the template for the DIFF context.
     *  
     *  @param s The template name.
     */
    public void setDiff( final String s )
    {
        m_mappings.put( ContextEnum.PAGE_DIFF.getRequestContext(), s );
    }

    /**
     *  Set the template for the INFO context.
     *  
     *  @param s The template name.
     */
    public void setInfo( final String s )
    {
        m_mappings.put( ContextEnum.PAGE_INFO.getRequestContext(), s );
    }

    /**
     *  Set the template for the PREVIEW context.
     *  
     *  @param s The template name.
     */
    public void setPreview( final String s )
    {
        m_mappings.put( ContextEnum.PAGE_PREVIEW.getRequestContext(), s );
    }

    /**
     *  Set the template for the CONFLICT context.
     *  
     *  @param s The template name.
     */
    public void setConflict( final String s )
    {
        m_mappings.put( ContextEnum.PAGE_CONFLICT.getRequestContext(), s );
    }

    /**
     *  Set the template for the FIND context.
     *  
     *  @param s The template name.
     */
    public void setFind( final String s )
    {
        m_mappings.put( ContextEnum.WIKI_FIND.getRequestContext(), s );
    }

    /**
     *  Set the template for the PREFS context.
     *  
     *  @param s The template name.
     */
    public void setPrefs( final String s )
    {
        m_mappings.put( ContextEnum.WIKI_PREFS.getRequestContext(), s );
    }

    /**
     *  Set the template for the ERROR context.
     *  
     *  @param s The template name.
     */
    public void setError( final String s )
    {
        m_mappings.put( ContextEnum.WIKI_ERROR.getRequestContext(), s );
    }

    /**
     *  Set the template for the EDIT context.
     *  
     *  @param s The template name.
     */
    public void setEdit( final String s )
    {
        m_mappings.put( ContextEnum.PAGE_EDIT.getRequestContext(), s );
    }

    /**
     *  Set the template for the COMMENT context.
     *  
     *  @param s The template name.
     */
    public void setComment( final String s )
    {
        m_mappings.put( ContextEnum.PAGE_COMMENT.getRequestContext(), s );
    }

    /**
     *  {@inheritDoc}
     */
    @Override public final int doWikiStartTag()
        throws IOException,
               ProviderException
    {
        return SKIP_BODY;
    }

    /**
     *  {@inheritDoc}
     */
    @Override public final int doEndTag()
        throws JspException
    {
        try
        {
            // Check the overridden templates first
            final String requestContext = m_wikiContext.getRequestContext();
            String contentTemplate = m_mappings.get( requestContext );

            // If not found, use the defaults
            if( contentTemplate == null ) {
                contentTemplate = m_wikiContext.getContentTemplate();
            }

            // If still no, something fishy is going on
            if( contentTemplate == null ) {
                throw new JspException( "This template uses <wiki:Content/> in an unsupported context: " + requestContext );
            }

            final String page = m_wikiContext.getEngine().getManager( TemplateManager.class ).findJSP( pageContext,
                                                                                                 m_wikiContext.getTemplate(),
                                                                                                 contentTemplate );
            pageContext.include( page );
        }
        catch( final ServletException e )
        {
            log.warn( "Including failed, got a servlet exception from sub-page. "+
                      "Rethrowing the exception to the JSP engine.", e );
            throw new JspException( e.getMessage() );
        }
        catch( final IOException e )
        {
            log.warn( "I/O exception - probably the connection was broken. "+
                      "Rethrowing the exception to the JSP engine.", e );
            throw new JspException( e.getMessage() );
        }

        return EVAL_PAGE;
    }
}
