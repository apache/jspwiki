<%@ page import="org.apache.log4j.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.tags.WikiTagBase" %>
<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>

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
    String headerTitle = "Previewing ";

    if( pagereq == null )
    {
        throw new ServletException("No page defined");
    }

    NDC.push( wiki.getApplicationName()+":"+pagereq );

    WikiPage wikipage = wiki.getPage( pagereq );

    WikiContext wikiContext = new WikiContext( wiki, wikipage );
    pageContext.setAttribute( WikiTagBase.ATTR_CONTEXT,
                              wikiContext );

    String pageurl = wiki.encodeName( pagereq );
    response.setContentType("text/html; charset="+wiki.getContentEncoding() );

    String usertext = wiki.safeGetParameter( request, "text" );

    WikiContext wikiContext = new WikiContext( wiki, pagereq );
%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
        "http://www.w3.org/TR/html4/loose.dtd">

<HTML>

<HEAD>
  <TITLE><%=wiki.getApplicationName()%>: Previewing <%=pagereq%></TITLE>
  <%@ include file="cssinclude.js" %>
</HEAD>

<BODY CLASS="preview" BGCOLOR="#F0F0F0">

<TABLE BORDER="0" CELLSPACING="8" width="95%">

  <TR>
    <TD CLASS="leftmenu" WIDTH="10%" VALIGN="top" NOWRAP="true">
       <%@ include file="templates/LeftMenu.jsp" %>
       <P>
       <%@ include file="templates/LeftMenuFooter.jsp" %>
    </TD>

    <TD CLASS="page" WIDTH="85%" VALIGN="top">

      <%@ include file="templates/PageHeader.jsp" %>

      <P>
      <B>This is a preview.  Hit "Back" on your browser to go back to editor.</B>
      </P>

      <P><HR></P>

      <%=wiki.textToHTML( wikiContext, usertext ) %>

      <P><HR>
    </TD>
  </TR>

</TABLE>

</BODY>

</HTML>

<%
    NDC.pop();
    NDC.remove();
%>

