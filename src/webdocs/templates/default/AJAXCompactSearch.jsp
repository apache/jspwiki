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
<%
  /* ********************* actual start ********************* */
  /* Create wiki context and check for authorization */
  WikiContext wikiContext = wiki.createContext( request, WikiContext.FIND );
  if(!wikiContext.hasAccess( response )) return;
  
  ArrayList items  = null; // all search results from lucene

  int pagesize    = 20;   // #items to show in one go
  int itemcount;          // #items in items
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