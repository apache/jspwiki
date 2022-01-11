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

<%@ page import="org.apache.logging.log4j.Logger" %>
<%@ page import="org.apache.logging.log4j.LogManager" %>
<%@ page import="org.apache.wiki.WatchDog" %>
<%@ page import="org.apache.wiki.api.core.*" %>
<%@ page import="org.apache.wiki.api.spi.Wiki" %>
<%@ page import="org.apache.wiki.auth.AuthorizationManager" %>
<%@ page import="org.apache.wiki.preferences.Preferences" %>
<%@ page import="org.apache.wiki.tags.InsertDiffTag" %>
<%@ page import="org.apache.wiki.ui.TemplateManager" %>
<%@ page import="org.apache.wiki.util.*" %>
<%@ page errorPage="/Error.jsp" %>
<%@ taglib uri="http://jspwiki.apache.org/tags" prefix="wiki" %>

<%!
    Logger log = LogManager.getLogger("JSPWiki");
%>

<%
    Engine wiki = Wiki.engine().find( getServletConfig() );
    // Create wiki context and check for authorization
    Context wikiContext = Wiki.context().create( wiki, request, ContextEnum.PAGE_DIFF.getRequestContext() );
    if( !wiki.getManager( AuthorizationManager.class ).hasAccess( wikiContext, response ) ) return;
    if( wikiContext.getCommand().getTarget() == null ) {
        response.sendRedirect( wikiContext.getURL( wikiContext.getRequestContext(), wikiContext.getName() ) );
        return;
    }
    String pagereq = wikiContext.getName();

    WatchDog w = WatchDog.getCurrentWatchDog( wiki );
    try
    {
    w.enterState("Generating INFO response",60);

    // Notused ?
    // String pageurl = wiki.encodeName( pagereq );

    // If "r1" is null, then assume current version (= -1)
    // If "r2" is null, then assume the previous version (=current version-1)

    // FIXME: There is a set of unnecessary conversions here: InsertDiffTag
    //        does the String->int conversion anyway.

    Page wikipage = wikiContext.getPage();

    String srev1 = request.getParameter("r1");
    String srev2 = request.getParameter("r2");

    int ver1 = -1, ver2 = -1;

    if( srev1 != null ) {
        ver1 = Integer.parseInt( srev1 );
    }

    if( srev2 != null ) {
        ver2 = Integer.parseInt( srev2 );
    } else {
        int lastver = wikipage.getVersion();
        if( lastver > 1 ) {
            ver2 = lastver-1;
        }
    }

    pageContext.setAttribute( InsertDiffTag.ATTR_OLDVERSION, Integer.valueOf(ver1), PageContext.REQUEST_SCOPE );
    pageContext.setAttribute( InsertDiffTag.ATTR_NEWVERSION, Integer.valueOf(ver2), PageContext.REQUEST_SCOPE );

    // log.debug("Request for page diff for '"+pagereq+"' from "+HttpUtil.getRemoteAddress(request)+" by "+request.getRemoteUser()+".  R1="+ver1+", R2="+ver2 );

    // Set the content type and include the response content
    response.setContentType("text/html; charset="+wiki.getContentEncoding() );
    String contentPage = wiki.getManager( TemplateManager.class ).findJSP( pageContext, wikiContext.getTemplate(), "ViewTemplate.jsp" );
%><wiki:Include page="<%=contentPage%>" />
<% } finally { w.exitState(); } %>