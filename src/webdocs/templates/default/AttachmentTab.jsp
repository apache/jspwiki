<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<fmt:setBundle basename="templates.default"/>
<div id="attachmentViewer">
  <div class="list">
     <h3><fmt:message key="attach.list"/></h3>
     <small><fmt:message key="attach.listsubtitle"/></small>
  
     <form action="#">
        <select name="attachSelect" id="attachSelect" size="16" 
                onchange="Wiki.showImage(this[this.selectedIndex], '%A4', 300, 300 )" >
        <option value="Attachment Info" selected="selected" ><fmt:message key="attach.noimageselected"/></option>
        <wiki:AttachmentsIterator id="att">
           <%-- use %A4 as delimiter:  Name, Link-url, Info-url, Size, Version --%>
           <option value="<wiki:PageName />%A4<wiki:LinkTo format='url' />%A4<wiki:PageInfoLink format='url' />%A4<fmt:message key="attach.bytes"><fmt:param><wiki:PageSize /></fmt:param></fmt:message>%A4<wiki:PageVersion />" >
             <wiki:PageName /> (<wiki:PageSize /> bytes)
           </option>
        </wiki:AttachmentsIterator>
        </select>
     </form>
     <div class="small"><fmt:message key="attach.selectimage"/></div>
  </div>
  
  <div class="preview">
    <h3><fmt:message key="attach.preview"/></h3>
    <div id="attachImg"><fmt:message key="attach.noimageselected"/></div>
  </div>

  <div style="clear:both; height:0px;" > </div>

</div>