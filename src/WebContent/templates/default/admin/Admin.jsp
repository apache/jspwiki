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
<%@ taglib uri="http://jakarta.apache.org/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="http://stripes.sourceforge.net/stripes.tld" prefix="s" %>
<%@ page import="org.apache.wiki.WikiContext" %>
<%@ page errorPage="${templates['Error.jsp']}" %>
<s:layout-render name="${templates['layout/DefaultLayout.jsp']}">

  <s:layout-component name="headTitle">
    JSPWiki Administration
  </s:layout-component>
  
  <s:layout-component name="pageTitle">
    JSPWiki Administration
  </s:layout-component>

  <s:layout-component name="content">
    <h1>JSPWiki Administration</h1>
    <div class="information">Not all things can be configured here.  Some things need to be configured
    in your <tt>jspwiki.properties</tt> file.</div>
    
      <wiki:TabbedSection defaultTab="${param['tab']}">
      
        <wiki:Tab id="security" title="Security">
          <jsp:include page="${templates['admin/tabs/Security.jsp']}" />
        </wiki:Tab>

        <wiki:Tab id="users" title="Users"
          beanclass="org.apache.wiki.action.AdministerProfilesActionBean" />
          
        <wiki:Tab id="groups" title="Groups">
          <div>
            <p>This is a list of all groups in this wiki.  If you click on the group name,
            you will be taken to the administration page of that particular group.</p>
            <p><wiki:Plugin plugin="Groups" /></p>
          </div>
        </wiki:Tab>
          
        <wiki:AdminBeanIterator type="core" id="ab">
          <wiki:Tab id="${ab.id}" title="${ab.title}">
            <div class="formcontainer">
              <s:form beanclass="org.apache.wiki.action.AdminActionBean" method="post" acceptcharset="UTF-8">
                <s:hidden name="tab-admin" value="core" />
                <s:hidden name="tab-core" value="${ab.title}" />
                <s:hidden name="bean" value="${ab.id}" />
                <%= ab.doGet( (WikiContext)request.getAttribute( "wikiActionBeanContext" ) ) %>
                <s:submit name="admin" value="Submit" />
              </s:form>
            </div>
          </wiki:Tab>
        </wiki:AdminBeanIterator>
          
        <wiki:AdminBeanIterator type="editors" id="ab">
          <wiki:Tab id="${ab.id}" title="${ab.title}">
            <div class="formcontainer"> 
              <s:form beanclass="org.apache.wiki.action.AdminActionBean" method="post" acceptcharset="UTF-8">
                <s:hidden name="tab-admin" value="editors" />
                <s:hidden name="tab-editors" value="${ab.title}" />
                <s:hidden name="bean" value="${ab.id}" />
                <%= ab.doGet( (WikiContext)request.getAttribute( "wikiActionBeanContext" ) ) %>
                <s:submit name="admin" value="Submit" />
              </s:form>
            </div>
          </wiki:Tab>
        </wiki:AdminBeanIterator>
    
        <wiki:Tab id="filters" title="Filters">
          <p>There will be more filter stuff here</p>
        </wiki:Tab>
        
      </wiki:TabbedSection>
    </div>
  </s:layout-component>
</s:layout-render>
