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
    String headerTitle = "Edit page ";

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

    String action = request.getParameter("action");
    String ok = request.getParameter("ok");
    String preview = request.getParameter("preview");

    //
    //  Set the response type before we branch.
    //

    response.setContentType("text/html; charset="+wiki.getContentEncoding() );

    log.debug("Request character encoding="+request.getCharacterEncoding());
    log.debug("Request content type+"+request.getContentType());
    log.debug("preview="+preview+", ok="+ok);

    if( ok != null )
    {
        log.info("Saving page "+pagereq+". User="+request.getRemoteUser()+", host="+request.getRemoteHost() );

        //  FIXME: I am not entirely sure if the JSP page is the
        //  best place to check for concurrent changes.  It certainly
        //  is the best place to show errors, though.
       
        long pagedate   = Long.parseLong(request.getParameter("edittime"));
        Date change     = wiki.pageLastChanged( pagereq );

        if( change != null && change.getTime() != pagedate )
        {
            //
            // Someone changed the page while we were editing it!
            //

            log.info("Page changed, warning user.");

            pageContext.forward( "PageModified.jsp" );
            return;
        }

        wiki.saveText( pagereq,
                       wiki.safeGetParameter( request, "text" ),
                       request );

        response.sendRedirect(wiki.getBaseURL()+"Wiki.jsp?page="+pageurl);
        return;
    }
    else if( preview != null )
    {
        log.debug("Previewing "+pagereq);
        pageContext.forward( "Preview.jsp" );
    }

    log.info("Editing page "+pagereq+". User="+request.getRemoteUser()+", host="+request.getRemoteHost() );

    //
    //  If the page does not exist, we'll get a null here.
    //
    long lastchange = 0;
    Date d = wiki.pageLastChanged( pagereq );
    if( d != null ) lastchange = d.getTime();

%>

<HTML>

<HEAD>
  <TITLE><%=wiki.getApplicationName()%> Edit: <%=pagereq%></TITLE>
  <META NAME="ROBOTS" CONTENT="NOINDEX">
  <%@ include file="cssinclude.js" %>
</HEAD>

<BODY class="edit" BGCOLOR="#D9E8FF" onLoad="document.forms[1].text.focus()">

<TABLE BORDER="0" CELLSPACING="8">

  <TR>
    <TD CLASS="leftmenu" WIDTH="15%" VALIGN="top" NOWRAP="true">
       <%@ include file="templates/LeftMenu.jsp" %>
       <P>
       <wiki:LinkTo page="TextFormattingRules">Help on editing</wiki:LinkTo>
       </P>
       <%@ include file="templates/LeftMenuFooter.jsp" %>
    </TD>

    <TD CLASS="page" WIDTH="85%" VALIGN="top">
      <%@ include file="templates/PageHeader.jsp" %>
      <!-- <H1>Edit page <%=pagereq%></H1> -->

      <FORM action="<%=wiki.getBaseURL()%>Edit.jsp?page=<%=pageurl%>&action=save" method="POST" 
            ACCEPT-CHARSET="ISO-8859-1,UTF-8">

      <INPUT type="hidden" name="page" value="<%=pagereq%>">
      <INPUT type="hidden" name="action" value="save">
      <INPUT type="hidden" name="edittime" value="<%=lastchange%>">

      <TEXTAREA CLASS="editor" wrap="virtual" name="text" rows="25" cols="80" style="width:100%;"><%=wiki.getText(pagereq)%></TEXTAREA>

      <P>
      <input type="submit" name="ok" value="Save" />
      &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
      <input type="submit" name="preview" value="Preview" />
      &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
      <wiki:LinkTo>Cancel</wiki:LinkTo>
      </FORM>

      </P>
      <P>
      <wiki:NoSuchPage page="EditPageHelp">
         Ho hum, it seems that the <wiki:EditLink page="EditPageHelp">EditPageHelp</wiki:EditLink>
         page is missing.  Someone must've done something to the installation...
      </wiki:NoSuchPage>
      </P>

      <wiki:InsertPage page="EditPageHelp" />

    </TD>
  </TR>

</TABLE>

</BODY>

</HTML>

<%
    NDC.pop();
    NDC.remove();
%>
