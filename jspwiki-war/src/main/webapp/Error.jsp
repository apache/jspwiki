<%--
    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.
--%>

<%@ page isErrorPage="true" %>
<%@ page import="org.apache.log4j.*" %>
<%@ page import="org.apache.wiki.*" %>
<%@ page import="org.apache.wiki.util.FileUtil" %>
<%@ taglib uri="http://jspwiki.apache.org/tags" prefix="wiki" %>
<%!
    Logger log = Logger.getLogger("JSPWiki");
%>
<%
    WikiEngine wiki = WikiEngine.getInstance( getServletConfig() );
    WikiContext wikiContext = new WikiContext( wiki, request, WikiContext.ERROR );
    String pagereq = wikiContext.getName();

    response.setContentType("text/html; charset="+wiki.getContentEncoding() );

    String msg = "An unknown error was caught by Error.jsp";

    Throwable realcause = null;

    msg = exception.getMessage();
    if( msg == null || msg.length() == 0 )
    {
        msg = "An unknown exception "+exception.getClass().getName()+" was caught by Error.jsp.";
    }

    //
    //  This allows us to get the actual cause of the exception.
    //  Note the cast; at least Tomcat has two classes called "JspException"
    //  imported in JSP pages.
    //


    if( exception instanceof javax.servlet.jsp.JspException )
    {
        log.debug("IS JSPEXCEPTION");
        realcause = ((javax.servlet.jsp.JspException)exception).getCause();
        log.debug("REALCAUSE="+realcause);
    }

    if( realcause == null ) realcause = exception;

    log.debug("Error.jsp exception is: ",exception);


    wikiContext.getWikiSession().addMessage( msg );
%>

<!doctype html>
<html lang="<c:out value='${prefs.Language}' default='en'/>" name="top">
  <head>
    <title><wiki:Variable var="applicationname" />: ERROR Page</title>
  </head>

  <body>
   <h3>JSPWiki has detected an error</h3>

   <dl>
      <dt>Error Message</dt>
      <dd>
         <wiki:Messages div="error" />
      </dd>
      <dt>Exception</dt>
      <dd><%=realcause.getClass().getName()%></dd>
      <dt>Place where detected</dt>
      <dd><%=FileUtil.getThrowingMethod(realcause)%></dd>
   </dl>
   <p>
   If you have changed the templates, please do check them.  This error message
   may show up because of that.  If you have not changed them, and you are
   either installing JSPWiki for the first time or have changed configuration,
   then you might want to check your configuration files.  If you are absolutely sure
   that JSPWiki was running quite okay or you can't figure out what is going
   on, then by all means, come over to <a href="http://jspwiki.apache.org/">jspwiki.apache.org</a>
   and tell us.  There is more information in the log file (like the full stack trace,
   which you should add to any error report).
   </p>
   <p>
   And don't worry - it's just a computer program.  Nothing really
   serious is probably going on: at worst you can lose a few nights
   sleep.  It's not like it's the end of the world.
   </p>

  </body>
</html>
