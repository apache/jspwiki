<%-- Provides a simple searchbox that can be easily included anywhere
     on the page --%>

<DIV class="searchbox">
  <FORM action="<wiki:Variable var="jspwiki.baseURL"/>Search.jsp"
      ACCEPT-CHARSET="ISO-8859-1,UTF-8">
    <wiki:LinkTo page="FindPage">Search Wiki:</wiki:LinkTo>
    <INPUT type="text" name="query" size="15">
    <INPUT type="submit" name="ok" value="Find!">
  </FORM>
</DIV>

