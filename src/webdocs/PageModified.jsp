<%@ page import="org.apache.log4j.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="java.util.Calendar,java.util.Date" %>

<%! 
    public void jspInit()
    {
        wiki = new WikiEngine( getServletContext() );
    }

    Category log = Category.getInstance("JSPWiki");
    WikiEngine wiki;

%>


<%
    String pagereq = request.getParameter("page");

    if( pagereq == null )
    {
        throw new ServletException("No page defined");
    }

    // FIXME: Should the usertext be removed from the session?
    String usertext = getSession().getParameter("usertext");

    log.info("Page concurrently modified "+pagereq);
%>

<HTML>

<HEAD>
  <TITLE>JSPWiki Error</TITLE>
</HEAD>

<BODY BGCOLOR="#FFFFFF">

<TABLE BORDER="0" CELLSPACING="8">

  <TR>
    <TD WIDTH="15%" VALIGN="top">
       <%@ include file="LeftMenu.jsp" %>
       <P>
       <A HREF="Wiki.jsp?page=EditingWikiPages">Help on editing</A>
       <A HREF="Edit.jsp?page=<%=pagereq%>">Go edit <%=pagereq%></A>
       </P>
       <%@ include file="LeftMenuFooter.jsp" %>
    </TD>
    <TD WIDTH="85%" VALIGN="top">
      <H1>Concurrent modification of <%=pagereq%></H1>

      <P>
      <B>Oops!  Someone modified the page while you were editing it!</B>
      </P>

      <P>Since I am stupid and can't figure out what the difference 
      between those pages is, you will need to do that for me.  I've printed here
      the text (in Wiki) of the new page, and the modifications you made.  You'll
      now copy the text onto a scratch pad (Notepad or emacs will do just fine),
      and then edit the page again.</P>

      <P>Note that when you go back into the editing mode, someone might have
      changed the page again.  So be quick.</P>

      <P><font color="#0000FF">Here is the modified text (by someone else):</FONT></P>

      <P><HR></P>

      <PRE>
        <%=wiki.getText(pagereq)%>
      </PRE>      

      <P><HR></P>

      <P><FONT COLOR="#0000FF">And here's your text:</FONT></P>

      <PRE>
        <%=usertext%>
      </PRE>

    </TD>
  </TR>

</TABLE>

</BODY>

</HTML>
