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

  String tabParam = (String)request.getParameter( "tab" );
  if ( tabParam == null ) tabParam = "info";

  String creationAuthor ="";

  //FIXME -- seems not to work correctly for attachments !!
  WikiPage firstPage = c.getEngine().getPageManager().getPage( wikiPage.getName(), 1 );
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
<wiki:PageExists>

<%-- part 1 : normal wiki pages --%>
<wiki:PageType type="page">

  <wiki:TabbedSection defaultTab="<%=tabParam%>">

  <wiki:Tab id="pagecontent"
         title='<%=LocaleSupport.getLocalizedMessage(pageContext, "actions.view")%>'
     accesskey="v"
	       url="<%=c.getURL(WikiContext.VIEW, c.getPage().getName())%>">
      <%--<wiki:Include page="PageTab.jsp"/> --%>
  </wiki:Tab>

  <wiki:Tab id="attach" title="<%= attTitle %>" accesskey="a">
    <wiki:Include page="AttachmentTab.jsp"/>
  </wiki:Tab>

  <%-- actual infopage content --%>
  <wiki:Tab id="info" title='<%=LocaleSupport.getLocalizedMessage(pageContext, "info.tab")%>' accesskey="i" >
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

    <div class="formhelp">
      <wiki:Messages div="error" topic="rename" prefix='<%=LocaleSupport.getLocalizedMessage(pageContext,"prefs.errorprefix.rename")%>'/>
    </div>

    <form action="<wiki:Link format='url' jsp='Rename.jsp'/>"
           class="wikiform"
              id="renameform"
        onsubmit="return Wiki.submitOnce(this);"
          method="post" accept-charset="<wiki:ContentEncoding />" >
      <p>
      <input type="hidden" name="page" value="<wiki:Variable var='pagename' />" />
      <input type="submit" name="rename" value="<fmt:message key='info.rename.submit' />" />
      <input type="text" name="renameto"
             value="<c:out value='${param.renameto}' default='<%= wikiPage.getName() %>'/>" size="40" />
      &nbsp;&nbsp;
      <input type="checkbox" name="references" checked="checked" />
      <fmt:message key="info.updatereferrers"/>
      </p>
    </form>
  </wiki:Permission>
  <wiki:Permission permission="!rename">
      <p><fmt:message key="info.rename.permission"/></p>
  </wiki:Permission>

  <wiki:Permission permission="delete">
    <form action="<wiki:Link format='url' context='<%=WikiContext.DELETE%>' />"
           class="wikiform"
              id="deleteForm"
          method="post" accept-charset="<wiki:ContentEncoding />"
        onsubmit="return( confirm('<fmt:message key="info.confirmdelete"/>') && Wiki.submitOnce(this) );">
      <p>
      <input type="submit" name="delete-all" id="delete-all"
            value="<fmt:message key='info.delete.submit'/>" />
      </p>
    </form>
  </wiki:Permission>
  <wiki:Permission permission="!delete">
      <p><fmt:message key="info.delete.permission"/></p>
  </wiki:Permission>

  <div class="collapsebox-closed" id="incomingLinks">
  <h4><fmt:message key="info.tab.incoming" /></h4>
    <wiki:LinkTo><wiki:PageName /></wiki:LinkTo>
    <wiki:Plugin plugin="ReferringPagesPlugin" args="before='*' after='\n' " />
  </div>

  <div class="collapsebox-closed" id="outgoingLinks">
  <h4><fmt:message key="info.tab.outgoing" /></h4>
    <wiki:Plugin plugin="ReferredPagesPlugin" args="depth='1' type='local'" />
  </div>

  <div class="clearbox"></div>

  <%-- DIFF section --%>
  <wiki:CheckRequestContext context='diff'>
     <wiki:Include page="DiffTab.jsp"/>
  </wiki:CheckRequestContext>
  <%-- DIFF section --%>

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
        <th scope="col"><fmt:message key="info.version"/></th>
        <th scope="col"><fmt:message key="info.date"/></th>
        <th scope="col"><fmt:message key="info.size"/></th>
        <th scope="col"><fmt:message key="info.author"/></th>
        <th scope="col"><fmt:message key="info.changes"/></th>
        <th class="changenote" scope="col"><fmt:message key="info.changenote"/></th>
      </tr>

      <wiki:HistoryIterator id="currentPage">
      <% if( ( startitem == -1 ) ||
             (  ( currentPage.getVersion() > startitem )
             && ( currentPage.getVersion() <= startitem + pagesize ) ) )
         {
       %>
      <tr>
        <td>
          <wiki:LinkTo version="<%=Integer.toString(currentPage.getVersion())%>">
            <wiki:PageVersion/>
          </wiki:LinkTo>
        </td>

	    <td style="white-space:nowrap;" jspwiki:sortvalue="<%= currentPage.getLastModified().getTime() %>">
        <fmt:formatDate value="<%= currentPage.getLastModified() %>" pattern="${prefs.DateFormat}" timeZone="${prefs.TimeZone}" />
        </td>
        <td style="white-space:nowrap;text-align:right;">
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
  </wiki:Tab>

  </wiki:TabbedSection>

</wiki:PageType>


<%-- part 2 : attachments --%>
<wiki:PageType type="attachment">
<%
  int MAXATTACHNAMELENGTH = 30;
  String progressId = c.getEngine().getProgressManager().getNewProgressIdentifier();
%>

  <wiki:TabbedSection defaultTab="<%=tabParam%>">
  <wiki:Tab id="pagecontent"
         title='<%=LocaleSupport.getLocalizedMessage(pageContext, "info.parent")%>'
     accesskey="v"
	       url="<%=c.getURL(WikiContext.VIEW, ((Attachment)wikiPage).getParentName()) %>">
  </wiki:Tab>

  <wiki:Tab id="info" title='<%=LocaleSupport.getLocalizedMessage(pageContext, "info.attachment.tab")%>' accesskey="i" >

  <h3><fmt:message key="info.uploadnew"/></h3>

  <wiki:Permission permission="upload">
  <form action="<wiki:Link jsp='attach' format='url'><wiki:Param name='progressid' value='<%=progressId%>'/></wiki:Link>"
         class="wikiform"
            id="uploadform"
      onsubmit="return Wiki.submitUpload(this, '<%=progressId%>');"
        method="post" accept-charset="<wiki:ContentEncoding/>"
       enctype="multipart/form-data" >

  <%-- Do NOT change the order of wikiname and content, otherwise the
       servlet won't find its parts. --%>

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
    <input type="hidden" name="page" value="<wiki:Variable var='pagename' />" />
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
     <div>
     <input type="submit" name="delete-all" id="delete-all"
           value="<fmt:message key='info.deleteattachment.submit' />" />
     </div>
    </form>
  </wiki:Permission>

  <%-- FIXME why not add pagination here - no need for large amounts of attach versions on one page --%>
  <h3><fmt:message key='info.attachment.history' /></h3>

  <div class="zebra-table"><div class="slimbox-img sortable">
  <table class="wikitable">
    <tr>
      <th scope="col"><fmt:message key="info.attachment.type"/></th>
      <%--<th scope="col"><fmt:message key="info.attachment.name"/></th>--%>
      <th scope="col"><fmt:message key="info.version"/></th>
      <th scope="col"><fmt:message key="info.size"/></th>
      <th scope="col"><fmt:message key="info.date"/></th>
      <th scope="col"><fmt:message key="info.author"/></th>
      <%--
      <wiki:Permission permission="upload">
         <th scope="col"><fmt:message key="info.actions"/></th>
      </wiki:Permission>
      --%>
      <th  class="changenote" scope="col"><fmt:message key="info.changenote"/></th>
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
      <td><a href="<wiki:Link version='<%=Integer.toString(att.getVersion())%>' format='url' />"
                       title="<%= name %>"
                       class="attachment" ><wiki:PageVersion /></a></td>
      <td style="white-space:nowrap;text-align:right;">
        <fmt:formatNumber value='<%=Double.toString(att.getSize()/1000.0) %>' groupingUsed='false' maxFractionDigits='1' minFractionDigits='1'/>&nbsp;<fmt:message key="info.kilobytes"/>
      </td>
	  <td style="white-space:nowrap;" jspwiki:sortvalue="<%= att.getLastModified().getTime() %>">
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
