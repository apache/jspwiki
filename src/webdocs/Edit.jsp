<%@ page import="org.apache.log4j.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.auth.AccessRuleSet" %>
<%@ page import="com.ecyrd.jspwiki.auth.WikiSecurityException" %>
<%@ page import="java.util.*" %>
<%@ page import="java.text.SimpleDateFormat" %>
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

    WikiPage wikipage      = wiki.getPage( pagereq, version );
    WikiPage latestversion = wiki.getPage( pagereq );

    if( wikipage == null )
    {
        wikipage = new WikiPage( pagereq );
        wiki.checkPermissions( wikipage );
        latestversion = wikipage;
    }

    AccessRuleSet accessRules = wikipage.getAccessRules();
    UserProfile userProfile = wiki.getUserProfile( request );

    if( accessRules.hasWriteAccess( userProfile ) == false )
    {
        if( wiki.useStrictLogin() )
        {
            // Need to get a sensible page to send to!
            String pageurl = wiki.encodeName( pagereq );
            response.sendRedirect(wiki.getBaseURL()+"Login.jsp?page="+pageurl);
        }
        else
        {
            StringBuffer buf = new StringBuffer();
            buf.append( "<h4>Unable to edit " + pagereq + ".</h4>\n" );
            buf.append( "You do not have sufficient privileges to edit this page.\n" );
            buf.append( "Have you logged in?\n" );
            throw new WikiSecurityException( buf.toString() );
        }
    }

    String action  = request.getParameter("action");
    String ok      = request.getParameter("ok");
    String preview = request.getParameter("preview");
    String cancel  = request.getParameter("cancel");
    String comment = request.getParameter("comment");
    String author  = wiki.safeGetParameter( request, "author" );

    //
    //  Set up the Wiki Context.
    //

    WikiContext wikiContext = new WikiContext( wiki, wikipage );
    wikiContext.setRequestContext( comment != null ? WikiContext.COMMENT : WikiContext.EDIT );
    wikiContext.setHttpRequest( request );

    pageContext.setAttribute( WikiTagBase.ATTR_CONTEXT,
                              wikiContext,
                              PageContext.REQUEST_SCOPE );

    //
    //  Set the response type before we branch.
    //

    response.setContentType("text/html; charset="+wiki.getContentEncoding() );
    response.addHeader( "Cache-control", "max-age=0" );
    response.addDateHeader( "Expires", new Date().getTime() );
    response.addDateHeader( "Last-Modified", new Date().getTime() );

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

        Date change = latestversion.getLastModified();

        if( change != null && change.getTime() != pagedate )
        {
            //
            // Someone changed the page while we were editing it!
            //

            log.info("Page changed, warning user.");

            pageContext.forward( "PageModified.jsp" );
            return;
        }

        //
        //  We expire ALL locks at this moment, simply because someone has
        //  already broken it.
        //
        PageLock lock = wiki.getPageManager().getCurrentLock( wikipage );
        wiki.getPageManager().unlockPage( lock );
        session.removeAttribute( "lock-"+pagereq );

        //
        //  If this is a comment, then we just append it to the page.
        //  If it is a full edit, then we will replace the previous contents.
        //
        if( comment != null )
        {
            StringBuffer pageText = new StringBuffer(wiki.getText( pagereq ));
            pageText.append( "\n\n----\n\n" );
            pageText.append( wiki.safeGetParameter( request, "text" ) );

            if( author != null )
            {
                Calendar cal = Calendar.getInstance();
                SimpleDateFormat fmt = new SimpleDateFormat("dd-MMM-yyyy");

                pageText.append("\n\n--"+author+", "+fmt.format(cal.getTime()));
            }

            wiki.saveText( pagereq, pageText.toString(), request );
        }
        else
        {
            wiki.saveText( pagereq,
                           wiki.safeGetParameter( request, "text" ),
                           request );
        }

        response.sendRedirect(wiki.getViewURL(pagereq));
        return;
    }
    else if( preview != null )
    {
        log.debug("Previewing "+pagereq);
        pageContext.forward( "Preview.jsp" );
    }
    else if( cancel != null )
    {
        log.debug("Cancelled editing "+pagereq);
        PageLock lock = (PageLock) session.getAttribute( "lock-"+pagereq );

        if( lock != null )
        {
            wiki.getPageManager().unlockPage( lock );
            session.removeAttribute( "lock-"+pagereq );
        }
        response.sendRedirect( wiki.getViewURL(pagereq) );
        return;
    }

    log.info("Editing page "+pagereq+". User="+request.getRemoteUser()+", host="+request.getRemoteHost() );

    //
    //  Determine and store the date the latest version was changed.  Since
    //  the newest version is the one that is changed, we need to track
    //  that instead of the edited version.
    //
    long lastchange = 0;
    
    Date d = latestversion.getLastModified();
    if( d != null ) lastchange = d.getTime();

    pageContext.setAttribute( "lastchange",
                              Long.toString( lastchange ),
                              PageContext.REQUEST_SCOPE );

    //
    //  Attempt to lock the page.
    //
    PageLock lock = wiki.getPageManager().lockPage( wikipage, 
                                                    wiki.getValidUserName(request) );

    if( lock != null )
    {
        session.setAttribute( "lock-"+pagereq, lock );
    }

    String contentPage = "templates/"+skin+"/EditTemplate.jsp";
%>

<wiki:Include page="<%=contentPage%>" />

<%
    NDC.pop();
    NDC.remove();
%>
