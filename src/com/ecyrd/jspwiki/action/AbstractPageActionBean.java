package com.ecyrd.jspwiki.action;

import net.sourceforge.stripes.validation.Validate;

import org.apache.jspwiki.api.WikiPage;

/**
 * Abstract {@link WikiActionBean} subclass used by all ActionBeans that use and
 * process {@link org.apache.jspwiki.api.WikiPage} objects bound to the
 * <code>page</code> request parameter. In particular, this subclass contains
 * special processing logic that ensures that, the <code>page</code>
 * properties of this object and its related
 * {@link com.ecyrd.jspwiki.WikiContext} are set to the same value. When
 * {@link #setPage(WikiPage)} is called by, for example, the Stripes controller,
 * the underlying
 * {@link com.ecyrd.jspwiki.ui.stripes.WikiActionBeanContext#setPage(WikiPage)}
 * method is called also.
 */
public class AbstractPageActionBean extends AbstractActionBean
{
    protected WikiPage m_page = null;

    /**
     * Returns the WikiPage; defaults to <code>null</code>.
     * 
     * @return the page
     */
    public WikiPage getPage()
    {
        return m_page;
    }

    /**
     * Sets the WikiPage property for this ActionBean, and also sets the
     * WikiActionBeanContext's page property to the same value by calling
     * {@link com.ecyrd.jspwiki.ui.stripes.WikiActionBeanContext#setPage(WikiPage)}.
     * 
     * @param page the wiki page.
     */
    @Validate( required = false )
    public void setPage( WikiPage page )
    {
        m_page = page;
        getContext().setPage( page );
    }

}
