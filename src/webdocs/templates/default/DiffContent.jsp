<%@ page import="com.ecyrd.jspwiki.tags.InsertDiffTag" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="java.util.*" %>
<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<fmt:setBundle basename="templates.DefaultResources"/>
<% 
  WikiContext c = WikiContext.findContext( pageContext );  
  List history = c.getEngine().getVersionHistory(c.getPage().getName());
  pageContext.setAttribute( "history", history );
 %>
<div id="diffcontent" >

    <wiki:PageExists>
    <div class="diffnote">

    <form action="<wiki:Link format="url" jsp="Diff.jsp"/>" 
          method="post"  accept-charset="UTF-8">
       <input type="hidden" name="page" value="<wiki:Variable var='pagename' />" />
         <fmt:message key="diff.difference">
            <fmt:param>
    <select id="r1" name="r1" onchange="this.form.submit();" >

    <c:forEach items="${history}" var="i">
       <option value="<c:out value="${i.version}"/>" <c:if test="${i.version == insertdiffold}">selected="selected"</c:if> ><c:out value="${i.version}"/></option>
    </c:forEach>
    </select>
    </fmt:param>
    <fmt:param>
    <select id="r2" name="r2" onchange="this.form.submit();" >
    
    <c:forEach items="${history}" var="i">
       <option value="<c:out value="${i.version}"/>" <c:if test="${i.version == insertdiffnew}">selected="selected"</c:if> ><c:out value="${i.version}"/></option>
    </c:forEach>
    </select>
    </fmt:param>
    </fmt:message>
    &nbsp;&nbsp;&nbsp;&nbsp;
    <a title="<fmt:message key="diff.gotofirst.title"/>" 
       href="#change-1"><fmt:message key="diff.gotofirst"/></a>&raquo;&raquo;
    </form>
    <br />
    <fmt:message key="diff.goback">
       <fmt:param><wiki:LinkTo><wiki:PageName/></wiki:LinkTo></fmt:param>
       <fmt:param><wiki:PageInfoLink>
          <fmt:message key="diff.versionhistory">
             <fmt:param><wiki:PageName /></fmt:param>
          </fmt:message>
          </wiki:PageInfoLink>
       </fmt:param>
    </fmt:message>
    </div>
    <br />
    <wiki:InsertDiff>
      <i><fmt:message key="diff.nodiff"/></i>
    </wiki:InsertDiff> 
    </wiki:PageExists>
    
    <wiki:NoSuchPage>
    <p>
    <fmt:message key="common.nopage">
       <fmt:param>
          <wiki:EditLink>
             <fmt:message key="common.createit"/>
          </wiki:EditLink>
       </fmt:param>
    </fmt:message>
    </p>
    </wiki:NoSuchPage>

</div>
