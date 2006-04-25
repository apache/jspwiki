<%@ page import="org.apache.log4j.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="java.util.*" %>
<%@ page import="org.apache.commons.lang.*"%>
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
    if(!wikiContext.hasAccess( response )) return;
    String pagereq = wikiContext.getPage().getName();

    // Get the search results
    Collection list = null;
    String query = request.getParameter( "query");
    String go    = request.getParameter("go");
    
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
        
        //
        //  Did the user click on "go"?
        //           
        if( go != null )
        {
            if( list != null && list.size() > 0 )
            {
                SearchResult sr = (SearchResult) list.iterator().next();
                
                WikiPage wikiPage = sr.getPage();
                
                String url = wikiContext.getViewURL( wikiPage.getName() );
                
                response.sendRedirect( url );
                
                return;
            }
        }                              
    }

    // Set the content type and include the response content
    response.setContentType("text/html; charset="+wiki.getContentEncoding() );
    String contentPage = wiki.getTemplateManager().findJSP( pageContext,
                                                            wikiContext.getTemplate(),
                                                            "ViewTemplate.jsp" );
%><wiki:Include page="<%=contentPage%>" /><%
    log.debug("SEARCH COMPLETE");
%>
