<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<fmt:setBundle basename="templates.DefaultResources"/>
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
  var reallydelete = confirm('<fmt:message key="info.confirmdelete"/>');

  return reallydelete;
}
</script>

<wiki:PageExists>

<wiki:TabbedSection defaultTab='<%=request.getParameter("tab")%>'>
<%-- part 1 : normal wiki pages --%>

<wiki:PageType type="page">

<wiki:Tab id="infocontent" title="<%=LocaleSupport.getLocalizedMessage(pageContext, "info.tab.info")%>">

   <table cellspacing="4">
       <tr>
           <td><b><fmt:message key="info.pagename"/></b></td>
           <td><wiki:LinkTo><wiki:PageName /></wiki:LinkTo></td>
       </tr>

       <wiki:PageType type="attachment">
           <tr>
              <td><b><fmt:message key="info.parent"/></b></td>
              <td><wiki:LinkToParent><wiki:ParentPageName /></wiki:LinkToParent></td>
           </tr>
       </wiki:PageType>

       <tr>
           <td><b><fmt:message key="info.lastmodified"/></b></td>
           <td><wiki:PageDate /></td>
       </tr>

       <tr>
           <td><b><fmt:message key="info.current"/></b></td>
           <td><wiki:PageVersion><fmt:message key="info.noversions"/></wiki:PageVersion></td>
       </tr>

       <tr>
           <td><b><fmt:message key="info.feed"/></b></td>
           <td><a href="<wiki:Link format="url" jsp="rss.jsp">
                           <wiki:Param name="page" value="<%=c.getName()%>"/>
                           <wiki:Param name="mode" value="wiki"/>
                        </wiki:Link>"><img src="<wiki:Link jsp="images/xml.png" format="url"/>" border="0" alt="[RSS]"/></a></td>
       </tr>

      <wiki:Permission permission="rename">
           <tr>
               <td><b><fmt:message key="info.rename"/></b></td>
               <td>
                    <form action="<wiki:Link format="url" jsp="Rename.jsp"/>"
                          method="post"  accept-charset="<wiki:ContentEncoding/>">
                        <input type="hidden" name="page" value="<wiki:Variable var="pagename" />"/>
                        <input type="text" name="renameto" value="<wiki:Variable var="pagename" />" size="40"/><br />
                        <input type="checkbox" name="references" checked="checked"/><fmt:message key="info.updatereferrers"/><br />
                        <input type="submit" value="Rename"/>
                    </form>
               </td>
           </tr>
       </wiki:Permission>

   <wiki:Permission permission="delete">
       <tr>
         <th><fmt:message key="info.delete"/></th>
         <td>
           <form name="deleteForm"
                 action="<wiki:Link format="url" context="<%=WikiContext.DELETE%>" />"
                 method="post"
                 accept-charset="<wiki:ContentEncoding />"
                 onsubmit="return confirmDelete()">
             <input type="submit" name="delete-all" value="<fmt:message key="info.delete.submit"/>"/>
           </form>
         </td>
       </tr>
   </wiki:Permission>

       <tr>
           <td valign="top"><b><fmt:message key="info.history"/></b></td>
           <td>
                <wiki:CheckVersion mode="first"><fmt:message key="info.noversions"/></wiki:CheckVersion>

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
                  <fmt:message key="info.showrevisions">
                    <fmt:param><%= latestVersion %></fmt:param>
                    <fmt:param>1</fmt:param>
                  </fmt:message>
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
                   <fmt:message key="info.showfrom">
                     <fmt:param><%= ((startofblock == 1) ? "first" : Integer.toString(size) ) %></fmt:param>
                     <fmt:param><%=endofblock%></fmt:param>
                     <fmt:param><%=startofblock %></fmt:param>
                   </fmt:message>
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
      <th><fmt:message key="info.version"/></th>
      <th><fmt:message key="info.date"/></th>
      <th><fmt:message key="info.author"/></th>
      <th><fmt:message key="info.size"/></th>
      <th><fmt:message key="info.changes"/></th>
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

          <wiki:DiffLink version="current" newVersion="previous"><fmt:message key="info.difftoprev"/></wiki:DiffLink>

          <wiki:CheckVersion mode="notlatest"> | </wiki:CheckVersion>

        </wiki:CheckVersion>

        <wiki:CheckVersion mode="notlatest">
          <wiki:DiffLink version="latest" newVersion="current"><fmt:message key="info.difftolast"/></wiki:DiffLink>
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

<wiki:Tab id="referingto" title="<%=LocaleSupport.getLocalizedMessage(pageContext, "info.tab.outgoing")%>">
  <p><wiki:Plugin plugin="ReferredPagesPlugin" args="depth=1" >body</wiki:Plugin></p>
</wiki:Tab>

<wiki:Tab id="referencedby" title="<%=LocaleSupport.getLocalizedMessage(pageContext, "info.tab.incoming")%>">
  <p><wiki:LinkTo><wiki:PageName /></wiki:LinkTo></p>
  <wiki:Plugin plugin="ReferringPagesPlugin" args="before='*' after='\n' " >body</wiki:Plugin>
</wiki:Tab>

<wiki:Tab id="attachments" title="<%=LocaleSupport.getLocalizedMessage(pageContext, "info.tab.attachments")%>">
   <wiki:Include page="AttachmentTab.jsp"/>
</wiki:Tab>

</wiki:PageType>

<%-- part 2 : attachments --%>

<wiki:PageType type="attachment">
<wiki:Tab id="infocontent" title="Attachment Info">
  <table>
    <tr>
    <th><fmt:message key="info.attachmentname"/></th>
    <td>
      <wiki:LinkToParent><wiki:ParentPageName /></wiki:LinkToParent> / <wiki:LinkTo><wiki:PageName /></wiki:LinkTo>
    </td>
    </tr>

    <tr>
    <th><fmt:message key="info.lastmodified"/></th>
    <td><wiki:PageDate format="<%= prefDateFormat %>" /> by <wiki:Author /></td>
    </tr>

    <tr>
    <th><fmt:message key="info.current"/></th>
    <td><wiki:PageVersion><fmt:message key="info.noversions"/></wiki:PageVersion></td>
    </tr>

    <tr>
    <th><fmt:message key="info.feed"/></th>
    <td><a href="<wiki:Link format="url" jsp="rss.jsp"><wiki:Param name="page" value="<%=c.getName()%>"/><wiki:Param name="mode" value="wiki"/></wiki:Link>"
           title="RSS link for <wiki:PageName />" >
        <img src="<wiki:Link format="url" jsp="images/xml.png"/>" border="0" alt="[RSS]"  />
        </a>
    </td>
    </tr>

    <tr>
    <th><fmt:message key="info.uploadnew"/></th>
    <td>
    <form action="<wiki:Link context="att" format="url" absolute="true"/>"
          method="post" enctype="multipart/form-data">

    <%-- Do NOT change the order of wikiname and content, otherwise the
        servlet won't find its parts. --%>

    <input type="hidden" name="page" value="<wiki:Variable var="pagename" />" />
    <%--
    In order to update this attachment with a newer version,
    please select a file name (click "Choose" button), then click on "Update".
    --%>
    <input type="file" name="content" />
    <br />
    Change note: <input type="text" name="changenote" maxlength="80" width="60" />
    <br />
    <input type="submit" name="upload" value="<fmt:message key="info.uploadnew.submit"/>" />
    <input type="hidden" name="action" value="upload" />
    <input type="hidden" name="nextpage" value="<wiki:PageInfoLink format="url"/>" />
    </form>
    </td>
    </tr>


   <wiki:Permission permission="delete">
   <tr>
     <th><fmt:message key="info.deleteattachment"/></th>
     <td>
         <form name="deleteForm"
               action="<wiki:Link format="url" context="<%=WikiContext.DELETE%>" />"
               method="post"
               accept-charset="<wiki:ContentEncoding />" onsubmit="return confirmDelete()">
           <input type="submit" name="delete-all" value="<fmt:message key="info.deleteattachment.submit"/>"/>
         </form>
     </td>
   </tr>
   </wiki:Permission>

   <tr>
    <th><fmt:message key="info.history"/></th>
    <td>
      <wiki:CheckVersion mode="first"><fmt:message key="info.noversions"/></wiki:CheckVersion>

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
               <option value="<wiki:Link context="info" format="url"><wiki:Param name="start" value="-1"/></wiki:Link>"
                      <%= ( (start == -1) ? "selected='selected'" : "") %> >
                  <fmt:message key="info.showrevisions">
                     <fmt:param><%= latestVersion %></fmt:param>
                     <fmt:param>1</fmt:param>
                  </fmt:message>
               </option>
          <%
             }

             while( endofblock > 0 )
             {
               if( startofblock < 1 ) { startofblock = 1;  }
               selected = ( (start >= startofblock) && (start <= endofblock) );
               if( selected ) start = startofblock; //defensive
          %>
               <option value="<wiki:Link context="info" format="url"><wiki:Param name="start" value="<%=Integer.toString(startofblock)%>"/></wiki:Link>" 
                       <%= (selected ? "selected='selected'" : "") %> >
                   <fmt:message key="info.showfrom">
                     <fmt:param><%= ((startofblock == 1) ? "first" : Integer.toString(size) ) %></fmt:param>
                     <fmt:param><%=endofblock%></fmt:param>
                     <fmt:param><%=startofblock %></fmt:param>
                   </fmt:message>
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
      <th><fmt:message key="info.version"/></th>
      <th><fmt:message key="info.date"/></th>
      <th><fmt:message key="info.author"/></th>
      <th><fmt:message key="info.size"/></th>
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
    <fmt:message key="common.nopage">
       <fmt:param><wiki:EditLink><fmt:message key="common.createit"/></wiki:EditLink></fmt:param>
    </fmt:message>
</wiki:NoSuchPage>

