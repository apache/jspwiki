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
import com.ecyrd.jspwiki.action.WikiActionBeanFactory;
import com.ecyrd.jspwiki.ui.WikiInterceptor;

/**
 * <p>
 *  Base class for JSPWiki tags.  You do not necessarily have
 *  to derive from this class, since this does some initialization.
 * </p>
 *  <p>
 *  This tag is only useful if you're having an "empty" tag, with
 *  no body content.
 * </p>
 *  <p>The order of init method processing for subclasses of WikiTagBase is as follows:</p>
 *  <ul>
 *  <li>{@link #initTag()}</li>
 *  <li>{@link #doStartTag()}</li>
 *  <li>{@link #doWikiStartTag()} - implemented by subclasses</li>
 *  </ul>
 *
 *  @since 2.0
 */
public abstract class WikiTagBase
    extends StripesTagSupport
    implements TryCatchFinally
{
    /**
     *  The name of the request attribute used to store ActionBeans (WikiContexts).
     *  @deprecated Use {@link WikiInterceptor#ATTR_ACTIONBEAN} instead
     */
    public static final String ATTR_CONTEXT = WikiInterceptor.ATTR_ACTIONBEAN;

    static    Logger    log = Logger.getLogger( WikiTagBase.class );

    protected WikiActionBean m_actionBean;

    private String m_id;

    /**
     * If the ActionBean is a WikiContext and the page is non-null, this value will be set automatically by {@link #doStartTag()}.
     */
    protected WikiPage m_page;

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
        m_actionBean = null;
        m_page = null;
        return;
    }
    
    /**
     * Initializes the tag, and sets an internal reference to the current WikiActionBean
     * by delegating to
     * {@link com.ecyrd.jspwiki.action.WikiInterceptor#findActionBean( PageContext )}.
     * (That method retrieves the WikiActionBean from page scope.).
     * If the WikiActionBean is a WikiContext, a reference to the current wiki page will be
     * set also. Both of these available as protected fields {@link #m_actionBean} and
     * {@link #m_page}, respectively. It is considered an error condition if the 
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
            m_actionBean = WikiActionBeanFactory.findActionBean( pageContext );
            
            // It's really bad news if the WikiActionBean wasn't injected (or saved as a variable!)
            if ( m_actionBean == null )
            {
                throw new JspException( "Can't find WikiActionBean in page or request context. Make sure JSP saves it as a variable." );
            }

            // If this is a WikiContext-style ActionBean, get the page (WikiInterceptor should have set it)
            m_page = null;
            if ( m_actionBean instanceof WikiContext )
            {
                m_page = ((WikiContext)m_actionBean).getPage();
                if ( m_page == null )
                {
                    throw new JspException( "WikiContext has a null WikiPage. This should not happen, and probably indicates a bug in JSPWiki's core classes!" );
                }
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
        m_actionBean = null;
        m_id = null;
        m_page = null;
    }

	/**
	 * Sets the ID for this tag. Note that the ID is sanitized using {@link com.ecyrd.jspwiki.TextUtil#replaceEntities(String)}.
	 * @param id the id for this tag
	 */
    public void setId( String id )
    {
		m_id = ( TextUtil.replaceEntities( id ) );
	}
    
}
