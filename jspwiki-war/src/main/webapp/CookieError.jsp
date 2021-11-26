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

<%@ page import="org.apache.wiki.api.core.*" %>
<%@ page import="org.apache.wiki.api.spi.Wiki" %>
<%@ page import="org.apache.wiki.ui.TemplateManager" %>
<%@ taglib prefix="wiki" uri="http://jspwiki.apache.org/tags" %>

<%
    Engine wiki = Wiki.engine().find( getServletConfig() );
    // Create wiki context; authorization check not needed
    Context wikiContext = Wiki.context().create( wiki, request, ContextEnum.PAGE_VIEW.getRequestContext() );
 
    // Set the content type and include the response content
    response.setContentType("text/html; charset="+wiki.getContentEncoding() );
    String contentPage = wiki.getManager( TemplateManager.class ).findJSP( pageContext, wikiContext.getTemplate(), "CookieErrorTemplate.jsp" );
%><wiki:Include page="<%=contentPage%>" />
