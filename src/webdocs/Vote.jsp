<%@ page import="org.apache.log4j.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.tags.WikiTagBase" %>
<%@ page import="com.ecyrd.jspwiki.plugin.VotePlugin" %>
<%@ page import="com.ecyrd.jspwiki.auth.*" %>
<%@ page errorPage="/Error.jsp" %>
<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%! 
    public void jspInit()
    {
        wiki = WikiEngine.getInstance( getServletConfig() );
    }
    Category log = Category.getInstance("JSPWiki"); 
    WikiEngine wiki;
%><%
    WikiContext wikiContext = wiki.createContext( request, WikiContext.VIEW );
    String pagereq = wikiContext.getPage().getName();
    String vote    = request.getParameter("vote");

    NDC.push( wiki.getApplicationName()+":"+pagereq );
    
    log.info("Vote '"+pagereq+"' from "+request.getRemoteAddr()+" by "+request.getRemoteUser() );


    pageContext.setAttribute( WikiTagBase.ATTR_CONTEXT,
                              wikiContext,
                              PageContext.REQUEST_SCOPE );

    VotePlugin plugin = new VotePlugin();

    plugin.vote( wikiContext, TextUtil.isPositive( vote ) ? 1 : -1 );

    response.sendRedirect( wiki.getBaseURL()+"Wiki.jsp?page=VoteOk" );

    NDC.pop();
    NDC.remove();
%>

