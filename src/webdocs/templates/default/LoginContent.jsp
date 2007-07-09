<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.auth.*" %>
<%@ page errorPage="/Error.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
<fmt:setBundle basename="templates.default"/>

<%!
  public void jspInit()
  {
    WikiEngine wiki = WikiEngine.getInstance( getServletConfig() );
    AuthenticationManager mgr = wiki.getAuthenticationManager();
    if ( mgr.isContainerAuthenticated() )
    {
      postURL = "j_security_check";
    }
    else
    {
      postURL = "Login.jsp";
    }
  }
  String postURL="";
%>
<% boolean supportsCookieAuthentication = WikiEngine.getInstance(getServletConfig()).getAuthenticationManager().allowsCookieAuthentication(); %>
<wiki:TabbedSection defaultTab="${param.tab}">

  <wiki:UserCheck status="notauthenticated">
  <wiki:Tab id="logincontent" title="<%=LocaleSupport.getLocalizedMessage(pageContext, "login.tab")%>">
    <%--<wiki:Include page='LoginTab.jsp'/>--%>

<form action="<%=postURL%>"
          id="login"
       class="wikiform"
    onsubmit="return Wiki.submitOnce(this);"
      method="post" accept-charset="<wiki:ContentEncoding />" >

<div class="center">

  <h3><fmt:message key="login.heading.login"><fmt:param><wiki:Variable var="applicationname" /></fmt:param></fmt:message></h3>

  <div class="formhelp"><fmt:message key="login.help"></fmt:message></div>

  <table>
    <tr>
      <td colspan="2" class="formhelp">
        <wiki:Messages div="error" prefix='<%=LocaleSupport.getLocalizedMessage(pageContext,"login.errorprefix")%>' />
      </td>
    </tr>
    <tr>
      <td><label for="j_username"><fmt:message key="login.login"/></label></td>
      <td><input type="text" size="24" value="<wiki:Variable var='uid' default='' />"
                 name="j_username" id="j_username" /></td>
    </tr>
    <tr>
      <td><label for="j_password"><fmt:message key="login.password"/></label></td>
      <td><input type="password" size="24"
                 name="j_password" id="j_password" /></td>
    </tr>
    <% if( supportsCookieAuthentication ) { %>
    <tr>
      <td><label for="j_remember"><fmt:message key="login.remember"/></label></td>
      <td><input type="checkbox"
                 name="j_remember" id="j_remember" /></td>
    </tr>
    <% } %>
    <tr>
      <td />
      <td>
        <input type="hidden" name="redirect" value="<wiki:Variable var='redirect' default='' />" />
        <input type="submit" name="submitlogin" value="<fmt:message key='login.submit.login'/>" />
      </td>
    </tr>
    </table>

    <div class="formhelp">
      <fmt:message key="login.lostpw"/>
      <%--<a href="LostPassword.jsp">--%>
      <a href="#" onclick="TabbedSection.onclick('lostpassword');"
                    title="<fmt:message key='login.lostpw.title'/>" >
        <fmt:message key="login.lostpw.getnew"/>
      </a>
    </div>
    <div class="formhelp">
      <fmt:message key="login.nopassword"/>
      <%--<a href="UserPreferences.jsp?tab=profile">--%>
      <%--FIXME shold be href"#register"--%>
      <a href="#" onclick="TabbedSection.onclick('register');"
                    title="<fmt:message key='login.registernow.title'/>" >
        <fmt:message key="login.registernow">
          <fmt:param><wiki:Variable var="applicationname" /></fmt:param>
        </fmt:message>
      </a>
    </div>

</div>
</form>


  </wiki:Tab>
  </wiki:UserCheck>

  <wiki:Tab id="lostpassword" title="<%=LocaleSupport.getLocalizedMessage(pageContext, "login.lostpw.tab")%>">

<%-- FIXME error flow on lostpw nok --%>

<div class="center">
<form action="LostPassword.jsp"
          id="lostpw"
       class="wikiform"
    onsubmit="return Wiki.submitOnce(this);"
      method="post" accept-charset="<wiki:ContentEncoding />" >

  <h3><fmt:message key="login.lostpw.heading" /></h3>

  <div class="formhelp"><fmt:message key="login.lostpw.help"></fmt:message></div>

  <table>
    <tr>
      <td colspan="2" class="formhelp">
        <wiki:Messages div="error" prefix='<%=LocaleSupport.getLocalizedMessage(pageContext,"login.errorprefix")%>' />
      </td>
    </tr>
    <tr>
      <td><label for="name"><fmt:message key="login.lostpw.name"/></label></td>
      <td><input type="text" size="24" name="name" id="name" /></td>
    </tr>
    <tr>
      <td />
      <td>
        <input type="hidden" name="action" value="resetPassword"/>
        <input type="submit" name="Submit" value="<fmt:message key='login.lostpw.submit'/>" />
      </td>
    </tr>
  </table>

    <div class="formhelp">
      <fmt:message key="login.nopassword"/>
      <%--<a href="UserPreferences.jsp?tab=profile">--%>
      <a href="#" onclick="TabbedSection.onclick('register');"
                    title="<fmt:message key='login.registernow.title'/>" >
        <fmt:message key="login.registernow">
          <fmt:param><wiki:Variable var="applicationname" /></fmt:param>
        </fmt:message>
      </a>
    </div>
    <div class="formhelp">
      <fmt:message key="login.invite"/>
      <a href="#" onclick="TabbedSection.onclick('logincontent');"
                    title="<fmt:message key='login.title'/>" >
        <fmt:message key="login.heading.login"><fmt:param><wiki:Variable var="applicationname" /></fmt:param></fmt:message>
      </a>
    </div>

</form>
</div>

  </wiki:Tab>

  <wiki:Permission permission='editProfile'>
  <wiki:Tab id="register" title="<%=LocaleSupport.getLocalizedMessage(pageContext, "login.register.tab")%>">
    <wiki:Include page='ProfileTab.jsp'/>
  </wiki:Tab>
  </wiki:Permission>

  <wiki:Tab id="loginpagehelp" title='<%=LocaleSupport.getLocalizedMessage(pageContext,"login.tab.help")%>' >
  <wiki:InsertPage page="LoginPageHelp" />
  <wiki:NoSuchPage page="LoginPageHelp">
    <div class="error">
      <fmt:message key="login.loginhelpmissing">
        <fmt:param><wiki:EditLink page="LoginPageHelp">LoginHelp</wiki:EditLink></fmt:param>
      </fmt:message>
    </div>
  </wiki:NoSuchPage>
</wiki:Tab>

</wiki:TabbedSection>