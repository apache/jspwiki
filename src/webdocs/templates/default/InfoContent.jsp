<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ page import="com.ecyrd.jspwiki.*" %>

<%
  /* see commonheader.jsp */
  String prefDateFormat = (String) session.getAttribute("prefDateFormat");
  String prefTimeZone   = (String) session.getAttribute("prefTimeZone");

  /* cookie settings */
  int size = 10; //default #revisions shown per page
  String s;
  s = (String)request.getParameter("size");  if( s != null ) size = Integer.parseInt(s);
  //String dateformat = "EEE, dd-MMM-yyyy [hh:mm]";


  WikiContext c = WikiContext.findContext( pageContext );
  
  int latestVersion = 0;
  try
  {
    latestVersion = c.getPage().getVersion();
  }
  catch( Exception  e )  { /* dont care */ }

  int start = latestVersion;
  s = (String)request.getParameter("start"); if( s != null ) start = Integer.parseInt(s);

%>

<script language="javascript" type="text/javascript">
function confirmDelete()
{
  var reallydelete = confirm("Please confirm that you want to delete content permanently!");

  return reallydelete;
}
</script>

<wiki:PageExists>

<wiki:TabbedSection defaultTab="<%=request.getParameter("tab")%>">
<%-- part 1 : normal wiki pages --%>

<wiki:PageType type="page">

<wiki:Tab id="infocontent" title="Page Info">

   <table cellspacing="4">
       <tr>
           <td><b>Page name</b></td>
           <td><wiki:LinkTo><wiki:PageName /></wiki:LinkTo></td>
       </tr>

       <wiki:PageType type="attachment">
           <tr>
              <td><b>Parent page</b></td>
              <td><wiki:LinkToParent><wiki:ParentPageName /></wiki:LinkToParent></td>
           </tr>
       </wiki:PageType>

       <tr>
           <td><b>Page last modified</b></td>
           <td><wiki:PageDate /></td>
       </tr>

       <tr>
           <td><b>Current page version</b></td>
           <td><wiki:PageVersion>No versions.</wiki:PageVersion></td>
       </tr>

       <tr>
           <td><b>Page feed</b></td>
           <td><a href="<wiki:Link format="url" jsp="rss.jsp">
                           <wiki:Param name="page" value="<%=c.getName()%>"/>
                           <wiki:Param name="mode" value="wiki"/>
                        </wiki:Link>"><img src="<wiki:Link jsp="images/xml.png" format="url"/>" border="0" alt="[RSS]"/></a></td>
       </tr>

      <wiki:Permission permission="rename">
           <tr>
               <td><b>Rename page</b></td>
               <td>
                    <form action="<wiki:Link format="url" jsp="Rename.jsp"/>"
                          method="post"  accept-charset="<wiki:ContentEncoding/>">
                        <input type="hidden" name="page" value="<wiki:Variable var="pagename" />"/>
                        <input type="text" name="renameto" value="<wiki:Variable var="pagename" />" size="40"/><br />
                        <input type="checkbox" name="references" checked="checked"/>Update referrers?<br />
                        <input type="submit" value="Rename"/>
                    </form>
               </td>
           </tr>
       </wiki:Permission>

   <wiki:Permission permission="delete">
       <tr>
         <th>Delete page</th>
         <td>
           <form name="deleteForm"
                 action="<wiki:Link format="url" context="<%=WikiContext.DELETE%>" />"
                 method="post"
                 accept-charset="<wiki:ContentEncoding />"
                 onsubmit="return confirmDelete()">
             <input type="submit" name="delete-all" value="Delete entire page"/>
           </form>
         </td>
       </tr>
   </wiki:Permission>

       <tr>
           <td valign="top"><b>Page revision history</b></td>
           <td>
                <wiki:CheckVersion mode="first">Only one version </wiki:CheckVersion>

      <wiki:CheckVersion mode="notfirst">
      <form><!--FIXME: WHat is this-->
        <select id="infoselect" name="infoselect"
                onchange="location.replace(this[this.selectedIndex].value)" >

          <%
             int startofblock = latestVersion-size+1;
             int endofblock = latestVersion;
             boolean selected = false;

             if( latestVersion > size ) // more than one item in dropdown list
             {
          %>
               <option value="<wiki:PageInfoLink format="url" />&amp;start=-1"
                      <%= ( (start == -1) ? "selected='selected'" : "") %> >
                  Show all revisions from <%= latestVersion %> down to 1
               </option>
          <%
             }

             while( endofblock > 0 )
             {
               if( startofblock < 1 ) { startofblock = 1;  }
               selected = ( (start >= startofblock) && (start <= endofblock) );
               if( selected ) start = startofblock; //defensive
          %>
               <option value="<wiki:PageInfoLink format="url" />&amp;start=<%=startofblock %>"
                       <%= (selected ? "selected='selected'" : "") %> >
                   Show <%= ((startofblock == 1) ? "first" : Integer.toString(size) ) %> revisions
                   from <%=endofblock%> to <%=startofblock %>
               </option>
          <%
               //if( startofblock == 1) startofblock = 0;
               startofblock -= size;
               endofblock   -= size;
             }
          %>

        </select>
      </form>
      </wiki:CheckVersion>
     </td>
   </tr>
</table>

  <wiki:CheckVersion mode="notfirst">

  <div id="versionhistory" class="zebra-table">

  <table class="wikitable">

    <tr>
      <th>Version</th>
      <th>Date</th>
      <th>Author</th>
      <th>Size</th>
      <th>Changes ...</th>
    </tr>

    <wiki:HistoryIterator id="currentPage">
    <% if( (start == -1)  ||
           ((currentPage.getVersion() >= start) && (currentPage.getVersion() < start+size)) )
       {
     %>
    <tr>
      <td>
        <wiki:LinkTo version="<%=Integer.toString(currentPage.getVersion())%>">
          <wiki:PageVersion/>
        </wiki:LinkTo>
      </td>

      <td><wiki:PageDate format="<%= prefDateFormat %>" /></td>
      <td><wiki:Author /></td>
      <td><wiki:PageSize /></td>

      <td>
        <wiki:CheckVersion mode="notfirst">

          <wiki:DiffLink version="current" newVersion="previous">to previous</wiki:DiffLink>

          <wiki:CheckVersion mode="notlatest"> | </wiki:CheckVersion>

        </wiki:CheckVersion>

        <wiki:CheckVersion mode="notlatest">
          <wiki:DiffLink version="latest" newVersion="current">to last</wiki:DiffLink>
        </wiki:CheckVersion>

      </td>
    </tr>
    <%
       String changeNote = (String)currentPage.getAttribute(WikiPage.CHANGENOTE);
       if( changeNote != null )
       { %>
       <tr><td>&nbsp;</td><td colspan="4" class="changenote"><%=changeNote%></td></tr>
       <% } %>
    <% } %>
    </wiki:HistoryIterator>
  </table>
  </div> <%-- versionhistory --%>
  </wiki:CheckVersion>
</wiki:Tab>

<wiki:Tab id="referingto" title="Outgoing Links">
  <p><wiki:Plugin plugin="ReferredPagesPlugin" args="depth=1" >body</wiki:Plugin></p>
</wiki:Tab>

<wiki:Tab id="referencedby" title="Incoming Links">
  <p><wiki:LinkTo><wiki:PageName /></wiki:LinkTo></p>
  <wiki:Plugin plugin="ReferringPagesPlugin" args="before='*' after='\n' " >body</wiki:Plugin>
</wiki:Tab>

<wiki:Tab id="attachments" title="Attachments">
   <wiki:Include page="AttachmentTab.jsp"/>
</wiki:Tab>

</wiki:PageType>

<%-- part 2 : attachments --%>

<wiki:PageType type="attachment">
<wiki:Tab id="infocontent" title="Attachment Info">
  <table>
    <tr>
    <th>Attachment Name</th>
    <td>
      <wiki:LinkToParent><wiki:ParentPageName /></wiki:LinkToParent> / <wiki:LinkTo><wiki:PageName /></wiki:LinkTo>
    </td>
    </tr>

    <tr>
    <th>Last modified</th>
    <td><wiki:PageDate format="<%= prefDateFormat %>" /> by <wiki:Author /></td>
    </tr>

    <tr>
    <th>Current version</th>
    <td><wiki:PageVersion>No versions.</wiki:PageVersion></td>
    </tr>

    <tr>
    <th>Page feed</th>
    <td><a href="<wiki:Link format='url' jsp='rss.jsp'><wiki:Param name='page' value='<%=c.getName()%>'/><wiki:Param name='mode' value='wiki'/></wiki:Link>"
           title="RSS link for <wiki:PageName />" >
        <img src="<wiki:Link format='url' jsp='images/xml.png'/>" border="0" alt="[RSS]"  />
        </a>
    </td>
    </tr>
    
    <wiki:Permission permission="upload">
    <tr>
    <th>Upload new version</th>
    <td>
    <form action="<wiki:Link context='att' format='url' absolute='true'/>"
          method="post" enctype="multipart/form-data">

    <%-- Do NOT change the order of wikiname and content, otherwise the
        servlet won't find its parts. --%>

    <input type="hidden" name="page" value="<wiki:Variable var='pagename' />" />
    <%--
    In order to update this attachment with a newer version,
    please select a file name (click "Choose" button), then click on "Update".
    --%>
    <input type="file" name="content" />
    <br />
    Change note: <input type="text" name="changenote" maxlength="80" width="60" />
    <br />
    <input type="submit" name="upload" value="Upload new attachment" />
    <input type="hidden" name="action" value="upload" />
    <input type="hidden" name="nextpage" value="<wiki:PageInfoLink format='url'/>" />
    </form>
    </td>
    </tr>
   </wiki:Permission>

   <wiki:Permission permission="delete">
   <tr>
     <th>Delete attachment</th>
     <td>
         <form name="deleteForm"
               action="<wiki:Link format='url' context='<%=WikiContext.DELETE%>' />"
               method="post"
               accept-charset="<wiki:ContentEncoding />" onsubmit="return confirmDelete()">
           <input type="submit" name="delete-all" value="Delete attachment"/>
         </form>
     </td>
   </tr>
   </wiki:Permission>

   <tr>
    <th>Page Revision History</th>
    <td>
      <wiki:CheckVersion mode="first">Only one version </wiki:CheckVersion>

      <wiki:CheckVersion mode="notfirst">
      <form action="">
        <select id="infoselect" name="infoselect"
                onchange="location.replace(this[this.selectedIndex].value)" >

          <%
             int startofblock = latestVersion-size+1;
             int endofblock = latestVersion;
             boolean selected = false;

             if( latestVersion > size ) // more than one item in dropdown list
             {
          %>
               <option value="<wiki:Link context='info' format='url'><wiki:Param name='start' value='-1'/></wiki:Link>"
                      <%= ( (start == -1) ? "selected='selected'" : "") %> >
                  Show all revisions from <%= latestVersion %> down to 1
               </option>
          <%
             }

             while( endofblock > 0 )
             {
               if( startofblock < 1 ) { startofblock = 1;  }
               selected = ( (start >= startofblock) && (start <= endofblock) );
               if( selected ) start = startofblock; //defensive
          %>
               <option value="<wiki:Link context='info' format='url'><wiki:Param name='start' value='<%=Integer.toString(startofblock)%>'/></wiki:Link>" 
                       <%= (selected ? "selected='selected'" : "") %> >
                   Show <%= ((startofblock == 1) ? "first" : Integer.toString(size) ) %> revisions
                   from <%=endofblock%> to <%=startofblock %>
               </option>
          <%
               //if( startofblock == 1) startofblock = 0;
               startofblock -= size;
               endofblock   -= size;
             }
          %>

        </select>
      </form>
      </wiki:CheckVersion>
    </td>
    </tr>
    
  </table>

  <wiki:CheckVersion mode="notfirst">

  <div id="versionhistory" class="zebra-table">

  <table class="wikitable">

    <tr>
      <th>Version</th>
      <th>Date</th>
      <th>Author</th>
      <th>Size</th>
    </tr>

    <wiki:HistoryIterator id="currentPage">
    <% if( (start == -1)  ||
           ((currentPage.getVersion() >= start) && (currentPage.getVersion() < start+size)) )
       {
     %>
    <tr>
      <td>
        <wiki:LinkTo version="<%=Integer.toString(currentPage.getVersion())%>">
          <wiki:PageVersion/>
        </wiki:LinkTo>
      </td>

      <td><wiki:PageDate format="<%= prefDateFormat %>" /></td>
      <td><wiki:Author /></td>
      <td><wiki:PageSize /></td>
    </tr>
      <%
        String changeNote = (String)currentPage.getAttribute(WikiPage.CHANGENOTE);
        if( changeNote != null )
        { %>
          <tr><td>&nbsp;</td><td colspan="4" class="changenote"><%=changeNote%></td></tr>
     <% } %>
    <% } %>
    </wiki:HistoryIterator>
  </table>
  </div> <%-- versionhistory --%>
  </wiki:CheckVersion>
  <div style="clear:both; height:0px;" > </div>

</wiki:Tab>
</wiki:PageType>

</wiki:TabbedSection>

</wiki:PageExists>


<wiki:NoSuchPage>
    This page does not exist.  Why don't you go and
    <wiki:EditLink>create it</wiki:EditLink>?
</wiki:NoSuchPage>

