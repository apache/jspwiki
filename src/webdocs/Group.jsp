<%@ page import="com.ecyrd.jspwiki.log.Logger" %>
<%@ page import="com.ecyrd.jspwiki.log.LoggerFactory" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.auth.NoSuchPrincipalException" %>
<%@ page import="com.ecyrd.jspwiki.auth.WikiSecurityException" %>
<%@ page import="com.ecyrd.jspwiki.auth.authorize.Group" %>
<%@ page import="com.ecyrd.jspwiki.auth.authorize.GroupManager" %>
<%@ page errorPage="/Error.jsp" %>
<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%! 
    Logger log = LoggerFactory.getLogger("JSPWiki"); 
%>

<%
    WikiEngine wiki = WikiEngine.getInstance( getServletConfig() );
    // Create wiki context and check for authorization
    WikiContext wikiContext = wiki.createContext( request, WikiContext.VIEW_GROUP );
    
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

