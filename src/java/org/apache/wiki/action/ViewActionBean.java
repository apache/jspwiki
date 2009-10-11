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

import java.net.URI;
import java.util.List;

import net.sourceforge.stripes.action.*;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.validation.LocalizableError;
import net.sourceforge.stripes.validation.Validate;
import net.sourceforge.stripes.validation.ValidationError;
import net.sourceforge.stripes.validation.ValidationErrors;

import org.apache.wiki.WikiEngine;
import org.apache.wiki.api.WikiException;
import org.apache.wiki.api.WikiPage;
import org.apache.wiki.auth.permissions.PagePermission;
import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;
import org.apache.wiki.ui.stripes.HandlerPermission;
import org.apache.wiki.ui.stripes.WikiRequestContext;

/**
 * Displays the wiki page a users requested, resolving special page names and
 * redirecting if needed.
 */
@UrlBinding( "/Wiki.action" )
public class ViewActionBean extends AbstractPageActionBean
{
    private static final Logger log = LoggerFactory.getLogger( ViewActionBean.class );

    private String m_renameTo = null;

    public ViewActionBean()
    {
        super();
    }

    /**
     * Handler that forwards to the page information display JSP
     * <code>/Attachments.jsp</code>.
     * 
     * @return a forward to the content template
     */
    @HandlesEvent( "attachments" )
    @HandlerPermission( permissionClass = PagePermission.class, target = "${page.path}", actions = PagePermission.VIEW_ACTION )
    public Resolution attachments()
    {
        return new ForwardResolution( "/Attachments.jsp" );
    }

    /**
     * Returns the name to rename the page to, or <code>null</code> if not
     * supplied.
     * 
     * @return the name to, if one was supplied as a parameter
     */
    public String getRenameTo()
    {
        return m_renameTo;
    }

    /**
     * Handler that forwards to the page information display JSP
     * <code>/PageInfo.jsp</code>.
     * 
     * @return a forward to the content template
     */
    @HandlesEvent( "info" )
    @HandlerPermission( permissionClass = PagePermission.class, target = "${page.path}", actions = PagePermission.VIEW_ACTION )
    @WikiRequestContext( "info" )
    public Resolution info()
    {
        return new ForwardResolution( "/PageInfo.jsp" );
    }

    /**
     * <p>
     * After the binding and validation
     * {@link LifecycleStage#BindingAndValidation} lifecycle stage executes,
     * this method determines whether the page name specified in the request is
     * actually a special page and redirects the user if needed. If no page was
     * specified in the request, this method sets the wiki page to the main
     * page.
     * </p>
     * <p>
     * For cases where the user specifies a page, JSPWiki needs to determine
     * what page the user is really going to; that is, either an existing page,
     * an alias for one, or a "special page" reference. This method considers
     * special page names from <code>jspwiki.properties</code>, and possible
     * aliases. To determine whether the page is a special page, this method
     * calls
     * {@link org.apache.wiki.content.resolver.SpecialPageNameResolver#getSpecialPageURI(String)}.
     * 
     * @return a {@link net.sourceforge.stripes.action.RedirectResolution} to
     *         the special page's real URL, if a special page was specified, or
     *         <code>null</code> otherwise
     */
    @After( stages = LifecycleStage.BindingAndValidation )
    public Resolution resolvePage() throws WikiException
    {
        WikiEngine engine = getContext().getEngine();

        if( isSpecialPageView() )
        {
            // The page might be null because it's a special page
            // WikiPageTypeConverter
            // refused to convert. If so, redirect.
            String pageName = getContext().getRequest().getParameter( "page" );
            if( pageName != null )
            {
                URI uri = getContext().getEngine().getSpecialPageReference( pageName );
                if( uri != null )
                {
                    return new RedirectResolution( uri.toString() );
                }
                else
                {
                    throw new WikiException( "Wiki page name " + pageName + " didn't parse. This is highly unusual." );
                }
            }

            // The user forget to supply a page name. Go to front page.
            if( log.isDebugEnabled() )
            {
                log.debug( "User did not supply a page name: defaulting to front page." );
            }
            setPage( engine.getFrontPage( null ) );
        }

        // If page still missing, it's an error condition
        if( getPage() == null )
        {
            throw new WikiException( "Page not supplied, and WikiEngine does not define a front page! This is highly unusual." );
        }

        // Is there an ALIAS attribute in the wiki page?
        String specialUrl = (String) getPage().getAttribute( WikiPage.ALIAS );
        if( specialUrl != null )
        {
            return new RedirectResolution( getContext().getViewURL( specialUrl ) );
        }

        // Is there a REDIRECT attribute in the wiki page?
        specialUrl = (String) getPage().getAttribute( WikiPage.REDIRECT );
        if( specialUrl != null )
        {
            return new RedirectResolution( getContext().getViewURL( specialUrl ) );
        }

        // Ok, the page exists. If attachment, make sure it's directed to the
        // "info" handler
        WikiPage page = getPage();
        String handler = getContext().getEventName();
        if( getPage().isAttachment() && !"info".equals( handler ) )
        {
            return new RedirectResolution( ViewActionBean.class, "info" ).addParameter( "page", page.getPath().toString() );
        }

        return null;
    }

    /**
     * {@inheritDoc}. This method overrides the superclass method by disabling
     * validation of the <code>page</code> field.
     */
    @Override
    @Validate( required = false )
    public void setPage( WikiPage page )
    {
        super.setPage( page );
    }

    /**
     * Sets the name to rename the page to
     * 
     * @param renameTo the page name to use
     */
    public void setRenameTo( String renameTo )
    {
        m_renameTo = renameTo;
    }

    /**
     * Default handler that simply forwards the user back to the display JSP
     * <code>/Wiki.jsp</code>. Every ActionBean needs a default handler to
     * function properly, so we use this (very simple) one.
     * 
     * @return a forward to the content template
     */
    @DefaultHandler
    @DontValidate
    @HandlesEvent( "view" )
    @HandlerPermission( permissionClass = PagePermission.class, target = "${page.path}", actions = PagePermission.VIEW_ACTION )
    @WikiRequestContext( "view" )
    public Resolution view()
    {
        return new ForwardResolution( "/Wiki.jsp" );
    }

    /**
     * Returns {@code true} if the WikiPageTypeConverter, upon converting the
     * page, determined that the page name parameter actually referred to a
     * special page.
     * 
     * @return {@code true} if the {@code page} parameter referred to a special
     *         page; {@code false} otherwise.
     */
    private boolean isSpecialPageView()
    {
        ValidationErrors errors = getContext().getValidationErrors();
        List<ValidationError> fieldErrors = errors.get( "page" );
        if( fieldErrors == null )
        {
            return false;
        }
        for( ValidationError error : fieldErrors )
        {
            if( error instanceof LocalizableError )
            {
                if( "edit.specialPage".equals( ((LocalizableError) error).getMessageKey() ) )
                {
                    return true;
                }
            }
        }
        return false;
    }
}
