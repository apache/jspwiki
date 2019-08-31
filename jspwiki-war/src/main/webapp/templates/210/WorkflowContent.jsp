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

<%@ page errorPage="/Error.jsp" %>
<%@ page import="org.apache.wiki.*" %>
<%@ taglib uri="http://jspwiki.apache.org/tags" prefix="wiki" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
<fmt:setLocale value="${prefs.Language}" />
<fmt:setBundle basename="templates.default"/>
<script language="JavaScript">
  function SubmitOutcomeIfSelected(selectId)
  {
    if ( selectId.selectedIndex > 0 )
    {
      // alert(selectId.selectedIndex);
      selectId.form.submit();
    }
  }
</script>
<%
  int i = 0;
  String evenOdd;
%>
<wiki:TabbedSection defaultTab='${param.tab} %>' >

<wiki:Tab id="pagecontent" title='<%=LocaleSupport.getLocalizedMessage(pageContext, "workflow.tab")%>' >

<h3><fmt:message key="workflow.heading" /></h3>
<p><fmt:message key="workflow.instructions"/></p>

<!-- Pending Decisions -->
<h4><fmt:message key="workflow.decisions.heading" /></h4>

<c:if test="${empty decisions}">
  <div class="information">
    <fmt:message key="workflow.noinstructions"/>
  </div>
</c:if>

<c:if test="${!empty decisions}">
  <p id="workflow-owner-instructions"><fmt:message key="workflow.owner.instructions"/></p>
  <table class="wikitable" aria-describedby="workflow-actor-instructions">
    <thead>
      <th scope="col" width="5%"  align="center"><fmt:message key="workflow.id"/></th>
      <th scope="col" width="45%" align="left"><fmt:message key="workflow.item"/></th>
      <th scope="col" width="15%" align="left"><fmt:message key="workflow.actions"/></th>
      <th scope="col" width="15%" align="left"><fmt:message key="workflow.requester"/></th>
      <th scope="col" width="20%" align="left"><fmt:message key="workflow.startTime"/></th>
    </thead>
    <tbody>
      <% i = 1; %>
      <c:forEach var="decision" items="${decisions}">
        <% evenOdd = (i % 2 == 0) ? "even" : "odd"; %>
        <tr class="<%=evenOdd%>">
          <!-- Workflow ID -->
          <td align="center"><c:out value="${decision.workflow.id}"/></td>
          <!-- Name of item -->
          <td align="left">
            <fmt:message key="${decision.messageKey}">
              <c:forEach var="messageArg" items="${decision.messageArguments}">
                <fmt:param><c:out value="${messageArg}"/></fmt:param>
              </c:forEach>
            </fmt:message>
          </td>
          <!-- Possible actions (outcomes) -->
          <td align="left">
            <form id="<c:out value='decision.${decision.id}'/>"
              action="<wiki:Link jsp='Workflow.jsp' format='url'/>" method="POST" accept-charset="UTF-8">
              <input type="hidden" name="action" value="decide" />
              <input type="hidden" name="id" value="<c:out value='${decision.id}' />" />
              <select name="outcome" onchange="SubmitOutcomeIfSelected(this)">
                <option value="-"><fmt:message key="select.one"/></option>
                <c:forEach var="outcome" items="${decision.availableOutcomes}"><option value="${outcome.messageKey}"><fmt:message key="${outcome.messageKey}"/></option>
                </c:forEach>
              </select>
            </form>
          </td>
          <!-- Requester -->
          <td align="left"><c:out value="${decision.owner.name}"/></td>
          <!-- When did the actor start this step? -->
          <td align="left">
            <fmt:formatDate value="${decision.startTime}" pattern="${prefs.DateFormat}" timeZone="${prefs.TimeZone}" />
		  </td>
        </tr>
        <!-- Hidden row with Decision details, if there are any -->
        <c:if test="${!empty decision.facts}">
          <tr class="<%=evenOdd%>" class="hideDiv">
            <td>&nbsp;</td>
            <td colspan="4" class="split">
              <a href="#"
                title="<fmt:message key='workflow.details.title' />"
              onclick="$('decision.<c:out value="${decision.workflow.id}"/>').toggle();" >
                <fmt:message key="workflow.details" />
              </a>
              <div class="hideDiv" id="<c:out value='decision.${decision.workflow.id}'/>">
                <c:forEach var="fact" items="${decision.facts}">
                  <h5><fmt:message key="${fact.messageKey}" /></h5>
                  <p><c:out escapeXml="false" value="${fact.value}"/></p>
                </c:forEach>
              </div>
            </td>
          </tr>
        </c:if>

        <% i++; %>
      </c:forEach>
    </tbody>
  </table>
</c:if>

<!-- Running workflows for which current user is the owner -->
<h4 id="workflows"><fmt:message key="workflow.workflows.heading" /></h4>

<c:if test="${empty workflows}">
  <div class="information">
    <fmt:message key="workflow.noinstructions"/>
  </div>
</c:if>

<c:if test="${!empty workflows}">
  <p id="workflow-owner-instructions"><fmt:message key="workflow.owner.instructions"/></p>
  <table class="wikitable" aria-describedby="workflow-owner-instructions">
    <thead>
      <th scope="col" width="5%"  align="center"><fmt:message key="workflow.id"/></th>
      <th scope="col" width="45%" align="left"><fmt:message key="workflow.item"/></th>
      <th scope="col" width="15%" align="left"><fmt:message key="workflow.actions"/></th>
      <th scope="col" width="15%" align="left"><fmt:message key="workflow.actor"/></th>
      <th scope="col" width="20%" align="left"><fmt:message key="workflow.startTime"/></th>
    </thead>
    <% i = 1; %>
    <tbody>
      <c:forEach var="workflow" items="${workflows}">
        <% evenOdd = (i % 2 == 0) ? "even" : "odd"; %>
        <tr class="<%=evenOdd%>">
          <!-- Workflow ID -->
          <td  align="center"><c:out value="${workflow.id}"/></td>
          <!-- Name of item -->
          <td align="left">
            <fmt:message key="${workflow.messageKey}">
              <c:forEach var="messageArg" items="${workflow.messageArguments}">
                <fmt:param><c:out value="${messageArg}"/></fmt:param>
              </c:forEach>
            </fmt:message>
          </td >
          <!-- Actions -->
          <td align="left">
            <form id="<c:out value='workflow.${workflow.id}'/>" action="<wiki:Link jsp='Workflow.jsp' format='url'/>" method="POST" accept-charset="UTF-8">
              <input type="submit" name="submit" value="<fmt:message key="outcome.step.abort" />" />
              <input type="hidden" name="action" value="abort" />
              <input type="hidden" name="id" value="<c:out value="${workflow.id}" />" />
            </form>
          </td>
          <!-- Current actor -->
          <td align="left"><c:out value="${workflow.currentActor.name}"/></td>
          <!-- When did the actor start this step? -->
          <td align="left">
            <fmt:formatDate value="${workflow.currentStep.startTime}" pattern="${prefs.DateFormat}" timeZone="${prefs.TimeZone}" />
          </td>
        </tr>
        <% i++; %>
      </c:forEach>
    </tbody>
  </table>
</c:if>

</wiki:Tab>
</wiki:TabbedSection>