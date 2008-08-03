package com.ecyrd.jspwiki.action;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import net.sourceforge.stripes.action.ActionBeanContext;

import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiSession;

/**
 * {@link net.sourceforge.stripes.action.ActionBeanContext} subclass that
 * contains a convenient reference to the current JSPWiki WikiEngine and the
 * user's HttpServletRequest. The WikiEngine reference is lazily initialized
 * when either {@link #setServletContext(ServletContext)} or
 * {@link #setRequest(HttpServletRequest)} is invoked. The HttpServletRequest
 * reference is set via {@link #setRequest(HttpServletRequest)}.
 * 
 * @author Andrew Jaquith
 */
public class WikiActionBeanContext extends ActionBeanContext
{
    private volatile WikiEngine m_engine = null;

    public WikiActionBeanContext()
    {
        super();
    }

    public WikiSession getWikiSession()
    {
        return WikiSession.getWikiSession( m_engine, getRequest() );
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
     * Returns the WikiEngine associated with this WikiActionBeanContext.
     * 
     * @return the wiki engine
     */
    public WikiEngine getWikiEngine()
    {
        return m_engine;
    }

    /**
     * Calls the superclass
     * {@link ActionBeanContext#setRequest(HttpServletRequest)} and lazily sets
     * the internal WikiEngine reference, if still <code>null</code>.
     * 
     * @param request the HTTP request
     */
    @Override
    public void setRequest( HttpServletRequest request )
    {
        super.setRequest( request );
        if( request != null )
        {
            // Lazily set the WikiEngine reference
            if( m_engine == null )
            {
                ServletContext servletContext = request.getSession().getServletContext();
                m_engine = WikiEngine.getInstance( servletContext, null );
            }
        }

        // Retrieve the WikiSession, which executes the login stack (if needed)
        WikiSession.getWikiSession( m_engine, request );
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

}
