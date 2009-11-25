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

import java.security.Permission;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.StreamingResolution;
import net.sourceforge.stripes.action.UrlBinding;

import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiSession;
import org.apache.wiki.auth.AuthorizationManager;
import org.apache.wiki.auth.permissions.PagePermission;
import org.apache.wiki.auth.permissions.PermissionFactory;
import org.apache.wiki.ui.stripes.HandlerPermission;
import org.apache.wiki.ui.stripes.WikiRequestContext;

@UrlBinding( "/rss.jsp" )
public class RSSActionBean extends AbstractActionBean
{
    @HandlesEvent( "rss" )
    @HandlerPermission( permissionClass = PagePermission.class, target = "${page.name}", actions = PagePermission.VIEW_ACTION )
    @WikiRequestContext( "rss" )
    public Resolution rss()
    {
        return null;
    }

    /**
     * Generates a StreamingResolution with the names and URLs of all pages the
     * user as has access to, following the SisterSites standard. This event
     * method respects ACLs on pages.
     * 
     * @see http://usemod.com/cgi-bin/mb.pl?SisterSitesImplementationGuide
     * @return
     */
    @HandlesEvent( "sisterSites" )
    public Resolution sisterSites()
    {
        Resolution r = new StreamingResolution( "text/plain; charset=UTF-8" )
        {
            @SuppressWarnings("deprecation")
            @Override
            protected void stream( HttpServletResponse response ) throws Exception
            {
                WikiEngine engine = getContext().getEngine();
                AuthorizationManager mgr = engine.getAuthorizationManager();
                WikiSession session = getContext().getWikiSession();
                Set<String> allPages = engine.getReferenceManager().findCreated();
                for( String page : allPages )
                {
                    if( page.indexOf( "/" ) == -1 )
                    {
                        Permission permission = PermissionFactory.getPagePermission( page, PagePermission.VIEW_ACTION );
                        if ( mgr.checkPermission( session, permission ) )
                        {
                            String url = engine.getViewURL( page );
                            response.getWriter().write( url + " " + page + "\n" );
                        }
                    }
                }
            }
            
        };
        return r;
    }
}
