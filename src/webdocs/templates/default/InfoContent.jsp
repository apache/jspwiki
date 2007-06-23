<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.auth.*" %>
<%@ page import="com.ecyrd.jspwiki.auth.permissions.*" %>
<%@ page import="java.security.Permission" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<fmt:setBundle basename="templates.default"/>

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
  int pagesize  = 20; //default #revisions shown per page
  int maxpages  = 9;  //max #paginations links -- choose odd figure
  int itemcount = 0;  //number of page versions

  WikiContext wikiContext = WikiContext.findContext(pageContext);
  WikiPage wikiPage = wikiContext.getPage();
  int attCount = wikiContext.getEngine().getAttachmentManager()
                            .listAttachments( wikiContext.getPage() ).size();
  String attTitle = LocaleSupport.getLocalizedMessage(pageContext, "attach.tab");
  if( attCount != 0 ) attTitle += " (" + attCount + ")";

  String creationDate   ="";
  String creationAuthor ="";
  //FIXME -- seems not to work correctly for attachments !!
  WikiPage firstPage = wikiContext.getEngine().getPage( wikiPage.getName(), 1 );
  if( firstPage != null )
  {
    creationAuthor = firstPage.getAuthor();
  }

  try
  {
    itemcount = wikiPage.getVersion(); /* highest version */
  }
  catch( Exception  e )  { /* dont care */ }

  int startitem = itemcount;

  String parm_start = (String)request.getParameter( "start" );
  if( parm_start != null ) startitem = Integer.parseInt( parm_start ) ;
  if( startitem > itemcount ) startitem = itemcount;
  if( startitem < -1 ) startitem = 0;

  String parm_pagesize = (String)request.getParameter( "pagesize" );
  if( parm_pagesize != null ) pagesize = Integer.parseInt( parm_pagesize ) ;

  if( startitem > -1 ) startitem = ( (startitem/pagesize) * pagesize );

  String linkAttr = "href='#' onclick='location.href= $(\"moreinfo\").href + \"&start=%s\"; ' ";
  String pagination = wiki_Pagination(startitem, itemcount, pagesize, maxpages, linkAttr, pageContext);

%>
<%--FIXME --%>
<a href="<wiki:PageInfoLink format='url' />" id="moreinfo" style="display:none" ><fmt:message key='actions.info'/></a>

<wiki:PageExists>

<%-- part 1 : normal wiki pages --%>
<wiki:PageType type="page">

  <wiki:TabbedSection defaultTab="info">
  
  <wiki:Tab id="pagecontent" 
         title='<%=LocaleSupport.getLocalizedMessage(pageContext, "actions.view")%>' 
     accesskey="v" 
	       url="<%=wikiContext.getURL(WikiContext.VIEW, wikiContext.getPage().getName())%>">
      <%--<wiki:Include page="PageTab.jsp"/> --%>
  </wiki:Tab>

  <wiki:Tab id="attach" title="<%= attTitle %>" accesskey="a">
    <wiki:Include page="AttachmentTab.jsp"/>
  </wiki:Tab>

  <wiki:Tab id="info" title="<%=LocaleSupport.getLocalizedMessage(pageContext, "info.tab")%>" accesskey="i" >

  <p>
  <fmt:message key='info.lastmodified'>
    <fmt:param><wiki:PageVersion >1</wiki:PageVersion></fmt:param>
    <fmt:param>
      <a href="<wiki:DiffLink format='url' version='latest' newVersion='previous' />"
        title="<fmt:message key='info.pagediff.title' />" >
        <fmt:formatDate value="<%= wikiPage.getLastModified() %>" pattern="${prefs['DateFormat']}" />
      </a>
    </fmt:param>
    <fmt:param><wiki:Author /></fmt:param>
  </fmt:message>

  <a href="<wiki:Link format='url' jsp='rss.jsp'>
             <wiki:Param name='page' value='<%=wikiPage.getName()%>'/>
             <wiki:Param name='mode' value='wiki'/>
           </wiki:Link>"
    title="<fmt:message key='info.rsspagefeed.title'>
             <fmt:param><wiki:PageName /></fmt:param>
           </fmt:message>" >
    <img src="<wiki:Link jsp='images/xml.png' format='url'/>" border="0" alt="[RSS]"/>
  </a>
  </p>

  <wiki:CheckVersion mode="notfirst">
    <p>
    <fmt:message key='info.createdon'>
      <%--<fmt:param><wiki:Link version="1"><%= creationDate %></wiki:Link></fmt:param>--%>
      <fmt:param>
        <wiki:Link version="1">
          <fmt:formatDate value="<%= firstPage.getLastModified() %>" pattern="${prefs['DateFormat']}" />
        </wiki:Link>
      </fmt:param>
      <fmt:param><%= creationAuthor %></fmt:param>
    </fmt:message>
    </p>
  </wiki:CheckVersion>

  <p>
  <wiki:Permission permission="rename">
    <form action="<wiki:Link format='url' jsp='Rename.jsp'/>"
           class="wikiform"
              id="renameform"
        onsubmit="return Wiki.submitOnce(this);"
          method="post" accept-charset="<wiki:ContentEncoding />" >

      <input type="hidden" name="page" value="<wiki:Variable var='pagename' />" />
      <input type="submit" name="rename" value="<fmt:message key='info.rename.submit' />" />
      <input type="text" name="renameto" value="<wiki:Variable var='pagename' />" size="40" />
      &nbsp;&nbsp;
      <input type="checkbox" name="references" checked="checked" />
      <fmt:message key="info.updatereferrers"/>

    </form>
  </wiki:Permission>
  <wiki:Permission permission="!rename">
      <fmt:message key="info.rename.permission"/>
  </wiki:Permission>
  </p>

  <p>
  <wiki:Permission permission="delete">
    <form action="<wiki:Link format='url' context='<%=WikiContext.DELETE%>' />"
           class="wikiform"
              id="deleteForm"
          method="post" accept-charset="<wiki:ContentEncoding />"
        onsubmit="return( confirm('<fmt:message key="info.confirmdelete"/>') && Wiki.submitOnce(this) );">

      <input type="submit" name="delete-all" id="delete-all" 
            value="<fmt:message key='info.delete.submit'/>" >

    </form>
  </wiki:Permission>
  <wiki:Permission permission="!delete">
      <fmt:message key="info.delete.permission"/>
  </wiki:Permission>
  </p>

  <br />

    <wiki:CheckVersion mode="first"><fmt:message key="info.noversions"/></wiki:CheckVersion>
    <wiki:CheckVersion mode="notfirst">
    <%-- if( itemcount > 1 ) { --%>

    <%= (pagination == null) ? "" : pagination %>

    <div class="zebra-table <wiki:CheckRequestContext context='info'>sortable table-filter</wiki:CheckRequestContext>">
    <table class="wikitable center" >
      <tr>
        <th><fmt:message key="info.version"/></th>
        <th><fmt:message key="info.date"/></th>
        <th><fmt:message key="info.size"/></th>
        <th><fmt:message key="info.author"/></th>
        <th><fmt:message key="info.changes"/></th>
      <th width="30%"><fmt:message key="info.changenote"/></th>
      </tr>

      <wiki:HistoryIterator id="currentPage">
      <% if( ( startitem == -1 ) ||
             (  ( currentPage.getVersion() >= startitem )
             && ( currentPage.getVersion() < startitem + pagesize ) ) )
         {
       %>
      <tr>
        <td>
          <wiki:LinkTo version="<%=Integer.toString(currentPage.getVersion())%>">
            <wiki:PageVersion/>
          </wiki:LinkTo>
        </td>

        <td><fmt:formatDate value="<%= currentPage.getLastModified() %>" pattern="${prefs['DateFormat']}" /></td>
        <td>
          <%--<fmt:formatNumber value='<%=Double.toString(currentPage.getSize()/1000.0)%>' groupingUsed='false' maxFractionDigits='1' minFractionDigits='1'/>&nbsp;Kb--%>
          <wiki:PageSize />
        </td>
        <td><wiki:Author /></td>

        <td>
          <wiki:CheckVersion mode="notfirst">
            <wiki:DiffLink version="current" newVersion="previous"><fmt:message key="info.difftoprev"/></wiki:DiffLink>
            <wiki:CheckVersion mode="notlatest"> | </wiki:CheckVersion>
          </wiki:CheckVersion>

          <wiki:CheckVersion mode="notlatest">
            <wiki:DiffLink version="latest" newVersion="current"><fmt:message key="info.difftolast"/></wiki:DiffLink>
          </wiki:CheckVersion>
        </td>

         <td class="changenote">
           <%
              String changeNote = (String)currentPage.getAttribute( WikiPage.CHANGENOTE );
              changeNote = (changeNote != null) ? TextUtil.replaceEntities( changeNote ) : "" ;
           %>
           <%= changeNote %>
         </td>

      </tr>
      <% } %>
      </wiki:HistoryIterator>

    </table>
    </div>
    <%= (pagination == null) ? "" : pagination %>

    <%-- } /* itemcount > 1 */ --%>
    </wiki:CheckVersion>
  </wiki:Tab>

  <wiki:Tab id="incomingLinks" title="<%=LocaleSupport.getLocalizedMessage(pageContext, "info.tab.links")%>">
	<table class="wikitable">
	<tr>
	<th><fmt:message key="info.tab.incoming" /></th>
	<th><fmt:message key="info.tab.outgoing" /></th>
	</tr>
	<tr>
	<td>
    <wiki:LinkTo><wiki:PageName /></wiki:LinkTo>
    <wiki:Plugin plugin="ReferringPagesPlugin" args="before='*' after='\n' " />
	</td>
	<td>
    <wiki:Plugin plugin="ReferredPagesPlugin" args="depth='1' type='local'" />
	</td>
	</tr>
	</table>
  </wiki:Tab>

  </wiki:TabbedSection> <%-- end of .tabs --%>


</wiki:PageType>



<%-- part 2 : attachments --%>
<wiki:PageType type="attachment">
<%
  int MAXATTACHNAMELENGTH = 30;
  String progressId = wikiContext.getEngine().getProgressManager().getNewProgressIdentifier();
%>

  <wiki:TabbedSection>
  <wiki:Tab id="info" title="<%=LocaleSupport.getLocalizedMessage(pageContext, "info.attachment.tab")%>" accesskey="i" >

  <h3><fmt:message key="info.uploadnew"/></h3>

  <wiki:Permission permission="upload">
  <form action="<wiki:Link jsp='attach' format='url' absolute='true'><wiki:Param name='progressid' value='<%=progressId%>'/></wiki:Link>" 
         class="wikiform"
            id="uploadform"
      onsubmit="return Wiki.submitUpload(this, '<%=progressId%>');"
        method="post" accept-charset="<wiki:ContentEncoding/>"
       enctype="multipart/form-data" >

  <%-- Do NOT change the order of wikiname and content, otherwise the
       servlet won't find its parts. --%>


  <input type="hidden" name="page" value="<wiki:Variable var='pagename' />" />
  <table>
  <tr>
    <td colspan="2"><div class="formhelp"><fmt:message key="info.uploadnew.help" /></div></td>
  </tr>
  <tr>
    <td><label for="content"><fmt:message key="info.uploadnew.filename" /></label></td>
    <td><input type="file" name="content" size="60"/></td>
  </tr>
  <tr>
    <td><label for="changenote"><fmt:message key="info.uploadnew.changenote" /></label></td>
    <td>
    <input type="text" name="changenote" maxlength="80" size="60" />
    </td>
  </tr>
  <tr>
    <td></td>
    <td>
    <input type="submit" name="upload" value="<fmt:message key='attach.add.submit'/>" id="upload" /> <input type="hidden" name="action"  value="upload" />
    <input type="hidden" name="nextpage" value="<wiki:PageInfoLink format='url'/>" />
        <div id="progressbar"><div class="ajaxprogress"></div></div>
    </td>
  </tr>
  </table>

  </form>
  </wiki:Permission>
  <wiki:Permission permission="!upload">
    <div class="formhelp"><fmt:message key="attach.add.permission"/></div>
  </wiki:Permission>

  <wiki:Permission permission="delete">

    <h3><fmt:message key="info.deleteattachment"/></h3>
    <form action="<wiki:Link format='url' context='<%=WikiContext.DELETE%>' />"
           class="wikiform"
              id="deleteForm"
          method="post" accept-charset="<wiki:ContentEncoding />"
        onsubmit="return( confirm('<fmt:message key="info.confirmdelete"/>') && Wiki.submitOnce(this) );" >

     <input type="submit" name="delete-all" id="delete-all" 
           value="<fmt:message key='info.deleteattachment.submit' />" />

    </form>

  </wiki:Permission>

  <p>
  <fmt:message key='info.backtoparentpage'>
    <fmt:param><a href="<wiki:LinkToParent format='url' />&tab=attachments"><wiki:ParentPageName /></a></fmt:param>
  </fmt:message>
  </p>


  <%-- FIXME why not add pagination here - no need for large amounts of attach versions on one page --%>
  <h3><fmt:message key='info.attachment.history' /></h3>

  <div class="zebra-table"><div class="slimbox-img sortable">
  <table class="wikitable">
    <tr>
      <th><fmt:message key="info.attachment.type"/></th>
      <%--<th><fmt:message key="info.attachment.name"/></th>--%>
      <th><fmt:message key="info.version"/></th>
      <th><fmt:message key="info.size"/></th>
      <th><fmt:message key="info.date"/></th>
      <th><fmt:message key="info.author"/></th>
      <%--
      <wiki:Permission permission="upload">
         <th><fmt:message key="info.actions"/></th>
      </wiki:Permission>
      --%>
      <th width="30%"><fmt:message key="info.changenote"/></th>
    </tr>

    <wiki:HistoryIterator id="att"><%-- <wiki:AttachmentsIterator id="att"> --%>
    <%
      String name = att.getName(); //att.getFileName();
      int dot = name.lastIndexOf(".");
      String attachtype = ( dot != -1 ) ? name.substring(dot+1) : "";

      String sname = name;
      if( sname.length() > MAXATTACHNAMELENGTH ) sname = sname.substring(0,MAXATTACHNAMELENGTH) + "...";
    %>

    <tr>
      <td><div id="attach-<%= attachtype %>" class="attachtype"><%= attachtype %></div></td>
      <%--<td><wiki:LinkTo title="<%= name %>" ><%= sname %></wiki:LinkTo></td>--%>
      <%--FIXME classs parameter throws java exception
      <td><wiki:Link version='<%=Integer.toString(att.getVersion())%>'
                       title="<%= name %>"
                       class="attachment" ><wiki:PageVersion /></wiki:Link></td>
      --%>
      <td><a href="<wiki:Link version='<%=Integer.toString(att.getVersion())%>' format='url' />"
                       title="<%= name %>"
                       class="attachment" ><wiki:PageVersion /></a></td>
      <td nowrap style="text-align:right;">
        <fmt:formatNumber value='<%=Double.toString(att.getSize()/1000.0) %>' groupingUsed='false' maxFractionDigits='1' minFractionDigits='1'/>&nbsp;<fmt:message key="info.kilobytes"/>
      </td>
	  <td nowrap><fmt:formatDate value="<%= att.getLastModified() %>" pattern="${prefs['DateFormat']}" /></td>
      <td><wiki:Author /></td>
      <%--
      // FIXME: This needs to be added, once we figure out what is going on.
      <wiki:Permission permission="upload">
         <td>
            <input type="button"
                   value="Restore"
                   url="<wiki:Link format='url' context='<%=WikiContext.UPLOAD%>'/>"/>
         </td>
      </wiki:Permission>
      --%>
      <td>
      <%
         String changeNote = (String)att.getAttribute(WikiPage.CHANGENOTE);
         if( changeNote != null ) {
         %><%=changeNote%><%
         }
      %>
      </td>
    </tr>
    </wiki:HistoryIterator><%-- </wiki:AttachmentsIterator> --%>

  </table>
  </div></div>
  </wiki:Tab>

  </wiki:TabbedSection> <%-- end of .tabs --%>

</wiki:PageType>

</wiki:PageExists>

<wiki:NoSuchPage>
  <fmt:message key="common.nopage">
    <fmt:param><wiki:EditLink><fmt:message key="common.createit"/></wiki:EditLink></fmt:param>
  </fmt:message>
</wiki:NoSuchPage>
