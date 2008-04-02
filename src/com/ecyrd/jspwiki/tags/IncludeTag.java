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

import java.io.IOException;
import javax.servlet.jsp.JspException;
import javax.servlet.ServletException;

import com.ecyrd.jspwiki.TextUtil;
import com.ecyrd.jspwiki.providers.ProviderException;

/**
 *  Includes an another JSP page, making sure that we actually pass
 *  the WikiContext correctly.
 *
 *  @since 2.0
 */
// FIXME: Perhaps unnecessary?
public class IncludeTag
    extends WikiTagBase
{
    private static final long serialVersionUID = 0L;
    
    protected String m_pageName;
    protected boolean m_searchTemplates;

    public void initTag()
    {
        super.initTag();
        m_pageName = null;
        m_searchTemplates = false;
    }

    public void setPage( String page )
    {
        m_pageName = page;
    }

    public String getPage()
    {
        return m_pageName;
    }

    public final int doWikiStartTag()
        throws IOException,
               ProviderException
    {
        // WikiEngine engine = m_wikiContext.getEngine();

        return SKIP_BODY;
    }

    public final int doEndTag()
        throws JspException
    {
        try
        {
            String page = m_actionBean.getEngine().getTemplateManager().findJSP( pageContext,
                                                                                  m_actionBean.getTemplate(),
                                                                                  m_pageName );
            
            if( page == null )
            {
                pageContext.getOut().println("No template file called '"+TextUtil.replaceEntities(m_pageName)+"'");
            }
            else
            {
                pageContext.include( page );
            }
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
