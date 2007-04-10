<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<fmt:setBundle basename="templates.default"/>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
        "http://www.w3.org/TR/html4/loose.dtd">

<html>

<head>
  <title><fmt:message key="upload.title"><fmt:param><wiki:Variable var="applicationname"/></fmt:param></fmt:message></title>
  <wiki:Include page="commonheader.jsp" />
  <meta name="robots" content="noindex">
</head>

<body class="upload" bgcolor="#FFFFFF">

      <h1 class="pagename"><fmt:message key="upload.heading.upload"><fmt:param><wiki:PageName/></fmt:param></fmt:message></h1>
      <hr /><p>

      <wiki:HasAttachments>
         <div id="attachments">
         <h3><fmt:message key="upload.attachments"/></h3>
         <div class="zebra-table">
         <table width="90%">
         <wiki:AttachmentsIterator id="att">
             <tr>
             <td><wiki:LinkTo><%=att.getFileName()%></wiki:LinkTo></td>
             <td><wiki:PageInfoLink><img src="<wiki:Link format="url" jsp="images/attachment_big.png"/>" border="0" alt="Info on <%=att.getFileName()%>"></wiki:PageInfoLink></td>
             <td><fmt:message key="attach.bytes"><fmt:param><%=att.getSize()%></fmt:param></fmt:message></td>
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
           <form action="<wiki:Link format="url" jsp="attach"/>" method="post" enctype="multipart/form-data" accept-charset="<wiki:ContentEncoding/>">

           <%-- Do NOT change the order of wikiname and content, otherwise the 
                servlet won't find its parts. --%>

           <input type="hidden" name="page" value="<wiki:Variable var="pagename"/>" />

           <fmt:message key="upload.info"/>
           
           <p>
           <input type="file" name="content" />
           <input type="submit" name="upload" value="Upload" />
           <input type="hidden" name="action" value="upload" /><br />
           Change note: <input type="text" name="changenote" maxlength="80" width="60" />
           <input type="hidden" name="nextpage" value="<wiki:UploadLink format="url"/>" />
           </p>
           </form>

           <wiki:Messages div="error" />

        </td>
        </tr>
        <tr>

        <td>
           <p><fmt:message key="upload.done"><fmt:param><wiki:LinkTo><wiki:PageName/></wiki:LinkTo></fmt:param></fmt:message>
           </p>
        </td>
        </tr>

      </table>


</body>

</html>
