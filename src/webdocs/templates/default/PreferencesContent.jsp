<%@ page errorPage="/Error.jsp" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
<fmt:setBundle basename="templates.default"/>
<%
  //  Determine the name for the user's favorites page
  WikiContext c = WikiContext.findContext( pageContext );
  String pagename = c.getName();
  String username = null;
 
  username = c.getEngine().getVariable( c, "username" );
  if( username == null ) username = "";

  String myFav = username + "Favorites";
%>

<h3><fmt:message key="prefs.heading.yourprofile"><fmt:param><wiki:Variable var="applicationname"/></fmt:param></fmt:message></h3>

<wiki:TabbedSection defaultTab='<%=request.getParameter("tab")%>'>
  <!-- Tab 1: user preferences -->
  <wiki:Tab id="prefs" title="<%=LocaleSupport.getLocalizedMessage(pageContext,"prefs.tab.prefs")%>">
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
          <form id="setCookie" action="<wiki:Link jsp="UserPreferences.jsp" format="url"><wiki:Param name="tab" value="prefs"/></wiki:Link>" 
                method="POST" accept-charset="UTF-8">
            <div class="block">
              <label><fmt:message key="prefs.assertedname"/></label>
              <input type="text" name="assertedName" size="30" value="<wiki:UserProfile property="loginname"/>" />
              <div class="description">
                <fmt:message key="prefs.assertedname.description">
                  <fmt:param><wiki:Variable var="applicationname" /></fmt:param>
                </fmt:message>
              </div>
              <div class="description">
                <fmt:message key="prefs.assertedname.description2">
                  <fmt:param><a onclick="TabbedSection.onclick('profile')" ><fmt:message key="prefs.assertedname.create"/></a></fmt:param>
                </fmt:message>
              </div>
              <input type="submit" name="ok" value="<fmt:message key="prefs.submit.setname"/>" />
              <input type="hidden" name="action" value="setAssertedName" />
            </div>
          </form>
        </div>
      </wiki:UserCheck>
      
      <!-- Clearing the 'asserted name' cookie -->
      <wiki:UserCheck status="asserted">
        <div class="formcontainer">
          <form id="clearCookie" action="<wiki:Link format="url" jsp="UserPreferences.jsp"><wiki:Param name="tab" value="prefs"/></wiki:Link>"
                method="POST" accept-charset="UTF-8">
            <div class="block">
              <div class="description">
                <fmt:message key="prefs.clear.description"/>
              </div>
              <input type="submit" name="ok" value="<fmt:message key="prefs.clear.submit"/>" />
              <input type="hidden" name="action" value="clearAssertedName" />
            </div>
          </form>
          <form action="<wiki:Link format="url" page="<%=myFav%>"/>" method="GET">
             <div class="block">
              <div class="description">
                 <fmt:message key="prefs.favs.description"/>
              </div>
              <input type="submit" name="ok" value="<fmt:message key="prefs.favs.submit"/>"/>
             </div>
          </form>
        </div>
      </wiki:UserCheck>
    </wiki:Permission>
  </wiki:Tab>
  
  <!-- Tab 2: If user can register, allow edits to profile -->
  <wiki:Tab id="profile" title="<%=LocaleSupport.getLocalizedMessage(pageContext,"prefs.tab.profile")%>">
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
        <div class="instructions">
          <wiki:Messages div="error" topic="profile" prefix='<%=LocaleSupport.getLocalizedMessage(pageContext,"prefs.errorprefix")%>'/>
        </div>
        <form id="editProfile" action="<wiki:Link jsp="UserPreferences.jsp" format="url"><wiki:Param name="tab" value="profile"/></wiki:Link>" 
              method="POST" accept-charset="UTF-8">
              
          <!-- Login name -->
          <div class="block">
            <label><fmt:message key="prefs.loginname"/></label>
            <wiki:UserProfile property="new">
              <input type="text" name="loginname" size="30" value="<wiki:UserProfile property="loginname"/>" />
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
              <label><fmt:message key="prefs.password"/></label>
              <input type="password" name="password" size="30" value="" />
              <div class="description">
                <fmt:message key="prefs.password.description"/>
              </div>
            </div>
      
            <div class="block">
              <label><fmt:message key="prefs.password2"/></label>
              <input type="password" name="password2" size="30" value="" />
              <div class="description">
                <fmt:message key="prefs.password2.description"/>
              </div>
            </div>
          </wiki:UserCheck>
          
          <!-- Full name -->
          <div class="block">
            <label><fmt:message key="prefs.fullname"/></label>
            <wiki:UserProfile property="new">
              <input type="text" name="fullname" size="30" value="<wiki:UserProfile property="fullname"/>" />
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
            <label><fmt:message key="prefs.email"/></label>
            <input type="text" name="email" size="30" value="<wiki:UserProfile property="email"/>" />
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
            <input type="submit" name="ok" value="<fmt:message key="prefs.save.submit"/>" />
            <input type="hidden" name="action" value="saveProfile" />
          </div>
        </form>
      </div>
    </wiki:Permission> 
  </wiki:Tab>
</wiki:TabbedSection>
