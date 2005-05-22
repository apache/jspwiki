<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>


<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
        "http://www.w3.org/TR/html4/loose.dtd">

<html>

<head>
  <title><wiki:Variable var="applicationname"/>: Add Attachment</title>
  <wiki:Include page="commonheader.jsp" />
  <meta name="robots" content="noindex">
</head>

<body class="upload" bgcolor="#FFFFFF">

      <h1 class="pagename">Upload new attachment to <wiki:PageName /></h1>
      <hr /><p>

      <wiki:HasAttachments>
         <div class="attachments">
         <h3>Currently existing attachments:</h3>
         <div class="zebra-table" align="center">
         <table width="90%">
         <wiki:AttachmentsIterator id="att">
             <tr>
             <td><wiki:LinkTo><%=att.getFileName()%></wiki:LinkTo></td>
             <td><wiki:PageInfoLink><img src="<wiki:BaseURL/>images/attachment_big.png" border="0" alt="Info on <%=att.getFileName()%>"></wiki:PageInfoLink></td>
             <td><%=att.getSize()%> bytes</td>
             </tr>
         </wiki:AttachmentsIterator>
         </table>
         </div>
         </div>
         <hr />

      </wiki:HasAttachments>

      <table border="0" width="100%">
      <tr>
        <td>
           <form action="<wiki:BaseURL/>attach" method="post" enctype="multipart/form-data" accept-charset="<wiki:ContentEncoding/>">

           <%-- Do NOT change the order of wikiname and content, otherwise the 
                servlet won't find its parts. --%>

           <input type="hidden" name="page" value="<wiki:Variable var="pagename"/>">

           In order to upload a new attachment to this page, please use the following
           box to find the file, then click on "Upload".

           <p>
           <input type="file" name="content">
           <input type="submit" name="upload" value="Upload">
           <input type="hidden" name="action" value="upload">
           <input type="hidden" name="nextpage" value="<wiki:UploadLink format="url"/>">
           </p>
           </form>

           <span class="error"><wiki:Variable var="msg"/></span>

        </td>

      </table>


</body>

</html>
