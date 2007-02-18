<%@ page import="java.util.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.ui.admin.*" %>
<%@ page errorPage="/Error.jsp" %>
<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<html>
<head>
<title>JSPWiki administartion</title>
  <wiki:Include page="commonheader.jsp"/>
</head>
<body class="view">
<div id="wikibody">
<h1>JSPWiki Administration</h1>
<div class="information">Not all things can be configured here.  Some things need to be configured
in your <tt>jspwiki.properties</tt> file.</div>

<%
    WikiEngine wiki = WikiEngine.getInstance( getServletConfig() );
    WikiContext ctx = WikiContext.findContext(pageContext);
    AdminBeanManager mgr = wiki.getAdminBeanManager();
 %>

<wiki:TabbedSection defaultTab='<%=request.getParameter("tab")%>'>

<wiki:Tab id="core" title="Core">
<p>Contains core setup options.</p>
</wiki:Tab>

<wiki:Tab id="users" title="Users">
<p>Contains users management.</p>
</wiki:Tab>

<wiki:Tab id="groups" title="Groups">
   <div>
   <p>This is a list of all groups in this wiki.  If you click on the group name,
   you will be taken to the administration page of that particular group.</p>
   <p>
   <wiki:Plugin plugin="Groups"/>
   </p>
   </div>
</wiki:Tab>


<wiki:Tab id="editors" title="Editors">
   <wiki:TabbedSection>
     <wiki:AdminBeanIterator type="editors" id="ab">
      <wiki:Tab id="${ab.title}" title="${ab.title}">
      
      <form action="Admin.jsp" method="post" accept-charset="UTF-8">
      <%
             out.write(ab.getHTML(ctx));
       %>
       </form>
      </wiki:Tab>
     </wiki:AdminBeanIterator>
   </wiki:TabbedSection>
</wiki:Tab>
<wiki:Tab id="filters" title="Filters">
<p>There will be more filter stuff here</p>
</wiki:Tab>
</wiki:TabbedSection>

</div>
</body>