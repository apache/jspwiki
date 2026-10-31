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

<%@page import="org.apache.wiki.api.core.Context" %>
<%@page import="org.apache.wiki.api.core.ContextEnum" %>
<%@page import="org.apache.wiki.api.core.Engine" %>
<%@page import="org.apache.wiki.api.spi.Wiki" %>
<%@page import="org.apache.wiki.auth.AuthorizationManager" %>
<%@page import="org.apache.wiki.auth.NoSuchPrincipalException" %>
<%@page import="org.apache.wiki.auth.UserManager" %>
<%@page import="org.apache.wiki.http.filter.CsrfProtectionFilter" %>
<%@page import="org.apache.wiki.notification.SubscriptionManager" %>
<%@page import="org.apache.wiki.pages.PageManager" %>
<%
  // Subscribes the user to the changes of a page, or removes the subscription when action is "unsubscribe".
  final Engine wiki = Wiki.engine().find( getServletConfig() );
  final Context wikiContext = Wiki.context().create( wiki, request, ContextEnum.PAGE_VIEW.getRequestContext() );
  if( !wiki.getManager( AuthorizationManager.class ).hasAccess( wikiContext, response ) ) {
      return;
  }
  if( !CsrfProtectionFilter.isCsrfProtectedPost( request ) || !wikiContext.getWikiSession().isAuthenticated() ) {
      response.sendRedirect( "error/Forbidden.html" );
      return;
  }
  final String pageName = wikiContext.getPage().getName();
  if( !wiki.getManager( PageManager.class ).pageExists( pageName ) ) {
      response.sendRedirect( "error/Forbidden.html" );
      return;
  }

  // a subscription is of no use without an address to send the mails to; the profile is looked up by login name,
  // which is also the key of the subscriptions
  final String loginName = wikiContext.getWikiSession().getLoginPrincipal().getName();
  String email = null;
  try {
      email = wiki.getManager( UserManager.class ).getUserDatabase().findByLoginName( loginName ).getEmail();
  } catch( final NoSuchPrincipalException e ) {
      // not a user of the wiki user database, so there is no profile to keep the subscription in
  }
  if( email == null || email.trim().isEmpty() ) {
      response.sendRedirect( wiki.getURL( ContextEnum.WIKI_PREFS.getRequestContext(), null, null ) );
      return;
  }

  final SubscriptionManager subscriptions = wiki.getManager( SubscriptionManager.class );
  if( "unsubscribe".equals( request.getParameter( "action" ) ) ) {
      subscriptions.unsubscribe( loginName, pageName );
  } else {
      subscriptions.subscribe( loginName, pageName );
  }
  response.sendRedirect( wikiContext.getViewURL( pageName ) );
%>
