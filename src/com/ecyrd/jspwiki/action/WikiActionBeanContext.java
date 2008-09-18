package com.ecyrd.jspwiki.action;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import net.sourceforge.stripes.action.ActionBeanContext;
import net.sourceforge.stripes.controller.FlashScope;

import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiPage;
import com.ecyrd.jspwiki.WikiSession;
import com.ecyrd.jspwiki.tags.WikiTagBase;

/**
 * <p>
 * {@link net.sourceforge.stripes.action.ActionBeanContext} subclass that
 * contains a convenient reference to the current JSPWiki WikiEngine and the
 * user's HttpServletRequest and WikiSession.
 * </p>
 * <p>
 * When the WikiActionBeanContext is created, callers <em>must</em> set the
 * WikiEngine reference by calling either {@link #setWikiEngine(WikiEngine)}
 * (which sets it directly), or {@link #setServletContext(ServletContext)}
 * (which sets it lazily). when {@link #setServletContext(ServletContext)}. The
 * HttpServletRequest reference is set via
 * {@link #setRequest(HttpServletRequest)}.
 * </p>
 * 
 * @author Andrew Jaquith
 */
public class WikiActionBeanContext extends ActionBeanContext
{
    private volatile WikiEngine m_engine = null;

    private volatile WikiSession m_wikiSession = null;

    /**
     * Constructs a new WikiActionBeanContext.
     */
    public WikiActionBeanContext()
    {
        super();
    }

    /**
     * Returns the WikiEngine associated with this WikiActionBeanContext.
     * 
     * @return the wiki engine
     */
    public WikiEngine getWikiEngine()
    {
        return m_engine;
    }

    /**
     * Returns the WikiSession associated with this WikiActionBeanContext.
     */
    public WikiSession getWikiSession()
    {
        return m_wikiSession;
    }

    /**
     * Calls the superclass
     * {@link ActionBeanContext#setServletContext(ServletContext)} and lazily
     * sets the internal WikiEngine reference, if still <code>null</code>.
     * 
     * @param servletContext the servlet context
     */
    @Override
    public void setServletContext( ServletContext servletContext )
    {
        super.setServletContext( servletContext );
        if( m_engine == null )
        {
            WikiEngine engine = WikiEngine.getInstance( servletContext, null );
            setWikiEngine( engine );
        }
    }

    /**
     * Sets the WikiEngine associated with this WikiActionBeanContext.
     * 
     * @param engine the wiki engine
     */
    public void setWikiEngine( WikiEngine engine )
    {
        m_engine = engine;
    }

    /**
     * Sets the WikiSession associated with this WikiActionBeanContext.
     * 
     * @param session the wiki session
     */
    public void setWikiSession( WikiSession session )
    {
        m_wikiSession = session;
    }

    /**
     * Adds a supplied ActionBean to "flash scope" so that it can be used by the next
     * HttpRequest. When this method is called, the ActionBean is stashed in the
     * request and the flash scope as attributes. For both, the bean is stored
     * under names {@link WikiActionBeanFactory#ATTR_ACTIONBEAN}
     * and {@link WikiTagBase#ATTR_CONTEXT}. This method assumes that the
     * method {@link #setRequest(HttpServletRequest)} has been previously called.
     * @param actionBean the action bean to add
     * @throws IllegalStateException if the request object has not been previously set
     * for this ActionBeanContext
     */
    public void flash( WikiActionBean actionBean )
    {
        if ( getRequest() == null )
        {
            throw new IllegalStateException( "Request not set! Cannot flash action bean." );
        }
        FlashScope flash = FlashScope.getCurrent( getRequest(), true);
        flash.put( actionBean );
        flash.put( WikiActionBeanFactory.ATTR_ACTIONBEAN, actionBean );
        
        // If not a WikiContext, synthesize a fake one
        WikiPage page = m_engine.getPage( m_engine.getFrontPage() );
        actionBean = m_engine.getWikiActionBeanFactory().newViewActionBean( getRequest(), getResponse(), page );
        
        // Stash the WikiContext
        flash.put( WikiTagBase.ATTR_CONTEXT, actionBean );
    }
    
}
