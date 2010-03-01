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
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://jakarta.apache.org/jspwiki.tld" prefix="wiki" %>
<%@ page errorPage="/Error.jsp" %>
<wiki:CheckRequestContext context='view|diff|edit|upload|info'>
  <div id='actionsBottom' class="pageactions"> 
    <wiki:PageExists>  
    <ul>
      <li>  
      <wiki:CheckVersion mode="latest">
         <fmt:message key="info.lastmodified">
            <fmt:param><wiki:PageVersion/></fmt:param>
            <fmt:param><wiki:DiffLink version="latest" newVersion="previous"><wiki:PageDate format='${prefs["TimeFormat"]}' /></wiki:DiffLink></fmt:param>
            <fmt:param><wiki:Author/></fmt:param>
         </fmt:message>
      </wiki:CheckVersion>
  
      <wiki:CheckVersion mode="notlatest">
        <fmt:message key="actions.publishedon">
           <fmt:param><wiki:PageDate format='${prefs["TimeFormat"]}' /></fmt:param>
           <fmt:param><wiki:Author/></fmt:param>
        </fmt:message>
      </wiki:CheckVersion>
      </li>
      <li><wiki:RSSImageLink mode="wiki" /></li>
      <li><a href="#top" 
            class="action quick2top" 
            title="<fmt:message key='actions.gototop' />">&laquo;</a></li>
    </ul> 
    </wiki:PageExists>
  
    <wiki:NoSuchPage><fmt:message key="actions.notcreated" /></wiki:NoSuchPage> 
  </div>
</wiki:CheckRequestContext>