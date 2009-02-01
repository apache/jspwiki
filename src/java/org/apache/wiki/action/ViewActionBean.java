/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.  
 */

package org.apache.wiki.action;

import net.sourceforge.stripes.action.*;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.validation.ValidationError;
import net.sourceforge.stripes.validation.ValidationErrors;

import org.apache.wiki.WikiEngine;
import org.apache.wiki.api.WikiException;
import org.apache.wiki.api.WikiPage;
import org.apache.wiki.auth.permissions.PagePermission;
import org.apache.wiki.content.ContentManager;
import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;
import org.apache.wiki.ui.stripes.HandlerPermission;
import org.apache.wiki.ui.stripes.WikiRequestContext;


/**
 * Displays the wiki page a users requested, resolving special page names and
 * redirecting if needed.
 * @author Andrew Jaquith
 *
 */
@UrlBinding("/Wiki.action")
public class ViewActionBean extends AbstractPageActionBean
{
    private Logger log = LoggerFactory.getLogger(ViewActionBean.class);

    public ViewActionBean()
    {
        super();
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
     *  {@link org.apache.wiki.action.WikiContextFactory#getSpecialPageResolution(String)}.
     *  @return a {@link net.sourceforge.stripes.action.RedirectResolution} to the special
     *  page's real URL, if a special page was specified, or <code>null</code> otherwise
     */
    @After(stages=LifecycleStage.BindingAndValidation)
    public Resolution resolvePage() throws WikiException
    {
        WikiPage page = getPage();
        ValidationErrors errors = this.getContext().getValidationErrors();
        WikiEngine engine = getContext().getEngine();
        
        // The user supplied a page that doesn't exist
        if ( errors.get("page" )!= null )
        {
            for (ValidationError pageParamError : errors.get("page"))
            {
                if ( "page".equals(pageParamError.getFieldName()) )
                {
                    String pageName = pageParamError.getFieldValue();
                    
                    // Is it a special page?
                    RedirectResolution resolution = getContext().getEngine().getWikiContextFactory().getSpecialPageResolution( pageName );
                    if ( resolution != null )
                    {
                        return resolution;
                    }

                    // Ok, it really doesn't exist. Send 'em to the "Create new page?" JSP
                    log.info("User supplied page name '" + pageName + "' that doesn't exist; redirecting to create pages JSP." );
                    return new RedirectResolution(NewPageActionBean.class).addParameter("page", pageName);
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
                if ( page == null )
                {
                    page = engine.getFrontPage(ContentManager.DEFAULT_SPACE);
                }
                setPage(page);
                return null;
            }
        }

        // If page still missing, it's an error condition
        if ( page == null )
        {
            throw new WikiException("Page not supplied, and WikiEngine does not define a front page! This is highly unusual.") ;
        }
        
        // Is there an ALIAS attribute in the wiki pge?
        String specialUrl = (String)page.getAttribute( WikiPage.ALIAS );
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
     * Default handler that simply forwards the user back to the display JSP <code>/Wiki.jsp</code>. 
     * Every ActionBean needs a default handler to function properly, so we use
     * this (very simple) one.
     * @return a forward to the content template
     */
    @DefaultHandler
    @DontValidate
    @HandlesEvent("view")
    @HandlerPermission(permissionClass=PagePermission.class, target="${page.qualifiedName}", actions=PagePermission.VIEW_ACTION)
    @WikiRequestContext("view")
    public Resolution view()
    {
        return new ForwardResolution( "/Wiki.jsp");
    }
    
    /**
     * Handler that forwards to the page information display JSP <code>/PageInfo.jsp</code>.
     * @return a forward to the content template
     */
    @HandlesEvent("info")
    @HandlerPermission(permissionClass=PagePermission.class, target="${page.qualifiedName}", actions=PagePermission.VIEW_ACTION)
    @WikiRequestContext("info")
    public Resolution info()
    {
        return new ForwardResolution( "/PageInfo.jsp");
    }

}
