<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.auth.*" %>
<%@ page import="com.ecyrd.jspwiki.ui.progress.*" %>
<%@ page import="com.ecyrd.jspwiki.auth.permissions.*" %>
<%@ page import="java.security.Permission" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<fmt:setBundle basename="templates.default"/>
<%
  int MAXATTACHNAMELENGTH = 30;
  WikiContext wikiContext = WikiContext.findContext(pageContext);
  String progressId = wikiContext.getEngine().getProgressManager().getNewProgressIdentifier();
%>

<h3><fmt:message key="attach.add"/></h3>
<wiki:Permission permission="upload">
  <wiki:Permission permission="upload">
  <form action="<wiki:Link jsp='attach' format='url' absolute='true'><wiki:Param name='progressid' value='<%=progressId%>'/></wiki:Link>"
         class="wikiform"
        method="post"
       enctype="multipart/form-data" accept-charset="<wiki:ContentEncoding/>"
      onsubmit="return Wiki.submitUpload(this, '<%=progressId%>');" >
    <input type="hidden" name="page" value="<wiki:Variable var="pagename"/>" />

    <table>
    <tr>
      <td colspan="2"><div class="formhelp"><fmt:message key="attach.add.info" /></div></td>
    </tr>
    <tr>
      <td><label for="content"><fmt:message key="attach.add.selectfile"/></label></td>
      <td><input type="file" name="content" id="content" size="60"/></td>
    </tr>
    <tr>
      <td><label for="changenote"><fmt:message key="attach.add.changenote"/></label></td>
      <td><input type="text" name="changenote" id="changenote" maxlength="80" size="60" />
    <input type="hidden" name="nextpage" value="<wiki:UploadLink format="url"/>" /></td>
    </tr>

   <tr>
      <td></td>
      <td>
        <input type="submit" name="upload" value="Upload<fmt:message key='attach.add.submit'/>" style="display:none;"/>
        <input type="button" name="uploax" value="<fmt:message key='attach.add.submit'/>" id="uploax" 
        	onclick="this.form.upload.click();"/>
        <input type="hidden" name="action" value="upload" />
        <div id="progressbar"><div class="ajaxprogress"></div></div>
      </td>
    </tr>
    </wiki:Permission>

    </table>
  </form>
  <wiki:Messages div="error" />
</wiki:Permission>
<wiki:Permission permission="!upload">
<div class="formhelp"><fmt:message key="attach.add.permission"/></div>
</wiki:Permission>


<%-- FIXME
<wiki:CheckRequestContext context='upload'>
  <p>Back to <wiki:Link><wiki:PageName/></wiki:Link></p>
</wiki:CheckRequestContext>
--%>

<wiki:HasAttachments>

<h3><fmt:message key="attach.list"/></h3>

  <%--<small><fmt:message key="attach.listsubtitle"/></small>--%>

  <wiki:Permission permission="delete">
    <%-- hidden delete form --%>
    <form action="tbd"
           class="wikiform"
            name="deleteForm" id="deleteForm" style="display:none;"
          method="post" accept-charset="<wiki:ContentEncoding />"
        onsubmit="return(confirm('<fmt:message key="attach.deleteconfirm"/>') && Wiki.submitOnce(this) );" >

      <input id="delete-all" name="delete-all" type="submit" value="Delete" />

    </form>
  </wiki:Permission>

  <div class="zebra-table"><div class="slimbox-img sortable">
  <table class="wikitable">
    <tr>
      <th><fmt:message key="info.attachment.type"/></th>
      <th><fmt:message key="info.attachment.name"/></th>
      <th><fmt:message key="info.size"/></th>
      <th><fmt:message key="info.version"/></th>
      <th><fmt:message key="info.date"/></th>
      <th><fmt:message key="info.author"/></th>
      <wiki:Permission permission="delete"><th><fmt:message key="info.actions"/></th></wiki:Permission>
      <th width="30%"><fmt:message key="info.changenote"/></th>
    </tr>

    <wiki:AttachmentsIterator id="att">
    <%
      String name = att.getFileName();
      int dot = name.lastIndexOf(".");
      String attachtype = ( dot != -1 ) ? name.substring(dot+1) : "";

      String sname = name;
      if( sname.length() > MAXATTACHNAMELENGTH ) sname = sname.substring(0,MAXATTACHNAMELENGTH) + "...";
    %>
    <tr>
      <td><div id="attach-<%= attachtype %>" class="attachtype"><%= attachtype %></div></td>
      <td><wiki:LinkTo title="<%= name %>" ><%= sname %></wiki:LinkTo></td>
      <td nowrap style="text-align:right;">
        <fmt:formatNumber value='<%=Double.toString(att.getSize()/1000.0)%>' groupingUsed='false' maxFractionDigits='1' minFractionDigits='1'/>&nbsp;<fmt:message key="info.kilobytes"/>
      </td>
      <td style="text-align:center;">
        <a href="<wiki:PageInfoLink format='url' />" title="<fmt:message key='attach.moreinfo.title'/>"><wiki:PageVersion /></a>
      </td>
	  <td nowrap><fmt:formatDate value="<%= att.getLastModified() %>" pattern="${prefDateFormat}" /></td>
      <td><wiki:Author /></td>
      <wiki:Permission permission="delete">
      <td>
          <input type="button"
                value="<fmt:message key='attach.delete'/>"
                  src="<wiki:Link format='url' context='<%=WikiContext.DELETE%>' />"
              onclick="$('deleteForm').setProperty('action',this.src); $('delete-all').click();" />
      </td>
      </wiki:Permission>
      <td>
      <%
         String changeNote = (String)att.getAttribute(WikiPage.CHANGENOTE);
         if( changeNote != null ) {
         %><%=changeNote%><%
         }
      %>
      </td>
    </tr>
    </wiki:AttachmentsIterator>

  </table>
  </div></div>

</wiki:HasAttachments>
