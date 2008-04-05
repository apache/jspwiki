<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ page language="java" pageEncoding="UTF-8"%>
<%@ page import="org.apache.log4j.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.ui.*" %>
<%@ page import="java.util.*" %>
<%@ page import="java.util.Collection" %>
<%@ page import="org.apache.commons.lang.*" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="com.ecyrd.jspwiki.auth.*" %>
<%@ page import="com.ecyrd.jspwiki.auth.permissions.*" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
<fmt:setLocale value="${prefs['Language']}" />
<fmt:setBundle basename="templates.default"/>
<%! 
  public void jspInit()
  {
    wiki = WikiEngine.getInstance( getServletConfig() );
  }
  Logger log = Logger.getLogger("JSPWikiSearch");
  WikiEngine wiki;
%>
<%
  /* ********************* actual start ********************* */
  /* FIXME: too much hackin on this level -- should better happen in toplevel jsp's */
  /* Create wiki context and check for authorization */
  WikiContext wikiContext = wiki.createContext( request, WikiContext.FIND );
  if(!wikiContext.hasAccess( response )) return;
 
  String query = request.getParameter( "query");

  if( (query != null) && ( !query.trim().equals("") ) )
  {
    try
    { 
      Collection list = wiki.findPages( query );

      //  Filter down to only those that we actually have a permission to view
      AuthorizationManager mgr = wiki.getAuthorizationManager();
  
      ArrayList items = new ArrayList();
      
      for( Iterator i = list.iterator(); i.hasNext(); )
      {
        SearchResult r = (SearchResult)i.next();
    
        WikiPage p = r.getPage();
    
        PagePermission pp = new PagePermission( p, PagePermission.VIEW_ACTION );

        try
        {            
          if( mgr.checkPermission( wikiContext.getWikiSession(), pp ) )
          {
            items.add( r );
          }
        }
        catch( Exception e ) { log.error( "Searching for page "+p, e ); }
      }
      
      pageContext.setAttribute( "searchresults", items, PageContext.REQUEST_SCOPE );
    }
    catch( Exception e )
    {
       wikiContext.getWikiSession().addMessage( e.getMessage() );
    }
  }
%>
<%
  int startitem = 0; // first item to show
  int maxitems = 20; // number of items to show in result

  String parm_start    = request.getParameter( "start");
  if( parm_start != null ) startitem = Integer.parseInt( parm_start ) ;

  Collection list = (Collection)pageContext.getAttribute( "searchresults", PageContext.REQUEST_SCOPE );
  if( startitem == -1 ) maxitems = list.size(); //show all
%>

<wiki:SearchResults>

  <h4><fmt:message key="find.heading.results"><fmt:param><c:out value="${param.query}"/></fmt:param></fmt:message></h4>

  <p>
  <fmt:message key="find.externalsearch"/>
    <a class="external" 
        href="http://www.google.com/search?q=<c:out value='${param.query}'/>"
        title="Google Search '<c:out value='${param.query}'/>'"
       target="_blank">Google</a><img class="outlink" src="images/out.png" alt="" />
    |     
    <a class="external" 
        href="http://en.wikipedia.org/wiki/Special:Search?search=<c:out value='${param.query}'/>" 
        title="Wikipedia Search '<c:out value='${param.query}'/>'"
       target="_blank">Wikipedia</a><img class="outlink" src="images/out.png" alt="" />
  </p>

  <wiki:SetPagination start="${param.start}" total="<%=list.size()%>" pagesize="20" maxlinks="9" 
                     fmtkey="info.pagination"
                       href="#" 
                    onclick="$('start').value=%s; SearchBox.runfullsearch();" />
  
    <div class="graphBars">
    <div class="zebra-table">
    <table class="wikitable" >

      <tr>
         <th align="left"><fmt:message key="find.results.page"/></th>
         <th align="left"><fmt:message key="find.results.score"/></th>
      </tr>

      <wiki:SearchResultIterator id="searchref" start="${param.start}" maxItems="<%=maxitems%>">
      <tr>
        <td><wiki:LinkTo><wiki:PageName/></wiki:LinkTo></td>
        <td><span class="gBar"><%= searchref.getScore() %></span></td>
      </tr>

	  <c:if test="${param.details == 'on'}">
<%
        String[] contexts = searchref.getContexts();
        if( (contexts != null) && (contexts.length > 0) ) 
        {
%>  
      <tr class="odd" >
        <td colspan="2" >
          <div class="fragment">
<%
          for (int i = 0; i < contexts.length; i++) 
          {
%>
            <%= (i > 0 ) ? "<span class='fragment_ellipsis'> ... </span>" : ""  %>
            <%= contexts[i]  %>
<%
          }
%>
           </div>
         </td>
       </tr>
<% 
        }
%>
	  </c:if><%-- details --%>
      </wiki:SearchResultIterator>

      <wiki:IfNoSearchResults>
        <tr>
          <td class="nosearchresult" colspan="2"><fmt:message key="find.noresults"/></td>
        </tr>
      </wiki:IfNoSearchResults>

      </table>
    </div>
    </div>
    ${pagination}

   </wiki:SearchResults>
