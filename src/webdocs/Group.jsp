<%@ page import="org.apache.log4j.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.auth.NoSuchPrincipalException" %>
<%@ page import="com.ecyrd.jspwiki.auth.WikiSecurityException" %>
<%@ page import="com.ecyrd.jspwiki.auth.authorize.Group" %>
<%@ page import="com.ecyrd.jspwiki.auth.authorize.GroupManager" %>
<%@ page errorPage="/Error.jsp" %>
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
    WikiContext wikiContext = wiki.createContext( request, WikiContext.VIEW_GROUP );
    if(!wikiContext.hasAccess( response )) return;
    
    // Extract the current user, group name, members
    WikiSession wikiSession = wikiContext.getWikiSession();
    GroupManager groupMgr = wiki.getGroupManager();
    Group group = null;
    try 
    {
        group = groupMgr.parseGroup( wikiContext, false );
        pageContext.setAttribute ( "Group", group, PageContext.REQUEST_SCOPE );
    }
    catch ( NoSuchPrincipalException e )
    {
        // New group; let GroupContent print out the message...
    }
    catch ( WikiSecurityException e )
    {
        wikiSession.addMessage( GroupManager.MESSAGES_KEY, e.getMessage() );
    }
    
    // Set the content type and include the response content
    response.setContentType("text/html; charset="+wiki.getContentEncoding() );
    String contentPage = wiki.getTemplateManager().findJSP( pageContext,
                                                            wikiContext.getTemplate(),
                                                            "ViewTemplate.jsp" );

%><wiki:Include page="<%=contentPage%>" />

