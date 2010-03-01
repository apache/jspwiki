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
<%-- Show existing attachments --%>
<wiki:HasAttachments>
  <h3><fmt:message key="attach.list" /></h3>
  <div class="zebra-table">
    <div class="slimbox-img sortable">
      <table class="wikitable">
        <tr>
          <th><fmt:message key="info.attachment.type" /></th>
          <th><fmt:message key="info.attachment.name" /></th>
          <th><fmt:message key="info.size" /></th>
          <th><fmt:message key="info.version" /></th>
          <th><fmt:message key="info.date" /></th>
          <th><fmt:message key="info.author" /></th>
          <wiki:Permission permission="delete"><th><fmt:message key="info.actions" /></th></wiki:Permission>
          <th class="changenote"><fmt:message key="info.changenote" /></th>
        </tr>

        <c:forEach var="attachment" items="${wikiActionBean.attachments}" >
          <tr>
          	<%-- The 'title' attribute is used to sort empty cells --%>
            <td title="${attachment.contentType}">
              <div class="mime ${fn:replace(attachment.contentType,'/','-')}" />
            </td>
            <td title="${attachment.name}">
              <s:link beanclass="org.apache.wiki.action.AttachmentActionBean" event="download">
                <s:param name="page" value="${attachment.path}" />
                ${wiki:shorten(attachment.name,30)}
              </s:link>
            </td>
            <td style="white-space:nowrap;text-align:right;">
              <fmt:formatNumber value="${attachment.size div 1000}" maxFractionDigits="1" minFractionDigits="1" />&nbsp;<fmt:message key="info.kilobytes" />
            </td>
            <c:set var="attachmentTitle"><fmt:message key="attach.moreinfo.title" /></c:set>
            <td style="text-align:center;">
              <s:link beanclass="org.apache.wiki.action.ViewActionBean" event="info" title="${attachmentTitle}">
                <s:param name="page" value="${attachment.path}" />
                ${attachment.version}
              </s:link>
            </td>
      	    <%-- FIXME
      	         Needs to return date/time converted to msec, to allow numeric sorting
      	         eg. attachment.lastModified.getTime() ...
      	         A ISO8601DATE does not sort properly.
      	    --%>
      	    <td jspwiki:sortvalue="${wiki:iso8601date(attachment.lastModified)}">
        	    <fmt:formatDate value="${attachment.lastModified}" pattern="${prefs.TimeFormat}" timeZone="${prefs.TimeZone}" />
        	</td>
            <td style="white-space:nowrap;" ><wiki:Author/></td>
            <wiki:Permission permission="delete">
              <td>
                <s:form beanclass="org.apache.wiki.action.DeleteActionBean">
                  <s:param name="page" value="${attachment.path}" />
                  <s:submit name="delete"><fmt:message key="attach.delete" /></s:submit>
                </s:form>
              </td>
            </wiki:Permission>
            <td class="changenote"><c:out value="${attachment.changeNote}"/></td>
          </tr>
        </c:forEach>

      </table>
    </div>
  </div>

</wiki:HasAttachments>
