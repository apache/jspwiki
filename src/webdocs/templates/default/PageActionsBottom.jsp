<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<fmt:setBundle basename="templates.default"/>
<%
  WikiContext c = WikiContext.findContext(pageContext);
  WikiPage wikipage = c.getPage();
  String frontpage = c.getEngine().getVariable(c,"jspwiki.frontPage");
%>
<%-- similar to PageActionsTop, except for
     ** no accesskey definitions
     ** additional quick2Top, summaryPageInfo  
     ** no Login/Logout, quick2Bottom 
  --%>
<div id='actionsBottom' class="pageactions"> 
  <ul style="float:right;">
  <li>
  <a href="<wiki:LinkTo page='<%=frontpage%>' format='url' />" class="action home"
    title="<fmt:message key='actions.home.title' ><fmt:param><%=frontpage%></fmt:param></fmt:message> ">
     <fmt:message key='actions.home' />
    </a>
  </li>
  <wiki:CheckRequestContext context='view|info|diff|upload'>
    <wiki:Permission permission="edit">
	  <li>
        <wiki:PageType type="page">
          <a href="<wiki:EditLink format='url' />" accesskey="e"  class="action edit"
            title="<fmt:message key='actions.edit.title'/>" ><fmt:message key='actions.edit'/></a>
        </wiki:PageType>
        <wiki:PageType type="attachment">
          <a href="<wiki:BaseURL/>Edit.jsp?page=<wiki:ParentPageName />" accesskey="e" class="action edit"
            title="<fmt:message key='actions.editparent.title'/>" ><fmt:message key='actions.editparent'/></a>
        </wiki:PageType>
      </li>
    </wiki:Permission>
  
    <wiki:PageExists>  
    <wiki:Permission permission="comment">
	  <li>
      <wiki:PageType type="page">
        <a href="Comment.jsp?page=<wiki:Variable var='pagename' />" class="action comment"
           title="<fmt:message key="actions.comment.title"/>" ><fmt:message key="actions.comment"/></a>
      </wiki:PageType>
      <wiki:PageType type="attachment">
        <a href="Comment.jsp?page=<wiki:ParentPageName />" class="action comment"
          title="<fmt:message key="actions.addcommenttoparent"/>" ><fmt:message key="actions.comment"/></a>
      </wiki:PageType>
      </li>
    </wiki:Permission>
    </wiki:PageExists>  
  </wiki:CheckRequestContext>
  
  
  <wiki:CheckRequestContext context='!prefs'>
    <wiki:CheckRequestContext context='!preview'>
	  <li>
      <a href="<wiki:Link jsp='UserPreferences.jsp' format='url' />" class="action prefs">
          <fmt:message key="actions.prefs"/>
      </a>
      </li>
    </wiki:CheckRequestContext>
  </wiki:CheckRequestContext>

  <li>
    <a href="#top" class="action quick2top" title="<fmt:message key='actions.gototop'/>" >&laquo;</a>
  </li>
  </ul>

  <%-- summary page info--%>
  <wiki:CheckRequestContext context='view|diff|edit|upload|info'>
    <div class="pageinfo">
      <wiki:PageExists>  
      <wiki:CheckVersion mode="latest">
         <fmt:message key="info.lastmodified">
            <fmt:param><wiki:PageVersion /></fmt:param>
            <fmt:param><wiki:DiffLink version="latest" newVersion="previous"><wiki:PageDate format='${prefs["DateFormat"]}'/></wiki:DiffLink></fmt:param>
            <fmt:param><wiki:Author /></fmt:param>
         </fmt:message>
      </wiki:CheckVersion>
  
      <wiki:CheckVersion mode="notlatest">
        <fmt:message key="actions.publishedon">
           <fmt:param><wiki:PageDate format='${prefs["DateFormat"]}'/></fmt:param>
           <fmt:param><wiki:Author /></fmt:param>
        </fmt:message>
      </wiki:CheckVersion>

      <a href="<wiki:Link format='url' jsp='rss.jsp'>
                 <wiki:Param name='page' value='<%=wikipage.getName()%>'/>
                 <wiki:Param name='mode' value='wiki'/>
               </wiki:Link>"
        title="<fmt:message key='info.rsspagefeed.title'>
                 <fmt:param><wiki:PageName /></fmt:param>
               </fmt:message>" >
        <img src="<wiki:Link jsp='images/xml.png' format='url'/>" alt="[RSS]"/>
      </a>
      </wiki:PageExists>

      <wiki:NoSuchPage><fmt:message key="actions.notcreated"/></wiki:NoSuchPage> 
    </div>
  </wiki:CheckRequestContext>

</div>
