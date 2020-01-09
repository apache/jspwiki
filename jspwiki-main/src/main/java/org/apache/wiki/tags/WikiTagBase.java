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
import org.apache.wiki.util.TextUtil;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.TagSupport;
import javax.servlet.jsp.tagext.TryCatchFinally;


/**
 *  Base class for JSPWiki tags.  You do not necessarily have to derive from this class, since this does some initialization.
 *  <P>
 *  This tag is only useful if you're having an "empty" tag, with no body content.
 *
 *  @since 2.0
 */
public abstract class WikiTagBase extends TagSupport implements TryCatchFinally {

    private static final long serialVersionUID = -1409836349293777141L;
    private static final Logger log = Logger.getLogger( WikiTagBase.class );

    protected WikiContext m_wikiContext;

    /**
     * This method calls the parent setPageContext() but it also provides a way for a tag to initialize itself before
     * any of the setXXX() methods are called.
     */
    public void setPageContext( final PageContext arg0 ) {
        super.setPageContext( arg0 );
        initTag();
    }

    /**
     *  This method is called when the tag is encountered within a new request, but before the setXXX() methods are called.
     *  The default implementation does nothing.
     *  @since 2.3.92
     */
    public void initTag() {
        m_wikiContext = null;
    }
    
    public int doStartTag() throws JspException {
        try {
            m_wikiContext = ( WikiContext )pageContext.getAttribute( WikiContext.ATTR_CONTEXT, PageContext.REQUEST_SCOPE );
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
     *  This method is allowed to do pretty much whatever he wants.
     *  We then catch all mistakes.
     */
    public abstract int doWikiStartTag() throws Exception;

    public int doEndTag() throws JspException {
        return EVAL_PAGE;
    }

    public void doCatch( final Throwable th ) throws Throwable {
    	log.error( th.getMessage(), th );
    }

    public void doFinally()
    {
        m_wikiContext = null;
    }

    public void setId( final String id)
    {
        super.setId( TextUtil.replaceEntities( id ) );
    }

}
