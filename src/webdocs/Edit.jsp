<%@ page import="org.apache.log4j.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.auth.AccessRuleSet" %>
<%@ page import="java.util.Calendar,java.util.Date" %>
<%@ page import="com.ecyrd.jspwiki.tags.WikiTagBase" %>
<%@ page import="com.ecyrd.jspwiki.WikiProvider" %>
<%@ page errorPage="/Error.jsp" %>
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
    String verstr  = request.getParameter("version");
    int    version = WikiProvider.LATEST_VERSION;

    if( verstr != null )
    {
        version = Integer.parseInt(verstr);    
    }

    if( pagereq == null )
    {
        throw new ServletException("No page defined");
    }

    String skin = wiki.getTemplateDir();

    NDC.push( wiki.getApplicationName()+":"+pagereq );    

    WikiPage wikipage = wiki.getPage( pagereq, version );

    if( wikipage == null )
    {
        wikipage = new WikiPage( pagereq );
    }

    AccessRuleSet accessRules = wikipage.getAccessRules();
    UserProfile userProfile = wiki.getUserProfile( request );
    
    if( accessRules.hasWriteAccess( userProfile ) == false )
    {
        StringBuffer buf = new StringBuffer();
        buf.append( "<h4>Unable to edit " + pagereq + ".</h4>\n" );
        buf.append( "You do not have sufficient privileges to view this page.\n" );
        buf.append( "Have you logged in?\n" );
        throw new WikiSecurityException( buf.toString() );
    }

    WikiContext wikiContext = new WikiContext( wiki, wikipage );
    wikiContext.setRequestContext( WikiContext.EDIT );
    pageContext.setAttribute( WikiTagBase.ATTR_CONTEXT,
                              wikiContext,
                              PageContext.REQUEST_SCOPE );

    String pageurl = wiki.encodeName( pagereq );

    String action  = request.getParameter("action");
    String ok      = request.getParameter("ok");
    String preview = request.getParameter("preview");

    //
    //  Set the response type before we branch.
    //

    response.setContentType("text/html; charset="+wiki.getContentEncoding() );

    //log.debug("Request character encoding="+request.getCharacterEncoding());
    //log.debug("Request content type+"+request.getContentType());
    log.debug("preview="+preview+", ok="+ok);

    if( ok != null )
    {
        log.info("Saving page "+pagereq+". User="+request.getRemoteUser()+", host="+request.getRemoteHost() );

        //  FIXME: I am not entirely sure if the JSP page is the
        //  best place to check for concurrent changes.  It certainly
        //  is the best place to show errors, though.
       
        long pagedate   = Long.parseLong(request.getParameter("edittime"));
        Date change     = wikipage.getLastModified();

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
    Date d = wikipage.getLastModified();
    if( d != null ) lastchange = d.getTime();

    pageContext.setAttribute( "lastchange",
                              Long.toString( lastchange ),
                              PageContext.REQUEST_SCOPE );

    String contentPage = "templates/"+skin+"/EditTemplate.jsp";
%>

<wiki:Include page="<%=contentPage%>" />

<%
    NDC.pop();
    NDC.remove();
%>
