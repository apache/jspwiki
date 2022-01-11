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

<%@ page import="java.util.Collection" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="org.apache.logging.log4j.Logger" %>
<%@ page import="org.apache.logging.log4j.LogManager" %>
<%@ page import="org.apache.wiki.api.core.Context" %>
<%@ page import="org.apache.wiki.api.core.ContextEnum" %>
<%@ page import="org.apache.wiki.api.core.Engine" %>
<%@ page import="org.apache.wiki.api.core.Session" %>
<%@ page import="org.apache.wiki.api.spi.Wiki" %>
<%@ page import="org.apache.wiki.auth.AuthorizationManager" %>
<%@ page import="org.apache.wiki.preferences.Preferences" %>
<%@ page import="org.apache.wiki.ui.TemplateManager" %>
<%@ page import="org.apache.wiki.workflow.Decision" %>
<%@ page import="org.apache.wiki.workflow.DecisionQueue" %>
<%@ page import="org.apache.wiki.workflow.NoSuchOutcomeException" %>
<%@ page import="org.apache.wiki.workflow.Outcome" %>
<%@ page import="org.apache.wiki.workflow.Workflow" %>
<%@ page import="org.apache.wiki.workflow.WorkflowManager" %>
<%@ page errorPage="/Error.jsp" %>
<%@ taglib uri="http://jspwiki.apache.org/tags" prefix="wiki" %>
<%!
    Logger log = LogManager.getLogger("JSPWiki");
%>

<%
    Engine wiki = Wiki.engine().find( getServletConfig() );
    // Create wiki context and check for authorization
    Context wikiContext = Wiki.context().create( wiki, request, ContextEnum.WIKI_WORKFLOW.getRequestContext() );
    if(!wiki.getManager( AuthorizationManager.class ).hasAccess( wikiContext, response )) return;
    
    // Extract the wiki session
    Session wikiSession = wikiContext.getWikiSession();
    
    // Get the current decisions
    DecisionQueue dq = wiki.getManager( WorkflowManager.class ).getDecisionQueue();

    if( "decide".equals(request.getParameter("action")) )
    {
        try
        {
          // Extract parameters for decision ID & decision outcome
          int id = Integer.parseInt( request.getParameter( "id" ) );
          String outcomeKey = request.getParameter("outcome");
          Outcome outcome = Outcome.forName( outcomeKey );
          // Iterate through our actor decisions and see if we can find an ID match
          Collection< Decision > decisions = dq.getActorDecisions(wikiSession);
          for (Iterator< Decision > it = decisions.iterator(); it.hasNext();) {
            Decision d = it.next();
            if( d.getId() == id ) {
              // Cool, we found it. Now make the decision.
              dq.decide( d, outcome, wikiContext );
            }
          }
        } catch ( NumberFormatException e ) {
           log.warn("Could not parse integer from parameter 'decision'. Somebody is being naughty.");
        } catch ( NoSuchOutcomeException e ) {
           log.warn("Could not look up Outcome from parameter 'outcome'. Somebody is being naughty.");
        }
    }
    if( "abort".equals(request.getParameter("action")) )
    {
        try
        {
          // Extract parameters for decision ID & decision outcome
          int id = Integer.parseInt( request.getParameter( "id" ) );
          // Iterate through our owner decisions and see if we can find an ID match
          Collection< Workflow > workflows = wiki.getManager( WorkflowManager.class ).getOwnerWorkflows(wikiSession);
          for (Iterator< Workflow > it = workflows.iterator(); it.hasNext();)
          {
            Workflow w = it.next();
            if (w.getId() == id)
            {
              // Cool, we found it. Now kill the workflow.
              w.abort( wikiContext );
            }
          }
        }
        catch ( NumberFormatException e )
        {
           log.warn("Could not parse integer from parameter 'decision'. Somebody is being naughty.");
        }
    }
    
    // Stash the current decisions/workflows
    request.setAttribute("decisions",   dq.getActorDecisions(wikiSession));
    request.setAttribute("workflows",   wiki.getManager( WorkflowManager.class ).getOwnerWorkflows( wikiSession ) );
    request.setAttribute("wikiSession", wikiSession);
    
    response.setContentType("text/html; charset="+wiki.getContentEncoding() );
    String contentPage = wiki.getManager( TemplateManager.class ).findJSP( pageContext, wikiContext.getTemplate(), "ViewTemplate.jsp" );
%><wiki:Include page="<%=contentPage%>" />

