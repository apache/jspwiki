<%@ page import="org.apache.log4j.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.attachment.*" %>
<%@ page import="java.util.*" %>
<%@ page import="com.ecyrd.jspwiki.tags.WikiTagBase" %>
<%@ page errorPage="/Error.jsp" %>
<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>

<!--
    This is a sample login page, in case you prefer a clear
    front page instead of the default sign-in type login box
    at the side of the normal entry page. Set this page in
    the welcome-file-list tag in web.xml to default here 
    when entering the site.
-->


<%! 
    public void jspInit()
    {
        wiki = WikiEngine.getInstance( getServletConfig() );
    }
    Category log = Category.getInstance("JSPWiki"); 
    WikiEngine wiki;
%>

<%
    String pagereq = wiki.safeGetParameter( request, "page" );
    if( pagereq == null || pagereq.length() == 0 )
        pagereq = wiki.getFrontPage();
    String userName = wiki.getUserName( request );

    NDC.push( wiki.getApplicationName() + ":Login.jsp"  );

    //WikiPage wikipage = wiki.getPage( pagereq );

    WikiContext wikiContext = new WikiContext( wiki, (WikiPage)null );
    wikiContext.setRequestContext( WikiContext.LOGIN );

    pageContext.setAttribute( WikiTagBase.ATTR_CONTEXT,
                              wikiContext,
                              PageContext.REQUEST_SCOPE );

    response.setContentType("text/html; charset="+wiki.getContentEncoding() );
%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
        "http://www.w3.org/TR/html4/loose.dtd">

<html>

<head>
  <title><wiki:Variable var="applicationname"/> login</title>
  <%@ include file="templates/default/cssinclude.js" %>
  <META NAME="ROBOTS" CONTENT="NOINDEX">
</head>

<body class="login" bgcolor="#FFFFFF">
  <BR>
  <BR>
  <FORM action="<wiki:Variable var="baseURL"/>Auth.jsp" ACCEPT-CHARSET="ISO-8859-1,UTF-8">
  <INPUT type="hidden" name="page" value="<%=pagereq%>">
  <DIV align="center">
    <TABLE BORDER="0" CELLSPACING="3" CELLPADDING="5" width="35%" BGCOLOR="#efefef"\>
      <TR>
        <TD colspan="2" bgcolor="#bfbfff">
          <DIV align="center">
            <H3>Welcome to <wiki:Variable var="applicationname"/></H3>
          </DIV>
        </TD>
      </TR>
      <TR>
        <TD>Login:</TD>
        <TD><input type="text" name="uid"></TD>
      </TR>
      <TR>
        <TD>Password:</TD>
        <TD><input type="password" name="passwd"></TD>
      </TR>
      <TR>
        <TD colspan="2">
          <DIV align="center">
            <input type="submit" name="action" value="login">
          </DIV>
        </TD>
      </TR>
    </TABLE>
  </DIV>
</body>

</html>
<%
    NDC.pop();
    NDC.remove();
%>
