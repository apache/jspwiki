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

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.TryCatchFinally;

import net.sourceforge.stripes.tag.StripesTagSupport;

import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.*;
import com.ecyrd.jspwiki.action.WikiActionBean;
import com.ecyrd.jspwiki.action.WikiInterceptor;

/**
 *  Base class for JSPWiki tags.  You do not necessarily have
 *  to derive from this class, since this does some initialization.
 *  <P>
 *  This tag is only useful if you're having an "empty" tag, with
 *  no body content.
 *
 *  @since 2.0
 */
public abstract class WikiTagBase
    extends StripesTagSupport
    implements TryCatchFinally
{
    public static final String ATTR_CONTEXT = "wikiContext";

    static    Logger    log = Logger.getLogger( WikiTagBase.class );

    protected WikiContext m_wikiContext;

    protected WikiActionBean m_wikiActionBean;

    private String m_id;

    /**
     *   This method calls the parent setPageContext() but it also
     *   provides a way for a tag to initialize itself before
     *   any of the setXXX() methods are called.
     */
    public void setPageContext(PageContext arg0)
    {
        super.setPageContext(arg0);
        
        initTag();
    }

    /**
     *  This method is called when the tag is encountered within a new request,
     *  but before the setXXX() methods are called. 
     *  The default implementation does nothing.
     *  @since 2.3.92
     */
    public void initTag()
    {
        m_wikiContext = null;
        m_id = null;
        return;
    }
    
    /**
     * Initializes the tag, and sets an internal reference to the current WikiActionBean
     * by delegating to
     * {@link com.ecyrd.jspwiki.action.WikiInterceptor#findActionBean(javax.servlet.ServletRequest)}.
     * (That method retrieves the WikiActionBean from page scope.).
     * If the WikiActionBean is a WikiContext, a specific reference to the WikiContext
     * will be set also. Both of these available as protected fields {@link #m_wikiActionBean} and
     * {@link #m_wikiContext}, respectively. It is considered an error condition if the 
     * WikiActionBean cannot be retrieved from the PageContext.
     * It's also an error condition if the WikiActionBean is actually a WikiContext, and it
     * returns a <code>null</code> WikiPage.
     */
    public int doStartTag()
        throws JspException
    {
        try
        {
            // Retrieve the ActionBean injected by WikiInterceptor
            m_wikiActionBean = WikiInterceptor.findActionBean( this.getPageContext().getRequest() );
            
            // It's really bad news if the WikiActionBean wasn't injected (or saved as a variable!)
            if ( m_wikiActionBean == null )
            {
                throw new JspException( "Can't find WikiActionBean in page or request context! (tag=" + this.getClass() + ")" );
            }

            // Retrieve the WikiContext injected by WikiInterceptor (could be a fake context!)
            m_wikiContext = (WikiContext) pageContext.getAttribute( ATTR_CONTEXT,
                                                                    PageContext.REQUEST_SCOPE );

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
     *  This method is allowed to do pretty much whatever he wants.
     *  We then catch all mistakes.
     */
    public abstract int doWikiStartTag() throws Exception;

    public int doEndTag()
        throws JspException
    {
        return EVAL_PAGE;
    }
    
    public int doAfterBody() throws JspException {
        return SKIP_BODY;
    }
    
    public String getId()
    {
        return m_id;
    }

    public void doCatch(Throwable arg0) throws Throwable
    {
    }

    public void doFinally()
    {
        m_wikiContext = null;
        m_id = null;
    }

    public void setId(String id)
    {
		m_id = ( TextUtil.replaceEntities( id ) );
	}

}
