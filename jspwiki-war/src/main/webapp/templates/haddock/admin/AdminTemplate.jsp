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
<%@ page import="org.apache.wiki.*" %>
<%@ page import="org.apache.wiki.api.engine.AdminBeanManager" %>
<%@ page import="org.apache.wiki.ui.*" %>
<%@ page import="org.apache.wiki.ui.admin.*" %>
<%@ page errorPage="/Error.jsp" %>
<%@ taglib uri="http://jspwiki.apache.org/tags" prefix="wiki" %>
<html>
<head>
<title>JSPWiki administration</title>
  <wiki:Include page="commonheader.jsp"/>
  <link rel="stylesheet" media="screen, projection, print" type="text/css"
        href="<wiki:Link format='url' templatefile='admin/admin.css'/>"/>
</head>
<body class="container context-view">
<div id="wikibody">
<div id="page" >
<h1>JSPWiki Administration</h1>
<div class="information">Not all things can be configured here.  Some things need to be configured
in your <tt>jspwiki.properties</tt> file.</div>

<%
    WikiEngine wiki = WikiEngine.getInstance( getServletConfig() );
    WikiContext ctx = WikiContext.findContext(pageContext);
    AdminBeanManager mgr = wiki.getAdminBeanManager();
 %>

<div class="tabs admin">

<h3>Core</h3>
<p>Contains core setup options.</p>
   <wiki:TabbedSection defaultTab="${param['tab-core']}">

     <wiki:AdminBeanIterator type="core" id="ab">
      <wiki:Tab id="${ab.id}" title="${ab.title}">

      <div class="formcontainer">
      <form action="Admin.jsp" method="post" accept-charset="UTF-8">
        <input type="hidden" name="tab-admin" value="core"/>
        <input type="hidden" name="tab-core" value="${ab.title}" />
        <input type="hidden" name="bean" value="${ab.id}" />
        <%
         out.write( ab.doGet(ctx) );
         %>
       </form>
       </div>
      </wiki:Tab>
     </wiki:AdminBeanIterator>
   </wiki:TabbedSection>

<h3>Users</h3>
   <wiki:Include page="admin/UserManagement.jsp"/>

<h3>Group</h3>
   <div>
   <p>This is a list of all groups in this wiki.  If you click on the group name,
   you will be taken to the administration page of that particular group.</p>
   <p>
   <wiki:Plugin plugin="Groups"/>
   </p>
   </div>

<h3>Editors</h3>

   <wiki:TabbedSection defaultTab="${param['tab-editors']}">
     <wiki:AdminBeanIterator type="editors" id="ab">
      <wiki:Tab id="${ab.id}" title="${ab.title}">

      <div class="formcontainer">
      <form action="Admin.jsp" method="post" accept-charset="UTF-8">
         <input type="hidden" name="tab-admin" value="editors"/>
         <input type="hidden" name="tab-editors" value="${ab.title}" />
         <%
         out.write( ab.doGet(ctx) );
         %>
       </form>
       </div>
      </wiki:Tab>
     </wiki:AdminBeanIterator>
   </wiki:TabbedSection>

<h3>Filters</h3>
<p>There will be more filter stuff here</p>

</div>
</div>

</div>
</body>