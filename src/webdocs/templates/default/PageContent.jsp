<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>

<%-- Inserts page content. --%>

      <%-- If the page is an older version, then offer a note and a possibility
           to restore this version as the latest one. --%>

      <wiki:CheckVersion mode="notlatest">
         <FONT COLOR="red">
            <P CLASS="versionnote">This is version <wiki:PageVersion/>.  
            It is not the current version, and thus it cannot be edited.<BR/>
            <wiki:LinkTo>[Back to current version]</wiki:LinkTo>&nbsp;&nbsp;
            <wiki:EditLink version="this">[Restore this version]</wiki:EditLink></P>
         </FONT>
         <HR />
      </wiki:CheckVersion>

      <%-- Inserts no text if there is no page. --%>

      <wiki:InsertPage />

      <wiki:NoSuchPage>
           <!-- FIXME: Should also note when a wrong version has been fetched. -->
           This page does not exist.  Why don't you go and
           <wiki:EditLink>create it</wiki:EditLink>?
      </wiki:NoSuchPage>

      <BR CLEAR="all" />

      <wiki:HasAttachments>
         <B>Attachments:</B>

         <DIV class="attachments" align="center">
         <table width="90%">
         <wiki:AttachmentsIterator id="att">
             <tr>
             <td><A HREF="attach?page=<%=att.getName()%>&wikiname=<%=att.getFileName()%>"><img src="images/attachment_big.png" alt="<%=att.getFileName()%>"></A></td>
             <td><wiki:PageInfoLink><%=att.getFileName()%></wiki:PageInfoLink></td>
             <td><%=att.getSize()%> bytes</td>
             </tr>
         </wiki:AttachmentsIterator>
         </table>
         </DIV>
      </wiki:HasAttachments>

      <P><HR />
      <table border="0" width="100%">
        <tr>
          <td align="left">
             <wiki:Permission permission="edit">
                 <wiki:EditLink>Edit this page</wiki:EditLink>&nbsp;&nbsp;
             </wiki:Permission>
             <wiki:PageInfoLink>More info...</wiki:PageInfoLink>&nbsp;&nbsp;
             <a href="javascript:window.open('<wiki:UploadLink format="url" />','Upload','width=640,height=480,toolbar=1,menubar=1,scrollbars=1,resizable=1,').focus()">Attach file...</a>
             <BR />
          </td>
        </tr>
        <tr>
          <td align="left">
             <FONT size="-1">
             
             <wiki:CheckVersion mode="latest">
                 <I>This page last changed on <wiki:DiffLink version="latest" newVersion="previous"><wiki:PageDate/></wiki:DiffLink> by <wiki:Author />.</I>
             </wiki:CheckVersion>

             <wiki:CheckVersion mode="notlatest">
                 <I>This particular version was published on <wiki:PageDate/> by <wiki:Author /></I>.
             </wiki:CheckVersion>
 
             <wiki:NoSuchPage>
                 <I>Page not created yet.</I>
             </wiki:NoSuchPage>

             </FONT>
          </td>
        </tr>
      </table>
