<%--
    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.
--%>

<%@ taglib uri="http://jspwiki.apache.org/tags" prefix="wiki" %>
<%@ page import="org.apache.wiki.*" %>
<%@ page import="org.apache.wiki.auth.*" %>
<%@ page import="org.apache.wiki.auth.permissions.*" %>
<%@ page import="org.apache.wiki.attachment.*" %>
<%@ page import="org.apache.wiki.i18n.InternationalizationManager" %>
<%@ page import="org.apache.wiki.preferences.Preferences" %>
<%@ page import="org.apache.wiki.util.TextUtil" %>
<%@ page import="java.security.Permission" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<fmt:setLocale value="${prefs.Language}" />
<fmt:setBundle basename="templates.default"/>
<%
  WikiContext c = WikiContext.findContext(pageContext);
  WikiPage wikiPage = c.getPage();
  int attCount = c.getEngine().getAttachmentManager().listAttachments( c.getPage() ).size();
  String attTitle = LocaleSupport.getLocalizedMessage(pageContext, "attach.tab");
  if( attCount != 0 ) attTitle += " (" + attCount + ")";

  String parm_renameto = (String)request.getParameter( "renameto" );
  if( parm_renameto == null ) parm_renameto = wikiPage.getName();

  String creationAuthor ="";

  //FIXME -- seems not to work correctly for attachments !!
  WikiPage firstPage = c.getEngine().getPage( wikiPage.getName(), 1 );
  if( firstPage != null )
  {
    creationAuthor = firstPage.getAuthor();

    if( creationAuthor != null && creationAuthor.length() > 0 )
    {
      creationAuthor = TextUtil.replaceEntities(creationAuthor);
    }
    else
    {
      creationAuthor = Preferences.getBundle( c, InternationalizationManager.CORE_BUNDLE ).getString( "common.unknownauthor" );
    }
  }

  int itemcount = 0;  //number of page versions
  try
  {
    itemcount = wikiPage.getVersion(); /* highest version */
  }
  catch( Exception  e )  { /* dont care */ }

  int pagesize = 20;
  int startitem = itemcount-1; /* itemcount==1-20 -> startitem=0-19 ... */

  String parm_start = (String)request.getParameter( "start" );
  if( parm_start != null ) startitem = Integer.parseInt( parm_start ) ;

  /* round to start of block: 0-19 becomes 0; 20-39 becomes 20 ... */
  if( startitem > -1 ) startitem = ((startitem)/pagesize) * pagesize;

  /* startitem drives the pagination logic */
  /* startitem=-1:show all; startitem=0:show block 1-20; startitem=20:block 21-40 ... */
%>

<div class="page-content">

<wiki:PageExists>


<wiki:PageType type="page">
<%-- part 1 : normal wiki pages 

  <wiki:TabbedSection defaultTab="info">

  <wiki:Tab id="pagecontent"
         title='<%=LocaleSupport.getLocalizedMessage(pageContext, "actions.view")%>'
     accesskey="v"
	       url="<%=c.getURL(WikiContext.VIEW, c.getPage().getName())%>">
      <%--<wiki:Include page="PageTab.jsp"/> -->
  </wiki:Tab>
--%>

  <div class="form-frame">
  <p>
  <fmt:message key='info.lastmodified'>
    <fmt:param><wiki:PageVersion >1</wiki:PageVersion></fmt:param>
    <fmt:param>
      <a href="<wiki:DiffLink format='url' version='latest' newVersion='previous' />"
        title="<fmt:message key='info.pagediff.title' />" >
        <fmt:formatDate value="<%= wikiPage.getLastModified() %>" pattern="${prefs.DateFormat}" timeZone="${prefs.TimeZone}" />
      </a>
    </fmt:param>
    <fmt:param><wiki:Author /></fmt:param>
  </fmt:message>
  <wiki:RSSImageLink mode="wiki"/>
  </p>

  <wiki:CheckVersion mode="notfirst">
  <p>
    <fmt:message key='info.createdon'>
      <fmt:param>
        <wiki:Link version="1">
          <fmt:formatDate value="<%= firstPage.getLastModified() %>" pattern="${prefs.DateFormat}" timeZone="${prefs.TimeZone}" />
        </wiki:Link>
      </fmt:param>
      <fmt:param><%= creationAuthor %></fmt:param>
    </fmt:message>
  </p>
  </wiki:CheckVersion>

  <wiki:Permission permission="rename">

    <wiki:Messages div="alert alert-danger" topic="rename" prefix='<%=LocaleSupport.getLocalizedMessage(pageContext,"prefs.errorprefix.rename")%>'/>

    <form action="<wiki:Link format='url' jsp='Rename.jsp'/>"
           class="form-group"
              id="renameform"
          method="post" accept-charset="<wiki:ContentEncoding />" >
      
      <input type="hidden" name="page" value="<wiki:Variable var='pagename' />" />
      <input class="btn btn-primary" type="submit" name="rename" value="<fmt:message key='info.rename.submit' />" />
      <input class="form-control form-col-50" type="text" name="renameto" value="<%= parm_renameto %>" size="40" />
      <label class="btn btn-default" for="references">
        <input class="checkbox-inline" type="checkbox" name="references" id="references" checked="checked" />
        <fmt:message key="info.updatereferrers"/>
      </label>
    </form>
  </wiki:Permission>
  <wiki:Permission permission="!rename">
    <p class="text-warning"><fmt:message key="info.rename.permission"/></p>
  </wiki:Permission>

  <wiki:Permission permission="delete">
    <form action="<wiki:Link format='url' context='<%=WikiContext.DELETE%>' />"
           class="form-group"
              id="deleteForm"
          method="post" accept-charset="<wiki:ContentEncoding />"
        onsubmit="return( confirm('<fmt:message key="info.confirmdelete"/>') && Wiki.submitOnce(this) );">
      <input class="btn btn-danger" type="submit" name="delete-all" id="delete-all"
            value="<fmt:message key='info.delete.submit'/>" />
    </form>
  </wiki:Permission>
  <wiki:Permission permission="!delete">
    <p class="text-warning"><fmt:message key="info.delete.permission"/></p>
  </wiki:Permission>

  </div>


  <div class="tabbedAccordion">
    <div class="tab-History">
	<%--
    <wiki:CheckVersion mode="first"><fmt:message key="info.noversions"/></wiki:CheckVersion>
	--%>
    <%-- if( itemcount > 1 ) { --%>

    <wiki:SetPagination start="<%=startitem%>" total="<%=itemcount%>" pagesize="<%=pagesize%>" maxlinks="9"
                       fmtkey="info.pagination"
                         href='<%=c.getURL(WikiContext.INFO, c.getPage().getName(), "start=%s")%>' />

    <div class="zebra-table sortable table-filter">
    <table class="wikitable" >
      <tr>
        <th><fmt:message key="info.version"/></th>
        <th><fmt:message key="info.date"/></th>
        <th><fmt:message key="info.size"/></th>
        <th><fmt:message key="info.author"/></th>
        <th><fmt:message key="info.changes"/></th>
        <th><fmt:message key="info.changenote"/></th>
      </tr>

      <wiki:HistoryIterator id="currentPage">
      <% if( ( startitem == -1 ) ||
             (  ( currentPage.getVersion() > startitem )
             && ( currentPage.getVersion() <= startitem + pagesize ) ) )
         {
       %>
      <tr>
        <td>
          <wiki:Link version="<%=Integer.toString(currentPage.getVersion())%>">
            <wiki:PageVersion/>
          </wiki:Link>
        </td>

	    <td class="nowrap" jspwiki:sortvalue="<%= currentPage.getLastModified().getTime() %>">
        <fmt:formatDate value="<%= currentPage.getLastModified() %>" pattern="${prefs.DateFormat}" timeZone="${prefs.TimeZone}" />
        </td>
        <td class="nowrap text-right">
          <c:set var="ff"><wiki:PageSize /></c:set>
          <fmt:formatNumber value='${ff/1000}' maxFractionDigits='3' minFractionDigits='1'/>&nbsp;<fmt:message key="info.kilobytes"/>
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
           <% String changenote = (String) currentPage.getAttribute( WikiPage.CHANGENOTE );  %>
		   <%= (changenote==null) ? "" : changenote  %>
         </td>

      </tr>
      <% } %>
      </wiki:HistoryIterator>

    </table>
    </div>
    ${pagination}
    <%-- } /* itemcount > 1 */ --%>

    </div>
    <div class="tab-PageReferences">
      <table class="wikitable table-condensed">
      <tr>
      <th><fmt:message key="info.tab.incoming" /></th>
      <th><fmt:message key="info.tab.outgoing" /></th>
      </tr>
      <tr>
      <td>
      <wiki:Link><wiki:PageName /></wiki:Link>
      <wiki:Plugin plugin="ReferringPagesPlugin" args="before='*' after='\n' " />
      </td>
      <td>
      <wiki:Plugin plugin="ReferredPagesPlugin" args="depth='1' type='local'" />
      </td>
      </tr>
      </table>
    </div>

    <%-- DIFF section --%>
    <wiki:CheckRequestContext context='diff'>
      <div class="tab-Difference">
      <wiki:Include page="DiffTab.jsp"/>
      </div>
    </wiki:CheckRequestContext>
    
  </div>


</wiki:PageType>


<%-- part 2 : attachments --%>
<wiki:PageType type="attachment">
<%
  int MAXATTACHNAMELENGTH = 30;
  String progressId = c.getEngine().getProgressManager().getNewProgressIdentifier();
%>

<wiki:Permission permission="upload">

  <form action="<wiki:Link jsp='attach' format='url' absolute='true'><wiki:Param name='progressid' value='<%=progressId%>'/></wiki:Link>"
         class="accordion-close"
            id="uploadform"
        method="post" accept-charset="<wiki:ContentEncoding/>"
       enctype="multipart/form-data" >

  <%-- Do NOT change the order of wikiname and content, otherwise the
       servlet won't find its parts. --%>

  <h4><fmt:message key="info.uploadnew"/></h4>

    <div class="form-group">
      <label class="control-label form-col-20" for="files"><fmt:message key="attach.add.selectfile"/></label>
      <ul class="list-group form-col-50">
        <li class="list-group-item droppable">
          <label>Select files <%--or drop them here!--%></label>
          <input type="file" name="files" id="files" size="60"/>
          <a class="hidden delete btn btn-danger btn-xs pull-right">Delete</a>
        </li> 
      </ul>
    </div>
    <div class="form-group">
      <label class="control-label form-col-20" for="changenote"><fmt:message key="attach.add.changenote"/></label>
      <input class="form-control form-col-50" type="text" name="changenote" id="changenote" maxlength="80" size="60" />
    </div>
    <div class="form-group">
    <input type="hidden" name="nextpage" value="<wiki:Link context='info' format='url'/>" /><%-- *** --%>
    <input type="hidden" name="page" value="<wiki:Variable var="pagename"/>" />
    <input class="btn btn-primary form-col-offset-20 form-col-50" 
           type="submit" name="upload" id="upload" disabled="disabled" value="<fmt:message key='attach.add.submit'/>" />
    <%--<input type="hidden" name="action" value="upload" />--%>
    </div>
    <div class="hidden form-col-offset-20 form-col-80 progress progress-striped active">
      <div class="progress-bar" data-progressid="<%=progressId%>" style="width: 100%;"></div>
    </div>

  </form>
</wiki:Permission>
<wiki:Permission permission="!upload">
  <div class="block-help bg-warning"><fmt:message key="attach.add.permission"/></div>
</wiki:Permission>

<wiki:Permission permission="delete">
  <%--<h4><fmt:message key="info.deleteattachment"/></h4>--%>
    <form action="<wiki:Link format='url' context='<%=WikiContext.DELETE%>' />"
           class="form-group"
              id="deleteForm"
          method="post" accept-charset="<wiki:ContentEncoding />"
        onsubmit="return( confirm('<fmt:message key="info.confirmdelete"/>') );" >
     
     <input class="btn btn-danger" type="submit" name="delete-all" id="delete-all"
           value="<fmt:message key='info.deleteattachment.submit' />" />
    </form>
</wiki:Permission>

<%-- FIXME why not add pagination here - number of attach versions of one page limited ?--%>
<%--<h4><fmt:message key='info.attachment.history' /></h4>--%>

  <div class="slimbox-attachments sortable table-filter-hover-sort table-filter">
  <table class="table">
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
      <th><fmt:message key="info.changenote"/></th>
    </tr>

    <wiki:HistoryIterator id="att"><%-- <wiki:AttachmentsIterator id="att"> --%>
    <%
      String name = att.getName(); //att.getFileName();
      int dot = name.lastIndexOf(".");
      String attachtype = ( dot != -1 ) ? name.substring(dot+1) : "&nbsp;";

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
      <td class="center"><a href="<wiki:Link version='<%=Integer.toString(att.getVersion())%>' format='url' />"
                       title="<%= name %>"
                       class="attachment" ><wiki:PageVersion /></a></td>
      <td class="nowrap text-right">
        <fmt:formatNumber value='<%=Double.toString(att.getSize()/1000.0) %>' groupingUsed='false' maxFractionDigits='1' minFractionDigits='1'/>&nbsp;<fmt:message key="info.kilobytes"/>
      </td>
	  <td class="nowrap" jspwiki:sortvalue="<%= att.getLastModified().getTime() %>">
	  <fmt:formatDate value="<%= att.getLastModified() %>" pattern="${prefs.DateFormat}" timeZone="${prefs.TimeZone}" />
	  </td>
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
      <td class='changenote'>
        <% String changenote = (String) att.getAttribute( WikiPage.CHANGENOTE ); %>
		<%= (changenote==null) ? "" : changenote  %>
      </td>
    </tr>
    </wiki:HistoryIterator><%-- </wiki:AttachmentsIterator> --%>

  </table>
  </div>

</wiki:PageType>

</wiki:PageExists> 

<wiki:NoSuchPage>
  <fmt:message key="common.nopage">
    <fmt:param><wiki:EditLink><fmt:message key="common.createit"/></wiki:EditLink></fmt:param>
  </fmt:message>
</wiki:NoSuchPage>

</div>
