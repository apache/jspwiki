<%@ taglib uri="http://jakarta.apache.org/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<wiki:TabbedSection defaultTab="profile">

<%-- Login tab --%>
<wiki:UserCheck status="notauthenticated">
<wiki:Tab id="logincontent" titleKey="login.tab" url="Login.jsp" />

<%-- Lost password tab --%>
<wiki:Tab id="lostpassword" titleKey="login.lostpw.tab" url="LostPassword.jsp" />
</wiki:UserCheck>

<%-- Register new user profile tab --%>
<wiki:Permission permission='editProfile'>
  <wiki:Tab id="profile" titleKey="login.register.tab" accesskey="p">
     <wiki:Include page="ProfileTab.jsp" />
  </wiki:Tab>
</wiki:Permission>

<%-- Help tab --%>
<wiki:Tab id="loginhelp" titleKey="login.tab.help">
  <wiki:InsertPage page="LoginHelp" />
  <wiki:NoSuchPage page="LoginHelp">
  <div class="error">
    <fmt:message key="login.loginhelpmissing">
       <fmt:param><wiki:EditLink page="LoginHelp">LoginHelp</wiki:EditLink></fmt:param>
    </fmt:message>
  </div>
  </wiki:NoSuchPage>
</wiki:Tab>

</wiki:TabbedSection>
