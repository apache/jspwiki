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
import org.apache.wiki.WikiContext;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.BodyTagSupport;
import javax.servlet.jsp.tagext.TryCatchFinally;
import java.io.IOException;


/**
 *  This is a class that provides the same services as the WikiTagBase, but this time it
 *   works for the BodyTagSupport base class.
 */
public abstract class WikiBodyTag extends BodyTagSupport implements TryCatchFinally {

	private static final long serialVersionUID = -6732266865112847897L;
	protected WikiContext m_wikiContext;
    private static final Logger log = Logger.getLogger( WikiBodyTag.class );

    public int doStartTag() throws JspException {
        try {
            m_wikiContext = (WikiContext) pageContext.getAttribute( WikiContext.ATTR_CONTEXT, PageContext.REQUEST_SCOPE );
            if( m_wikiContext == null ) {
                throw new JspException("WikiContext may not be NULL - serious internal problem!");
            }

            return doWikiStartTag();
        } catch( final Exception e ) {
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

    public void doCatch(Throwable arg0) throws Throwable {
    }

    public void doFinally()
    {
        m_wikiContext = null;
    }  
    
}
