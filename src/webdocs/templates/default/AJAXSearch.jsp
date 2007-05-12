<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ page import="org.apache.log4j.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.auth.*" %>
<%@ page import="com.ecyrd.jspwiki.auth.permissions.*" %>
<%@ page import="java.util.*" %>
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
  String wiki_Pagination( int startitem, int itemcount, int pagesize, int maxpages, 
                                  String linkAttr )
  {    
    if( itemcount <= pagesize ) return null;
  
    int maxs = pagesize * maxpages;
    int mids = pagesize * ( maxpages / 2 );

    StringBuffer pagination = new StringBuffer();
    pagination.append( "<div class='pagination'>Pagination: " ); 

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
      appendLink ( pagination, linkAttr, 0, pagesize, "first" );
    if( (startitem != -1 ) && (startitem-pagesize >= 0) ) 
      appendLink( pagination, linkAttr, startitem-pagesize, pagesize, "previous" );

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
          appendLink( pagination, linkAttr, cursor, pagesize, Integer.toString(1+cursor/pagesize) );
        }
        cursor += pagesize;
      }     
    }

    if( (startitem != -1) && (startitem + pagesize < itemcount) ) 
      appendLink( pagination, linkAttr, startitem+pagesize, pagesize, "next" );

    if( (startitem == -1) || (cursormax < itemcount) ) 
      appendLink ( pagination, linkAttr, ( (itemcount/pagesize) * pagesize ), pagesize, "last" );

    if( startitem == -1 ) 
    { 
      pagination.append( "<span class='cursor'>all</span>&nbsp;&nbsp;" ); 
    } 
    else
    {
      appendLink ( pagination, linkAttr, -1 , -1, "all" );
    }
    
    pagination.append( " (Total items: " + itemcount + ")</div>" );
    
    return pagination.toString();
  } 

  // linkAttr : use '%s' to replace with cursor offset
  // eg :
  // linkAttr = "href='#' title='%s' onclick='$(start).value= %s; updateSearchResult();'";
  void appendLink( StringBuffer sb, String linkAttr, int linkFrom, int pagesize, String linkText )
  {
    String title = "Show all pages";
    if( linkFrom > -1 ) title = "Show page from " + (linkFrom+1) + " to "+ (linkFrom+pagesize) ;

    sb.append( "<a title=\"" + title + "\" " );
    sb.append( TextUtil.replaceString( linkAttr, "%s", Integer.toString(linkFrom) ) );
    sb.append( ">" + linkText + "</a>&nbsp;&nbsp;" );
  } ;

%>
<%
  /* ********************* actual start ********************* */
  /* Create wiki context and check for authorization */
  WikiContext wikiContext = wiki.createContext( request, WikiContext.FIND );
  if(!wikiContext.hasAccess( response )) return;
  
  int startitem    = 0;    // first item to show
  int pagesize    = 20;   // #items to show in one go
  int maxpages     = 9;    // max pagination links -- odd figure
  int itemcount;           // #items in items
  ArrayList items  = null; // all search results from lucene

  String parm_start    = request.getParameter( "start");
  if( parm_start != null ) startitem = Integer.parseInt( parm_start ) ;

  String parm_pagesize = request.getParameter( "pagesize"); 
  if( parm_pagesize != null ) pagesize = Integer.parseInt( parm_pagesize ) ;

  String parm_details = request.getParameter( "details");

  String parm_compact = request.getParameter( "compact");

  String query = request.getParameter( "query");
  if( (query != null) && ( !query.trim().equals("") ) )
  {
    try
    { 
      Collection list = wiki.findPages( query );

      //  Filter down to only those that we actually have a permission to view
      AuthorizationManager mgr = wiki.getAuthorizationManager();
  
      items = new ArrayList();
      
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

      itemcount = items.size(); 



      if( parm_compact != null )
      {
%>
          <ul>
<%
        if( itemcount > 0 )
        {
          int cursor = 0;
          Iterator m_iterator = items.iterator();
          while( m_iterator.hasNext() && ( cursor < pagesize ) ) 
          {
            cursor++;
            SearchResult r = (SearchResult) m_iterator.next();
            String p = wiki.beautifyTitle( r.getPage().getName() );
%>
            <li><wiki:Link page="<%= p %>" ><%= p %></wiki:Link></li>
<%
          }
%>
         </ul> 
<%
        }

      }
      else
      {
%>

  <h3>Search results for '<%= query %>'</h3>
  <p>
  Try this same search on 
     <a class="external" 
        href="http://www.google.com/search?q=<%=query%>"
        title="Google Search '<%=query%>'"
        target="_blank">Google</a><img class="outlink" src="images/out.png" alt="" />
     or
     <a class="external" 
        href="http://en.wikipedia.org/wiki/Special:Search?search=<%=query%>" 
        title="Wikipedia Search '<%=query%>'"
        target="_blank">Wikipedia</a><img class="outlink" src="images/out.png" alt="" />
  </p>
<% 
  if( itemcount > 0 )
  {
    String anchorAttr = "href='#' title='Show search block starting at %s' onclick='$(\"start\").value=%s; updateSearchResult();'";
    String pagination = wiki_Pagination( startitem, itemcount, pagesize, maxpages, anchorAttr ) ;
%>
    <div class="graphBars">
    <div class="zebra-table">
    <table class="wikitable">
<%  if( pagination != null ) { %>
      <tr>
        <th colspan="2"><%= pagination %></th>
      </tr>
<%  } %>
      <tr>
        <th align="left">Page</th>
        <th align="left">Score</th>
      </tr>
<%
    int cursor = 0;
    Iterator m_iterator = items.iterator();
    while( m_iterator.hasNext() && ( cursor < startitem ) ) 
    { 
       cursor++;
       m_iterator.next();
    }
     
    int cursormax = startitem + pagesize;
    if( startitem == -1 ) cursormax = itemcount;

    while( m_iterator.hasNext() && ( cursor < cursormax ) ) 
    {
       cursor++;
       SearchResult r = (SearchResult) m_iterator.next();
       String p = wiki.beautifyTitle( r.getPage().getName() );
%>
        <tr>
          <td><%= cursor %>. <wiki:Link page="<%= p %>" ><%= p %></wiki:Link></td>
          <td><span class="gBar"><%= r.getScore() %></span></td>
        </tr>  
<%
      if( parm_details != null )
      {
        String[] contexts = r.getContexts();
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
    } /* while */
%>
<%  if( pagination != null ) { %>
      <tr>
        <th colspan="2"><%= pagination %></th>
      </tr>
<%  } %>
    </table> 
    </div>
    </div>
<%    
        }
      }
    }
    catch( Exception e )
    {
      wikiContext.getWikiSession().addMessage( e.getMessage() );
    }
    
    query = TextUtil.replaceEntities( query );
    pageContext.setAttribute( "query",
                              query,
                              PageContext.REQUEST_SCOPE );
  }

  // Set the content type and include the response content
  response.setContentType("text/html; charset="+wiki.getContentEncoding() );
%>
