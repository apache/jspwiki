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

<%@ taglib uri="http://jspwiki.apache.org/tags" prefix="wiki" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
<fmt:setLocale value="${prefs.Language}" />
<fmt:setBundle basename="templates.default"/>
<div class="page-content prettify">

<h4><fmt:message key="conflict.oops.title"/></h4>
  <div class="error">
    <fmt:message key="conflict.oops" />
  </div>
  <wiki:Link cssClass="btn btn-primary btn-block" context="edit" >
    <fmt:message key="conflict.goedit" >
      <fmt:param><wiki:PageName /></fmt:param>
    </fmt:message>
  </wiki:Link>
<br />
<div class="columns">
<h4><fmt:message key="conflict.modified"/></h4>
  <pre>${conflicttext}</pre>
<hr />
<h4><fmt:message key="conflict.yourtext"/></h4>
  <pre>${usertext}</pre>
</div>

</div>