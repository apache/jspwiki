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

<%@ page import="org.apache.wiki.*" %>
<%@ page import="org.apache.wiki.auth.*" %>
<%@ page import="org.apache.wiki.ui.progress.*" %>
<%@ page import="org.apache.wiki.auth.permissions.*" %>
<%@ page import="java.security.Permission" %>
<%@ taglib uri="http://jspwiki.apache.org/tags" prefix="wiki" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<fmt:setLocale value="${prefs.Language}" />
<fmt:setBundle basename="templates.default"/>
<%
  int MAXATTACHNAMELENGTH = 30;
  WikiContext c = WikiContext.findContext(pageContext);
%>
<c:set var="progressId" value="<%= c.getEngine().getProgressManager().getNewProgressIdentifier() %>" />
<div class="page-content">
<wiki:Permission permission="upload">

  <form action="<wiki:Link jsp='attach' format='url' absolute='true'><wiki:Param name='progressid' value='${progressId}'/></wiki:Link>"
         class="accordion<wiki:HasAttachments>-close</wiki:HasAttachments>"
            id="uploadform"
        method="post"
       enctype="multipart/form-data" accept-charset="<wiki:ContentEncoding/>" >

    <h4><fmt:message key="attach.add"/></h4>
    <input type="hidden" name="nextpage" value="<wiki:Link context='upload' format='url'/>" />
    <input type="hidden" name="page" value="<wiki:Variable var="pagename"/>" />
    <input type="hidden" name="action" value="upload" />

    <wiki:Messages div="alert alert-danger" />

    <%-- <p><fmt:message key="attach.add.info" /></p> --%>
    <div class="form-group">
      <label class="control-label form-col-20" for="files"><fmt:message key="attach.add.selectfile"/></label>

      <ul class="list-group form-col-50">
        <li class="list-group-item droppable">
          <a class="hidden delete btn btn-danger btn-xs pull-right">Delete</a>
          <label>Select files <span class='canDragAndDrop'>or drop them here!</span></label>
          <input type="file" name="files" id="files" size="60" multiple="multiple"/>
        </li>
      </ul>
    </div>
    <div class="form-group">
      <label class="control-label form-col-20" for="changenote"><fmt:message key="attach.add.changenote"/></label>
      <input class="form-control form-col-50" type="text" name="changenote" id="changenote" maxlength="80" size="60" />
    </div>
    <div class="form-group">
      <input class="btn btn-success form-col-offset-20 form-col-50"
             type="submit" name="upload" id="upload" disabled="disabled" value="<fmt:message key='attach.add.submit'/>" />
    </div>
    <div class="hidden form-col-offset-20 form-col-50 progress progress-striped active">
      <div class="progress-bar" data-progressid="${progressId}" style="width: 100%;"></div>
    </div>

  </form>
</wiki:Permission>
<wiki:Permission permission="!upload">
  <div class="warning"><fmt:message key="attach.add.permission"/></div>
</wiki:Permission>

<wiki:HasAttachments>

<%--<h3><fmt:message key="attach.list"/></h3>--%>

  <wiki:Permission permission="delete">
    <%-- hidden delete form --%>
    <form action="tbd"
           class="hidden"
            name="deleteForm" id="deleteForm"
          method="post" accept-charset="<wiki:ContentEncoding />" >

      <%--TODO: "nextpage" is not yet implemented in Delete.jsp
      --%>
      <input type="hidden" name="nextpage" value="<wiki:Link context='upload' format='url'/>" />
      <input id="delete-all" name="delete-all" type="submit"
        data-modal="+ .modal"
             value="Delete" />
      <div class="modal"><fmt:message key='attach.deleteconfirm'/></div>

    </form>
  </wiki:Permission>

  <div class="slimbox-attachments table-filter-striped-sort-condensed">
  <table class="table">
    <tr>
      <th><fmt:message key="info.attachment.name"/></th>
      <th><fmt:message key="info.version"/></th>
      <th><fmt:message key="info.date"/></th>
      <th><fmt:message key="info.size"/></th>
      <th><fmt:message key="info.attachment.type"/></th>
      <th><fmt:message key="info.author"/></th>
      <th><fmt:message key="info.actions"/></th>
      <th><fmt:message key="info.changenote"/></th>
    </tr>

    <wiki:AttachmentsIterator id="att">
    <tr>

      <%-- see styles/fontjspwiki/icon.less : icon-file-<....>-o  --%>
      <c:set var="parts" value="${fn:split(att.fileName, '.')}" />
      <c:set var="type" value="${ fn:length(parts)>1 ? parts[fn:length(parts)-1] : ''}" />

      <td class="attach-name" title="${att.fileName}"><wiki:LinkTo>${att.fileName}</wiki:LinkTo></td>

      <td><wiki:PageVersion /></td>

      <td class="nowrap" data-sortvalue="${att.lastModified.time}">
        <fmt:formatDate value="${att.lastModified}" pattern="${prefs.DateFormat}" timeZone="${prefs.TimeZone}" />
      </td>

      <td class="nowrap" title="${att.size} bytes">
        <%-- <fmt:formatNumber value='${att.size/1024.0}' maxFractionDigits='1' minFractionDigits='1'/>&nbsp;<fmt:message key="info.kilobytes"/> --%>
        <%= org.apache.commons.io.FileUtils.byteCountToDisplaySize( att.getSize() ) %>
      </td>

      <td class="attach-type"><span class="icon-file-${fn:toLowerCase(type)}-o"></span>${type}</td>

      <td><wiki:Author /></td>

      <td class="nowrap">
        <a class="btn btn-primary btn-xs" href="<wiki:Link context='info' format='url'/>" title="<fmt:message key='attach.moreinfo.title'/>">
          <fmt:message key="attach.moreinfo"/>
        </a>
        <wiki:Permission permission="delete">
          <input type="button"
                class="btn btn-danger btn-xs"
                value="<fmt:message key='attach.delete'/>"
                  src="<wiki:Link format='url' context='<%=WikiContext.DELETE%>' ><wiki:Param name='tab' value='attach' /></wiki:Link>"
              onclick="document.deleteForm.action=this.src; document.deleteForm['delete-all'].click();" />
        </wiki:Permission>
      </td>

      <c:set var="changenote" value="<%= (String)att.getAttribute( WikiPage.CHANGENOTE ) %>" />
      <td class="changenote">${changenote}</td>

    </tr>
    </wiki:AttachmentsIterator>

  </table>
  </div>

</wiki:HasAttachments>

</div>

