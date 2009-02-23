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

import javax.servlet.http.HttpServletRequest;

import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.RedirectResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.validation.*;

import org.apache.wiki.PageManager;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.api.WikiPage;
import org.apache.wiki.attachment.Attachment;
import org.apache.wiki.auth.permissions.PagePermission;
import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;
import org.apache.wiki.providers.ProviderException;
import org.apache.wiki.ui.stripes.HandlerPermission;
import org.apache.wiki.ui.stripes.WikiRequestContext;

/**
 * ActionBean for deleting a wiki page or attachment, optionally for a specific
 * version.
 */
@UrlBinding( "/Delete.jsp" )
public class DeleteActionBean extends AbstractPageActionBean
{
    private Logger log = LoggerFactory.getLogger( DeleteActionBean.class );

    private int m_version = Integer.MIN_VALUE;

    /**
     * Event handler method that deletes the wiki page or attachment.
     * 
     * @return a RedirectResolution to the parent page (if the item to be
     *         deleted was an attachment); the front page (if the item to be
     *         deleted was all versions of a wiki page); or the wiki page (if
     *         just a single version was deleted).
     * @throws ProviderException if the delete failed for any reason
     */
    @HandlesEvent( "delete" )
    @HandlerPermission( permissionClass = PagePermission.class, target = "${page.qualifiedName}", actions = PagePermission.DELETE_ACTION )
    @WikiRequestContext( "del" )
    public Resolution delete() throws ProviderException
    {
        // If all versions of a page or attachment should be deleted, redirect
        // to the main page (for page) or parent page (for attachment)
        WikiEngine engine = getContext().getEngine();
        String pageName = m_page.getName();
        if( m_version == Integer.MIN_VALUE )
        {
            HttpServletRequest request = getContext().getRequest();
            log.info( "Deleting page " + pageName + ". User=" + request.getRemoteUser() + ", host="
                      + request.getRemoteAddr() );
            engine.deletePage( pageName );
        }

        // Just delete a single version
        else
        {
            WikiPage p = engine.getPage( pageName, m_version );
            log.debug( "Deleting page=" + pageName + ", version=" + m_version );
            engine.deleteVersion( p );
        }

        // If attachment deleted; always redirect to parent page
        if( m_page instanceof Attachment )
        {
            String redirPage = ((Attachment) m_page).getParentName();
            return new RedirectResolution( ViewActionBean.class, "view" ).addParameter( "page", redirPage );
        }

        // If no more versions left, redirect to main page, otherwise INFO page
        String redirPage = engine.pageExists( pageName ) ? pageName : engine.getFrontPage();
        return new RedirectResolution( ViewActionBean.class, "view" ).addParameter( "page", redirPage );
    }

    /**
     * Returns the version to delete
     * 
     * @return the version
     */
    public int getVersion()
    {
        return m_version;
    }

    /**
     * {@inheritDoc}
     */
    @Validate( required = true )
    public void setPage( WikiPage page )
    {
        super.setPage( page );
    }
    
    /**
     * Sets the version to delete. If not set, all versions of the page or
     * attachment will be deleted.
     * 
     * @param version the version
     */
    public void setVersion( int version )
    {
        m_version = version;
    }

    /**
     * Validates the version number of the page or attachment that should be
     * deleted. If the version was not set, or is a valid version number,
     * validation succeeds. Otherwise, if the version number supplied does not
     * exist, validation fails.
     */
    @ValidationMethod( when = ValidationState.ALWAYS )
    public void validateBeforeDelete() throws ProviderException
    {
        // If no version number supplied, always succeeds.
        if( m_version == Integer.MIN_VALUE )
        {
            return;
        }

        // If version supplied exists, validation succeeds also.
        WikiEngine engine = getContext().getEngine();
        PageManager pm = engine.getPageManager();
        if( pm.pageExists( getPage().getName(), m_version ) )
        {
            // While we're at it, set the correct version for the bean
            m_page = engine.getPage( getPage().getName(), m_version );
            return;
        }

        // Oops! User supplied an invalid version
        ValidationErrors errors = getContext().getValidationErrors();
        errors.add( "version", new LocalizableError( "version.invalid" ) );
    }

}
