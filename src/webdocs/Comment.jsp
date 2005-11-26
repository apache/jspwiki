<%@ page import="org.apache.log4j.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="java.util.*" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="com.ecyrd.jspwiki.tags.WikiTagBase" %>
<%@ page import="com.ecyrd.jspwiki.ui.EditorManager" %>
<%@ page import="com.ecyrd.jspwiki.util.HttpUtil" %>
<%@ page import="com.ecyrd.jspwiki.auth.AuthorizationManager" %>
<%@ page import="java.security.Principal" %>
<%@ page import="java.security.Permission" %>
<%@ page import="com.ecyrd.jspwiki.auth.login.CookieAssertionLoginModule" %>
<%@ page import="com.ecyrd.jspwiki.auth.permissions.PagePermission" %>
<%@ page errorPage="/Error.jsp" %>
<%@ page import="javax.servlet.http.Cookie" %>
<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>

<%! 
    public void jspInit()
    {
        wiki = WikiEngine.getInstance( getServletConfig() );
    }

    Logger log = Logger.getLogger("JSPWiki");
    WikiEngine wiki;
%>


<%
    WikiContext wikiContext = wiki.createContext( request, WikiContext.COMMENT );
    WikiSession wikiSession = wikiContext.getWikiSession(); 
    String user = wikiSession.getUserPrincipal().getName();
    if ( !wikiSession.isAuthenticated() && wikiSession.isAnonymous() ) 
    {
        user  = request.getParameter( "author" );
    }
    String action  = request.getParameter("action");
    String ok      = request.getParameter("ok");
    String preview = request.getParameter("preview");
    String cancel  = request.getParameter("cancel");
    String edit    = request.getParameter("edit");
    String author  = request.getParameter( "author" );
    String link    = request.getParameter( "link" );
    String remember = request.getParameter("remember");
    String pagereq = wikiContext.getPage().getName();

    NDC.push( wiki.getApplicationName()+":"+pagereq );    

    WikiPage wikipage = wikiContext.getPage();
    WikiPage latestversion = wiki.getPage( pagereq );

    if( latestversion == null )
    {
        latestversion = wikiContext.getPage();
    }

    AuthorizationManager mgr = wiki.getAuthorizationManager();

    Permission requiredPermission = new PagePermission( wikipage, "comment" );
    if( !mgr.checkPermission( wikiSession,
                              requiredPermission ) )
    {
        log.info("User "+user+" has no access - redirecting to login page.");
        String pageurl = wiki.encodeName( pagereq );
        response.sendRedirect( wiki.getBaseURL()+"Login.jsp?page="+pageurl );
        return;
    }

    pageContext.setAttribute( WikiTagBase.ATTR_CONTEXT,
                              wikiContext,
                              PageContext.REQUEST_SCOPE );

    String storedlink = HttpUtil.retrieveCookieValue( request, "link" );
    if( storedlink == null ) storedlink = "";
    
    pageContext.setAttribute( "link", storedlink, PageContext.REQUEST_SCOPE );

    //
    //  Set the response type before we branch.
    //

    response.setContentType("text/html; charset="+wiki.getContentEncoding() );
    response.setHeader( "Cache-control", "max-age=0" );
    response.setDateHeader( "Expires", new Date().getTime() );
    response.setDateHeader( "Last-Modified", new Date().getTime() );

    log.debug("preview="+preview+", ok="+ok);

    if( ok != null )
    {
        log.info("Saving page "+pagereq+". User="+user+", host="+request.getRemoteAddr() );

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
        //  Set author information
        //

        wikipage.setAuthor( user );

        StringBuffer pageText = new StringBuffer(wiki.getPureText( wikipage ));

        log.debug("Page initial contents are "+pageText.length()+" chars");

        //
        //  Add a line on top only if we need to separate it from the content.
        //
        if( pageText.length() > 0 )
        {
            pageText.append( "\n\n----\n\n" );
        }        

        pageText.append( EditorManager.getEditedText(pageContext) );

        log.debug("Author name ="+author);
        if( author != null && author.length() > 0 )
        {
            String signature = author;
            
            if( link != null )
            {
                link = HttpUtil.guessValidURI( link );
                
                signature = "["+author+"|"+link+"]";
            }
            
            Calendar cal = Calendar.getInstance();
            SimpleDateFormat fmt = new SimpleDateFormat("dd-MMM-yyyy");

            pageText.append("\n\n--"+signature+", "+fmt.format(cal.getTime()));
        }

        if( remember != null )
        {
            if( link != null )
            {
                Cookie linkcookie = new Cookie("link", link);
                linkcookie.setMaxAge(1001*24*60*60);
                response.addCookie( linkcookie );
            }

            CookieAssertionLoginModule.setUserCookie( response, user );            
        }

        wiki.saveText( wikiContext, pageText.toString() );

        response.sendRedirect(wiki.getViewURL(pagereq));
        return;
    }
    else if( preview != null )
    {
        log.debug("Previewing "+pagereq);
        if( author == null ) author = "";
        pageContext.forward( "Preview.jsp?action=comment&author="+author );
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

    log.info("Commenting page "+pagereq+". User="+request.getRemoteUser()+", host="+request.getRemoteAddr() );

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

	//  This is a hack to get the preview to work.
	pageContext.setAttribute( "comment", Boolean.TRUE, PageContext.REQUEST_SCOPE );
	
    //
    //  Attempt to lock the page.
    //
    PageLock lock = wiki.getPageManager().lockPage( wikipage, 
                                                    user );

    if( lock != null )
    {
        session.setAttribute( "lock-"+pagereq, lock );
    }

    String contentPage = wiki.getTemplateManager().findJSP( pageContext,
                                                            wikiContext.getTemplate(),
                                                            "EditTemplate.jsp" );

%>

<wiki:Include page="<%=contentPage%>" />

<%
    NDC.pop();
    NDC.remove();
%>
