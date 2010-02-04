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
<s:layout-render name="${templates['layout/DefaultLayout.jsp']}">
  <s:layout-component name="content">
    <wiki:TabbedSection defaultTab="${param.tab}">
      <wiki:NoSuchPage>
        <wiki:Tab id="${param.tab}" titleKey="info.tab" accesskey="i">
          <fmt:message key="common.nopage">
            <fmt:param><wiki:EditLink><fmt:message key="common.createit" /></wiki:EditLink></fmt:param>
          </fmt:message>
        </wiki:Tab>
      </wiki:NoSuchPage>

      <wiki:PageExists>

        <%-- View tab --%>
        <wiki:Tab id="view" titleKey="view.tab" accesskey="v"
          beanclass="org.apache.wiki.action.ViewActionBean" event="view">
          <wiki:Param name="page" value="${wikiActionBean.page.name}" />
        </wiki:Tab>

        <%-- Edit tab --%>
        <wiki:Permission permission="edit">
          <wiki:Tab id="edit" titleKey="edit.tab.edit" accesskey="e"
            beanclass="org.apache.wiki.action.EditActionBean" event="edit">
            <wiki:Param name="page" value="${wikiActionBean.page.name}" />
          </wiki:Tab>
        </wiki:Permission>
          
        <%-- Attachments tab --%>
        <wiki:Tab id="attachments" accesskey="a"
          title="${wiki:attachmentsTitle(request.Locale, wikiActionBean.attachments)}">
          <%-- Add attachments --%>
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
          <%-- Existing attachments --%>
          <jsp:include page="${templates['tabs/AttachmentsTab.jsp']}" />
        </wiki:Tab>

        <%-- Info tab --%>
        <wiki:Tab id="info" titleKey="info.tab" accesskey="i">
        
          <%-- Rename page --%>
          <wiki:Permission permission="rename">
            <s:form beanclass="org.apache.wiki.action.RenameActionBean" class="wikiform" id="renameform" method="post" acceptcharset="UTF-8" >
              <p>
                <s:hidden name="page" />
                <s:submit name="rename" />
                <s:text name="renameTo" size="40" />
                &nbsp;
                <s:checkbox name="changeReferences" />
                <fmt:message key="info.updatereferrers" />
              </p>
            </s:form>
          </wiki:Permission>
          
          <%-- Delete page --%>
          <wiki:Permission permission="delete">
            <s:form beanclass="org.apache.wiki.action.DeleteActionBean" class="wikiform" id="deleteForm" method="post" acceptcharset="UTF-8">
              <p>
                <s:hidden name="page" />
                <s:submit name="delete" />
              </p>
            </s:form>
          </wiki:Permission>
          
          <%-- Referring pages, previous versions --%>
          <jsp:include page="${'tabs/PageInfoTab.jsp'}" />

        </wiki:Tab>
      </wiki:PageExists>
    </wiki:TabbedSection>
  </s:layout-component>
</s:layout-render>
