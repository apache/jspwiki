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
<%@ taglib uri="http://jakarta.apache.org/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://stripes.sourceforge.net/stripes.tld" prefix="s" %>
<wiki:TabbedSection defaultTab="viewgroup">
  <wiki:Tab id="viewgroup" titleKey="group.tab">
  
    <%-- Print the group name and any messages or errors --%>
    <h3><s:format value="${wikiActionBean.group}" /></h3>
    <c:choose>

      <%-- Is this a new group? --%>
      <c:when test="${wikiActionBean.new}">
        <wiki:Permission permission="createGroups">
          <fmt:message key="group.createsuggestion">
            <fmt:param>
              <s:link beanclass="org.apache.wiki.action.GroupActionBean" event="create">
                <s:param name="group" value="${wikiActionBean.group.name}" />
                <fmt:message key="group.createit" />
              </s:link>
            </fmt:param>
          </fmt:message>
        </wiki:Permission>
      </c:when>
      
      <%-- This is an existing group --%>
      <c:otherwise>

        <div class="description">
          <fmt:message key="group.groupintro">
            <fmt:param><em><s:format value="${wikiActionBean.group}" /></em></fmt:param>
          </fmt:message>
        </div>
      
        <%-- The group exists: display the member list --%>
        <div class="formcontainer">
          <%-- Member list --%>
          <div>
            <s:label for="members" />
            <c:forEach items="${wikiActionBean.members}" var="member" varStatus="loop">
              <s:format value="${member}" />&nbsp;
            </c:forEach>
          </div>
          <%-- Creator/modifier info --%>
          <div class="description">
            <fmt:message key='group.modifier'>
              <fmt:param><c:out value="${wikiActionBean.group.modifier}" /></fmt:param>
              <fmt:param>
                <fmt:formatDate value="${wikiActionBean.group.lastModified}" pattern="${prefs.TimeFormat}" timeZone="${prefs.TimeZone}" />
              </fmt:param>
            </fmt:message>
          </div>
          <div class="description">
            <fmt:message key="group.creator">
              <fmt:param><c:out value="${wikiActionBean.group.creator}" /></fmt:param>
              <fmt:param><fmt:formatDate value="${wikiActionBean.group.created}" pattern="${prefs.TimeFormat}" timeZone="${prefs.TimeZone}" /></fmt:param>
            </fmt:message>
          </div>
        </div>
        
        <%-- Delete group --%>
        <wiki:Permission permission="deleteGroup">
          <c:set var="confirm" value="<fmt:message key='grp.deletegroup.confirm'/>" scope="page"/>
          <s:form beanclass="org.apache.wiki.action.GroupActionBean" class="wikiform"
            id="deleteGroup"
            onsubmit="return( confirm('${confirm}') );"
            method="POST" acceptcharset="UTF-8">
            <s:submit name="delete"><fmt:message key="actions.deletegroup" /></s:submit>
          </s:form>
        </wiki:Permission>
      </c:otherwise>

    </c:choose>
  
  </wiki:Tab>

  <%-- If user has rights to edit the group, provide a link --%>
  <wiki:Permission permission="editGroup">
    <wiki:Tab id="editgroup" titleKey="actions.editgroup"
             url="EditGroup.jsp?group=${wikiActionBean.group.name}" accesskey="e" />
  </wiki:Permission>

</wiki:TabbedSection>
