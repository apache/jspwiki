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

<%@page import="org.apache.wiki.api.core.Engine" %>
<%@page import="org.apache.wiki.api.spi.Wiki" %>
<%@page import="org.apache.wiki.auth.AuthenticationManager" %>
<%@page import="org.apache.wiki.auth.login.CookieAssertionLoginModule" %>
<%@page import="org.apache.wiki.auth.login.CookieAuthenticationLoginModule"%>
<%
  Engine wiki = Wiki.engine().find( getServletConfig() );
  wiki.getManager( AuthenticationManager.class ).logout( request );

  // Clear the user cookie
  CookieAssertionLoginModule.clearUserCookie( response );

  // Delete the login cookie
  CookieAuthenticationLoginModule.clearLoginCookie( wiki, request, response );

  // Redirect to the webroot
  // TODO: Should redirect to a "goodbye" -page?
  response.sendRedirect(".");
%>
