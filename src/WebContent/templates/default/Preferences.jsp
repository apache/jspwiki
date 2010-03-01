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
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
<%@ page errorPage="/Error.jsp" %>
<s:layout-render name="${templates['layout/DefaultLayout.jsp']}">

  <s:layout-component name="script">
    <script type="text/javascript" src="<wiki:Link format='url' jsp='scripts/jspwiki-prefs.js' />"></script>
  </s:layout-component>

  <s:layout-component name="content">
    <wiki:TabbedSection defaultTab="${param.tab}">
    
      <%-- Preferences tab --%>
      <wiki:Tab id="prefs" titleKey="prefs.tab.prefs" accesskey="p">
        <h3><fmt:message key="prefs.heading"><fmt:param><wiki:Variable var="applicationname" /></fmt:param></fmt:message></h3>
        <!-- Any messages or errors? -->
        <div class="errors"><s:errors beanclass="org.apache.wiki.action.UserPreferencesActionBean" globalErrorsOnly="true" /></div>

        <div class="formcontainer">
          <s:form beanclass="org.apache.wiki.action.UserPreferencesActionBean"
              class="wikiform" id="setCookie" method="post" acceptcharset="UTF-8">
            <s:param name="tab" value="prefs" />
              
            <%-- Asserted name --%>
            <wiki:UserCheck status="notauthenticated">
              <div>
                <s:label for="assertedName" />
                <s:text id="assertedName" name="assertedName" size="20" />
                <s:errors field="assertedName" />
                <div class="description">
                  <fmt:message key="prefs.assertedname.description">
                    <fmt:param><wiki:Variable var="applicationname" /></fmt:param>
                    <fmt:param>
                      <a href="<wiki:Link jsp='Login.jsp' format='url'></wiki:Link>">
                        <fmt:message key="prefs.assertedname.create" />
                      </a>
                    </fmt:param>
                  </fmt:message>
                </div>
              </div>
            </wiki:UserCheck>
                
            <%-- Editor --%>
            <div>
              <s:label for="editor" />
              <select id="editor" name="editor">
                <wiki:EditorIterator id="edt">
                  <option <%=edt.isSelected()%> value="<%=edt.getName()%>"><%=edt.getName()%></option>
                </wiki:EditorIterator>
              </select>
              <s:errors field="editor" />
            </div>
            
            <%-- Section editing --%>
            <div>
              <s:label for="sectionEditing" />
              <s:checkbox id="sectionEditing" name="sectionEditing"  />&nbsp;<fmt:message key="prefs.user.sectionediting.text" />
              <s:errors field="sectionEditing" />
            </div>
            
            <%-- Skin --%>
            <div>
              <s:label for="skin" />
              <s:select id="skin" name="skin">
                <s:options-collection collection="${prefs.availableSkins}" />
              </s:select>
              <s:errors field="skin" />
            </div>
            
            <%-- Locale --%>
            <c:if test='${not empty prefs.availableLocales}'>
              <div>
                <s:label for="locale" />
                <s:select name="locale">
                  <s:options-map map="${prefs.availableLocales}" />
                </s:select>
                <s:errors field="locale" />
              </div>
            </c:if>
            
            <%-- Orientation --%>
            <div>
              <s:label for="orientation" />
              <s:select id="orientation" name="orientation">
                <s:options-enumeration enum="org.apache.wiki.preferences.Preferences.Orientation" label="name" />
              </s:select>
              <s:errors field="orientation" />
            </div>
            
            <%-- Time format --%>
            <div>
              <s:label for="timeFormat" />
              <s:select id="timeFormat" name="timeFormat">
                <s:options-map map="${prefs.availableTimeFormats}" />
              </s:select>
              <s:errors field="timeFormat" />
            </div>
            
            <%-- Time zone --%>
            <div>
              <s:label for="timeZone" />
              <s:select id="timeZone" name="timeZone">
                <c:forEach var="item" items="${prefs.availableTimeZones}" >
                  <s:option value="${item}" formatType="id" ><s:format value="${item}" /></s:option>
                </c:forEach>
              </s:select>
              <s:errors field="timeZone" />
            </div>
            
            <%-- Save user preferences --%>
            <s:submit name="save" accesskey="s" />
            <div class="formhelp">
              <fmt:message key='prefs.cookies' />
            </div>

          </s:form>
        </div>
        
        <%-- Clearing the 'asserted name' and other prefs in the cookie --%>
        <h3><fmt:message key='prefs.clear.heading' /></h3>
        <div class="formcontainer">
          <s:form beanclass="org.apache.wiki.action.UserPreferencesActionBean"
                         id="clearCookie"
                     method="post"
              acceptcharset="UTF-8">
            <div>
              <s:submit name="clearAssertedName" />
            </div>
            <div class="formhelp"><fmt:message key="prefs.clear.description" /></div>
          </s:form>
        </div>
      </wiki:Tab>
    
      <%-- Profile tab --%>
      <wiki:UserCheck status="authenticated">
        <wiki:Tab id="profile" titleKey="prefs.tab.profile" accesskey="o">
          <wiki:Permission permission="editProfile">
            <jsp:include page="${templates['tabs/ProfileTab.jsp']}" />
          </wiki:Permission>
        </wiki:Tab>
      </wiki:UserCheck>
    
    </wiki:TabbedSection>
  </s:layout-component>

</s:layout-render>
