<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://stripes.sourceforge.net/stripes.tld" prefix="stripes" %>
<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<fmt:setBundle basename="templates.DefaultResources"/>

<stripes:layout-render name="/WEB-INF/jsp/templates/default/ViewTemplate.jsp">
  <stripes:layout-component name="contents">

    <h3><fmt:message key="prefs.heading.yourprofile"><fmt:param><wiki:Variable var="applicationname"/></fmt:param></fmt:message></h3>
    
    <wiki:TabbedSection defaultTab='<%=request.getParameter("tab")%>'>
      <!-- Tab 1: user preferences -->
      <wiki:Tab id="prefs" title="prefs.tab.prefs">
        <div class="formcontainer">
          <div class="instructions">
            <fmt:message key="prefs.instructions"/>
          </div>
        </div>
        <wiki:Permission permission="editPreferences">
          <wiki:UserCheck status="anonymous">
            <div class="formcontainer">
              <div class="instructions">
                <wiki:Messages div="error" topic="prefs" prefix="Could not save prefs: "/>
              </div>
              <stripes:form id="createAssertedName" action="/UserPreferences.action" method="POST" acceptcharset="UTF-8">
                <stripes:errors/>
                <div class="block">
                  <stripes:label for="assertedName" />
                  <stripes:text name="assertedName" size="30"><wiki:UserProfile property="asserted"/></stripes:text>
                  <div class="description">
                    <fmt:message key="prefs.wikiname.description">
                      <fmt:param><wiki:Variable var="applicationname" /></fmt:param>
                    </fmt:message>
                  </div>
                  <div class="description">
                    <fmt:message key="prefs.wikiname.description2">
                      <fmt:param><a onclick="TabbedSection.onclick('profile')" ><fmt:message key="prefs.wikiname.create"/></a></fmt:param>
                    </fmt:message>
                  </div>
                  <stripes:hidden name="tab" value="prefs" />
                  <stripes:submit name="createAssertedName"/>
                </div>
              </stripes:form>
            </div>
          </wiki:UserCheck>
          
          <!-- Clearing the 'asserted name' cookie -->
          <wiki:UserCheck status="asserted">
            <div class="formcontainer">
              <stripes:form id="clearAssertedName" action="/UserPreferences.action" method="POST" acceptcharset="UTF-8">
                <div class="block">
                  <div class="description">
                    <fmt:message key="prefs.clear.description"/>
                  </div>
                  <stripes:hidden name="tab" value="prefs" />
                  <stripes:submit name="clearAssertedName"/>
                </div>
              </stripes:form>
              <stripes:form id="editFavorites" action="/UserPreferences.action" method="GET">
                 <div class="block">
                  <div class="description">
                     <fmt:message key="prefs.favs.description"/>
                  </div>
                  <stripes:hidden name="tab" value="prefs" />
                  <stripes:submit name="editFavorites"/>
                 </div>
              </stripes:form>
            </div>
          </wiki:UserCheck>
        </wiki:Permission>
      </wiki:Tab>
      
      <!-- Tab 2: If user can register, allow edits to profile -->
      <wiki:Tab id="profile" title="prefs.tab.profile">
        <wiki:Permission permission="editProfile">
          <div class="formcontainer">
            <div class="instructions">
              <wiki:UserProfile property="new">
                <fmt:message key="prefs.newprofile"/>
              </wiki:UserProfile>
              <wiki:UserProfile property="exists">
                <fmt:message key="prefs.oldprofile"/>
              </wiki:UserProfile>
            </div>
            <stripes:form id="saveProfile" action="/UserProfile.action" method="POST" acceptcharset="UTF-8">
                  
              <!-- Any errors? -->
              <stripes:errors/>
        
              <!-- Login name -->
              <div class="block">
                <stripes:label for="loginName"/>
                <wiki:UserProfile property="new">
                  <stripes:text name="loginName" size="30" value="${this.loginName}"/>
                  <div class="description">
                    <fmt:message key="prefs.loginname.description"/>
                  </div>
                </wiki:UserProfile>
                <wiki:UserProfile property="exists">
                  <p><wiki:UserProfile property="loginname"/></p>
                  <div class="description">
                    <fmt:message key="prefs.loginname.exists"/>
                  </div>
                </wiki:UserProfile>
              </div>
              
              <!-- Password; not displayed if container auth used -->
              <wiki:UserCheck status="setPassword">
                <div class="block">
                  <stripes:label for="password"/>
                  <stripes:password name="password" size="30" value="" />
                  <div class="description">
                    <fmt:message key="prefs.password.description"/>
                  </div>
                </div>
          
                <div class="block">
                  <stripes:label for="passwordAgain"/>
                  <stripes:password name="passwordAgain" size="30" value="" />
                  <div class="description">
                    <fmt:message key="prefs.password2.description"/>
                  </div>
                </div>
              </wiki:UserCheck>
              
              <!-- Full name -->
              <div class="block">
                <stripes:label for="fullName"/>
                <wiki:UserProfile property="new">
                  <stripes:text name="fullName" size="30" value="${this.fullName}" />
                  <div class="description">
                    <fmt:message key="prefs.fullname.description"/>
                  </div>
                </wiki:UserProfile>
                <wiki:UserProfile property="exists">
                  <p><wiki:UserProfile property="fullname"/></p>
                  <div class="description">
                    <fmt:message key="prefs.fullname.exists"/>
                  </div>
                </wiki:UserProfile>
              </div>
               
              <!-- E-mail -->
              <div class="block">
                <stripes:label for="email"/>
                <stripes:text name="email" size="30" value="${this.email}" />
                <div class="description">
                  <fmt:message key="prefs.email.description"/>
                </div>
              </div>
              
              <div class="block">
                <wiki:UserCheck status="assertionsAllowed">
                  <div class="instructions">
                    <fmt:message key="prefs.cookie.info"/>
                  </div>
                </wiki:UserCheck>
                <wiki:UserProfile property="exists">
                  <div class="instructions">
                    <fmt:message key="prefs.acl.info">
                      <fmt:param><strong><wiki:UserProfile property="wikiname"/></strong></fmt:param>
                      <fmt:param><strong><wiki:UserProfile property="fullname"/></strong></fmt:param>
                      <fmt:param><strong><wiki:UserProfile property="roles" /></strong></fmt:param>
                      <fmt:param><strong><wiki:UserProfile property="groups" /></strong></fmt:param>
                    </fmt:message>
                  </div>
                </wiki:UserProfile>
                <div class="instructions">
                  <wiki:UserProfile property="exists">
                    <fmt:message key="prefs.lastmodified">
                      <fmt:param><wiki:UserProfile property="created"/></fmt:param>
                      <fmt:param><wiki:UserProfile property="modified"/></fmt:param>
                    </fmt:message>
                  </wiki:UserProfile>
                  <fmt:message key="prefs.save.description"/>
                </div>
                <stripes:hidden name="tab" value="profile" />
                <stripes:submit name="ok" />
              </div>
            </stripes:form>
          </div>
        </wiki:Permission> 
      </wiki:Tab>
    </wiki:TabbedSection>

  </stripes:layout-component>
</stripes:layout-render>
