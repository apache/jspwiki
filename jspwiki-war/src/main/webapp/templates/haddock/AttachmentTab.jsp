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
<%@ page import="org.apache.wiki.*" %>
<%@ page import="org.apache.wiki.auth.*" %>
<%@ page import="org.apache.wiki.ui.progress.*" %>
<%@ page import="org.apache.wiki.auth.permissions.*" %>
<%@ page import="java.security.Permission" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<fmt:setLocale value="${prefs.Language}" />
<fmt:setBundle basename="templates.default"/>
<%
  int MAXATTACHNAMELENGTH = 30;
  WikiContext c = WikiContext.findContext(pageContext);
  String progressId = c.getEngine().getProgressManager().getNewProgressIdentifier();
%>

<div class="page-content">
<wiki:Permission permission="upload">

  <form action="<wiki:Link jsp='attach' format='url' absolute='true'><wiki:Param name='progressid' value='<%=progressId%>'/></wiki:Link>"
         class="accordion-close"
            id="uploadform"
        method="post"
       enctype="multipart/form-data" accept-charset="<wiki:ContentEncoding/>" >


    <h4><fmt:message key="attach.add"/></h4>
    <wiki:Messages div="alert alert-danger" />

    <%--
    <p><fmt:message key="attach.add.info" /></p>
    --%>
    <div class="form-group">
      <label class="control-label form-col-20" for="files"><fmt:message key="attach.add.selectfile"/></label>

      <ul class="list-group form-col-50">
        <li class="list-group-item droppable">
          <label>Select files <%--or drop them here!--%></label>
          <input type="file" name="files" id="files" size="60"/>
          <a class="hidden delete btn btn-danger btn-xs pull-right">Delete</a>
        </li> 
      </ul>
    </div>
    <div class="form-group">
      <label class="control-label form-col-20" for="changenote"><fmt:message key="attach.add.changenote"/></label>
      <input class="form-control form-col-50" type="text" name="changenote" id="changenote" maxlength="80" size="60" />
    </div>
    <div class="form-group">
    <input type="hidden" name="nextpage" value="<wiki:Link context='upload' format='url'/>" />
    <input type="hidden" name="page" value="<wiki:Variable var="pagename"/>" />
    <input class="btn btn-primary form-col-offset-20 form-col-50" 
           type="submit" name="upload" id="upload" disabled="disabled" value="<fmt:message key='attach.add.submit'/>" />
    <input type="hidden" name="action" value="upload" />
    </div>
    <div class="hidden form-col-offset-20 form-col-80 progress progress-striped active">
      <div class="progress-bar" data-progressid="<%=progressId%>" style="width: 100%;"></div>
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
              id="deleteForm"
          method="post" accept-charset="<wiki:ContentEncoding />"
        onsubmit="return(confirm('<fmt:message key="attach.deleteconfirm"/>') );" >

      <%--TODO: "nextpage" is not yet implemented in Delete.jsp 
      <input type="hidden" name="nextpage" value="<wiki:Link context='upload' format='url'/>" />
      --%>
      <input class="btn btn-danger btn-xs" id="delete-all" name="delete-all" type="submit" value="Delete" />

    </form>
  </wiki:Permission>

  <div class="slimbox-attachments sortable table-filter-hover-sort table-filter">
  <table class="table">
    <tr>
      <th><fmt:message key="info.attachment.type"/></th>
      <th><fmt:message key="info.attachment.name"/></th>
      <th><fmt:message key="info.size"/></th>
      <th><fmt:message key="info.version"/></th>
      <th><fmt:message key="info.date"/></th>
      <th><fmt:message key="info.author"/></th>
      <wiki:Permission permission="delete"><th><fmt:message key="info.actions"/></th></wiki:Permission>
      <th><fmt:message key="info.changenote"/></th>
    </tr>

    <wiki:AttachmentsIterator id="att">
    <%
      String name = att.getFileName();
      int dot = name.lastIndexOf(".");
      String attachtype = ( dot != -1 ) ? name.substring(dot+1).toLowerCase() : "&nbsp;";

      String sname = name;
      if( sname.length() > MAXATTACHNAMELENGTH ) sname = sname.substring(0,MAXATTACHNAMELENGTH) + "...";
    %>
    <tr>
      <td><div id="attach-<%= attachtype %>" class="attachtype"><%= attachtype %></div></td>
      <td><wiki:LinkTo title="<%= name %>" ><%= sname %></wiki:LinkTo></td>
      <td class="nowrap text-right">
        <fmt:formatNumber value='<%=Double.toString(att.getSize()/1000.0)%>' maxFractionDigits='1' minFractionDigits='1'/>&nbsp;<fmt:message key="info.kilobytes"/>
      </td>
      <td class="center">
        <a href="<wiki:Link context='info' format='url'/>" title="<fmt:message key='attach.moreinfo.title'/>"><wiki:PageVersion /></a>
      </td>
	  <td class="nowrap" jspwiki:sortvalue="<%= att.getLastModified().getTime() %>">
	  <fmt:formatDate value="<%= att.getLastModified() %>" pattern="${prefs.DateFormat}" timeZone="${prefs.TimeZone}" />
	  </td>
      <td><wiki:Author /></td>
      <wiki:Permission permission="delete">
      <td>
          <input type="button"
                class="btn btn-danger btn-xs" 
                value="<fmt:message key='attach.delete'/>"
                  src="<wiki:Link format='url' context='<%=WikiContext.DELETE%>' />"
              onclick="$('deleteForm').set('action',this.src); $('delete-all').click();" />

      </td>
      </wiki:Permission>
      <td class="changenote">
           <% String changenote = (String) att.getAttribute( WikiPage.CHANGENOTE );  %>
		   <%= (changenote==null) ? "" : changenote  %>
      </td>
    </tr>
    </wiki:AttachmentsIterator>

  </table>
  </div>

</wiki:HasAttachments>

</div>

