<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ page language="java" pageEncoding="UTF-8" %>
<%@ page import="com.ecyrd.jspwiki.log.Logger" %>
<%@ page import="com.ecyrd.jspwiki.log.LoggerFactory" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.auth.*" %>
<%@ page import="com.ecyrd.jspwiki.auth.permissions.*" %>
<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/stripes.tld" prefix="stripes" %>
<stripes:useActionBean beanclass="com.ecyrd.jspwiki.action.ViewActionBean" event="view" />
<%! 
  public void jspInit()
  {
    wiki = WikiEngine.getInstance( getServletConfig() );
  }
  Logger log = LoggerFactory.getLogger("JSPWikiSearch");
  WikiEngine wiki;
%>
<%
  // Copied from a top-level jsp -- which would be a better place to put this 
  WikiContext wikiContext = wiki.createContext( request, WikiContext.VIEW );

  response.setContentType("text/html; charset="+wiki.getContentEncoding() );
  
  String wikimarkup = request.getParameter( "wikimarkup" );
%>
<wiki:Translate><%= wikimarkup %></wiki:Translate>