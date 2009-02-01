<%@ page import="java.util.*" %>
<%@ page import="org.apache.wiki.*" %>
<%@ page import="org.apache.wiki.ui.*" %>
<%@ page import="org.apache.wiki.ui.admin.*" %>
<%@ page errorPage="/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="http://stripes.sourceforge.net/stripes.tld" prefix="stripes" %>
<%@ page import="org.apache.wiki.action.WikiContextFactory" %>
<html>
<head>
<title>JSPWiki administration</title>
  <wiki:Include page="commonheader.jsp" />
  <link rel="stylesheet" media="screen, projection, print" type="text/css" href="<wiki:Link format='url' templatefile='admin/admin.css' />" />
</head>
<body class="view">
<div id="wikibody">
<div id="page">
<h1>JSPWiki Administration</h1>
<div class="information">Not all things can be configured here.  Some things need to be configured
in your <tt>jspwiki.properties</tt> file.</div>

<%
    WikiEngine wiki = WikiEngine.getInstance( getServletConfig() );
    WikiContext ctx = WikiContextFactory.findContext( pageContext );
    AdminBeanManager mgr = wiki.getAdminBeanManager();
 %>

<wiki:TabbedSection defaultTab="${param['tab-admin']}">

<wiki:Tab id="core" title="Core">
<p>Contains core setup options.</p>
   <wiki:TabbedSection defaultTab="${param['tab-core']}">

     <wiki:AdminBeanIterator type="core" id="ab">
      <wiki:Tab id="${ab.id}" title="${ab.title}">
      
      <div class="formcontainer">
      <form action="Admin.jsp" method="post" accept-charset="UTF-8">
        <input type="hidden" name="tab-admin" value="core" />
        <input type="hidden" name="tab-core" value="${ab.title}" />
        <input type="hidden" name="bean" value="${ab.id}" />
        <%
         out.write( ab.doGet(ctx) );
         %>
       </form>
       </div>
      </wiki:Tab>
     </wiki:AdminBeanIterator>
   </wiki:TabbedSection>
</wiki:Tab>

<wiki:Tab id="users" title="Users">
   <wiki:Include page="admin/UserManagement.jsp" />
</wiki:Tab>

<wiki:Tab id="groups" title="Groups">
   <div>
   <p>This is a list of all groups in this wiki.  If you click on the group name,
   you will be taken to the administration page of that particular group.</p>
   <p>
   <wiki:Plugin plugin="Groups" />
   </p>
   </div>
</wiki:Tab>


<wiki:Tab id="editors" title="Editors">
   <wiki:TabbedSection defaultTab="${param['tab-editors']}">
     <wiki:AdminBeanIterator type="editors" id="ab">
      <wiki:Tab id="${ab.id}" title="${ab.title}">
      
      <div class="formcontainer"> 
      <form action="Admin.jsp" method="post" accept-charset="UTF-8">
         <input type="hidden" name="tab-admin" value="editors" />
         <input type="hidden" name="tab-editors" value="${ab.title}" />
         <%
         out.write( ab.doGet(ctx) );
         %>
       </form>
       </div>
      </wiki:Tab>
     </wiki:AdminBeanIterator>
   </wiki:TabbedSection>
</wiki:Tab>
<wiki:Tab id="filters" title="Filters">
<p>There will be more filter stuff here</p>
</wiki:Tab>
</wiki:TabbedSection>
</div>

</div>
</body>