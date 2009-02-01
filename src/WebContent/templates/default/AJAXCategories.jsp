<%@ page import="org.apache.wiki.*" %>
<%@ taglib uri="http://jakarta.apache.org/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="http://stripes.sourceforge.net/stripes.tld" prefix="stripes" %>
<stripes:useActionBean beanclass="org.apache.wiki.action.ViewActionBean" event="view" id="wikiActionBean" />
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
  String pagereq = wikiContext.getPage().getName();

  response.setContentType("text/html; charset="+wiki.getContentEncoding() );
%>
<div class='categoryTitle'><wiki:LinkTo><wiki:PageName/></wiki:LinkTo></div>
<div class='categoryText'><wiki:Plugin plugin="ReferringPagesPlugin" args="max='20' before='*' after='\n' " /></div>