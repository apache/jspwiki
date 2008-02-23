<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://stripes.sourceforge.net/stripes.tld" prefix="stripes" %>
<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<fmt:setBundle basename="templates.DefaultResources"/>

<stripes:useActionBean binding="/ViewGroup.action"/>

<stripes:layout-render name="/WEB-INF/jsp/templates/default/ViewTemplate.jsp">
  <stripes:layout-component name="contents">

    <script language="javascript" type="text/javascript">
    function confirmDelete()
    {
      var reallydelete = confirm("<fmt:message key="group.areyousure"><fmt:param>${actionBean.group.name}</fmt:param></fmt:message>");
    
      return reallydelete;
    }
    </script>
    
    <h3>Group ${actionBean.group.name}</h3>
    
    <c:choose>
      <c:when test="${empty actionBean.group.created}">
        <!-- Group doesn't exist; offer user opportunity to create one if allowed. -->
        <fmt:message key="group.doesnotexist"/>
        <wiki:Permission permission="createGroups">
          <fmt:message key="group.createsuggestion">
            <fmt:param>
              <wiki:Link jsp="NewGroup.jsp">
                <wiki:Param name="group"><%=request.getParameter("group")%></wiki:Param>
                <fmt:message key="group.createit"/>
              </wiki:Link>
            </fmt:param>
          </fmt:message>
        </wiki:Permission>
      </c:when>
      
      <c:otherwise>
        <!-- Group exists; print details. -->
        <div class="formcontainer">
        
          <!-- Group name and instructions -->
          <div class="instructions">
            <fmt:message key="group.groupintro">
              <fmt:param><em>${actionBean.group.name}</em></fmt:param>
            </fmt:message>
          </div>
        
          <!-- Members -->
          <div class="block">
            <label><fmt:message key="group.members"/><label>
            <stripes:label for="group.members" />
            <c:forEach items="${actionBean.group.members}" var="member" varStatus="loop">
              <br>${member.name}</br>
            </c:forEach>
          </div>
          
          <!-- Created/modified info -->
          <div class="instructions">
            <fmt:message key="group.modifier">
               <fmt:param>${actionBean.group.modifier}</fmt:param>
               <fmt:param>${actionBean.group.modified}</fmt:param>
            </fmt:message>
            <br />
            <fmt:message key="group.creator">
               <fmt:param>${actionBean.group.creator}</fmt:param>
               <fmt:param>${actionBean.group.created}</fmt:param>
            </fmt:message> 
          </div>
          
        </div>
      </c:otherwise>
      
    </c:choose>

  </stripes:layout-component>
</stripes:layout-render>
