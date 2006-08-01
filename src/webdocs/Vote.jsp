<%@ page import="org.apache.log4j.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.plugin.VotePlugin" %>
<%@ page errorPage="/Error.jsp" %>
<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%! 
    public void jspInit()
    {
        wiki = WikiEngine.getInstance( getServletConfig() );
    }
    Logger log = Logger.getLogger("JSPWiki"); 
    WikiEngine wiki;
%><%
    // Create wiki context and check for authorization
    WikiContext wikiContext = wiki.createContext( request, WikiContext.VIEW );
    String pagereq = wikiContext.getName();
    
    String vote    = request.getParameter("vote");
    log.info("Vote '"+pagereq+"' from "+request.getRemoteAddr()+" by "+request.getRemoteUser() );

    VotePlugin plugin = new VotePlugin();

    plugin.vote( wikiContext, TextUtil.isPositive( vote ) ? 1 : -1 );

    response.sendRedirect( wiki.getBaseURL()+"Wiki.jsp?page=VoteOk" );
%>

