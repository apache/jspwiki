<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="http://stripes.sourceforge.net/stripes.tld" prefix="stripes" %>
<stripes:useActionBean beanclass="com.ecyrd.jspwiki.action.ViewActionBean" var="context"/>

<table>
  <thead>
    <th align="left">Tag or expression</th>
    <th align="left">Value</th>
  </thead>
  <tr>
    <td>
      ${'${'}wikiPage.name}
    </td>
    <td>
      '${wikiPage.name}'
    </td>
  </tr>
  <tr>
    <td>
      ${'${'}wikiSession.userPrincipal}
    </td>
    <td>
      '${wikiSession.userPrincipal}'
    </td>
  </tr>
  <tr>
    <td>
      ${'${'}wikiEngine.frontPage}
    </td>
    <td>
      '${wikiEngine.frontPage}'
    </td>
  </tr>
  <tr>
    <td>
      ${'${'}wikiActionBean.currentUser}
    </td>
    <td>
      '${wikiActionBean.currentUser}'
    </td>
  </tr>
  <tr>
    <td>
      &lt;wiki:BaseURL /&gt;
    </td>
    <td>
      '<wiki:BaseURL/>'
    </td>
  </tr>
  <tr>
    <td>
      &lt;wiki:Link format='url' templatefile='jspwiki_print.css'/&gt;
    </td>
    <td>
      '<wiki:Link format='url' templatefile='jspwiki_print.css'>Test</wiki:Link>'
    </td>
  </tr>
  <tr>
    <td>
      &lt;wiki:Link format='url' jsp='images/favicon.png'/&gt;
    </td>
    <td>
      '<wiki:Link format='url' jsp='images/favicon.png'/>'
    </td>
  </tr>
  <tr>
    <td>
      &lt;wiki:EditLink/&gt;
    </td>
    <td>
      '<wiki:EditLink>Link to edit page</wiki:EditLink>'
    </td>
  </tr>
</table>
