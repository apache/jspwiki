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
<%@ page import="org.apache.wiki.*" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
<%@ page import="org.apache.wiki.action.WikiContextFactory" %>
<%
  WikiContext c = WikiContextFactory.findContext( pageContext );
  int attCount = c.getEngine().getAttachmentManager().listAttachments(c.getPage()).size();
  String attTitle = LocaleSupport.getLocalizedMessage(pageContext, "attach.tab");
  if( attCount != 0 ) attTitle += " (" + attCount + ")";
%>
  
<wiki:TabbedSection defaultTab="editcontent">

  <wiki:Tab id="editcontent" titleKey="edit.tab.edit" accesskey="e">
    <wiki:Editor/>
  </wiki:Tab>
  
  <wiki:PageExists>  
    <wiki:Tab id="attach" title="<%= attTitle %>" accesskey="a" url="Attachments.jsp?page=${wikiActionBean.page.name}" />
    <wiki:Tab id="info" titleKey="info.tab" url="PageInfo.jsp?page=${wikiActionBean.page.name}" accesskey="i" />
  </wiki:PageExists>  

  <wiki:Tab id="edithelp" titleKey="edit.tab.help" accesskey="h">
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