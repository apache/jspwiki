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

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.TryCatchFinally;

import org.apache.wiki.*;
import org.apache.wiki.action.WikiActionBean;
import org.apache.wiki.action.WikiContextFactory;
import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;
import org.apache.wiki.ui.stripes.WikiInterceptor;
import org.apache.wiki.util.TextUtil;

import net.sourceforge.stripes.tag.StripesTagSupport;



/**
 *  <p>Base class for JSPWiki tags.  Tags need not necessarily
 *  derive from this class, since this base class initializes certain
 *  state properties that not all tag classes need.</p>
 *  <p>The default method stubs defined in this class are only useful for
 *  empty element tags, with no body content. Subclasses can override
 *  any of these methods to provide customized functionality.</p>
 *
 *  @since 2.0
 */
public abstract class WikiTagBase
    extends StripesTagSupport
    implements TryCatchFinally
{
    /**
     * @deprecated use {@link org.apache.wiki.action.WikiContextFactory#saveContext(javax.servlet.ServletRequest, WikiContext)}
     * and {@link org.apache.wiki.action.WikiContextFactory#findContext(PageContext)} to set the WikiContext instead
     */
    public static final String ATTR_CONTEXT = "wikiContext";

    static    Logger    log = LoggerFactory.getLogger( WikiTagBase.class );

    protected WikiContext m_wikiContext;

    protected WikiActionBean m_wikiActionBean;

    private String m_id;

    /**
     * {@inheritDoc}
     * <p>This implementation calls the superclass method setPageContext(),
     * and also calls {{@link #initTag()} so that the tags (or subclasses) can
     * initialize <code>set<var>XXX</var>()</code> property methods are
     * called.</p>
     */
    public void setPageContext(PageContext arg0)
    {
        super.setPageContext(arg0);
        initTag();
    }

    /**
     *  Initialization method that resets the tag to a known state.
     *  This method is called after the tag is encountered within a new request,
     *  but before the <code>set<var>XXX</var>()</code> methods are called. 
     *  The default implementation nulls out the internal
     * references to the WikiActionBean, WikiContext, and tag ID.
     *  @since 2.3.92
     */
    public void initTag()
    {
        m_wikiActionBean = null;
        m_wikiContext = null;
        m_id = null;
        return;
    }
    
    /**
     * <p>Superclass initializer that sets common state attributes for WikiTagBase
     * subclasses. Subclasses should generally override {@link #doWikiStartTag()}, rather
     * than this method, to initialize themselves.</p>
     * <p>This method performs the following initialization steps. First, it sets an
     * internal reference to the current WikiActionBean obtained by calling
     * {@link org.apache.wiki.ui.stripes.WikiInterceptor#findActionBean(PageContext)},
     * which retrieves the WikiActionBean from page scope.
     * Then, the object returned by {@link WikiActionBean#getContext()} will be 
     * set as the page's WikiContext. Both of these objects are made available to
     * subclasses as protected fields {@link #m_wikiActionBean} and
     * {@link #m_wikiContext}, respectively.</p>
     * <p> It is considered an error condition if the 
     * WikiActionBean cannot be retrieved from the PageContext. This can happen if
     * WikiTagBase (or a subclass tag) is used in a JSP that doesn't set or use an
     * ActionBean. For this reason, JSPs that use WikiTagBase-subclassed tags should
     * always contain a &lt;stripes:useActionBean&gt; tag at the top of the page to ensure
     * that the correct ActionBean is set.</p>
     */
    public int doStartTag()
        throws JspException
    {
        try
        {
            // Retrieve the ActionBean injected by WikiInterceptor
            m_wikiActionBean = WikiInterceptor.findActionBean( this.getPageContext() );
            if ( m_wikiActionBean == null )
            {
                throw new JspException( "Can't find WikiActionBean in page context! (tag=" + this.getClass() + ")" );
            }

            // Retrieve the WikiContext -- always WikiActionBean.getContext() unless IteratorTag changed it
            m_wikiContext = WikiContextFactory.findContext( pageContext );
            if( m_wikiContext == null )
            {
                throw new JspException("WikiContext may not be NULL - serious internal problem!");
            }

            return doWikiStartTag();
        }
        catch( Exception e )
        {
            log.error( "Tag failed", e );
            throw new JspException( "Tag failed, check logs: " + ((e.getMessage() == null) ? e : e.getMessage()) );
        }
    }

    /**
     *  Initialization method used by WikiTagBase subclasses to initialize themselves.
     *  Subclasses can override this method to do just about anything. Implementations
     *  should return {@link javax.servlet.jsp.tagext.Tag#EVAL_BODY_INCLUDE},
     *  or {@link javax.servlet.jsp.tagext.Tag#SKIP_BODY}. If the subclass implements
     *  {@link javax.servlet.jsp.tagext.BodyTag}, it may also return
     *  {@link javax.servlet.jsp.tagext.BodyTag#EVAL_BODY_BUFFERED}.
     *  Any exceptions that are thrown can then be dealt with by
     *  {@link #doCatch(Throwable)} or {@link #doFinally()}.
     *  @see javax.servlet.jsp.tagext.Tag#doStartTag()
     */
    public abstract int doWikiStartTag() throws Exception;

    public int doEndTag()
        throws JspException
    {
        return EVAL_PAGE;
    }
    
    /**
     * 
     */
    public int doAfterBody() throws JspException 
    {
        return SKIP_BODY;
    }
    
    /**
     * 
     */
    public String getId()
    {
        return m_id;
    }

    /**
     * {@inheritDoc}. The default implementation re-throws the Throwable.
     * @param cause the Throwable exception that caused the error
     */
    public void doCatch(Throwable cause ) throws Throwable
    {
        throw cause;
    }

    /**
     * {@inheritDoc}
     * <p>The default implementation nulls out the internal
     * references to the WikiActionBean, WikiContext, and tag ID.</p>
     */
    public void doFinally()
    {
        m_wikiActionBean = null;
        m_wikiContext = null;
        m_id = null;
    }

    /**
     * <p>The default implementation sanitizes the tag ID before setting
     * it by escaping potentially dangerous characters.</p>
     */
    public void setId(String id)
    {
		m_id = ( TextUtil.replaceEntities( id ) );
	}

}
