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
    String headerTitle = "";

    if( pagereq == null )
    {
        pagereq = "Main";
    }

    NDC.push( wiki.getApplicationName()+":"+pagereq );

    String pageurl = wiki.encodeName( pagereq );
    
    log.info("Request for page '"+pagereq+"' from "+request.getRemoteHost()+" by "+request.getRemoteUser() );

    String specialpage = wiki.getSpecialPageReference( pagereq );

    if( specialpage != null )
    {
        response.sendRedirect( specialpage );
        return;        
    }

    //
    //  Determine requested version.  If version == -1,
    //  then fetch current version.
    //
    int version          = -1;
    String rev           = request.getParameter("version");
    String pageReference = "this page";
    String versionInfo   = "";

    if( rev != null )
    {
        version = Integer.parseInt( rev );
        pageReference = "current version";
    }

    WikiPage wikipage = wiki.getPage( pagereq, version );

    WikiContext wikiContext = new WikiContext( wiki, wikipage );
    pageContext.setAttribute( WikiTagBase.ATTR_CONTEXT,
                              wikiContext );

    // In the future, user access permits affect this
    boolean isEditable = (version < 0);

    String rssURL = wiki.getGlobalRSSURL();
    //
    //  Alright, then start responding.
    //

    response.setContentType("text/html; charset="+wiki.getContentEncoding() );
%>

<%@ include file="ViewTemplate.jsp" %>

<%
    NDC.pop();
    NDC.remove();
%>

