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
<%@ page import="org.apache.wiki.plugin.*" %>
<%@ page errorPage="/Error.jsp" %>
<%@ taglib uri="http://jspwiki.apache.org/tags" prefix="wiki" %>
<%! 
    Logger log = Logger.getLogger("JSPWiki"); 
%>

<%
    WikiEngine wiki = WikiEngine.getInstance( getServletConfig() );
    // Create wiki context; no need to check for authorization since the 
    // redirect will take care of that
    WikiContext wikiContext = new WikiContext( wiki, request, WikiContext.EDIT );
    String pagereq = wikiContext.getName();
    
    // Redirect if the request was for a 'special page'
    String specialpage = wiki.getSpecialPageReference( pagereq );
    if( specialpage != null )
    {
        // FIXME: Do Something Else
        response.sendRedirect( specialpage );
        return;
    }

    WeblogEntryPlugin p = new WeblogEntryPlugin();
    
    String newEntry = p.getNewEntryPage( wiki, pagereq );

    // Redirect to a new page for user to edit
    response.sendRedirect( wikiContext.getURL( WikiContext.EDIT, newEntry ) );
%>

