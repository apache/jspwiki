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

<%@ page import="org.apache.wiki.tags.InsertDiffTag" %>
<%@ page import="org.apache.wiki.*" %>
<%@ page import="java.util.*" %>
<%@ taglib uri="http://jspwiki.apache.org/tags" prefix="wiki" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<fmt:setLocale value="${prefs.Language}" />
<fmt:setBundle basename="templates.default"/>

<% 
  WikiContext c = WikiContext.findContext( pageContext );  
  List history = c.getEngine().getVersionHistory(c.getPage().getName());
  pageContext.setAttribute( "history", history );
  pageContext.setAttribute( "diffprovider", c.getEngine().getVariable(c,"jspwiki.diffProvider"));
 %>

<wiki:PageExists>
<form action="<wiki:Link jsp='Diff.jsp' format='url' />" method="get" accept-charset="UTF-8">
<div class="collapsebox" id="diffcontent">
  <h4>
       <input type="hidden" name="page" value="<wiki:Variable var='pagename' />" />
       <fmt:message key="diff.difference">
         <fmt:param>
           <select id="r1" name="r1" onchange="this.form.submit();" >
           <c:forEach items="${history}" var="i">
             <option value="<c:out value='${i.version}'/>" <c:if test="${i.version == olddiff}">selected="selected"</c:if> ><c:out value="${i.version}"/></option>
           </c:forEach>
           </select>
         </fmt:param>
         <fmt:param>
           <select id="r2" name="r2" onchange="this.form.submit();" >
           <c:forEach items="${history}" var="i">
             <option value="<c:out value='${i.version}'/>" <c:if test="${i.version == newdiff}">selected="selected"</c:if> ><c:out value="${i.version}"/></option>
           </c:forEach>
           </select>
         </fmt:param>
       </fmt:message>
  </h4>

  <c:if test='${diffprovider eq "ContextualDiffProvider"}' >
    <div class="diffnote">
      <a href="#change-1" title="<fmt:message key='diff.gotofirst.title'/>" class="diff-nextprev" >
         <fmt:message key="diff.gotofirst"/>
      </a>&raquo;&raquo;
    </div>
  </c:if>

  <div class="diffbody">
    <wiki:InsertDiff><i><fmt:message key="diff.nodiff"/></i></wiki:InsertDiff> 
  </div>
</div>
</form>
</wiki:PageExists>