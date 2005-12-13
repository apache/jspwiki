<%@ page import="org.apache.log4j.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="java.util.*" %>
<%@ page import="com.ecyrd.jspwiki.tags.WikiTagBase" %>
<%@ page errorPage="/Error.jsp" %>
<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>

<%! 
    public void jspInit()
    {
        wiki = WikiEngine.getInstance( getServletConfig() );
    }

    Logger log = Logger.getLogger("JSPWikiSearch");
    WikiEngine wiki;
%>

<%
    // Create wiki context and check for authorization
    WikiContext wikiContext = wiki.createContext( request, WikiContext.FIND );
    wikiContext.checkAccess( response );
    String pagereq = wikiContext.getPage().getName();
    NDC.push( wiki.getApplicationName()+":"+pagereq );

    // Get the search results
    Collection list = null;
    String query = request.getParameter( "query");
    if( query != null )
    {
        log.info("Searching for string "+query);

        try
        {
            list = wiki.findPages( query );

            pageContext.setAttribute( "searchresults",
                                      list,
                                      PageContext.REQUEST_SCOPE );
        }
        catch( Exception e )
        {
            wikiContext.getWikiSession().addMessage( e.getMessage() );
        }
        
        query = TextUtil.replaceEntities( query );

        pageContext.setAttribute( "query",
                                  query,
                                  PageContext.REQUEST_SCOPE );
    }

    // Stash the wiki context
    pageContext.setAttribute( WikiTagBase.ATTR_CONTEXT,
                              wikiContext,
                              PageContext.REQUEST_SCOPE );

    // Set the content type and include the response content
    response.setContentType("text/html; charset="+wiki.getContentEncoding() );
    String contentPage = wiki.getTemplateManager().findJSP( pageContext,
                                                            wikiContext.getTemplate(),
                                                            "ViewTemplate.jsp" );
%><wiki:Include page="<%=contentPage%>" /><%
    // Clean up the logger and clear UI messages
    NDC.pop();
    NDC.remove();
    wikiContext.getWikiSession().clearMessages();
    
    log.info("SEARCH COMPLETE");
%>
