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
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://jakarta.apache.org/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="http://stripes.sourceforge.net/stripes.tld" prefix="s" %>
<%@ page errorPage="/Error.jsp" %>
<s:layout-render name="${templates['layout/DefaultLayout.jsp']}">
  <s:layout-component name="content">
    <h3><fmt:message key="workflow.heading" /></h3>
    <p><fmt:message key="workflow.instructions" /></p>
    <s:errors/>
    
    <!-- Pending Decisions -->
    <h4><fmt:message key="workflow.decisions.heading" /></h4>
    
    <c:if test="${empty wikiActionBean.decisions}">
      <div class="information">
        <fmt:message key="workflow.noinstructions" />
      </div>
    </c:if>
    
    <c:if test="${!empty wikiActionBean.decisions}">
      <div class="formhelp">
        <fmt:message key="workflow.actor.instructions" />
      </div>
      <table class="wikitable">
        <thead>
          <tr>
            <th width="5%" align="center"><fmt:message key="workflow.id" /></th>
            <th width="45%" align="left"><fmt:message key="workflow.item" /></th>
            <th width="15%" align="left"><fmt:message key="workflow.actions" /></th>
            <th width="15%" align="left"><fmt:message key="workflow.requester" /></th>
            <th width="20%" align="left"><fmt:message key="workflow.startTime" /></th>
          </tr>
        </thead>
        <tbody>
          <c:forEach var="decision" items="${wikiActionBean.decisions}" varStatus="loop">
            <tr class="${((loop.index % 2) == 0) ? 'even' : 'odd'}">
              <!-- Workflow ID -->
              <td align="center"><c:out value="${decision.workflow.id}" /></td>
              <!-- Name of item -->
              <td align="left">
                <fmt:message key="${decision.messageKey}">
                  <c:forEach var="messageArg" items="${decision.messageArguments}">
                    <fmt:param><c:out value="${messageArg}" /></fmt:param>
                  </c:forEach>
                </fmt:message>
              </td>
              <!-- Possible actions (outcomes) -->
              <td align="left">
                <s:form id="decision.${decision.id}" beanclass="org.apache.wiki.action.WorkflowActionBean" method="POST" acceptcharset="UTF-8">
                  <input type="hidden" name="id" value="${decision.id}" />
                  <s:select name="outcome" value="${decision.defaultOutcome.messageKey}">
                    <s:options-collection collection="${decision.availableOutcomes}" value="messageKey" label="messageKey" />
                  </s:select>
                  <s:submit name="decide" />
                </s:form>
              </td>
              <!-- Requester -->
              <td align="left"><c:out value="${decision.owner.name}" /></td>
              <!-- When did the actor start this step? -->
              <td align="left">
                <fmt:formatDate value="${decision.startTime}" pattern="${prefs.TimeFormat}" timeZone="${prefs.TimeZone}" />
        		  </td>
            </tr>
            <!-- Hidden row with Decision details, if there are any -->
            <c:if test="${!empty decision.facts}">
              <tr class="${((loop.index % 2) == 0) ? 'even' : 'odd'}" class="hideDiv">
                <td>&nbsp;</td>
                <td colspan="4" class="split">
                  <a href="#" 
                    title="<fmt:message key='workflow.details.title' />"
                  onclick="$('decision.<c:out value="${decision.workflow.id}"/>').toggle();" >
                    <fmt:message key="workflow.details" />
                  </a>
                  <div class="hideDiv" id="<c:out value='decision.${decision.workflow.id}' />">
                    <c:forEach var="fact" items="${decision.facts}">
                      <h5><fmt:message key="${fact.messageKey}" /></h5>
                      <p><c:out escapeXml="false" value="${fact.value}" /></p>
                    </c:forEach>
                  </div>
                </td>
              </tr>
            </c:if>
          </c:forEach>
        </tbody>
      </table>
    </c:if>
    
    <!-- Running workflows for which current user is the owner -->
    <h4><fmt:message key="workflow.workflows.heading" /></h4>
    
    <c:if test="${empty wikiActionBean.workflows}">
      <div class="information">
        <fmt:message key="workflow.noinstructions" />
      </div>
    </c:if>
    
    <c:if test="${!empty wikiActionBean.workflows}">
      <div class="formhelp">
        <fmt:message key="workflow.owner.instructions" />
      </div>
      <table class="wikitable">
        <thead>
          <tr>
            <th width="5%" align="center"><fmt:message key="workflow.id" /></th>
            <th width="45%" align="left"><fmt:message key="workflow.item" /></th>
            <th width="15%" align="left"><fmt:message key="workflow.actions" /></th>
            <th width="15%" align="left"><fmt:message key="workflow.actor" /></th>
            <th width="20%" align="left"><fmt:message key="workflow.startTime" /></th>
          </tr>
        </thead>
        <tbody>
          <c:forEach var="workflow" items="${wikiActionBean.workflows}" varStatus="loop">
            <tr class="${((loop.index % 2) == 0) ? 'even' : 'odd'}">
              <!-- Workflow ID -->
              <td align="center"><c:out value="${workflow.id}" /></td>
              <!-- Name of item -->
              <td align="left">
                <fmt:message key="${workflow.messageKey}">
                  <c:forEach var="messageArg" items="${workflow.messageArguments}">
                    <fmt:param><c:out value="${messageArg}" /></fmt:param>
                  </c:forEach>
                </fmt:message>
              </td>
              <!-- Actions -->
              <td align="left">
                <s:form id="workflow.${workflow.id}" beanclass="org.apache.wiki.action.WorkflowActionBean" method="POST" acceptcharset="UTF-8">
                  <input type="hidden" name="id" value="${workflow.id}" />
                  <s:submit name="abort"/>
                </s:form>
              </td>
              <!-- Current actor -->
              <td align="left"><c:out value="${workflow.currentActor.name}" /></td>
              <!-- When did the actor start this step? -->
              <td align="left">
                <fmt:formatDate value="${workflow.currentStep.startTime}" pattern="${prefs.TimeFormat}" timeZone="${prefs.TimeZone}" />
              </td>
            </tr>
          </c:forEach>
        </tbody>
      </table>
    </c:if>
  </s:layout-component>
</s:layout-render>
