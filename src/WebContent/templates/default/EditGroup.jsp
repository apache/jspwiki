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
<s:layout-render name="${templates['layout/DefaultLayout.jsp']}">
  <s:layout-component name="content">
    <wiki:TabbedSection defaultTab="editgroup">
      <c:choose>
      
          <%-- This a new group --%>
          <c:when test="${wikiActionBean.new}">
            <wiki:Tab id="editgroup" titleKey="actions.editgroup">
              <h3><fmt:message key="newgroup.heading.create" /></h3>
              <!-- Any messages or errors? -->
              <div class="instructions"><s:messages /></div>
              <div class="errors"><s:errors globalErrorsOnly="true" /></div>
    
              <div class="formcontainer">
                <s:form beanclass="org.apache.wiki.action.GroupActionBean"
                               id="editGroup"
                           method="POST"
                    acceptcharset="UTF-8">
                  <%-- Group name --%>
                  <div>
                    <s:label for="group" />
                    <s:text name="group" size="30" />
                    <s:errors field="group" />
                    <div class="description"><fmt:message key="newgroup.name.description" /></div>
                  </div>
                  <%-- Member list --%>
                  <div>
                    <s:label for="members" />
                    <c:set var="memberList">
                      <c:forEach items="${wikiActionBean.members}" var="member" varStatus="loop">
                        <s:format value="${member}" />&#x000D;
                      </c:forEach>
                    </c:set>
                    <textarea name="members" cols="30" rows="10">${memberList}</textArea>
                    <s:errors field="members" />
                    <div class="description"><fmt:message key="members.description" /></div>
                  </div>
                  <%-- Cancel or save the group --%>
                  <s:submit name="view" />
                  <s:submit name="save" />
                </s:form>
              </div>
            </wiki:Tab>
          </c:when>
    
          <%-- This is an existing group --%>
          <c:otherwise>
          
            <%-- View group tab --%>
            <wiki:Tab id="viewgroup" titleKey="group.tab" accesskey="v"
              beanclass="org.apache.wiki.action.GroupActionBean">
              <wiki:Param name="group" value="${wikiActionBean.group.name}" />
            </wiki:Tab>
    
            <%-- Edit group tab --%>
            <wiki:Tab id="editgroup" titleKey="actions.editgroup">
              <h3><s:format value="${wikiActionBean.group}" /></h3>
              <!-- Any messages or errors? -->
              <div class="instructions"><s:messages /></div>
              <div class="errors"><s:errors globalErrorsOnly="true" /></div>
    
              <div class="formcontainer">
                <s:form beanclass="org.apache.wiki.action.GroupActionBean"
                               id="editGroup"
                           method="POST"
                    acceptcharset="UTF-8">
                  <s:hidden name="group" value="${wikiActionBean.group.name}" />
                  <%-- Group name --%>
                  <div class="description">
                    <fmt:message key="group.groupintro">
                      <fmt:param><em><s:format value="${wikiActionBean.group}" /></em></fmt:param>
                    </fmt:message>
                  </div>
                  <%-- Member list --%>
                  <div>
                    <s:label for="members" />
                    <c:set var="memberList">
                      <c:forEach items="${wikiActionBean.members}" var="member" varStatus="loop"><s:format value="${member}" />&#x000D;</c:forEach>
                    </c:set>
                    <textarea name="members" cols="30" rows="10">${memberList}</textArea>
                    <s:errors field="members" />
                    <div class="description"><fmt:message key="members.description" /></div>
                  </div>
                  <%-- Creator/modifier info --%>
                  <div class="description">
                    <fmt:message key="group.creator">
                      <fmt:param><c:out value="${wikiActionBean.group.creator}" /></fmt:param>
                      <fmt:param><c:out value="${wikiActionBean.group.created}" /></fmt:param>
                    </fmt:message>
                  </div>
                  <%-- Cancel or save the group --%>
                  <s:submit name="view" />
                  <s:submit name="save" />
                </s:form>
              </div>
            </wiki:Tab>

          </c:otherwise>
      </c:choose>
    </wiki:TabbedSection>
  </s:layout-component>
</s:layout-render>
