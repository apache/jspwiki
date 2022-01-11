<%--
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
--%>

<%@ page import="java.text.*" %>
<%@ page import="java.util.*" %>
<%@ page import="org.apache.logging.log4j.Logger" %>
<%@ page import="org.apache.logging.log4j.LogManager" %>
<%@ page import="org.apache.wiki.api.core.*" %>
<%@ page import="org.apache.wiki.api.spi.Wiki" %>
<%@ page import="org.apache.wiki.attachment.AttachmentManager" %>
<%@ page import="org.apache.wiki.auth.AuthorizationManager" %>
<%@ page import="org.apache.wiki.auth.permissions.*" %>
<%@ page import="org.apache.wiki.pages.PageManager" %>
<%@ page import="org.apache.wiki.preferences.Preferences" %>
<%@ page import="org.apache.wiki.references.ReferenceManager" %>
<%@ page import="org.apache.wiki.rss.*" %>
<%@ page import="org.apache.wiki.util.*" %>
<%!
    Logger log = LogManager.getLogger("JSPWiki");
%>
<%
    /*
     *  This JSP creates support for the SisterSites standard, as specified by
     *  http://usemod.com/cgi-bin/mb.pl?SisterSitesImplementationGuide
     */
    Engine wiki = Wiki.engine().find( getServletConfig() );
    // Create wiki context and check for authorization
    Context wikiContext = Wiki.context().create( wiki, request, ContextEnum.PAGE_RSS.getRequestContext() );
    if( !wiki.getManager( AuthorizationManager.class ).hasAccess( wikiContext, response ) ) return;
    
    Set< String > allPages = wiki.getManager( ReferenceManager.class ).findCreated();
    
    response.setContentType("text/plain; charset=UTF-8");
    for( String pageName : allPages ) {
        // Let's not add attachments.
        if( wiki.getManager( AttachmentManager.class ).getAttachmentInfoName( wikiContext, pageName ) != null ) continue;

        Page wikiPage = wiki.getManager( PageManager.class ).getPage( pageName );
        if( wikiPage != null ) { // there's a possibility the wiki page may get deleted between the call to reference manager and now...
            PagePermission permission = PermissionFactory.getPagePermission( wikiPage, "view" );
            boolean allowed = wiki.getManager( AuthorizationManager.class ).checkPermission( wikiContext.getWikiSession(), permission );
            if( allowed ) {
                String url = wikiContext.getViewURL( pageName );
                out.write( url + " " + pageName + "\n" );
            }
        }
    }
 %>