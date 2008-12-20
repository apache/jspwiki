<%@ taglib uri="http://jakarta.apache.org/jspwiki.tld" prefix="wiki" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.action.*" %>
<%@ page import="com.ecyrd.jspwiki.auth.*" %>
<%@ page errorPage="/Error.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://stripes.sourceforge.net/stripes.tld" prefix="stripes" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
<wiki:TabbedSection defaultTab="profile">

<%-- Login functionality --%>
<wiki:UserCheck status="notauthenticated">
<wiki:Tab id="logincontent" titleKey="login.tab" url="Login.jsp?tab=logincontent" />

<%-- Lost pasword functionality --%>
<wiki:Tab id="lostpassword" titleKey="login.lostpw.tab" url="Login.jsp?tab=lostpassword" />
</wiki:UserCheck>

<%-- Register new user profile --%>
<wiki:Permission permission='editProfile'>
  <wiki:Tab id="profile" titleKey="login.register.tab" accesskey="p">
     <wiki:Include page="ProfileTab.jsp" />
  </wiki:Tab>
</wiki:Permission>

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
