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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.jsp.JspException;

import org.apache.log4j.Logger;
import org.apache.wiki.WikiContext;
import org.apache.wiki.api.exceptions.ProviderException;


/**
 *  Is used as a "super include" tag, which can include the proper context
 *  based on the wikicontext.
 *
 *  @since 2.2
 */
public class ContentTag extends WikiTagBase {

    private static final long serialVersionUID = 0L;
    private static final Logger log = Logger.getLogger( ContentTag.class );
    
    private Map<String, String> m_mappings = new HashMap<String, String>();

    /**
     *  Set the template for the VIEW context.
     *  
     *  @param s The template name.
     */
    public void setView( String s )
    {
        m_mappings.put( WikiContext.VIEW, s );
    }

    /**
     *  Set the template for the DIFF context.
     *  
     *  @param s The template name.
     */
    public void setDiff( String s )
    {
        m_mappings.put( WikiContext.DIFF, s );
    }

    /**
     *  Set the template for the INFO context.
     *  
     *  @param s The template name.
     */
    public void setInfo( String s )
    {
        m_mappings.put( WikiContext.INFO, s );
    }

    /**
     *  Set the template for the PREVIEW context.
     *  
     *  @param s The template name.
     */
    public void setPreview( String s )
    {
        m_mappings.put( WikiContext.PREVIEW, s );
    }

    /**
     *  Set the template for the CONFLICT context.
     *  
     *  @param s The template name.
     */
    public void setConflict( String s )
    {
        m_mappings.put( WikiContext.CONFLICT, s );
    }

    /**
     *  Set the template for the FIND context.
     *  
     *  @param s The template name.
     */
    public void setFind( String s )
    {
        m_mappings.put( WikiContext.FIND, s );
    }

    /**
     *  Set the template for the PREFS context.
     *  
     *  @param s The template name.
     */
    public void setPrefs( String s )
    {
        m_mappings.put( WikiContext.PREFS, s );
    }

    /**
     *  Set the template for the ERROR context.
     *  
     *  @param s The template name.
     */
    public void setError( String s )
    {
        m_mappings.put( WikiContext.ERROR, s );
    }

    /**
     *  Set the template for the EDIT context.
     *  
     *  @param s The template name.
     */
    public void setEdit( String s )
    {
        m_mappings.put( WikiContext.EDIT, s );
    }

    /**
     *  Set the template for the COMMENT context.
     *  
     *  @param s The template name.
     */
    public void setComment( String s )
    {
        m_mappings.put( WikiContext.COMMENT, s );
    }

    /**
     *  {@inheritDoc}
     */
    public final int doWikiStartTag()
        throws IOException,
               ProviderException
    {
        return SKIP_BODY;
    }

    /**
     *  {@inheritDoc}
     */
    public final int doEndTag()
        throws JspException
    {
        try
        {
            // Check the overridden templates first
            String requestContext = m_wikiContext.getRequestContext();
            String contentTemplate = m_mappings.get( requestContext );

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
