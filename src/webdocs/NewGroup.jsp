<%@ page import="java.util.Iterator" %>
<%@ page import="org.apache.log4j.*" %>
<%@ page import="com.ecyrd.jspwiki.WikiContext" %>
<%@ page import="com.ecyrd.jspwiki.WikiEngine" %>
<%@ page import="com.ecyrd.jspwiki.WikiPage" %>
<%@ page import="com.ecyrd.jspwiki.WikiSession" %>
<%@ page import="com.ecyrd.jspwiki.filters.RedirectException" %>
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
    // Create wiki context and check for authorization
    WikiContext wikiContext = wiki.createContext( request, WikiContext.CREATE_GROUP );
    wikiContext.checkAccess( response );
    String user = wikiContext.getCurrentUser().getName();
    NDC.push( wiki.getApplicationName()+":"+ wikiContext.getPage().getName() );
    
    // Extract the group name, members and action attributes
    String ok      = request.getParameter( "ok" );
    String name    = request.getParameter( "name" );
    String members = request.getParameter( "members" );
    if ( name == null ) { name = ""; }
    name = name.trim();
    if ( members == null ) { members = user; }
    members = members.trim();
    
    if( ok != null || "save".equals(request.getParameter("action")) )
    {
        // Validate the group
        WikiSession wikiSession = wikiContext.getWikiSession();
        if ( name == null || name.length() < 1 ) 
        {
            wikiSession.addMessage("Group name may not be blank.");
        }
        if ( members == null || members.length() < 1 )
        {
            wikiSession.addMessage("The group must have at least one member.");
        }
        
        // If page already exists, disallow
        String groupPage = "Group" + name;
        if ( wiki.pageExists( groupPage ) )
        {
            wikiSession.addMessage("A group named '" + name + "' already exists. Choose another.");
            log.error( "User " + user + " tried to create a group page " + groupPage + ", but it already exists!" );
        }
        
        // If no errors, build and save the group page; otherwise redirect to self
        if ( wikiSession.getMessages().length > 0 )
        {
            response.sendRedirect( "NewGroup.jsp?name=" + name + "&members=" + members );
            return;
        }
        else
        {
            WikiContext groupContext = new WikiContext( wiki, request, new WikiPage( wiki, groupPage ) );
            log.info("Creating group "+groupPage+". User="+user+", host="+request.getRemoteAddr() );

            //  Set author information
            groupContext.getPage().setAuthor( user );
            log.info( groupContext.getPage().getName() );

            // Create the actual page text
            // By default, allow the members of the group to edit it.
            String text = "[{ALLOW edit " + name + "}]\n" +
                "[{SET members='" + members + "'}]\n" +
                "This is a wiki group. Edit this page to see its members.";
            try
            {
                wiki.saveText( groupContext, text );
            }
            catch( RedirectException ex )
            {
                log.error( "Couldn't save page " + groupPage + ": " + ex.getMessage() );
                wikiSession.addMessage( ex.getMessage() );
                response.sendRedirect( ex.getRedirect() );
                return;
            }
        }
        response.sendRedirect( wiki.getBaseURL()+"Wiki.jsp" );
    }
    
    pageContext.setAttribute( "name", name, PageContext.REQUEST_SCOPE );
    pageContext.setAttribute( "members", members, PageContext.REQUEST_SCOPE );
    
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
%>
