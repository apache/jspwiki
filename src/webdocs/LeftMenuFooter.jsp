<%
    boolean regular = ( wiki.getSpecialPageReference( pagereq ) == null );
    if( regular )
    {
%>
   <BR><BR><BR>
   Referrers:
   <BR>
   <jspwiki:linklist action="getref" page="<%=pageurl%>" linesep="true"/>
<%  
    }
%>
   <BR><BR><BR>
   <DIV ALIGN="left" CLASS="small">
   <%=Release.APPNAME%> v<%=Release.VERSTR%>
   </DIV>

