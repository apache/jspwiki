<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>

<wiki:PageExists>

   <table cellspacing="4">
       <tr>
           <td><B>Page name</B></td>
           <td><wiki:LinkTo><wiki:PageName /></wiki:LinkTo></td>
       </tr>

       <wiki:IsAttachment>
           <tr>
              <td><B>Parent page</B></td>
              <td>FIXME</td>
           </tr>
       </wiki:IsAttachment>

       <tr>
           <td><B>Page last modified</B></td>
           <td><wiki:PageDate /></td>
       </tr>

       <tr>
           <td><B>Current page version</B></td>
           <td><wiki:PageVersion>No versions.</wiki:PageVersion></td>
       </tr>

       <tr>
           <td valign="top"><b>Page revision history</b></td>
           <td>
               <table border="1" cellpadding="4">
                   <tr>
                        <th>Version</th>
                        <th>Date (and differences to current)</th>
                        <th>Author</th>
                        <th>Size</th>
                        <th>Changes from previous</th>
                   </tr>
                   <wiki:HistoryIterator id="currentPage">
                     <tr>
                         <td>
                             <wiki:LinkTo version="<%=Integer.toString(currentPage.getVersion())%>">
                                  <wiki:PageVersion/>
                             </wiki:LinkTo>
                         </td>

                         <td>
                             <wiki:DiffLink version="latest" 
                                            newVersion="<%=Integer.toString(currentPage.getVersion())%>">
                                 <wiki:PageDate/>
                             </wiki:DiffLink>
                         </td>

                         <td><wiki:Author /></td>
                         <td><wiki:PageSize /></td>
                         <td>
                              <% if( currentPage.getVersion() > 1 ) { %>
                                   <wiki:DiffLink version="<%=Integer.toString(currentPage.getVersion())%>" 
                                                  newVersion="<%=Integer.toString(currentPage.getVersion()-1)%>">
                                       from version <%=currentPage.getVersion()-1%> to <%=currentPage.getVersion()%>
                                   </wiki:DiffLink>
                               <% } %>
                         </td>
                     </tr>
                   </wiki:HistoryIterator>
               </table>
           </td>
      </tr>
</table>
             
    <BR />
    <wiki:LinkTo>Back to <wiki:PageName/></wiki:LinkTo>

</wiki:PageExists>


<wiki:NoSuchPage>
    This page does not exist.  Why don't you go and
    <wiki:EditLink>create it</wiki:EditLink>?
</wiki:NoSuchPage>
