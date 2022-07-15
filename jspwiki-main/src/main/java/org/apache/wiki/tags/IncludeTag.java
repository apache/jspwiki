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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.wiki.api.exceptions.ProviderException;
import org.apache.wiki.ui.TemplateManager;
import org.apache.wiki.util.TextUtil;

import javax.servlet.ServletException;
import javax.servlet.jsp.JspException;
import java.io.IOException;

/**
 *  Includes an another JSP page, making sure that we actually pass the WikiContext correctly.
 *
 *  @since 2.0
 */
// FIXME: Perhaps unnecessary?
public class IncludeTag extends WikiTagBase {

    private static final long serialVersionUID = 0L;
    private static final Logger LOG = LogManager.getLogger( IncludeTag.class );
    
    protected String m_page;

    @Override
    public void initTag() {
        super.initTag();
        m_page = null;
    }

    public void setPage( final String page )
    {
        m_page = page;
    }

    public String getPage()
    {
        return m_page;
    }

    @Override
    public final int doWikiStartTag() throws IOException, ProviderException {
        return SKIP_BODY;
    }

    @Override
    public final int doEndTag() throws JspException {
        try {
            final String page = m_wikiContext.getEngine().getManager( TemplateManager.class ).findJSP( pageContext,
                                                                                                 m_wikiContext.getTemplate(),
                                                                                                 m_page );

            if( page == null ) {
                pageContext.getOut().println( "No template file called '" + TextUtil.replaceEntities( m_page ) + "'" );
            } else {
                pageContext.include( page );
            }
        } catch( final ServletException e ) {
            LOG.warn( "Including failed, got a servlet exception from sub-page. Rethrowing the exception to the JSP engine.", e );
            throw new JspException( e.getMessage() );
        } catch( final IOException e ) {
            LOG.warn( "I/O exception - probably the connection was broken. Rethrowing the exception to the JSP engine.", e );
            throw new JspException( e.getMessage() );
        }

        return EVAL_PAGE;
    }

}
