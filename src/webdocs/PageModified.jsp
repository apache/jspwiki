<%@ page import="org.apache.log4j.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="java.util.Calendar,java.util.Date" %>
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
    String headerTitle = "Concurrent modification of ";

    if( pagereq == null )
    {
        throw new ServletException("No page defined");
    }

    String pageurl = wiki.encodeName( pagereq );
    String usertext = wiki.safeGetParameter( request, "text" );

    WikiPage wikipage = wiki.getPage( pagereq, version );

    WikiContext wikiContext = new WikiContext( wiki, wikipage );
    pageContext.setAttribute( WikiTagBase.ATTR_CONTEXT,
                              wikiContext );

    response.setContentType("text/html; charset="+wiki.getContentEncoding() );

    usertext = TextUtil.replaceString( usertext, "<", "&lt;" );
    usertext = TextUtil.replaceString( usertext, ">", "&gt;" );
    usertext = TextUtil.replaceString( usertext, "\n", "<BR />" );

    String conflicttext = wiki.getText(pagereq);

    conflicttext = TextUtil.replaceString( conflicttext, "<", "&lt;" );
    conflicttext = TextUtil.replaceString( conflicttext, ">", "&gt;" );
    conflicttext = TextUtil.replaceString( conflicttext, "\n", "<BR />" );

    log.info("Page concurrently modified "+pagereq);
%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
        "http://www.w3.org/TR/html4/loose.dtd">

<HTML>

<HEAD>
  <TITLE><%=wiki.getApplicationName()%> Error - Concurrent modification of <%=pagereq%></TITLE>
  <META NAME="ROBOTS" CONTENT="NOINDEX">
  <%@ include file="cssinclude.js" %>
</HEAD>

<BODY BGCOLOR="#FFFFFF">

<TABLE BORDER="0" CELLSPACING="8">

  <TR>
    <TD CLASS="leftmenu" WIDTH="15%" VALIGN="top" NOWRAP="true">
       <%@ include file="LeftMenu.jsp" %>
       <BR><BR>
       <P>
       <A HREF="<%=wiki.getBaseURL()%>Edit.jsp?page=<%=pageurl%>">Go edit <%=pagereq%></A>
       </P>
       <P>
       <%@ include file="LeftMenuFooter.jsp" %>
       </P>
    </TD>
    <TD CLASS="page" WIDTH="85%" VALIGN="top">
      <%@ include file="PageHeader.jsp" %>

      <P>
      <B>Oops!  Someone modified the page while you were editing it!</B>
      </P>

      <P>Since I am stupid and can't figure out what the difference
      between those pages is, you will need to do that for me.  I've
      printed here the text (in Wiki) of the new page, and the
      modifications you made.  You'll now need to copy the text onto a
      scratch pad (Notepad or emacs will do just fine), and then edit
      the page again.</P>

      <P>Note that when you go back into the editing mode, someone might have
      changed the page again.  So be quick.</P>

      <P><font color="#0000FF">Here is the modified text (by someone else):</FONT></P>

      <P><HR></P>

      <TT>
        <%=conflicttext%>
      </TT>      

      <P><HR></P>

      <P><FONT COLOR="#0000FF">And here's your text:</FONT></P>

      <TT>
        <%=usertext%>
      </TT>

      <P><HR></P>

      <P>
       <I><A HREF="<%=wiki.getBaseURL()%>Edit.jsp?page=<%=pageurl%>">Go edit <%=pagereq%></A></I>
      </P>

    </TD>
  </TR>

</TABLE>

</BODY>

</HTML>
