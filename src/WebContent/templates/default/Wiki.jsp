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
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://jakarta.apache.org/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="http://stripes.sourceforge.net/stripes.tld" prefix="s" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
<%@ page import="org.apache.wiki.*" %>
<%@ page import="org.apache.wiki.action.WikiContextFactory" %>
<%@ page import="org.apache.wiki.api.WikiPage" %>
<%@ page import="org.apache.wiki.attachment.*" %>
<%@ page import="org.apache.wiki.util.TextUtil" %>
<%@ page errorPage="/Error.jsp" %>
<%
	WikiContext c = WikiContextFactory.findContext( pageContext );
  WikiPage p = c.getPage();
	String pagename = p.getName();

	/* check possible permalink (blogentry) pages */
	String blogcommentpage="";
	String mainblogpage="";
	if( pagename.indexOf("_blogentry_") != -1 )
	{
		blogcommentpage = TextUtil.replaceString( pagename, "blogentry", "comments" );
		mainblogpage = pagename.substring(0, pagename.indexOf("_blogentry_"));
	}
%>
<s:layout-render name="${templates['layout/DefaultLayout.jsp']}">

  <%-- If wiki page is current, allow search engines to spider it --%>
  <wiki:CheckVersion mode="latest">
    <s:layout-component name="headMetaRobots">
      <meta name="robots" content="index,follow" />
    </s:layout-component>
  </wiki:CheckVersion>
    
  <s:layout-component name="content">
    <wiki:TabbedSection defaultTab="view">

      <wiki:NoSuchPage>
        <wiki:Tab id="view" titleKey="view.tab" accesskey="v">
          <fmt:message key="common.nopage">
            <fmt:param><wiki:EditLink><fmt:message key="common.createit" /></wiki:EditLink></fmt:param>
          </fmt:message>
        </wiki:Tab>
      </wiki:NoSuchPage>

      <wiki:PageExists>

        <%-- View tab --%>
        <wiki:Tab id="view" titleKey="view.tab" accesskey="v">
            <%-- If the page is an older version, then offer a note and a possibility
                 to restore this version as the latest one. --%>
          <wiki:CheckVersion mode="notlatest">
            <s:form beanclass="org.apache.wiki.action.ViewActionBean" method="get" acceptcharset='UTF-8'>
              <s:hidden name="page" />     
              <div class="warning">
                <fmt:message key="view.oldversion">
                  <fmt:param>
                    <select id="version" name="version" onchange="this.form.submit();">
<% 
int latestVersion = c.getEngine().getPage( pagename, WikiProvider.LATEST_VERSION ).getVersion();
int thisVersion = p.getVersion();

if( thisVersion == WikiProvider.LATEST_VERSION ) thisVersion = latestVersion; //should not happen
  for( int i = 1; i <= latestVersion; i++) 
  {
%> 
                      <option value="<%= i %>" <%= ((i==thisVersion) ? "selected='selected'" : "") %> ><%= i %></option>
<%
  }    
%>
                    </select>
                  </fmt:param>
                </fmt:message>  
                <br/>
                <wiki:LinkTo><fmt:message key="view.backtocurrent" /></wiki:LinkTo>&nbsp;&nbsp;
                <wiki:EditLink version="this"><fmt:message key="view.restore" /></wiki:EditLink>
              </div>
          
            </s:form>
          </wiki:CheckVersion>
          
          <%-- Inserts no text if there is no page. --%>
          <wiki:InsertPage/>
          
<% if( ! mainblogpage.equals("") ) { %>
          <wiki:PageExists page="<%= mainblogpage%>">
          
            <% if( ! blogcommentpage.equals("") ) { %>
            <wiki:PageExists page="<%= blogcommentpage%>">
            	<div class="weblogcommentstitle"><fmt:message key="blog.commenttitle" /></div>
              <div class="weblogcomments"><wiki:InsertPage page="<%= blogcommentpage%>" /></div>
            </wiki:PageExists>
            <% }; %>
            <div class="information">	
          	  <wiki:Link page="<%= mainblogpage %>"><fmt:message key="blog.backtomain" /></wiki:Link>&nbsp; &nbsp;
          	  <wiki:Link context="comment" page="<%= blogcommentpage%>"><fmt:message key="blog.addcomments" /></wiki:Link>
            </div>
          
          </wiki:PageExists>
<% }; %>

          <wiki:PageType type="attachment">
            <div class="information">
      	    <fmt:message key="info.backtoparentpage">
      	      <fmt:param><wiki:LinkToParent><wiki:ParentPageName/></wiki:LinkToParent></fmt:param>
              </fmt:message>
            </div>
            <div style="overflow:hidden;">
              <wiki:Translate>[${wikiActionBean.page.name}]</wiki:Translate>
            </div>
          </wiki:PageType>    
        </wiki:Tab>

        <%-- Attachments tab --%>
        <wiki:Tab id="attachments" accesskey="a"
          title="${wiki:attachmentsTitle(request.Locale, wikiActionBean.attachments)}"
          beanclass="org.apache.wiki.action.ViewActionBean" event="attachments">
          <wiki:Param name="page" value="${wikiActionBean.page.name}" />
        </wiki:Tab>
        
        <%-- Info tab --%>
        <wiki:Tab id="info" titleKey="info.tab" accesskey="i"
          beanclass="org.apache.wiki.action.ViewActionBean" event="info">
          <wiki:Param name="page" value="${wikiActionBean.page.name}" />
        </wiki:Tab>

        <%-- Edit tab --%>
        <wiki:Permission permission="edit">
          <wiki:Tab id="edit" titleKey="edit.tab.edit" accesskey="e"
            beanclass="org.apache.wiki.action.EditActionBean" event="edit">
            <wiki:Param name="page" value="${wikiActionBean.page.name}" />
          </wiki:Tab>
        </wiki:Permission>

      </wiki:PageExists>

    </wiki:TabbedSection>
  </s:layout-component>
  
</s:layout-render>
