package com.ecyrd.jspwiki.ui.stripes;

import java.security.Principal;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import net.sourceforge.stripes.action.ActionBeanContext;
import net.sourceforge.stripes.action.ForwardResolution;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiPage;
import com.ecyrd.jspwiki.WikiSession;
import com.ecyrd.jspwiki.action.WikiActionBean;
import com.ecyrd.jspwiki.ui.TemplateManager;

/**
 * <p>
 * {@link net.sourceforge.stripes.action.ActionBeanContext} subclass that
 * implements the {@link com.ecyrd.jspwiki.WikiContext} interface by wrapping
 * a {@link DefaultWikiContext} delegate. WikiActionBeanContext  maintains references to the current 
 * JSPWiki WikiEngine and the user's HttpServletRequest and WikiSession.
 * </p>
 * <p>
 * When the WikiActionBeanContext is created, callers <em>must</em> set the
 * WikiEngine reference by calling either {@link #setEngine(WikiEngine)}
 * (which sets it directly), or {@link #setServletContext(ServletContext)}
 * (which sets it lazily). when {@link #setServletContext(ServletContext)}. The
 * HttpServletRequest reference is set via
 * {@link #setRequest(HttpServletRequest)}.
 * </p>
 * 
 * @author Andrew Jaquith
 */
public class WikiActionBeanContext extends ActionBeanContext implements WikiContext
{
    private DefaultWikiContext m_delegate;

    /**
     * Constructs a new WikiActionBeanContext.
     */
    public WikiActionBeanContext()
    {
        super();
        m_delegate = new DefaultWikiContext();      // Initialize the delegate
    }

    /**
     * Returns the request context for this ActionBean by looking up the value
     * of the annotation {@link WikiRequestContext} associated with the current
     * event handler method for this ActionBean. The current event handler is
     * obtained from {@link WikiActionBeanContext#getEventName()}. Note that if
     * this ActionBean does not have a a current event handler assigned, or if
     * the event handler method does not contain the WikiRequestContext
     * annotation, this method will return
     * {@link com.ecyrd.jspwiki.WikiContext#NONE}.
     */
    public String getRequestContext()
    {
        return m_delegate.getRequestContext();
    }

    /**
     * Sets the request context. See above for the different request contexts
     * (VIEW, EDIT, etc.) This argument must correspond exactly to the value of
     * a Stripes event handler method's
     * {@link com.ecyrd.jspwiki.ui.stripes.WikiRequestContext} annotation for the
     * bean class. For event handlers that do not have an
     * {@linkplain com.ecyrd.jspwiki.ui.stripes.WikiRequestContext} annotation,
     * callers can supply a request context value based on the bean class and
     * the event name; see the
     * {@link com.ecyrd.jspwiki.ui.stripes.HandlerInfo#getRequestContext()}
     * documentation for more details.
     * 
     * @param arg The request context (one of the predefined contexts.)
     * @throws IllegalArgumentException if the supplied request context does not
     *             correspond to a
     *             {@linkplain com.ecyrd.jspwiki.ui.stripes.WikiRequestContext}
     *             annotation, or the automatically request context name
     */
    public void setRequestContext( String arg )
    {
        HandlerInfo handler = getEngine().getWikiContextFactory().findEventHandler( arg );
        setEventName( handler.getEventName() );
        m_delegate.setRequestContext( arg );
    }

    /**
     *  {@inheritDoc}.
     */
    @Override
    public void setEventName( String eventName )
    {
        super.setEventName( eventName );
    }

    /**
     *  {@inheritDoc}. Also calls {@link DefaultWikiContext#setHttpRequest(HttpServletRequest)} on
     *  the DefaultWikiContext delegate.
     */
    @Override
    public void setRequest( HttpServletRequest request )
    {
        super.setRequest( request );
        m_delegate.setHttpRequest( request );
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
        if( m_delegate.getEngine() == null )
        {
            WikiEngine engine = WikiEngine.getInstance( servletContext, null );
            m_delegate.setEngine( engine );
        }
    }

    /**
     *  Sets the WikiEngine by calling {@link DefaultWikiContext#setEngine(WikiEngine)} on
     *  the DefaultWikiContext delegate.
     */
    public void setEngine( WikiEngine engine )
    {
        m_delegate.setEngine( engine );
    }

    /**
     *  Sets the WikiSession by calling {@link DefaultWikiContext#setEngine(WikiEngine)} on
     *  the DefaultWikiContext delegate.
     */
    public void setWikiSession( WikiSession wikiSession )
    {
        m_delegate.setWikiSession( wikiSession );
    }
    
    /**
     *  {@inheritDoc}
     */
    public Object clone()
    {
        WikiActionBeanContext copy = new WikiActionBeanContext();
        copy.m_delegate = m_delegate;
        copy.setEngine( getEngine() );
        copy.setEventName( getEventName() );
        copy.setRequest( getRequest() );
        copy.setResponse( getResponse() );
        copy.setServletContext( getServletContext() );
        copy.setValidationErrors( getValidationErrors() );
        return copy;
    }

    /**
     *  {@inheritDoc}
     */
    public WikiContext deepClone()
    {
        WikiActionBeanContext copy = new WikiActionBeanContext();
        copy.m_delegate = (DefaultWikiContext) m_delegate.deepClone();
        copy.setEngine( getEngine() );
        copy.setEventName( getEventName() );
        copy.setRequest( getRequest() );
        copy.setResponse( getResponse() );
        copy.setServletContext( getServletContext() );
        copy.setValidationErrors( getValidationErrors() );
        return copy;
    }

    /**
     *  {@inheritDoc}
     */
    public ResourceBundle getBundle( String bundle ) throws MissingResourceException
    {
        return m_delegate.getBundle( bundle );
    }

    /**
     * Returns a Stripes ForwardResolution for the full path to the resolved content page.
     * @return the resolution
     */
    public ForwardResolution getContentPage( WikiActionBean actionBean, TemplateManager.Template template, String contentJsp )
    {
        TemplateManager mgr = getEngine().getTemplateManager();
        WikiActionBeanContext wikiContext = actionBean.getContext();
        wikiContext.setVariable( "contentTemplate", contentJsp );
        String contentPage = mgr.findResource( wikiContext, wikiContext.getTemplate(), template.template() );
        return new ForwardResolution( contentPage );
    }

    /**
     *  {@inheritDoc}
     */
    public Principal getCurrentUser()
    {
        return m_delegate.getCurrentUser();
    }

    /**
     *  {@inheritDoc}
     */
    public WikiEngine getEngine()
    {
        return m_delegate.getEngine();
    }

    /**
     *  {@inheritDoc}
     */
    public String getHttpParameter( String paramName )
    {
        return m_delegate.getHttpParameter( paramName );
    }

    /**
     *  {@inheritDoc}
     */
    public HttpServletRequest getHttpRequest()
    {
        return m_delegate.getHttpRequest();
    }

    /**
     *  {@inheritDoc}
     */
    public WikiPage getPage()
    {
        return m_delegate.getPage();
    }

    /**
     *  {@inheritDoc}
     */
    public WikiPage getRealPage()
    {
        return m_delegate.getRealPage();
    }

    /**
     *  {@inheritDoc}
     */
    public String getTemplate()
    {
        return m_delegate.getTemplate();
    }

    /**
     *  {@inheritDoc}
     */
    public String getURL( String context, String page )
    {
        return m_delegate.getURL( context, page );
    }

    /**
     *  {@inheritDoc}
     */
    public String getURL( String context, String page, String params )
    {
        return m_delegate.getURL( context, page, params );
    }

    /**
     *  {@inheritDoc}
     */
    public Object getVariable( String key )
    {
        return m_delegate.getVariable( key );
    }

    /**
     *  {@inheritDoc}
     */
    public String getViewURL( String page )
    {
        return m_delegate.getViewURL( page );
    }

    /**
     *  {@inheritDoc}
     */
    public WikiSession getWikiSession()
    {
        return m_delegate.getWikiSession();
    }

    /**
     *  {@inheritDoc}
     */
    public boolean hasAdminPermissions()
    {
        return m_delegate.hasAdminPermissions();
    }

    /**
     *  {@inheritDoc}
     */
    public void setPage( WikiPage page )
    {
        m_delegate.setPage( page );
    }

    /**
     *  {@inheritDoc}
     */
    public WikiPage setRealPage( WikiPage page )
    {
        return m_delegate.setRealPage( page );
    }

    /**
     *  {@inheritDoc}
     */
    public void setTemplate( String dir )
    {
        m_delegate.setTemplate( dir );
    }

    /**
     *  {@inheritDoc}
     */
    public void setVariable( String key, Object data )
    {
        m_delegate.setVariable( key, data );
    }
    
}
