<%@ page import="org.apache.log4j.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="java.util.Date" %>
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

    String skin = wiki.getTemplateDir();

    if( pagereq == null )
    {
        throw new ServletException("No page defined");
    }

    NDC.push( wiki.getApplicationName()+":"+pagereq );

    WikiPage wikipage = wiki.getPage( pagereq );

    if( wikipage == null )
    {
        wikipage = new WikiPage( pagereq );
    }

    WikiContext wikiContext = new WikiContext( wiki, wikipage );
    wikiContext.setRequestContext( WikiContext.PREVIEW );
    wikiContext.setHttpRequest( request );

    pageContext.setAttribute( WikiTagBase.ATTR_CONTEXT,
                              wikiContext,
                              PageContext.REQUEST_SCOPE );

    response.setContentType("text/html; charset="+wiki.getContentEncoding() );

    pageContext.setAttribute( "usertext",
                              wiki.safeGetParameter( request, "text" ),
                              PageContext.REQUEST_SCOPE );

    long lastchange = 0;

    Date d = wikipage.getLastModified();
    if( d != null ) lastchange = d.getTime();

    pageContext.setAttribute( "lastchange",
                              Long.toString( lastchange ),
                              PageContext.REQUEST_SCOPE );


    String contentPage = "templates/"+skin+"/ViewTemplate.jsp";
%>
<wiki:Include page="<%=contentPage%>" />
<%
    NDC.pop();
    NDC.remove();
%>

