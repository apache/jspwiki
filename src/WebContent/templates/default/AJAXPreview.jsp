<%@ taglib uri="http://jakarta.apache.org/jspwiki.tld" prefix="wiki" %>
<%@ page language="java" pageEncoding="UTF-8" %>
<%@ page import="org.apache.wiki.log.Logger" %>
<%@ page import="org.apache.wiki.log.LoggerFactory" %>
<%@ page import="org.apache.wiki.*" %>
<%@ page import="org.apache.wiki.auth.*" %>
<%@ page import="org.apache.wiki.auth.permissions.*" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://stripes.sourceforge.net/stripes.tld" prefix="stripes" %>
<stripes:useActionBean beanclass="org.apache.wiki.action.ViewActionBean" event="view" id="wikiActionBean" />
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