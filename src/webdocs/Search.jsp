<%@ page import="org.apache.log4j.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="java.util.*" %>
<%@ page import="com.ecyrd.jspwiki.tags.WikiTagBase" %>
<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>

<%! 
    public void jspInit()
    {
        wiki = WikiEngine.getInstance( getServletConfig() );
    }

    Category log = Category.getInstance("JSPWikiSearch");
    WikiEngine wiki;
%>


<%
    String pagereq = "FindPage";
    String skin    = wiki.getTemplateDir();

    NDC.push( wiki.getApplicationName()+": Search" );

    String pageurl = wiki.encodeName( pagereq );    

    String query = wiki.safeGetParameter( request, "query");
    Collection list = null;

    WikiPage wikipage = new WikiPage( pagereq );

    WikiContext wikiContext = new WikiContext( wiki, wikipage );
    wikiContext.setRequestContext( "find" );

    pageContext.setAttribute( WikiTagBase.ATTR_CONTEXT,
                              wikiContext,
                              PageContext.REQUEST_SCOPE );

    response.setContentType("text/html; charset="+wiki.getContentEncoding() );

    if( query != null )
    {
        log.info("Searching for string "+query);

        list = wiki.findPages( query );

        pageContext.setAttribute( "searchresults",
                                  list,
                                  PageContext.REQUEST_SCOPE );

        pageContext.setAttribute( "query",
                                  query,
                                  PageContext.REQUEST_SCOPE );

        log.info("Found "+list.size()+" pages");
    }

    String contentPage = "templates/"+skin+"/ViewTemplate.jsp";
%>

<wiki:Include page="<%=contentPage%>" />

<%
    NDC.pop();
    NDC.remove();
%>
