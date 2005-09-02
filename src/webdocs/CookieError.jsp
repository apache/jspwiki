<%@ page import="org.apache.log4j.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.tags.WikiTagBase" %>
<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>

<%! 
    public void jspInit()
    {
        wiki = WikiEngine.getInstance( getServletConfig() );
    }
    WikiEngine wiki;
%>

<%
    String skin    = wiki.safeGetParameter( request, "skin" );

    if( skin == null )
    {
        skin = wiki.getTemplateDir();
    }

    WikiContext wikiContext = wiki.createContext( request, WikiContext.VIEW );
 
    pageContext.setAttribute( WikiTagBase.ATTR_CONTEXT,
                              wikiContext,
                              PageContext.REQUEST_SCOPE );

    //
    //  Alright, then start responding.
    //

    response.setContentType("text/html; charset="+wiki.getContentEncoding() );

    String contentPage = "templates/"+skin+"/CookieErrorTemplate.jsp";
%>

  <wiki:Include page="<%=contentPage%>" />
