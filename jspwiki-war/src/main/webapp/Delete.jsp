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

<%@ page import="java.util.*" %>
<%@ page import="org.apache.logging.log4j.Logger" %>
<%@ page import="org.apache.logging.log4j.LogManager" %>
<%@ page import="org.apache.wiki.api.core.*" %>
<%@ page import="org.apache.wiki.api.spi.Wiki" %>
<%@ page import="org.apache.wiki.attachment.Attachment" %>
<%@ page import="org.apache.wiki.auth.AuthorizationManager" %>
<%@ page import="org.apache.wiki.pages.PageManager" %>
<%@ page import="org.apache.wiki.preferences.Preferences" %>
<%@ page import="org.apache.wiki.tags.BreadcrumbsTag" %>
<%@ page import="org.apache.wiki.tags.BreadcrumbsTag.FixedQueue" %>
<%@ page import="org.apache.wiki.ui.TemplateManager" %>
<%@ page import="org.apache.wiki.util.HttpUtil" %>
<%@ page import="org.apache.wiki.util.TextUtil" %>
<%@ page errorPage="/Error.jsp" %>
<%@ taglib uri="http://jspwiki.apache.org/tags" prefix="wiki" %>

<%!
    Logger log = LogManager.getLogger("JSPWiki");
%>

<%
    Engine wiki = Wiki.engine().find( getServletConfig() );
    // Create wiki context and check for authorization
    Context wikiContext = Wiki.context().create( wiki, request, ContextEnum.PAGE_DELETE.getRequestContext() );
    if( !wiki.getManager( AuthorizationManager.class ).hasAccess( wikiContext, response ) ) return;
    if( wikiContext.getCommand().getTarget() == null ) {
        response.sendRedirect( wikiContext.getURL( wikiContext.getRequestContext(), wikiContext.getName() ) );
        return;
    }
    String pagereq = wikiContext.getName();

    Page wikipage      = wikiContext.getPage();
    Page latestversion = wiki.getManager( PageManager.class ).getPage( pagereq );

    String delete = request.getParameter( "delete" );
    String deleteall = request.getParameter( "delete-all" );

    if( latestversion == null )
    {
        latestversion = wikiContext.getPage();
    }

    // If deleting an attachment, go to the parent page.
    String redirTo = pagereq;
    if( wikipage instanceof Attachment ) {
        redirTo = ((Attachment)wikipage).getParentName();
    }

    if( deleteall != null ) {
        log.info("Deleting page "+pagereq+". User="+request.getRemoteUser()+", host="+HttpUtil.getRemoteAddress(request) );

        wiki.getManager( PageManager.class ).deletePage( pagereq );

        FixedQueue trail = (FixedQueue) session.getAttribute( BreadcrumbsTag.BREADCRUMBTRAIL_KEY );
        if( trail != null )
        {
            trail.removeItem( pagereq );
            session.setAttribute( BreadcrumbsTag.BREADCRUMBTRAIL_KEY, trail );
        }

        response.sendRedirect( TextUtil.replaceString( wiki.getURL( ContextEnum.PAGE_VIEW.getRequestContext(), redirTo, "tab="+request.getParameter("tab") ),"&amp;","&" ));
        return;
    } else if( delete != null ) {
        log.info("Deleting a range of pages from "+pagereq);

        for( Enumeration< String > params = request.getParameterNames(); params.hasMoreElements(); ) {
            String paramName = params.nextElement();

            if( paramName.startsWith("delver") ) {
                int version = Integer.parseInt( paramName.substring(7) );

                Page p = wiki.getManager( PageManager.class ).getPage( pagereq, version );

                log.debug("Deleting version "+version);
                wiki.getManager( PageManager.class ).deleteVersion( p );
            }
        }

        response.sendRedirect(
            TextUtil.replaceString( wiki.getURL( ContextEnum.PAGE_VIEW.getRequestContext(), redirTo, "tab=" + request.getParameter( "tab" ) ),"&amp;","&" )
        );

        return;
    }

    // Set the content type and include the response content
    // FIXME: not so.
    response.setContentType("text/html; charset="+wiki.getContentEncoding() );
    String contentPage = wiki.getManager( TemplateManager.class ).findJSP( pageContext, wikiContext.getTemplate(), "EditTemplate.jsp" );
%><wiki:Include page="<%=contentPage%>" />

