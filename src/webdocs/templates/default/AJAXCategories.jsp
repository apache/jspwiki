<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%! 
  public void jspInit()
  {
    wiki = WikiEngine.getInstance( getServletConfig() );
  }
  WikiEngine wiki;
%>
<%
  // Copied from a top-level jsp -- which would be a better place to put this 
  WikiContext wikiContext = wiki.createContext( request, WikiContext.VIEW );
  if( !wikiContext.hasAccess( response ) ) return;
  String pagereq = wikiContext.getPage().getName();

  response.setContentType("text/html; charset="+wiki.getContentEncoding() );
%>
<div class='categoryTitle'><wiki:LinkTo><wiki:PageName /></wiki:LinkTo></div>
<div class='categoryText'><wiki:Plugin plugin="ReferringPagesPlugin" args="max='20' before='*' after='\n' " /></div>