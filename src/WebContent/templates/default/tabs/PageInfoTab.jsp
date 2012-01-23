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
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://jakarta.apache.org/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="http://stripes.sourceforge.net/stripes.tld" prefix="s" %>
<%@ page errorPage="/Error.jsp" %>

<%-- Referring pages --%>
<div class='line'>
  <div class="collapsebox-closed unit size1of2" id="incomingLinks">
    <h4><fmt:message key="info.tab.incoming" /></h4>
    <wiki:Plugin plugin="ReferringPagesPlugin" args="before='*' after='\n' " />
  </div>
  <div class="collapsebox-closed lastUnit size1of2 " id="outgoingLinks">
    <h4><fmt:message key="info.tab.outgoing" /></h4>
    <wiki:Plugin plugin="ReferredPagesPlugin" args="depth='1' type='local'" />
  </div>
</div>

<%-- Diff page versions --%>
<wiki:CheckRequestContext context="diff">
  <s:form beanclass="org.apache.wiki.action.DiffActionBean" method="get" acceptcharset="UTF-8">
    <div class="collapsebox" id="diffcontent">
      <h4>
        <s:hidden name="page" />
        <fmt:message key="diff.difference">
          <fmt:param>
            <s:select id="r1" name="r1" value="${wikiActionBean.r1}" onchange="this.form.submit();">
              <s:options-collection collection="${wikiActionBean.history}" value="version" label="version" />
            </s:select>
          </fmt:param>
          <fmt:param>
            <s:select id="r2" name="r2" value="${wikiActionBean.r2}" onchange="this.form.submit();">
              <s:options-collection collection="${wikiActionBean.history}" value="version" label="version" />
            </s:select>
          </fmt:param>
        </fmt:message>
      </h4>
      <c:if test='${diffprovider eq "ContextualDiffProvider"}'>
        <div class="diffnote">
          <a href="#change-1" title="<fmt:message key='diff.gotofirst.title' />" class="diff-nextprev">
              <fmt:message key="diff.gotofirst" />
          </a>&raquo;&raquo;
        </div>
      </c:if>
      <div class="diffbody">
        <wiki:InsertDiff><i><fmt:message key="diff.nodiff" /></i></wiki:InsertDiff> 
      </div>
    </div>
  </s:form>
</wiki:CheckRequestContext>

<%-- Previous versions --%>
<wiki:CheckVersion mode="notfirst">
  <c:if test="${fn:length(wikiActionBean.history) > 1}">
    <div class="zebra-table sortable">
      <table class="wikitable">
        <tr>
          <th><fmt:message key="info.version" /></th>
          <th><fmt:message key="info.date" /></th>
          <th><fmt:message key="info.size" /></th>
          <th><fmt:message key="info.author" /></th>
          <th><fmt:message key="info.changes" /></th>
          <th class='changenote'><fmt:message key="info.changenote" /></th>
        </tr>
        <c:forEach var="page" items="${wikiActionBean.history}">
          <tr>
            <td>
              <s:link beanclass="org.apache.wiki.action.ViewActionBean" event="view">
                <s:param name="page" value="${page.name}" />
                <s:param name="version" value="${page.version}" />
                ${page.version}
              </s:link>
            </td>

            <td>
              <fmt:formatDate value="${page.lastModified}" pattern="${prefs.TimeFormat}" timeZone="${prefs.TimeZone}" />
            </td>

            <td style="white-space:nowrap;text-align:right;">
              <c:set var="ff"><wiki:PageSize/></c:set>
              <fmt:formatNumber value='${ff/1000}' maxFractionDigits='3' minFractionDigits='1' />&nbsp;<fmt:message key="info.kilobytes" />
            </td>

            <td>${page.author}</td>

            <td>
              <c:if test="${page.version > 1}">
                <s:link beanclass="org.apache.wiki.action.DiffActionBean" event="diff">
                  <s:param name="page" value="${page.name}" />
                  <s:param name="r1" value="${page.version}" />
                  <s:param name="r2" value="${page.version - 1}" />
                  <fmt:message key="info.difftoprev" />
                </s:link>
              </c:if>
              <c:if test="${page.version < wikiActionBean.page.version}">
                <s:link beanclass="org.apache.wiki.action.DiffActionBean" event="diff">
                  <s:param name="page" value="${page.name}" />
                  <s:param name="r1" value="${page.version}" />
                  <s:param name="r2" value="${wikiActionBean.page.version}" />
                  <fmt:message key="info.difftolast" />
                </s:link>
              </c:if>
            </td>
  
            <td class="changenote">${page.changeNote}</td>
          </tr>
        </c:forEach>

      </table>
    </div>
  </c:if>
</wiki:CheckVersion>
