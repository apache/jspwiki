<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<fmt:setBundle basename="templates.default"/>
<%
  /* see commonheader.jsp */
  String prefDateFormat = (String) session.getAttribute("prefDateFormat");
  String prefTimeZone   = (String) session.getAttribute("prefTimeZone");
  String group              = request.getParameter( "group" );

  String homepage = "Main";
  WikiContext wikiContext = WikiContext.findContext(pageContext);
  WikiPage wikiPage = wikiContext.getPage();
  try 
  { 
    homepage = wikiContext.getEngine().getFrontPage(); 
  } 
  catch( Exception  e )  { /* dont care */ } ;
%>
<%-- similar to PageActionsTop, except for
     ** no accesskey definitions
     ** additional quick2Top, summaryPageInfo  
     ** no Login/Logout, quick2Bottom 
  --%>
<div id='actionsBottom' class="pageactions"> 

  <span class="quick2Top">
    <a href="#top" title="<fmt:message key='actions.gototop'/>" >&laquo;</a>
  </span>

  <span class="actionHome">
    <a href="<wiki:LinkTo page='<%= homepage %>' format='url' />"
       title="<fmt:message key='actions.home.title' ><fmt:param><%=homepage%></fmt:param></fmt:message>" >
     <fmt:message key='actions.home' />
    </a>
  </span>
  
  <wiki:CheckRequestContext context='view|info|diff|upload'>
    <wiki:Permission permission="edit">
      <span class="actionEdit">
        <wiki:PageType type="page">
          <a href="<wiki:EditLink format='url' />" accesskey="e" 
            title="<fmt:message key='actions.edit.title'/>" ><fmt:message key='actions.edit'/></a>
        </wiki:PageType>
        <wiki:PageType type="attachment">
          <a href="<wiki:BaseURL/>Edit.jsp?page=<wiki:ParentPageName />" accesskey="e" 
            title="<fmt:message key='actions.editparent.title'/>" ><fmt:message key='actions.editparent'/></a>
        </wiki:PageType>
      </span>
    </wiki:Permission>
  
    <wiki:Permission permission="comment">
    <span>
      <wiki:PageType type="page">
        <a href="Comment.jsp?page=<wiki:Variable var='pagename' />"
           title="<fmt:message key="actions.comment.title"/>" ><fmt:message key="actions.comment"/></a>
      </wiki:PageType>
      <wiki:PageType type="attachment">
        <a href="Comment.jsp?page=<wiki:ParentPageName />"
          title="<fmt:message key="actions.addcommenttoparent"/>" ><fmt:message key="actions.comment"/></a>
      </wiki:PageType>
      </span>
    </wiki:Permission>
  </wiki:CheckRequestContext>
  
  
  <wiki:CheckRequestContext context='!prefs'>
    <wiki:CheckRequestContext context='!preview'>
      <span class="actionPrefs">
        <wiki:Link jsp="UserPreferences.jsp"><fmt:message key="actions.prefs"/></wiki:Link>
      </span>
    </wiki:CheckRequestContext>
  </wiki:CheckRequestContext>

  <%-- summary page info--%>
  <wiki:CheckRequestContext context='view|diff|edit|upload|info'>
    <div class="pageInfo">
      <wiki:CheckVersion mode="latest">
         <fmt:message key="info.lastmodified">
            <fmt:param><wiki:PageVersion /></fmt:param>
            <fmt:param><wiki:DiffLink version="latest" newVersion="previous"><wiki:PageDate format='${prefDateFormat}'/></wiki:DiffLink></fmt:param>
            <fmt:param><wiki:Author /></fmt:param>
         </fmt:message>
      </wiki:CheckVersion>
  
      <wiki:CheckVersion mode="notlatest">
        <fmt:message key="actions.publishedon">
           <fmt:param><wiki:PageDate format='${prefDateFormat}'/></fmt:param>
           <fmt:param><wiki:Author /></fmt:param>
        </fmt:message>
      </wiki:CheckVersion>

      <wiki:PageExists>  
      <a href="<wiki:Link format='url' jsp='rss.jsp'>
                 <wiki:Param name='page' value='<%=wikiPage.getName()%>'/>
                 <wiki:Param name='mode' value='wiki'/>
               </wiki:Link>"
        title="<fmt:message key='info.rsspagefeed.title'>
                 <fmt:param><wiki:PageName /></fmt:param>
               </fmt:message>" >
        <img src="<wiki:Link jsp='images/xml.png' format='url'/>" border="0" alt="[RSS]"/>
      </a>
      </wiki:PageExists>

      <wiki:NoSuchPage><fmt:message key="actions.notcreated"/></wiki:NoSuchPage> 
    </div>
  </wiki:CheckRequestContext>

</div>
