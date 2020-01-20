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

<%@ page import="org.apache.wiki.*" %>
<%@ page import="org.apache.wiki.preferences.Preferences" %>
<%@ taglib uri="http://jspwiki.apache.org/tags" prefix="wiki" %>
<%! 
  public void jspInit()
  {
    wiki = WikiEngine.getInstance( getServletConfig() );
  }
  WikiEngine wiki;
%>
<%
  // Copied from a top-level jsp -- which would be a better place to put this 
  WikiContext wikiContext = new WikiContext( wiki, request, WikiContext.VIEW );
  if( !wiki.getAuthorizationManager().hasAccess( wikiContext, response ) ) return;
  String pagereq = wikiContext.getPage().getName();

  response.setContentType("text/html; charset="+wiki.getContentEncoding() );
%>
<div class='categoryTitle'><wiki:LinkTo><wiki:PageName /></wiki:LinkTo></div>
<div class='categoryText'><wiki:Plugin plugin="ReferringPagesPlugin" args="max='20' before='*' after='\n' " /></div>