<%@ page import="org.apache.log4j.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.plugin.*" %>
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
    // Create wiki context; no need to check for authorization since the 
    // redirect will take care of that
    WikiContext wikiContext = wiki.createContext( request, WikiContext.EDIT );
    String pagereq = wikiContext.getName();
    
    // Redirect if the request was for a 'special page'
    String specialpage = wiki.getSpecialPageReference( pagereq );
    if( specialpage != null )
    {
        // FIXME: Do Something Else
        response.sendRedirect( specialpage );
        return;
    }

    WeblogEntryPlugin p = new WeblogEntryPlugin();
    
    String newEntry = p.getNewEntryPage( wiki, pagereq );

    // Redirect to a new page for user to edit
    response.sendRedirect( wiki.getEditURL(newEntry) );
%>

