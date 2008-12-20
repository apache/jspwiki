<%@ page import="com.ecyrd.jspwiki.log.Logger" %>
<%@ page import="com.ecyrd.jspwiki.log.LoggerFactory" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.auth.NoSuchPrincipalException" %>
<%@ page import="com.ecyrd.jspwiki.auth.WikiSecurityException" %>
<%@ page import="com.ecyrd.jspwiki.auth.authorize.GroupManager" %>
<%@ page errorPage="/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/jspwiki.tld" prefix="wiki" %>

<%! 
    Logger log = LoggerFactory.getLogger("JSPWiki");
%>

<%
    WikiEngine wiki = WikiEngine.getInstance( getServletConfig() );
    // Create wiki context and check for authorization
    WikiContext wikiContext = wiki.createContext( request, WikiContext.DELETE_GROUP );

    WikiSession wikiSession = wikiContext.getWikiSession();
    GroupManager groupMgr = wiki.getGroupManager();
    String name = request.getParameter( "group" );
    
    if ( name == null )
    {
        // Group parameter was null
        wikiSession.addMessage( GroupManager.MESSAGES_KEY, "Parameter 'group' cannot be null." );
        response.sendRedirect( "Group.jsp" );
    }

    // Check that the group exists first
    try
    {
        groupMgr.getGroup( name );
    }
    catch ( NoSuchPrincipalException e )
    {
        // Group does not exist
        wikiSession.addMessage( GroupManager.MESSAGES_KEY, e.getMessage() );
        response.sendRedirect( "Group.jsp" );
    }

    // Now, let's delete the group
    try 
    {
        groupMgr.removeGroup( name );
        response.sendRedirect( "." );
    }
    catch ( WikiSecurityException e )
    {
        // Send error message
        wikiSession.addMessage( GroupManager.MESSAGES_KEY, e.getMessage() );
        response.sendRedirect( "Group.jsp" );
    }

%>

