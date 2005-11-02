<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>

<div style="overflow:auto;">
  <div class="list">
     <h3>List of attachments</h3>
     <small>(Click on an image to see a preview)</small>
  
     <form action="#">
        <select name="attachSelect" id="attachSelect" size="16" 
                onchange="Wiki.showImage(this[this.selectedIndex], '%A0', 300, 300 )" >
        <option value="Attachment Info" selected="selected" >--- No Image Selected ---</option>
        <wiki:AttachmentsIterator id="att">
           <%-- use %A0 as delimiter:  Name, Link-url, Info-url, Size, Version --%>
           <option value="<wiki:PageName />%A0<wiki:LinkTo format='url' />%A0<wiki:PageInfoLink format='url' />%A0<wiki:PageSize /> bytes%A0<wiki:PageVersion />" >
             <wiki:PageName /> (<wiki:PageSize /> bytes)
           </option>
        </wiki:AttachmentsIterator>
        </select>
     </form>
  </div>
  
  <div class="preview">
    <h3>Image preview</h3>
    <div id="attachImg">No Image Selected </div>
  </div>

</div>