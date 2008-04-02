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
import javax.servlet.jsp.tagext.BodyTagSupport;
import javax.servlet.jsp.tagext.TryCatchFinally;

import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.action.WikiActionBeanFactory;

/**
 *  This is a class that provides the same services as the WikiTagBase, but this time it
 *   works for the BodyTagSupport base class.
 * 
 *
 */
public abstract class WikiBodyTag extends BodyTagSupport
    implements TryCatchFinally
{
    protected WikiContext m_wikiContext;
    static    Logger    log = Logger.getLogger( WikiBodyTag.class );

    public int doStartTag() throws JspException
    {
        try
        {
            m_wikiContext = (WikiContext) WikiActionBeanFactory.findActionBean( pageContext );

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
     * A local stub for doing tags.  This is just called after the local variables
     * have been set.
     * @return As doStartTag()
     * @throws JspException
     * @throws IOException
     */
    public abstract int doWikiStartTag() throws JspException, IOException;

    public void doCatch(Throwable arg0) throws Throwable
    {
    }

    public void doFinally()
    {
        m_wikiContext = null;
    }  
    
    
}
