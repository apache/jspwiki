package com.ecyrd.jspwiki.action;

import java.security.Principal;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.servlet.http.HttpServletRequest;

import net.sourceforge.stripes.action.ActionBean;
import net.sourceforge.stripes.action.ActionBeanContext;

import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiSession;

/**
 * ActionBean sub-interface that includes getter/setter methods used by JSPWiki, including the
 * request context, skin, template, URL pattern, wiki session and wiki engine.
 * @author Andrew Jaquith
 *
 */
public interface WikiActionBean extends ActionBean
{
    
    /**
     * Returns the ActionBeanContext for the WikiActionBean, using a co-variant
     * return type of WikiActionBeanContext. 
     */
    public abstract WikiActionBeanContext getContext();

    /**
     * Convenience method that gets the current user. Delegates the lookup to
     * the WikiSession associated with this WikiActionBean. May return null, in
     * case the current user has not yet been determined; or this is an internal
     * system. If the WikiSession has not been set, <em>always</em> returns
     * null.
     */
    public abstract Principal getCurrentUser();
    
    /**
     * Sets the WikiActionBeanContext for the ActionBean. This method <em>should</em>
     * be called immediately after bean creation.
     */
    public void setContext(ActionBeanContext context);

    /**
     * Returns the WikiEngine, which may be <code>null</code> if this instance
     * was created without invoking the WikiActionBeanContext methods
     * {@link WikiActionBeanContext#setRequest(HttpServletRequest)} or
     * {@link WikiActionBeanContext#setServletContext(javax.servlet.ServletContext)}.
     */
    public abstract WikiEngine getEngine();

    /**
     * Returns the request context for the WikiActionBean. By convention,
     * this method will return the string value of the annotation 
     * {@link WikiRequestContext}, which is required for all concrete WikiActionBean 
     * classes.
     * @deprecated perform <code>instanceof</code> comparisons instead
     */
    public abstract String getRequestContext();

    /**
     * Returns the "skin" to be used for this ActionBean.
     * 
     * @return the skin
     */
    public abstract String getSkin();

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
    public abstract String getTemplate();

    /**
     * Returns the WikiSession associated with the context. This method is
     * guaranteed to always return a valid WikiSession. If this context was
     * constructed without an associated HttpServletRequest, it will return
     * {@link WikiSession#guestSession(WikiEngine)}.
     */
    public abstract WikiSession getWikiSession();

    /**
     * Sets the skin to be used with this ActionBean. This value will override
     * the template returned by {@link #getTemplate()}. Normally, this method
     * is invoked by Stripes when binding request parameters to the ActionBean.
     * 
     * @param skin
     *            the skin to use
     */
    public abstract void setSkin(String skin);

    /**
     * Sets the template to be used for this request.
     * 
     * @since 2.1.15.
     */
    public abstract void setTemplate(String dir);

    /**
     *  Gets a previously set variable.
     *
     *  @param key The variable name.
     *  @return The variable contents.
     */
    public Object getVariable(String key);

    /**
     *  Sets a variable.  The variable is valid while the WikiContext is valid,
     *  i.e. while page processing continues.  The variable data is discarded
     *  once the page processing is finished.
     *
     *  @param key The variable name.
     *  @param data The variable value.
     */
    public void setVariable(String key, Object data);

    
    // FIXME: This method should really cache the ResourceBundles or something...
    public ResourceBundle getBundle( String bundle ) throws MissingResourceException;
}
