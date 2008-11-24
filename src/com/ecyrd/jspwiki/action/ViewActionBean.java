package com.ecyrd.jspwiki.action;

import net.sourceforge.stripes.action.*;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.validation.Validate;
import net.sourceforge.stripes.validation.ValidationError;
import net.sourceforge.stripes.validation.ValidationErrors;

import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiException;
import com.ecyrd.jspwiki.WikiPage;
import com.ecyrd.jspwiki.auth.permissions.PagePermission;

/**
 * Displays the wiki page a users requested, resolving special page names and
 * redirecting if needed.
 * @author Andrew Jaquith
 *
 */
@UrlBinding("/Wiki.action")
public class ViewActionBean extends AbstractActionBean
{
    private Logger log = Logger.getLogger(ViewActionBean.class);
    
    private WikiPage m_page = null;

    public ViewActionBean()
    {
        super();
    }

    /**
     * Returns the WikiPage; defaults to <code>null</code>.
     * @return the page
     */
    public WikiPage getPage()
    {
        return m_page;
    }

    /**
     * <p>After the binding and validation  {@link LifecycleStage#BindingAndValidation}
     * lifecycle stage executes, this method determines whether the
     * page name specified in the request is actually a special page and
     * redirects the user if needed. If no page was specified in the request, this method
     * sets the wiki page to the main page.</p>
     * <p>For cases where the user specifies a page, JSPWiki needs to determine
     * what page the user is really going to; that is, either an existing page, an alias
     * for one, or a "special page" reference. This method considers
     * special page names from <code>jspwiki.properties</code>, and possible aliases.
     * To determine whether the page is a special page, this method calls
     *  {@link com.ecyrd.jspwiki.action.WikiActionBeanFactory#getSpecialPageReference(String)}.
     *  @return a {@link net.sourceforge.stripes.action.RedirectResolution} to the special
     *  page's real URL, if a special page was specified, or <code>null</code> otherwise
     */
    @After(stages=LifecycleStage.BindingAndValidation)
    public Resolution resolvePage() throws WikiException
    {
        WikiPage page = getPage();
        ValidationErrors errors = this.getContext().getValidationErrors();
        WikiEngine engine = getContext().getEngine();
        
        // If user supplied a page that doesn't exist, redirect to the "create pages" ActionBean
        if ( errors.get("page" )!= null )
        {
            for (ValidationError pageParamError : errors.get("page"))
            {
                if ( "page".equals(pageParamError.getFieldName()) )
                {
                    String newPage = pageParamError.getFieldValue();
                    log.info("User supplied page name '" + newPage + "' that doesn't exist; redirecting to create pages JSP." );
                    RedirectResolution resolution = new RedirectResolution(NewPageActionBean.class);
                    resolution.addParameter("page", newPage);
                    return resolution;
                }
            }
        }

        // If page not supplied, try retrieving the front page to avoid NPEs
        if (page == null)
        {
            if ( log.isDebugEnabled() )
            {
                log.debug("User did not supply a page name: defaulting to front page.");
            }
            if ( engine != null )
            {
                // Bind the front page to the action bean
                page = engine.getPage( engine.getFrontPage() );
                setPage(page);
                return null;
            }
        }

        // If page still missing, it's an error condition
        if ( page == null )
        {
            throw new WikiException("Page not supplied, and WikiEngine does not define a front page! This is highly unusual.") ;
        }
        
        // Ok, the user supplied a page. That's nice. But is it a special page?
        String pageName = page.getName();
        String specialUrl = getContext().getEngine().getWikiActionBeanFactory().getSpecialPageReference( pageName );
        if ( specialUrl != null )
        {
            return new RedirectResolution( getContext().getViewURL( specialUrl ) );
        }

        // Is there an ALIAS attribute in the wiki pge?
        specialUrl = (String)page.getAttribute( WikiPage.ALIAS );
        if( specialUrl != null )
        {
            return new RedirectResolution( getContext().getViewURL( specialUrl ) );
        }
        
        // Is there a REDIRECT attribute in the wiki page?
        specialUrl = (String)page.getAttribute( WikiPage.REDIRECT );
        if( specialUrl != null )
        {
            return new RedirectResolution( getContext().getViewURL( specialUrl ) );
        }
        
        // If we got this far, it means the user supplied a page parameter, AND it exists
        return null;
    }

    /**
     * Sets the page.
     * @param page the wiki page.
     */
    @Validate( required = false)
    public void setPage( WikiPage page )
    {
        m_page = page;
        getContext().setPage( page );
    }
    
    /**
     * Default handler that simply forwards the user back to the same page. 
     * Every ActionBean needs a default handler to function properly, so we use
     * this (very simple) one.
     * @return a forward resolution back to the same page
     */
    @DefaultHandler
    @HandlesEvent("view")
    @HandlerPermission(permissionClass=PagePermission.class, target="${page.qualifiedName}", actions=PagePermission.VIEW_ACTION)
    @WikiRequestContext("view")
    public Resolution view()
    {
        return new ForwardResolution(ViewActionBean.class);
    }
    
}
