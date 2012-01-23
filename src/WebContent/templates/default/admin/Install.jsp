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
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib uri="http://stripes.sourceforge.net/stripes.tld" prefix="s"%>
<%@ page errorPage="/Error.jsp" %>
<s:layout-render name="${templates['layout/StaticLayout.jsp']}">

  <s:layout-component name="headTitle">
    <fmt:message key="install.title" />
  </s:layout-component>

  <s:layout-component name="pageTitle">
    <fmt:message key="install.title" />
  </s:layout-component>

  <s:layout-component name="content">
    <p><fmt:message key="install.intro.p1" /></p>
    <p><fmt:message key="install.intro.p2" /></p>
    
    <!-- Any messages or errors? -->
    <div class="instructions"><s:messages /></div>
    <div class="errors"><s:errors globalErrorsOnly="true" /></div>
    
    <div class="formcontainer">
      <s:form beanclass="org.apache.wiki.action.InstallActionBean">
      
        <!-- Admin password, application name, base URL and page directory -->
        <h3><fmt:message key="install.basics" /></h3>
        <div>
          <s:label for="adminPassword" />
          <s:text name="adminPassword" size="20" />
          <s:errors field="adminPassword" />
          <div class="description"><fmt:message key="install.adminPassword.description" /></div>
        </div>
        
        <div>
          <s:label for="properties.jspwiki.jspwiki_applicationName" />
          <s:text name="properties.jspwiki.jspwiki_applicationName" size="20" />
          <s:errors field="properties.jspwiki.jspwiki_applicationName" />
          <div class="description"><fmt:message key="install.applicationName.description" /></div>
        </div>
        <div>
          <s:label for="properties.jspwiki.jspwiki_baseURL" />
          <s:text name="properties.jspwiki.jspwiki_baseURL" size="40" />
          <s:errors field="properties.jspwiki.jspwiki_baseURL" />
          <div class="description"><fmt:message key="install.baseURL.description" /></div>
        </div>
        <div>
          <s:label for="properties.priha.priha_provider_defaultProvider_directory" />
          <s:text name="properties.priha.priha_provider_defaultProvider_directory" size="50" />
          <s:errors field="properties.priha.priha_provider_defaultProvider_directory" />
          <div class="description"><fmt:message key="install.pageDir.description" /></div>
        </div>
        <div>
          <s:label for="properties.jspwiki.default_set_of_pages_directory" />
          <s:text name="properties.jspwiki.default_set_of_pages_directory" size="50" />
          <s:errors field="properties.jspwiki.default_set_of_pages_directory" />
          <div class="description"><fmt:message key="install.initialPageDir.description" /></div>
        </div>
        
        <!-- Advanced settings: security, logging/work directories -->
        <h3><fmt:message key="install.advanced" /></h3>
        <div>
          <s:label for="logDirectory" />
          <s:text name="logDirectory" size="50" />
          <s:errors field="logDirectory" />
          <div class="description"><fmt:message key="install.logDirectory.description" /></div>
        </div>
        <div>
          <s:label for="properties.jspwiki.jspwiki_workDir" />
          <s:text name="properties.jspwiki.jspwiki_workDir" size="40" />
          <s:errors field="properties.jspwiki.jspwiki_workDir" />
          <div class="description"><fmt:message key="install.workDir.description" /></div>
        </div>
        <div>
          <s:label for="properties.jspwiki.jspwiki_approver_workflow_saveWikiPage" />
          <s:text name="properties.jspwiki.jspwiki_approver_workflow_saveWikiPage" size="40" />
          <s:errors field="properties.jspwiki.jspwiki_approver_workflow_saveWikiPage" />
          <div class="description"><fmt:message key="install.approver.saveWikiPage.description" /></div>
        </div>
        <div>
          <s:label for="properties.jspwiki.jspwiki_approver_workflow_createUserProfile" />
          <s:text name="properties.jspwiki.jspwiki_approver_workflow_createUserProfile" size="40" />
          <s:errors field="properties.jspwiki.jspwiki_approver_workflow_createUserProfile" />
          <div class="description"><fmt:message key="install.approver.createUserProfile.description" /></div>
        </div>
        <div>
          <s:label for="properties.jspwiki.jspwiki_userdatabase" />
          <s:select id="userdatabase" name="properties.jspwiki.jspwiki_userdatabase">
            <s:option value="org.apache.wiki.auth.user.XMLUserDatabase">XML (default)</s:option>
            <s:option value="org.apache.wiki.auth.user.LdapUserDatabase">LDAP</s:option>
          </s:select>
          <s:errors field="properties.jspwiki.jspwiki_userdatabase" />
          <div class="description"><fmt:message key="install.userdatabase.description" /></div>
        </div>
        <div>
          <s:button id="ldap.showConfig" name="ldap.showConfig"
            onclick="$('ldapDivConfig').style.display='block'; $('ldap.showConfig').style.display='none';" />
        </div>
      
        <!-- LDAP config -->
        <div id="ldapDivConfig" style="display:none;">
          <h3><fmt:message key="install.ldap" /></h3>
          <p><fmt:message key="install.ldap.description" /></p>
          <div>
            <s:label for="properties.jspwiki.ldap_config" />
            <s:select id="ldap.config" name="properties.jspwiki.ldap_config">
              <s:options-enumeration enum="org.apache.wiki.auth.LdapConfig.Default" label="name" />
            </s:select>
            <s:errors field="properties.jspwiki.ldap_config" />
            <div class="description"><fmt:message key="ldap.config.description" /></div>
          </div>
          <!-- LDAP connection settings and test button -->
          <div>
            <s:label for="properties.jspwiki.ldap_connectionURL" />
            <s:text name="properties.jspwiki.ldap_connectionURL" size="40" />
            <s:errors field="properties.jspwiki.ldap_connectionURL" />
            <div class="description"><fmt:message key="ldap.connectionURL.description" /></div>
          </div>
          <div>
            <s:label for="properties.jspwiki.ldap_authentication" />
            <s:select id="ldap.authentication" name="properties.jspwiki.ldap_authentication">
              <s:option value="DIGEST-MD5">DIGEST-MD5</s:option>
              <s:option value="simple">simple</s:option>
            </s:select>
            <s:errors field="properties.jspwiki.ldap_authentication" />
            <div class="description"><fmt:message key="ldap.authentication.description" /></div>
          </div>
          <div>
            <s:label for="properties.jspwiki.ldap_ssl" />
            <s:checkbox name="properties.jspwiki.ldap_ssl" />
            <div class="description"><fmt:message key="ldap.ssl.description" /></div>
          </div>
          <s:button name="ldapConnection" onclick="Stripes.submitFormEvent(form, 'testLdapConnection', 'ldapConnResults', null);" />
          <div class="description" id="ldapConnResults"></div>
          <!-- LDAP authentication settings and test button -->
          <div>
            <s:label for="properties.jspwiki.ldap_bindUser" />
            <s:text name="properties.jspwiki.ldap_bindUser" size="40" />
            <s:errors field="properties.jspwiki.ldap_bindUser" />
            <div class="description"><fmt:message key="ldap.bindUser.description" /></div>
          </div>
          <div>
            <s:label for="bindPassword" />
            <s:text name="bindPassword" size="20" />
            <s:errors field="bindPassword" />
            <div class="description"><fmt:message key="ldap.bindPassword.description" /></div>
          </div>
          <s:button name="ldapAuthentication" onclick="Stripes.submitFormEvent(form, 'testLdapAuthentication', 'ldapAuthResults', null);" />
          <div class="description" id="ldapAuthResults"></div>
          <!-- LDAP user database settings and test button -->
          <div>
            <s:label for="properties.jspwiki.ldap_userBase" />
            <s:text name="properties.jspwiki.ldap_userBase" size="40" />
            <s:errors field="properties.jspwiki.ldap_userBase" />
            <div class="description"><fmt:message key="ldap.userBase.description" /></div>
          </div>
          <s:button name="ldapUsers" onclick="Stripes.submitFormEvent(form, 'testLdapUsers', 'ldapUserResults', null);" />
          <div class="description" id="ldapUserResults"></div>
          <!-- LDAP authorizer settings and test button -->
          <div>
            <s:label for="properties.jspwiki.ldap_roleBase" />
            <s:text name="properties.jspwiki.ldap_roleBase" size="40" />
            <s:errors field="properties.jspwiki.ldap_roleBase" />
            <div class="description"><fmt:message key="ldap.roleBase.description" /></div>
          </div>
          <s:button name="ldapRoles" onclick="Stripes.submitFormEvent(form, 'testLdapRoles', 'ldapRoleResults', null);" />
          <div class="description" id="ldapRoleResults"></div>
        </div>
        
        <!-- Save the configuration -->
        <p><fmt:message key="install.configure.description" /><p>
        <s:submit name="save" />

      </s:form>
    </div>
  </s:layout-component>

</s:layout-render>
