<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>

<%-- Inserts page content. --%>

<div id="pagecontent">
      <%-- If the page is an older version, then offer a note and a possibility
           to restore this version as the latest one. --%>

      <wiki:CheckVersion mode="notlatest">
         <font color="red">
            <p class="versionnote">This is version <wiki:PageVersion/>.  
            It is not the current version, and thus it cannot be edited.<br />
            <wiki:LinkTo>[Back to current version]</wiki:LinkTo>&nbsp;&nbsp;
            <wiki:EditLink version="this">[Restore this version]</wiki:EditLink></p>
         </font>
         <hr />
      </wiki:CheckVersion>

      <%-- Inserts no text if there is no page. --%>

      <wiki:InsertPage />

      <wiki:NoSuchPage>
           <!-- FIXME: Should also note when a wrong version has been fetched. -->
           This page does not exist.  Why don't you go and
           <wiki:EditLink>create it</wiki:EditLink>?
      </wiki:NoSuchPage>

</div>

      <wiki:HasAttachments>
         <div class="attachments">

         <h3>Attachments:</h3>

         <div class="zebra-table" align="center">
         <table width="90%">
         <wiki:AttachmentsIterator id="att">
             <tr>
             <td><wiki:LinkTo><%=att.getFileName()%></wiki:LinkTo></td>
             <td><wiki:PageInfoLink><img src="<wiki:BaseURL/>images/attachment_big.png" border="0" alt="Info on <%=att.getFileName()%>" /></wiki:PageInfoLink></td>
             <td><%=att.getSize()%> bytes</td>
             </tr>
         </wiki:AttachmentsIterator>
         </table>
         </div>
         </div>
      </wiki:HasAttachments>

