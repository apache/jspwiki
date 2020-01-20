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

<%@ page import="org.apache.log4j.*" %>
<%@ page import="org.apache.wiki.*" %>
<%@ page errorPage="/Error.jsp" %>
<%@ taglib uri="http://jspwiki.apache.org/tags" prefix="wiki" %>
<%!
    /**
     * This page contains the logic for finding and including
       the correct login form, which is usually loaded from
       the template directory's LoginContent.jsp page.
       It should not be requested directly by users. If
       container-managed authentication is in force, the container
       will prevent direct access to it.
     */
    Logger log = Logger.getLogger("JSPWiki"); 

%>
<%
    WikiEngine wiki = WikiEngine.getInstance( getServletConfig() );
    // Retrieve the Login page context, then go and find the login form

    WikiContext wikiContext = (WikiContext) pageContext.getAttribute( WikiContext.ATTR_CONTEXT, PageContext.REQUEST_SCOPE );
    
    // If no context, it means we're using container auth.  So, create one anyway
    if( wikiContext == null ) {
        wikiContext = new WikiContext( wiki, request, WikiContext.LOGIN );
        pageContext.setAttribute( WikiContext.ATTR_CONTEXT, wikiContext, PageContext.REQUEST_SCOPE );
    }
    
    response.setContentType("text/html; charset="+wiki.getContentEncoding() );
    String contentPage = wiki.getTemplateManager().findJSP( pageContext, wikiContext.getTemplate(), "ViewTemplate.jsp" );
                                                            
    log.debug("Login template content is: " + contentPage);
    
%><wiki:Include page="<%=contentPage%>" />