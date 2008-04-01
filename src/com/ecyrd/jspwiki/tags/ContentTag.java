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
