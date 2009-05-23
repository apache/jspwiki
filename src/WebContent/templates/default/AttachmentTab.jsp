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
<%@ taglib uri="http://stripes.sourceforge.net/stripes.tld" prefix="s" %>
<%@ page import="org.apache.wiki.*" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
<%@ page import="org.apache.wiki.action.WikiContextFactory" %>
<%@ page import="org.apache.wiki.util.TextUtil" %>
<%@ page import="org.apache.wiki.api.WikiPage" %>
<%
  int MAXATTACHNAMELENGTH = 30;
  WikiContext c = WikiContextFactory.findContext( pageContext );
  String progressId = c.getEngine().getProgressManager().getNewProgressIdentifier();

  int attCount = c.getEngine().getAttachmentManager().listAttachments(c.getPage()).size();
  String attTitle = LocaleSupport.getLocalizedMessage(pageContext, "attach.tab");
  if( attCount != 0 ) attTitle += " (" + attCount + ")";
%>
<wiki:TabbedSection defaultTab="attach">

  <wiki:Tab id="pagecontent" titleKey="view.tab" accesskey="v" url="Wiki.jsp?page=${wikiActionBean.page.name}"/>
      
  <wiki:Tab id="attach" title="<%= attTitle %>" accesskey="a">
    <div id="addattachment">
      <h3><fmt:message key="attach.add" /></h3>
      <wiki:Permission permission="upload">
        <s:form beanclass="org.apache.wiki.action.AttachmentActionBean" class="wikiform" id="uploadform" acceptcharset="UTF-8">
          <s:param name="progressid" value="<%=progressId%>" />
          <s:param name="page" value="${wikiActionBean.page.name}" />
          <table>
          <tr>
            <td colspan="2"><div class="formhelp"><fmt:message key="attach.add.info" /></div></td>
          </tr>
          <tr>
            <td colspan="2"><s:errors field="newAttachments" /></td>
          </tr>
          <tr>
            <td><s:label for="attachfile0" name="attach.add.selectfile" /></td>
            <td><s:file name="newAttachments[0]" id="attachfile0" /><br/>
              <s:file name="newAttachments[1]" id="attachfile1" /><br/>
              <s:file name="newAttachments[2]" id="attachfile2" /></td>
          </tr>
          <tr>
            <td><s:label for="attachnote" name="attach.add.changenote" /></td>
            <td><s:text name="changenote" id="attachnote" maxlength="80" size="60" /></td>
          </tr>
      
          <tr>
            <td></td>
            <td>
              <s:submit name="upload" id="upload" />
              <div id="progressbar"><div class="ajaxprogress"></div></div>
            </td>
          </tr>
      
          </table>
        </s:form>
      </wiki:Permission>
      <wiki:Permission permission="!upload">
        <div class="formhelp"><fmt:message key="attach.add.permission" /></div>
      </wiki:Permission>
    </div>
    
    <wiki:HasAttachments>
    
      <h3><fmt:message key="attach.list" /></h3>
    
      <%--<small><fmt:message key="attach.listsubtitle"/></small>--%>
    
      <wiki:Permission permission="delete">
        <%-- hidden delete form --%>
      </wiki:Permission>
    
      <div class="zebra-table">
        <div class="slimbox-img sortable">
          <table class="wikitable">
            <tr>
              <th><fmt:message key="info.attachment.name" /></th>
              <th><fmt:message key="info.attachment.type" /></th>
              <th><fmt:message key="info.size" /></th>
              <th><fmt:message key="info.version" /></th>
              <th><fmt:message key="info.date" /></th>
              <th><fmt:message key="info.author" /></th>
              <wiki:Permission permission="delete"><th><fmt:message key="info.actions" /></th></wiki:Permission>
              <th class="changenote"><fmt:message key="info.changenote" /></th>
            </tr>
        
            <wiki:AttachmentsIterator id="att">
    <%
      String name = att.getFileName();
      String sname = name;
      if( sname.length() > MAXATTACHNAMELENGTH ) sname = sname.substring(0,MAXATTACHNAMELENGTH) + "...";
    %>
              <tr>
                <td><wiki:LinkTo title="<%= name %>"><%= sname %></wiki:LinkTo></td>
                <td><div class="attachtype"><%= att.getContentType() %></div></td>
                <td style="white-space:nowrap;text-align:right;">
                  <fmt:formatNumber value='<%=Double.toString(att.getSize()/1000.0)%>' maxFractionDigits='1' minFractionDigits='1' />&nbsp;<fmt:message key="info.kilobytes" />
                </td>
                <td style="text-align:center;">
                  <a href="<wiki:PageInfoLink format='url' />" title="<fmt:message key='attach.moreinfo.title' />"><wiki:PageVersion/></a>
                </td>
            	  <td style="white-space:nowrap;"><fmt:formatDate value="<%= att.getLastModified() %>" pattern="${prefs.DateFormat}" timeZone="${prefs.TimeZone}" /></td>
                <td><wiki:Author/></td>
                <wiki:Permission permission="delete">
                  <td>
                    <input type="button" value="<fmt:message key='attach.delete' />" src="<wiki:Link format='url' context='<%=WikiContext.DELETE%>' />" onclick="$('deleteForm').setProperty('action',this.src); $('delete-all').click();" />
                  </td>
                </wiki:Permission>
                <td class="changenote">
    <%
        String changeNote = TextUtil.replaceEntities((String)att.getAttribute(WikiPage.CHANGENOTE));
        if( changeNote != null ) {
        %><%=changeNote%><%
        }
    %>
                </td>
              </tr>
            </wiki:AttachmentsIterator>
        
          </table>
        </div>
      </div>
    
    </wiki:HasAttachments>
  </wiki:Tab>
  
  <wiki:Tab id="info" titleKey="info.tab" url="PageInfo.jsp?page=${wikiActionBean.page.name}" accesskey="i" />

</wiki:TabbedSection>
