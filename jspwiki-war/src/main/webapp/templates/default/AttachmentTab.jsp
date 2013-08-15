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

<div id="addattachment">
<h3><fmt:message key="attach.add"/></h3>
<wiki:Permission permission="upload">
  <form action="<wiki:Link jsp='attach' format='url' absolute='true'><wiki:Param name='progressid' value='<%=progressId%>'/></wiki:Link>"
         class="wikiform"
            id="uploadform"
        method="post"
       enctype="multipart/form-data" accept-charset="<wiki:ContentEncoding/>"
      onsubmit="return Wiki.submitUpload(this, '<%=progressId%>');" >
    <table>
    <tr>
      <td colspan="2"><div class="formhelp"><fmt:message key="attach.add.info" /></div></td>
    </tr>
    <tr>
      <td><label for="attachfilename"><fmt:message key="attach.add.selectfile"/></label></td>
      <td><input type="file" name="content" id="attachfilename" size="60"/></td>
    </tr>
    <tr>
      <td><label for="attachnote"><fmt:message key="attach.add.changenote"/></label></td>
      <td><input type="text" name="changenote" id="attachnote" maxlength="80" size="60" />
    <input type="hidden" name="nextpage" value="<wiki:UploadLink format="url"/>" /></td>
    </tr>

   <tr>
      <td></td>
      <td>
        <input type="hidden" name="page" value="<wiki:Variable var="pagename"/>" />
        <input type="submit" name="upload" id="upload" value="<fmt:message key='attach.add.submit'/>" />
        <input type="hidden" name="action" value="upload" />
        <div id="progressbar"><div class="ajaxprogress"></div></div>
      </td>
    </tr>

    </table>
  </form>

  <wiki:Messages div="error" />

</wiki:Permission>
<wiki:Permission permission="!upload">
<div class="formhelp"><fmt:message key="attach.add.permission"/></div>
</wiki:Permission>
</div>

<wiki:HasAttachments>

<h3><fmt:message key="attach.list"/></h3>

  <%--<small><fmt:message key="attach.listsubtitle"/></small>--%>

  <wiki:Permission permission="delete">
    <%-- hidden delete form --%>
    <form action="tbd"
           class="wikiform"
              id="deleteForm" style="display:none;"
          method="post" accept-charset="<wiki:ContentEncoding />"
        onsubmit="return(confirm('<fmt:message key="attach.deleteconfirm"/>') && Wiki.submitOnce(this) );" >

      <input id="delete-all" name="delete-all" type="submit" value="Delete" />

    </form>
  </wiki:Permission>

  <div class="zebra-table"><div class="slimbox-img sortable">
  <table class="wikitable">
    <tr>
      <th><fmt:message key="info.attachment.type"/></th>
      <th><fmt:message key="info.attachment.name"/></th>
      <th><fmt:message key="info.size"/></th>
      <th><fmt:message key="info.version"/></th>
      <th><fmt:message key="info.date"/></th>
      <th><fmt:message key="info.author"/></th>
      <wiki:Permission permission="delete"><th><fmt:message key="info.actions"/></th></wiki:Permission>
      <th class="changenote"><fmt:message key="info.changenote"/></th>
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
      <td style="white-space:nowrap;text-align:right;">
        <fmt:formatNumber value='<%=Double.toString(att.getSize()/1000.0)%>' maxFractionDigits='1' minFractionDigits='1'/>&nbsp;<fmt:message key="info.kilobytes"/>
      </td>
      <td style="text-align:center;">
        <a href="<wiki:PageInfoLink format='url' />" title="<fmt:message key='attach.moreinfo.title'/>"><wiki:PageVersion /></a>
      </td>
	  <td style="white-space:nowrap;" jspwiki:sortvalue="<%= att.getLastModified().getTime() %>">
	  <fmt:formatDate value="<%= att.getLastModified() %>" pattern="${prefs.DateFormat}" timeZone="${prefs.TimeZone}" />
	  </td>
      <td><wiki:Author /></td>
      <wiki:Permission permission="delete">
      <td>
          <input type="button"
                value="<fmt:message key='attach.delete'/>"
                  src="<wiki:Link format='url' context='<%=WikiContext.DELETE%>' />"
              onclick="$('deleteForm').setProperty('action',this.src); $('delete-all').click();" />
      </td>
      </wiki:Permission>
      <td class="changenote">
      <%
         String changeNote = (String)att.getAttribute(WikiPage.CHANGENOTE);
         if( changeNote != null ) {
         %><%=changeNote%><%
         }
      %>
      </td>
    </tr>
    </wiki:AttachmentsIterator>

  </table>
  </div></div>

</wiki:HasAttachments>
