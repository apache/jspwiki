<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://stripes.sourceforge.net/stripes.tld" prefix="stripes" %>
<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>

<wiki:TabbedSection defaultTab='${param.tab}' >

  <wiki:Tab id="pagecontent" title='<fmt:message key="view.tab"/>' accesskey="v">
    <wiki:Include page="/WEB-INF/jsp/templates/default/PageTab.jsp"/>
    <wiki:PageType type="attachment">
      <div class="information">
	    <fmt:message key="info.backtoparentpage" >
	      <fmt:param><wiki:LinkToParent><wiki:ParentPageName/></wiki:LinkToParent></fmt:param>
        </fmt:message>
      </div>
      <div style="overflow:hidden;">
        <wiki:Translate>[${wikiPage.name}]</wiki:Translate>
      </div>
    </wiki:PageType>
  </wiki:Tab>

  <wiki:PageExists>

  <wiki:PageType type="page">
  <wiki:Tab id="attach" title="<fmt:message key='attach.tab'/><c:if test='${wikiPage.attachments > 0}'> (${wikiPage.attachments.size})</c:if>" accesskey="a">
    <wiki:Include page="/WEB-INF/jsp/templates/default/AttachmentTab.jsp"/>
  </wiki:Tab>
  </wiki:PageType>

  <wiki:Tab id="info" title="<fmt:message key='info.tab'/>"
           url="<wiki:Link format='url' jsp='PageInfo.jsp'><wiki:Param name='page' value='${wikiPage.name}'/></wiki:Link>"
           accesskey="i" >
  </wiki:Tab>

  </wiki:PageExists>

</wiki:TabbedSection>
