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
import javax.servlet.http.HttpSession;

import org.apache.wiki.WikiContext;
import org.apache.wiki.auth.permissions.PagePermission;
import org.apache.wiki.filters.SpamFilter;
import org.apache.wiki.ui.EditorManager;
import org.apache.wiki.ui.stripes.HandlerPermission;
import org.apache.wiki.ui.stripes.WikiRequestContext;

import net.sourceforge.stripes.action.*;


@HttpCache( allow = false )
@UrlBinding( "/Edit.jsp" )
public class EditActionBean extends AbstractPageActionBean
{
    @DefaultHandler
    @HandlesEvent( "edit" )
    @HandlerPermission( permissionClass = PagePermission.class, target = "${page.qualifiedName}", actions = PagePermission.EDIT_ACTION )
    @WikiRequestContext( "edit" )
    public Resolution edit()
    {
        return null;
    }

    /**
     * Event that extracts the current state of the edited page from the HTTP
     * session and redirects the user to the previewer JSP.
     * 
     * @return a forward resolution back to the preview page.
     */
    @HandlesEvent( "preview" )
    @HandlerPermission( permissionClass = PagePermission.class, target = "${page.qualifiedName}", actions = PagePermission.VIEW_ACTION )
    @WikiRequestContext( "preview" )
    public Resolution preview()
    {
        WikiContext context = getContext();
        HttpServletRequest request = context.getHttpRequest();
        HttpSession session = request.getSession();

        request.setAttribute( EditorManager.ATTR_EDITEDTEXT, session.getAttribute( EditorManager.REQ_EDITEDTEXT ) );

        String lastchange = SpamFilter.getSpamHash( context.getPage(), request );
        request.setAttribute( "lastchange", lastchange );

        return new ForwardResolution( "/Preview.jsp" );
    }

    /**
     * Event that diffs the current state of the edited page and forwards the
     * user to the diff JSP.
     * 
     * @return a forward resolution back to the preview page.
     */
    @WikiRequestContext( "diff" )
    @HandlesEvent( "diff" )
    @HandlerPermission( permissionClass = PagePermission.class, target = "${page.qualifiedName}", actions = PagePermission.VIEW_ACTION )
    public Resolution diff()
    {
        return new ForwardResolution( "/Diff.jsp" );
    }
}
