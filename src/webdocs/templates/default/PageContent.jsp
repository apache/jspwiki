<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
<fmt:setBundle basename="templates.DefaultResources"/>

<%-- Inserts page content. --%>

<div id="pagecontent">
  <%-- If the page is an older version, then offer a note and a possibility
       to restore this version as the latest one. --%>

  <wiki:CheckVersion mode="notlatest">
    <div class="warning">
      <fmt:message key="view.oldversion">
        <fmt:param><wiki:PageVersion/></fmt:param>
      </fmt:message>  
      <br />
      <wiki:LinkTo><fmt:message key="view.backtocurrent"/></wiki:LinkTo>&nbsp;&nbsp;
      <wiki:EditLink version="this"><fmt:message key="view.restore"/></wiki:EditLink>
    </div>
  </wiki:CheckVersion>

  <%-- Inserts no text if there is no page. --%>

  <wiki:InsertPage />

  <wiki:NoSuchPage>
    <%-- FIXME: Should also note when a wrong version has been fetched. --%>

    <fmt:message key="common.nopage">
      <fmt:param><wiki:EditLink><fmt:message key="common.createit"/></wiki:EditLink></fmt:param>
    </fmt:message>
  </wiki:NoSuchPage>

</div>

<wiki:HasAttachments>
  <div id="attachments">
    <h3><fmt:message key="view.heading.attachments"/></h3>
    <div class="zebra-table">
      <table>
        <wiki:AttachmentsIterator id="att">
          <tr>
            <td><wiki:LinkTo><%=att.getFileName()%></wiki:LinkTo></td>
            <td><wiki:PageInfoLink><img src="<wiki:Link format="url" jsp="images/attachment_big.png"/>" border="0" alt="Info on <%=att.getFileName()%>" /></wiki:PageInfoLink></td>
            <td><fmt:formatNumber value="${att.size}"/> <fmt:message key="attach.bytes"/></td>
          </tr>
        </wiki:AttachmentsIterator>
      </table>
    </div>
  </div>
</wiki:HasAttachments>

