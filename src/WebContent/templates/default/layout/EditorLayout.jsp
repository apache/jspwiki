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
<%@ page errorPage="/Error.jsp" %>
<s:layout-definition>
  <wiki:TabbedSection defaultTab="edit">
  
    <%-- View tab --%>
    <wiki:Tab id="view" titleKey="view.tab" accesskey="v">
      <wiki:InsertPage/>
    </wiki:Tab>

    <%-- Attachments tab --%>
    <wiki:Tab id="attachments" accesskey="a"
      title="${wiki:attachmentsTitle(request.Locale, wikiActionBean.attachments)}">
      <jsp:include page="${templates['tabs/AttachmentsTab.jsp']}" />
    </wiki:Tab>
      
    <%-- Info tab --%>
    <wiki:Tab id="info" titleKey="info.tab" accesskey="i">
      <jsp:include page="${templates['tabs/PageInfoTab.jsp']}" />
    </wiki:Tab>

    <%-- Editor tab --%>
    <wiki:Tab id="edit" titleKey="edit.tab.edit" accesskey="e">
      <s:layout-component name="editor" />
    </wiki:Tab>
    
    <%-- Preview tab --%>
    <wiki:Tab id="preview" titleKey="preview.tab" accesskey="p"
      onclick="Stripes.submitFormEvent('editform', 'preview', 'previewContent', null);">
      <div class="information">
        <fmt:message key="preview.info" />
      </div>
      <div id="previewContent"></div>
    </wiki:Tab>

    <%-- Help tab --%>
    <wiki:Tab id="help" titleKey="edit.tab.help" accesskey="h">
      <wiki:InsertPage page="EditPageHelp" />
      <wiki:NoSuchPage page="EditPageHelp">
        <div class="error">
          <fmt:message key="comment.edithelpmissing">
            <fmt:param><wiki:EditLink page="EditPageHelp">EditPageHelp</wiki:EditLink></fmt:param>
          </fmt:message>
        </div>
      </wiki:NoSuchPage>  
    </wiki:Tab>
  
  </wiki:TabbedSection>
</s:layout-definition>
