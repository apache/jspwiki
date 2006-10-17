<%@ page import="org.apache.log4j.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.filters.*" %>
<%@ page import="java.util.*" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="com.ecyrd.jspwiki.ui.EditorManager" %>
<%@ page import="com.ecyrd.jspwiki.util.HttpUtil" %>
<%@ page import="com.ecyrd.jspwiki.auth.login.CookieAssertionLoginModule" %>
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
    // Create wiki context and check for authorization
    WikiContext wikiContext = wiki.createContext( request, WikiContext.COMMENT );
    if( !wikiContext.hasAccess( response ) ) return;
    String pagereq = wikiContext.getName();
    
    WikiSession wikiSession = wikiContext.getWikiSession(); 
    String storedUser = wikiSession.getUserPrincipal().getName();

    if ( wikiSession.isAnonymous() ) 
    {
        storedUser  = request.getParameter( "author" );
    }

    String ok      = request.getParameter("ok");
    String preview = request.getParameter("preview");
    String cancel  = request.getParameter("cancel");
    String author  = request.getParameter("author");
    String link    = request.getParameter("link");
    String remember = request.getParameter("remember");
    String changenote = request.getParameter( "changenote" );
    
    WikiPage wikipage = wikiContext.getPage();
    WikiPage latestversion = wiki.getPage( pagereq );
    if( latestversion == null )
    {
        latestversion = wikiContext.getPage();
    }

    //
    //  Setup everything for the editors and possible preview.  We store everything in the
    //  session.
    //
    
    if( remember == null )
    {
        remember = (String)session.getAttribute("remember");
    }

    if( remember == null ) remember = "false";
    
    session.setAttribute("remember",remember);
    
    if( author == null )
    {
        author = storedUser;
    }    
    if( author == null || author.length() == 0 ) author = "AnonymousCoward";

    session.setAttribute("author",author);
    
    if( link == null )
    {
        link = HttpUtil.retrieveCookieValue( request, "link" );
        if( link == null ) link = "";
    }
    
    session.setAttribute( "link", link );

    //
    //  Branch
    //
    log.debug("preview="+preview+", ok="+ok);

    if( ok != null )
    {
        log.info("Saving page "+pagereq+". User="+storedUser+", host="+request.getRemoteAddr() );

        //  Modifications are written here before actual saving
        
        WikiPage modifiedPage = (WikiPage)wikiContext.getPage().clone();

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
        //  Set author and changenote information
        //

        modifiedPage.setAuthor( storedUser );

        if( changenote == null ) changenote = (String) session.getAttribute("changenote");
        
        session.removeAttribute("changenote");
        
        modifiedPage.setAttribute( WikiPage.CHANGENOTE, "Comment by "+storedUser );

        //
        //  Build comment part
        //
            
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
            
            if( link != null && link.length() > 0 )
            {
                link = HttpUtil.guessValidURI( link );
                
                signature = "["+author+"|"+link+"]";
            }
            
            Calendar cal = Calendar.getInstance();
            SimpleDateFormat fmt = new SimpleDateFormat("dd-MMM-yyyy");

            pageText.append("\n\n--"+signature+", "+fmt.format(cal.getTime()));
        }

        if( TextUtil.isPositive(remember) )
        {
            if( link != null )
            {
                Cookie linkcookie = new Cookie("link", link);
                linkcookie.setMaxAge(1001*24*60*60);
                response.addCookie( linkcookie );
            }

            CookieAssertionLoginModule.setUserCookie( response, author );            
        }
        else
        {
            session.removeAttribute("link");
            session.removeAttribute("author");
        }

        try
        {
            wikiContext.setPage( modifiedPage );
            wiki.saveText( wikiContext, pageText.toString() );
        }
        catch( RedirectException e )
        {
            session.setAttribute( VariableManager.VAR_MSG, e.getMessage() );
            response.sendRedirect( e.getRedirect() );
            return;
        }
        response.sendRedirect(wiki.getViewURL(pagereq));
        return;
    }
    else if( preview != null )
    {
        log.debug("Previewing "+pagereq);
        pageContext.forward( "Preview.jsp?action=comment" );
        return;
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
                                                    storedUser );

    if( lock != null )
    {
        session.setAttribute( "lock-"+pagereq, lock );
    }

    // Set the content type and include the response content
    response.setContentType("text/html; charset="+wiki.getContentEncoding() );
    response.setHeader( "Cache-control", "max-age=0" );
    response.setDateHeader( "Expires", new Date().getTime() );
    response.setDateHeader( "Last-Modified", new Date().getTime() );
    String contentPage = wiki.getTemplateManager().findJSP( pageContext,
                                                            wikiContext.getTemplate(),
                                                            "EditTemplate.jsp" );

%><wiki:Include page="<%=contentPage%>" />
