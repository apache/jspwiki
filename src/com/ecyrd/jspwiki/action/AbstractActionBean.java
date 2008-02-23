package com.ecyrd.jspwiki.action;

import java.security.Principal;
import java.util.*;

import javax.servlet.http.HttpServletRequest;

import net.sourceforge.stripes.action.ActionBeanContext;

import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiSession;

/**
 * <p>Abstract ActionBean superclass for all wiki actions, such as page actions ({@link com.ecyrd.jspwiki.WikiContext} and subclasses),
 * group actions (e.g., {@link ViewGroupActionBean}), user actions (e.g., {@link UserPreferencesActionBean}) and others.</p>
 * 
 * 
 * @author Andrew Jaquith
 */
@WikiRequestContext("none")
public abstract class AbstractActionBean implements WikiActionBean
{
    protected  Map<String,Object> m_variableMap = new HashMap<String,Object>();
    
    private WikiActionBeanContext m_actionBeanContext = null;

    /**
     * The JSP for this WikiContext.
     */
    private String m_skin = null;

    private String m_template = null;
    
    private static final String DEFAULT_TEMPLATE = "default";
    
    private final String m_requestContext;
    
    /**
     * Creates a new instance of this class, without a WikiEngine, Request or
     * WikiPage.
     */
    protected AbstractActionBean()
    {
        super();
        m_requestContext = getClass().getAnnotation( WikiRequestContext.class ).value();
    }

    /**
     * Returns the content template for this ActionBean.
     */
    public final String getContentTemplate()
    {
        return this.getClass().getAnnotation(WikiRequestContext.class).value();
    }

    /**
     * Returns the Stripes ActionBeanContext associated this WikiContext, lazily
     * creating one if necessary.
     * 
     * @throws IllegalStateException
     */
    public WikiActionBeanContext getContext()
    {
        if (m_actionBeanContext == null)
        {
            setContext(new WikiActionBeanContext());
        }
        return m_actionBeanContext;
    }

    /**
     * Convenience method that gets the current user. Delegates the lookup to
     * the WikiSession associated with this WikiContect. May return null, in
     * case the current user has not yet been determined; or this is an internal
     * system. If the WikiSession has not been set, <em>always</em> returns
     * null.
     */
    public Principal getCurrentUser()
    {
        return getWikiSession().getUserPrincipal();
    }

    /**
     * Returns the WikiEngine, which may be <code>null</code> if this instance
     * was created without invoking the WikiActionBeanContext methods
     * {@link WikiActionBeanContext#setRequest(HttpServletRequest)} or
     * {@link WikiActionBeanContext#setServletContext(javax.servlet.ServletContext)}.
     */
    public WikiEngine getEngine()
    {
        return getContext().getWikiEngine();
    }

    /**
     * Returns the request context for this ActionBean by looking up the 
     * value of the annotation {@link WikiRequestContext}. Note that if the
     * annotation is not present, this method will return {@link ecyrd.jspwiki.WikiContext#NONE}.
     */
    public String getRequestContext()
    {
        return m_requestContext;
    }
    
    /**
     * Returns the "skin" to be used for this ActionBean.
     * 
     * @return the skin
     */
    public String getSkin()
    {
        return m_skin;
    }

    /**
     * <p>
     * Gets the template that is to be used throughout this request. The value
     * returned depends on the whether the current HTTP request has supplied a
     * custom skin or template name. In order of preference:
     * </p>
     * <ul>
     * <li>The "skin", if set by {@link #setSkin(String)} or if the HTTP
     * parameter <code>skin</code> was bound by Stripes</li>
     * <li>The template, if set by {@link #setTemplate(String)} or if the HTTP
     * parameter <code>template</code> was bound by Stripes</li>
     * <li>The WikiEngine's default template, as returned by
     * {@link WikiEngine#getTemplateDir()}</li>
     * <li>The value <code>default</code></li>
     * </ul>
     * 
     * @since 2.1.15.
     */
    public String getTemplate()
    {
        if ( m_skin != null ) 
        {
            return m_skin;
        }
        else if ( m_template != null )
        {
            return m_template;
        }
        else if ( getEngine() != null ) 
        {
            return getEngine().getTemplateDir();
        }
        return DEFAULT_TEMPLATE;
    }

    /**
     * Returns the WikiSession associated with the context. This method is
     * guaranteed to always return a valid WikiSession. If this context was
     * constructed without an associated HttpServletRequest, it will return
     * {@link WikiSession#guestSession(WikiEngine)}.
     */
    public WikiSession getWikiSession()
    {
        return getContext().getWikiSession();
    }

    /**
     *  Gets a previously set variable.
     *
     *  @param key The variable name.
     *  @return The variable contents.
     */
    public Object getVariable( String key )
    {
        return m_variableMap.get( key );
    }

    /**
     *  Sets a variable.  The variable is valid while the WikiContext is valid,
     *  i.e. while page processing continues.  The variable data is discarded
     *  once the page processing is finished.
     *
     *  @param key The variable name.
     *  @param data The variable value.
     */
    public void setVariable( String key, Object data )
    {
        m_variableMap.put( key, data );
    }

    /**
     * Sets the Stripes ActionBeanContext associated with this WikiContext. It
     * will also update the cached HttpRequest.
     */
    public void setContext(ActionBeanContext context)
    {
        m_actionBeanContext = ((WikiActionBeanContext) context);
    }

    /**
     * Sets the skin to be used with this ActionBean. This value will override
     * the template returned by {@link #getTemplate()}. Normally, this method
     * is invoked by Stripes when binding request parameters to the ActionBean.
     * 
     * @param skin
     *            the skin to use
     */
    public void setSkin(String skin)
    {
        m_skin = skin;
    }

    /**
     * Sets the template to be used for this request.
     * 
     * @since 2.1.15.
     */
    public void setTemplate(String dir)
    {
        m_template = dir;
    }
    
    /**
     *  Locates the i18n ResourceBundle given.  This method interprets
     *  the request locale, and uses that to figure out which language the
     *  user wants.
     *  @see com.ecyrd.jspwiki.i18n.InternationalizationManager
     *  @param bundle The name of the bundle you are looking for.
     *  @return A resource bundle object
     *  @throws MissingResourceException If the bundle cannot be found
     */
    // FIXME: This method should really cache the ResourceBundles or something...
    public ResourceBundle getBundle( String bundle ) throws MissingResourceException
    {
        Locale loc = null;

        if( m_actionBeanContext != null && m_actionBeanContext.getRequest() != null )
        {
            loc = m_actionBeanContext.getRequest().getLocale();
        }
        ResourceBundle b = getEngine().getInternationalizationManager().getBundle(bundle, loc);

        return b;
    }
}
