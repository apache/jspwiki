<%@ page import="org.apache.log4j.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.filters.*" %>
<%@ page import="java.util.*" %>
<%@ page import="com.ecyrd.jspwiki.tags.WikiTagBase" %>
<%@ page import="com.ecyrd.jspwiki.tags.EditorAreaTag" %>
<%@ page import="com.ecyrd.jspwiki.auth.AuthorizationManager" %>
<%@ page import="java.security.Permission" %>
<%@ page import="java.security.Principal" %>
<%@ page import="com.ecyrd.jspwiki.auth.permissions.WikiPermission" %>
<%@ page import="com.ecyrd.jspwiki.htmltowiki.HtmlStringToWikiTranslator" %>
<%@ page import="com.ecyrd.jspwiki.auth.authorize.DefaultGroupManager" %>
<%@ page import="com.ecyrd.jspwiki.auth.permissions.PagePermission" %>
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
    WikiContext wikiContext = wiki.createContext( request, WikiContext.EDIT );
    WikiSession wikiSession = wikiContext.getWikiSession(); 
    String user = wikiSession.getUserPrincipal().getName();
    if ( !wikiSession.isAuthenticated() && wikiSession.isAnonymous() )
    {
        user  = wiki.safeGetParameter( request, "author" );
    }
    String action  = request.getParameter("action");
    String ok      = request.getParameter("ok");
    String preview = request.getParameter("preview");
    String cancel  = request.getParameter("cancel");
    String append  = request.getParameter("append");
    String edit    = request.getParameter("edit");
    String author  = wiki.safeGetParameter( request, "author" );
    String text    = wiki.safeGetParameter( request, EditorAreaTag.AREA_NAME );

    //
    //  Context is created; continue
    //

    String pagereq = wikiContext.getPage().getName();

    NDC.push( wiki.getApplicationName()+":"+pagereq );    

    //
    //  WYSIWYG editor sends us its greetings
    //
    String htmlText = wiki.safeGetParameter( request, "htmlPageText" );
    if( htmlText != null && cancel == null ) 
    {
        text = new HtmlStringToWikiTranslator().translate(htmlText,wikiContext);
    }

    WikiPage wikipage = wikiContext.getPage();
    Permission requiredPermission = null;
    WikiPage latestversion = wiki.getPage( pagereq );

    if( latestversion == null )
    {
        latestversion = wikiContext.getPage();
    }
    
    //
    //  Figure out which is the proper permission for this
    //  particular editing action.
    //
    if( wiki.pageExists( wikipage ) )
    {
        requiredPermission = new PagePermission( wikipage, "edit" );
    }
    else
    {
        if ( pagereq.startsWith( DefaultGroupManager.GROUP_PREFIX ) )
    	{
            requiredPermission = WikiPermission.CREATE_GROUPS;
        }
        else
        {
            requiredPermission = WikiPermission.CREATE_PAGES;
        }
    }   

    AuthorizationManager mgr = wiki.getAuthorizationManager();

    if( !mgr.checkPermission(  wikiSession,
                               requiredPermission ) )
    {
        log.info("User "+user+" has no access - redirecting to login page.");
        String msg = "You do not seem to have the permissions for this operation. Would you like to login as another user?";
        wikiContext.setVariable( "msg", msg );
        String pageurl = wiki.encodeName( pagereq );
        response.sendRedirect( wiki.getBaseURL()+"Login.jsp?page="+pageurl );
        return;
    }

    pageContext.setAttribute( WikiTagBase.ATTR_CONTEXT,
                              wikiContext,
                              PageContext.REQUEST_SCOPE );

    //
    //  Set the response type before we branch.
    //

    response.setContentType("text/html; charset="+wiki.getContentEncoding() );
    response.setHeader( "Cache-control", "max-age=0" );
    response.setDateHeader( "Expires", new Date().getTime() );
    response.setDateHeader( "Last-Modified", new Date().getTime() );

    //log.debug("Request character encoding="+request.getCharacterEncoding());
    //log.debug("Request content type+"+request.getContentType());
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

        wikiContext.getPage().setAuthor( user );        

        //
        //  Figure out the actual page text
        //

        if( text == null )
        {
            throw new ServletException( "No parameter text set!" );
        }

        //
        //  If this is an append, then we just append it to the page.
        //  If it is a full edit, then we will replace the previous contents.
        //

        try
        {
            if( append != null )
            {
                StringBuffer pageText = new StringBuffer(wiki.getText( pagereq ));

                pageText.append( text );

                wiki.saveText( wikiContext, pageText.toString() );

            }
            else
            {
                wiki.saveText( wikiContext, text );
            }
        }
        catch( RedirectException ex )
        {
            session.setAttribute("msg", ex.getMessage());
            response.sendRedirect( ex.getRedirect() );
            return;
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

    log.info("Editing page "+pagereq+". User="+user+", host="+request.getRemoteAddr() );

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
