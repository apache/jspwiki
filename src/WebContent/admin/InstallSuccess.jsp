<%-- 
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
--%>
<%@ page import="org.apache.wiki.*"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib uri="http://stripes.sourceforge.net/stripes.tld" prefix="s"%>
<%@ page errorPage="/Error.jsp" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" 
   "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
  <head>
    <title><fmt:message key="install.title" /></title>
    <link rel="stylesheet" media="screen, projection" type="text/css" href='<wiki:Link format="url" templatefile="jspwiki.css" />' />
  </head>
  <body class="view">
    <div id="wikibody">
      <div id="page">
        <div id="pagecontent">
          <h1><fmt:message key="install.success" /></h1>
          <p><fmt:message key="install.success.description" /></p>
          <s:link beanclass="org.apache.wiki.action.ViewActionBean"><fmt:message key="install.success.clickhere" /></s:link>
        </div>
      </div>
    </div>
  </body>
</html>
