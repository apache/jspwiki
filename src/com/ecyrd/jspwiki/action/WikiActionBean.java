package com.ecyrd.jspwiki.action;

import net.sourceforge.stripes.action.ActionBean;
import net.sourceforge.stripes.action.ActionBeanContext;

/**
 * ActionBean sub-interface.
 * @author Andrew Jaquith
 *
 */
public interface WikiActionBean extends ActionBean
{
    /**
     * Returns the ActionBeanContext for the WikiActionBean, using a co-variant
     * return type of WikiActionBeanContext. 
     */
    public WikiActionBeanContext getContext();

    /**
     * Sets the WikiActionBeanContext for the ActionBean. This method <em>should</em>
     * be called immediately after bean creation.
     */
    public void setContext(ActionBeanContext context);
}
