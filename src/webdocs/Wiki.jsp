<%@ page import="org.apache.log4j.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.tags.WikiTagBase" %>
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
    String skin    = wiki.safeGetParameter( request, "skin" );

    if( pagereq == null )
    {
        pagereq = wiki.getFrontPage();
    }

    if( skin == null )
    {
        skin = wiki.getTemplateDir();
    }

    NDC.push( wiki.getApplicationName()+":"+pagereq );
    
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

    if( rev != null )
    {
        version = Integer.parseInt( rev );
    }

    WikiPage wikipage = wiki.getPage( pagereq, version );

    if( wikipage == null )
    {
        wikipage = new WikiPage( pagereq );
    }

    WikiContext wikiContext = new WikiContext( wiki, wikipage );
    wikiContext.setRequestContext( WikiContext.VIEW );
    wikiContext.setHttpRequest( request );

    pageContext.setAttribute( WikiTagBase.ATTR_CONTEXT,
                              wikiContext,
                              PageContext.REQUEST_SCOPE );

    //
    //  Alright, then start responding.
    //

    response.setContentType("text/html; charset="+wiki.getContentEncoding() );

    String contentPage = "templates/"+skin+"/ViewTemplate.jsp";
%>

<wiki:Include page="<%=contentPage%>" />

<%
    NDC.pop();
    NDC.remove();
%>

