<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ page language="java" pageEncoding="UTF-8"%>
<%@ page import="org.apache.log4j.*" %>
<%@ page import="java.util.Collection" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.ui.*" %>
<%@ page import="java.util.*" %>
<%@ page import="org.apache.commons.lang.*" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="com.ecyrd.jspwiki.auth.*" %>
<%@ page import="com.ecyrd.jspwiki.auth.permissions.*" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
<fmt:setBundle basename="templates.default"/>
<%! 
  public void jspInit()
  {
    wiki = WikiEngine.getInstance( getServletConfig() );
  }
  Logger log = Logger.getLogger("JSPWikiSearch");
  WikiEngine wiki;
%>
<%!  
  // FIXME: this should better be something like a wiki:Pagination TLD tag
  // FIXME: how to i18n
  //
  // 0 20 40 60
  // 0 20 40 60 80 next
  // previous 20 40 *60* 80 100 next
  // previous 40 60 80 100 120

  /* makePagination : return html string with pagination links 
   *    (eg:  previous 1 2 3 next)
   * startitem  : cursor
   * itemcount  : total number of items
   * pagesize   : number of items per page
   * maxpages   : number of pages directly accessible via a pagination link
   * linkAttr : html attributes of the generated links: use '%s' to replace with item offset
   */
  String wiki_Pagination( int startitem, int itemcount, int pagesize, int maxpages, String linkAttr, PageContext pageContext )
  {    
    if( itemcount <= pagesize ) return null; 
  
    int maxs = pagesize * maxpages;
    int mids = pagesize * ( maxpages / 2 );

    StringBuffer pagination = new StringBuffer();
    pagination.append( "<div class='pagination'>");
    pagination.append( LocaleSupport.getLocalizedMessage(pageContext, "info.pagination") );


    int cursor = 0;
    int cursormax = itemcount;
 
    if( itemcount > maxs )   //need to calculate real window ends
    { 
      if( startitem > mids ) cursor = startitem - mids;
      if( (cursor + maxs) > itemcount ) 
        cursor = ( ( 1 + itemcount/pagesize ) * pagesize ) - maxs ; 
      
      cursormax = cursor + maxs;
    }
               
    if( (startitem == -1) || (cursor > 0) ) 
      appendLink ( pagination, linkAttr, 0, pagesize, 
                   LocaleSupport.getLocalizedMessage(pageContext, "info.pagination.first"), pageContext );
    if( (startitem != -1 ) && (startitem-pagesize >= 0) ) 
      appendLink( pagination, linkAttr, startitem-pagesize, pagesize, 
                  LocaleSupport.getLocalizedMessage(pageContext, "info.pagination.previous"), pageContext );

    if( startitem != -1 )
    {
      while( cursor < cursormax )
      {
        if( cursor == startitem ) 
        { 
          pagination.append( "<span class='cursor'>" + (1+cursor/pagesize)+ "</span>&nbsp;&nbsp;" ); 
        } 
        else 
        { 
          appendLink( pagination, linkAttr, cursor, pagesize, Integer.toString(1+cursor/pagesize), pageContext );
        }
        cursor += pagesize;
      }     
    }

    if( (startitem != -1) && (startitem + pagesize < itemcount) ) 
      appendLink( pagination, linkAttr, startitem+pagesize, pagesize, 
                  LocaleSupport.getLocalizedMessage(pageContext, "info.pagination.next"), pageContext );

    if( (startitem == -1) || (cursormax < itemcount) ) 
      appendLink ( pagination, linkAttr, ( (itemcount/pagesize) * pagesize ), pagesize, 
                   LocaleSupport.getLocalizedMessage(pageContext, "info.pagination.last"), pageContext );

    if( startitem == -1 ) 
    { 
      pagination.append( "<span class='cursor'>" ); 
      pagination.append( LocaleSupport.getLocalizedMessage(pageContext, "info.pagination.all") );
      pagination.append( "</span>&nbsp;&nbsp;" ); 
    } 
    else
    {
      appendLink ( pagination, linkAttr, -1 , -1, 
                   LocaleSupport.getLocalizedMessage(pageContext, "info.pagination.all"), pageContext );
    }
    
    //pagination.append( " (Total items: " + itemcount + ")</div>" );
    pagination.append( LocaleSupport.getLocalizedMessage(pageContext, "info.pagination.total", new Object[]{new Integer(itemcount)} ) );
    pagination.append( "</div>" );
    
    return pagination.toString();
  } 

  // linkAttr : use '%s' to replace with cursor offset
  // eg :
  // linkAttr = "href='#' title='%s' onclick='$(start).value= %s; updateSearchResult();'";
  void appendLink( StringBuffer sb, String linkAttr, int linkFrom, int pagesize, String linkText, PageContext pageContext )
  {
    String title =  LocaleSupport.getLocalizedMessage(pageContext, "info.pagination.showall");
    //if( linkFrom > -1 ) title = "Show page from " + (linkFrom+1) + " to "+ (linkFrom+pagesize) ;
    if( linkFrom > -1 ) 
      title = LocaleSupport.getLocalizedMessage(pageContext, "info.pagination.show", new Object[]{new Integer(linkFrom+1), new Integer(linkFrom+pagesize) } );

    sb.append( "<a title=\"" + title + "\" " );
    sb.append( TextUtil.replaceString( linkAttr, "%s", Integer.toString(linkFrom) ) );
    sb.append( ">" + linkText + "</a>&nbsp;&nbsp;" );
  } ;

%>
<%
  /* ********************* actual start ********************* */
  /* FIXME: too much hackin on this level */
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
  /**************************************************************/ 
 
  int startitem    = 0;    // first item to show
  int pagesize    = 20;   // #items to show in one go
  int maxpages     = 9;    // max pagination links -- odd figure
  int itemcount;           // #items in items

  String parm_start    = request.getParameter( "start");
  if( parm_start != null ) startitem = Integer.parseInt( parm_start ) ;

  String parm_pagesize = request.getParameter( "pagesize"); 
  if( parm_pagesize != null ) pagesize = Integer.parseInt( parm_pagesize ) ;

  String parm_details = request.getParameter( "details");

  /**********/  
  String start = (String)request.getParameter("start");
    
  int startVal = 0;
    
  try
  {
    startVal = Integer.parseInt(start);
  } catch(Exception e) {}
    
  if( startVal < 0 ) startVal = 0;
    
  int endVal = startVal + 20;
    
  Collection list = (Collection)pageContext.getAttribute( "searchresults", PageContext.REQUEST_SCOPE );
                                                             
 int prevSize = 0, nextSize = 0;
    
 if( list != null )
 {
   if( endVal > list.size() ) endVal = list.size();
   prevSize = Math.max( startVal, 20 );
   nextSize = Math.min(list.size() - endVal, 20);
 }

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
  

<%--          <p>
          <i><fmt:message key="find.resultsstart">
             <fmt:param><wiki:SearchResultsSize/></fmt:param>
             <fmt:param><%=startVal+1%></fmt:param>
             <fmt:param><%=endVal%></fmt:param>
             </fmt:message>
          </p>
<%--
          <% if( startVal > 0 ) { %>
          <wiki:Link jsp="Search.jsp">
              <wiki:Param name="query" value="<%=URLEncoder.encode(query)%>"/>
              <wiki:Param name="start" value="<%=Integer.toString(startVal-prevSize)%>"/>
              <fmt:message key="find.getprevious">
                 <fmt:param><%=prevSize%></fmt:param>
              </fmt:message>
          </wiki:Link>.
          <% } %>
          
          <% if( endVal < list.size() ) { %>
          <wiki:Link jsp="Search.jsp">
              <wiki:Param name="query" value="<%=URLEncoder.encode(query)%>"/>
              <wiki:Param name="start" value="<%=Integer.toString(endVal)%>"/>
              <fmt:message key="find.getnext">
                 <fmt:param><%=nextSize%></fmt:param>
              </fmt:message>
          </wiki:Link>.
          <% } %>
          </p>
          <p>
--%>
<%
  String linkAttr = "href='#' title='Show search block starting at %s' onclick='$(\"start\").value=%s; updateSearchResult();'";
  String pagination = wiki_Pagination(startitem, list.size(), pagesize, maxpages, linkAttr, pageContext);
%>
    <%= (pagination == null) ? "" : pagination %>

    <div class="graphBars">
    <div class="zebra-table">
    <table class="wikitable" >

      <tr>
         <th align="left"><fmt:message key="find.results.page"/></th>
         <th align="left"><fmt:message key="find.results.score"/></th>
      </tr>

      <wiki:SearchResultIterator id="searchref" start="<%=Integer.toString(startVal)%>" maxItems="20">
      <tr>
        <td><wiki:LinkTo><wiki:PageName/></wiki:LinkTo></td>
        <td><span class="gBar"><%= searchref.getScore() %></span></td>
      </tr>

<%
      if( parm_details != null )
      {
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
      } /* parm_details */
%>

        </wiki:SearchResultIterator>

        <wiki:IfNoSearchResults>
        <tr>
          <td colspan="2"><b><fmt:message key="find.noresults"/></b></td>
        </tr>
        </wiki:IfNoSearchResults>

      </table>
    </div>
    </div>

    <%= (pagination == null) ? "" : pagination %>


   </wiki:SearchResults>
