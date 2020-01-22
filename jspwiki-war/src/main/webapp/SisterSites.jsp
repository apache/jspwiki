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

<%@ page import="java.util.*,org.apache.wiki.*" %>
<%@ page import="org.apache.log4j.*" %>
<%@ page import="java.text.*" %>
<%@ page import="org.apache.wiki.preferences.Preferences" %>
<%@ page import="org.apache.wiki.rss.*" %>
<%@ page import="org.apache.wiki.util.*" %>
<%!
    Logger log = Logger.getLogger("JSPWiki");
%>
<%
    /*
     *  This JSP creates support for the SisterSites standard,
     *  as specified by http://usemod.com/cgi-bin/mb.pl?SisterSitesImplementationGuide
     *
     *  FIXME: Does not honor the ACL's on the pages.
     */
    WikiEngine wiki = WikiEngine.getInstance( getServletConfig() );
    // Create wiki context and check for authorization
    WikiContext wikiContext = new WikiContext( wiki, request, "rss" );
    if(!wiki.getAuthorizationManager().hasAccess( wikiContext, response )) return;
    
    Set< String > allPages = wiki.getReferenceManager().findCreated();
    
    response.setContentType("text/plain; charset=UTF-8");
    for( Iterator< String > i = allPages.iterator(); i.hasNext(); )
    {
        String pageName = i.next();
        
        // Let's not add attachments.
        // TODO: This is a kludge and not forward-compatible.
        
        if( pageName.indexOf("/") != -1 ) continue; 
        String url = wikiContext.getViewURL( pageName );
        
        out.write( url + " " + pageName + "\n" );
    }
 %>