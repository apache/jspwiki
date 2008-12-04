package com.ecyrd.jspwiki.action;

import com.ecyrd.jspwiki.ui.stripes.WikiActionBeanContext;

import net.sourceforge.stripes.action.ActionBeanContext;

/**
 * <p>
 * Abstract ActionBean superclass for all wiki actions, such as page actions ({@link com.ecyrd.jspwiki.WikiContext}
 * and subclasses), group actions (e.g., {@link GroupActionBean}), user
 * actions (e.g., {@link UserPreferencesActionBean}) and others.
 * </p>
 * 
 * @author Andrew Jaquith
 */
public abstract class AbstractActionBean implements WikiActionBean
{
    private WikiActionBeanContext m_actionBeanContext = null;

    /**
     * Creates a new instance of this class, without a WikiEngine, Request or
     * WikiPage.
     */
    protected AbstractActionBean()
    {
        super();
    }

    /**
     * Returns the Stripes ActionBeanContext associated this WikiContext. This
     * method may return <code>null</code>, and callers should check for this
     * condition.
     * 
     * @throws IllegalStateException
     */
    public WikiActionBeanContext getContext()
    {
        return m_actionBeanContext;
    }

    /**
     * Sets the Stripes ActionBeanContext associated with this WikiContext. It
     * will also update the cached HttpRequest.
     */
    public void setContext( ActionBeanContext context )
    {
        m_actionBeanContext = ((WikiActionBeanContext) context);
    }

}
