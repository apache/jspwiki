<%-- Provides a small login/logout form to include in a side bar. --%>

<DIV class="loginbox">
  <P>
  <HR>
  <wiki:UserCheck status="unvalidated">
    <FORM action="<wiki:Variable var="baseURL"/>Auth.jsp" ACCEPT-CHARSET="ISO-8859-1,UTF-8">
      <INPUT type="hidden" name="page" value="<wiki:PageName/>">
      <INPUT type="text" name="uid" size="8">
      <BR>
      <INPUT type="password" name="passwd" size="8">
      <BR>
      <INPUT type="submit" name="action" value="login">
    </FORM>
  </wiki:UserCheck>
  <wiki:UserCheck status="validated">
    <FORM action="<wiki:Variable var="baseURL"/>Auth.jsp" ACCEPT-CHARSET="ISO-8859-1,UTF-8">
      <INPUT type="hidden" name="page" value="<wiki:PageName/>">
      <INPUT type="submit" name="action" value="logout">
    </FORM>
  </wiki:UserCheck>

</DIV>

