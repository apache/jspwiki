<%@ page import="com.ecyrd.jspwiki.tags.InsertDiffTag" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>

<div id="diffcontent" >

    <wiki:PageExists>
    <div class="diffnote">

    <form action="<wiki:Link format="url" jsp="Diff.jsp"/>" 
          method="post"  accept-charset="UTF-8">
       <input type="hidden" name="page" value="<wiki:Variable var='pagename' />" />     
    Difference between version
    <select id="r1" name="r1" onchange="this.form.submit();" >
    <% 
       WikiContext c = WikiContext.findContext( pageContext );
       int latestVersion = c.getPage().getVersion();;
       int ii = 0;
       ii = ((Integer)pageContext.getAttribute(InsertDiffTag.ATTR_OLDVERSION, 
                                               PageContext.REQUEST_SCOPE)).intValue();
       if( ii == WikiProvider.LATEST_VERSION ) ii = latestVersion;
       for( int i = 1; i <= latestVersion; i++) 
       {
    %> 
       <option value="<%= i %>" <%= ((i==ii) ? "selected='selected'" : "") %> ><%= i %></option>
    <%
       }    
    %>
    </select>
    and version
    <select id="r2" name="r2" onchange="this.form.submit();" >
    <% 
       ii = ((Integer)pageContext.getAttribute(InsertDiffTag.ATTR_NEWVERSION, 
                                               PageContext.REQUEST_SCOPE)).intValue();
       for( int i = 1; i <= latestVersion; i++) 
       {
    %> 
       <option value="<%= i %>" <%= ((i==ii) ? "selected='selected'" : "") %> ><%= i %></option>
    <%
       }    
    %>
    </select>
    &nbsp;&nbsp;&nbsp;&nbsp;
    <a title="Go to first change in this document" 
       href="#change-1">View first change</a>&raquo;&raquo;
    </form>
    <br />
    Back to <wiki:LinkTo><wiki:PageName/></wiki:LinkTo>, or
    <wiki:PageInfoLink><wiki:PageName/> version history</wiki:PageInfoLink>
    </div>
    <br />
    <wiki:InsertDiff>
      <i>No difference detected.</i>
    </wiki:InsertDiff> 
    </wiki:PageExists>
    
    <wiki:NoSuchPage>
    <p>
    This page does not exist.  Why don't you go and <wiki:EditLink>create it</wiki:EditLink>?
    </p>
    </wiki:NoSuchPage>

</div>
