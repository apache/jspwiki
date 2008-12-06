package com.ecyrd.jspwiki.action;

import net.sourceforge.stripes.validation.Validate;

import com.ecyrd.jspwiki.WikiPage;

/**
 * Abstract WikiActionBean subclass used by all ActionBeans that use and process
 * WikiPages using the <code>page</code> request parameter. In particular, this
 * subclass contains special processing logic that sets the page property
 * of the underlying {@link WikiActionBeanContext} when the {@link #setPage(WikiPage)}
 * method is called by the Stripes controller.
 */
public class AbstractPageActionBean extends AbstractActionBean
{
    protected WikiPage m_page = null;
    
    /**
     * Returns the WikiPage; defaults to <code>null</code>.
     * @return the page
     */
    public WikiPage getPage()
    {
        return m_page;
    }

    /**
     * Sets the WikiPage property for this ActionBean, and also
     * sets the WikiActionBeanContext's page property to the same
     * value by calling
     * {@link com.ecyrd.jspwiki.ui.stripes.WikiActionBeanContext#setPage(WikiPage)}.
     * @param page the wiki page.
     */
    @Validate( required = false)
    public void setPage( WikiPage page )
    {
        m_page = page;
        getContext().setPage( page );
    }
    
}
