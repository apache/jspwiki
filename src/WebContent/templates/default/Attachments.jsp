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
<s:layout-render name="${templates['DefaultLayout.jsp']}">
  <s:layout-component name="content">
    <wiki:TabbedSection defaultTab="attach">
      
      <wiki:NoSuchPage>
        <wiki:Tab id="attach" titleKey="view.tab" accesskey="v">
          <fmt:message key="common.nopage">
            <fmt:param><wiki:EditLink><fmt:message key="common.createit" /></wiki:EditLink></fmt:param>
          </fmt:message>
        </wiki:Tab>
      </wiki:NoSuchPage>

      <wiki:PageExists>
      
        <%-- Content tab --%>
        <wiki:Tab id="pagecontent" titleKey="view.tab" accesskey="v" beanclass="org.apache.wiki.action.ViewActionBean" event="view">
          <wiki:Param name="page" value="${wikiActionBean.page.name}" />
        </wiki:Tab>
      
        <%-- Attachments tab --%>
        <wiki:Tab id="attach" accesskey="a"
          title="${wiki:attachmentsTitle(request.Locale, wikiActionBean.attachments)}">
          <%-- Add new attachments --%>
          <div id="addattachment">
            <h3><fmt:message key="attach.add" /></h3>
            <wiki:Permission permission="upload">
              <s:form beanclass="org.apache.wiki.action.AttachmentActionBean" class="wikiform" id="uploadform" acceptcharset="UTF-8">
                <s:param name="progressid" value="${wikiEngine.progressManager.newProgressIdentifier}" />
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
          </div>
      
          <%-- Show existing attachments --%>
          <wiki:HasAttachments>
            <h3><fmt:message key="attach.list" /></h3>
            <wiki:Permission permission="delete">
              <%-- hidden delete form --%>
            </wiki:Permission>
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
      
                  <c:forEach var="att" items="${wikiActionBean.attachments}" >
                    <tr>
                    	<%-- The 'title' attribute is used to sort empty cells --%>
                      <td title="${att.contentType}">
                        <div class="${fn:replace(att.contentType,'/','-')}">&nbsp;</div>
                      </td>
                      <td title="${att.name}">
                        <s:link beanclass="org.apache.wiki.action.AttachmentActionBean" event="download">
                          <s:param name="page" value="${att.path}" />
                          ${wiki:shorten(att.name,30)}
                        </s:link>
                      </td>
                      <td style="white-space:nowrap;text-align:right;">
                        <fmt:formatNumber value="${att.size div 1000}" maxFractionDigits="1" minFractionDigits="1" />&nbsp;<fmt:message key="info.kilobytes" />
                      </td>
                      <c:set var="attTitle"><fmt:message key="attach.moreinfo.title" /></c:set>
                      <td style="text-align:center;">
                        <s:link beanclass="org.apache.wiki.action.ViewActionBean" event="info" title="${attTitle}">
                          <s:param name="page" value="${att.path}" />
                          ${att.version}
                        </s:link>
                      </td>
                	    <td jspwiki:sortvalue="${wiki:iso8601date(att.lastModified)}">
                  	    <fmt:formatDate value="${att.lastModified}" pattern="${prefs.TimeFormat}" timeZone="${prefs.TimeZone}" />
                  	</td>
                      <td style="white-space:nowrap;" ><wiki:Author/></td>
                      <wiki:Permission permission="delete">
                        <td>
                          <s:form beanclass="org.apache.wiki.action.DeleteActionBean">
                            <s:param name="page" value="${att.path}" />
                            <s:submit name="delete"><fmt:message key="attach.delete" /></s:submit>
                          </s:form>
                        </td>
                      </wiki:Permission>
                      <td class="changenote"><c:out value="${att.changeNote}"/></td>
                    </tr>
                  </c:forEach>
      
                </table>
              </div>
            </div>
      
          </wiki:HasAttachments>
        </wiki:Tab>

        <%-- Info tab --%>
        <wiki:Tab id="info" titleKey="info.tab" accesskey="i" beanclass="org.apache.wiki.action.ViewActionBean" event="info">
          <wiki:Param name="page" value="${wikiActionBean.page.name}" />
        </wiki:Tab>
      </wiki:PageExists>

    </wiki:TabbedSection>
  </s:layout-component>
</s:layout-render>
