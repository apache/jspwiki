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
import java.util.Collection;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;

import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.api.WikiPage;
import org.apache.wiki.content.PageNotFoundException;
import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;
import org.apache.wiki.providers.ProviderException;



/**
 *  Iterates through tags.
 *
 *  <P><B>Attributes</B></P>
 *  <UL>
 *    <LI>page - Page name to refer to.  Default is the current page.
 *  </UL>
 *
 *  @since 2.0
 */

// FIXME: Too much in common with IteratorTag - REFACTOR
public class HistoryIteratorTag
    extends IteratorTag
{
    private static final long serialVersionUID = 0L;
    
    static    Logger    log = LoggerFactory.getLogger( HistoryIteratorTag.class );

    public final int doStartTag()
    {
        m_wikiContext = (WikiContext) pageContext.getAttribute( WikiTagBase.ATTR_CONTEXT,
                                                                PageContext.REQUEST_SCOPE );

        WikiEngine engine = m_wikiContext.getEngine();
        WikiPage   page;

        page = m_wikiContext.getPage();

        try
        {
            if( page != null && engine.pageExists(page) )
            {
                Collection<WikiPage> versions;
                try
                {
                    versions = engine.getVersionHistory( page.getName() );
                }
                catch( PageNotFoundException e )
                {
                    // There is no history
                    return SKIP_BODY;
                }

                m_iterator = versions.iterator();

                if( m_iterator.hasNext() )
                {
                    WikiContext context = (WikiContext)m_wikiContext.clone();
                    context.setPage( (WikiPage)m_iterator.next() );
                    pageContext.setAttribute( WikiTagBase.ATTR_CONTEXT,
                                              context,
                                              PageContext.REQUEST_SCOPE );
                    pageContext.setAttribute( getId(),
                                              context.getPage() );
                }
                else
                {
                    return SKIP_BODY;
                }
            }

            return EVAL_BODY_BUFFERED;
        }
        catch( ProviderException e )
        {
            log.error("Provider failed while trying to iterator through history",e);
            // FIXME: THrow something.
        }

        return SKIP_BODY;
    }

    public final int doAfterBody()
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
            WikiContext context = (WikiContext)m_wikiContext.clone();
            context.setPage( (WikiPage)m_iterator.next() );
            pageContext.setAttribute( WikiTagBase.ATTR_CONTEXT,
                                      context,
                                      PageContext.REQUEST_SCOPE );
            pageContext.setAttribute( getId(),
                                      context.getPage() );
            return EVAL_BODY_BUFFERED;
        }

        return SKIP_BODY;
    }
}
