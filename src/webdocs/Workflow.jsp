<%@ page import="java.util.Collection" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="com.ecyrd.jspwiki.log.Logger" %>
<%@ page import="com.ecyrd.jspwiki.log.LoggerFactory" %>
<%@ page import="com.ecyrd.jspwiki.WikiContext" %>
<%@ page import="com.ecyrd.jspwiki.WikiSession" %>
<%@ page import="com.ecyrd.jspwiki.WikiEngine" %>
<%@ page import="com.ecyrd.jspwiki.workflow.Decision" %>
<%@ page import="com.ecyrd.jspwiki.workflow.DecisionQueue" %>
<%@ page import="com.ecyrd.jspwiki.workflow.NoSuchOutcomeException" %>
<%@ page import="com.ecyrd.jspwiki.workflow.Outcome" %>
<%@ page import="com.ecyrd.jspwiki.workflow.Workflow" %>
<%@ page errorPage="/Error.jsp" %>
<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="http://stripes.sourceforge.net/stripes.tld" prefix="stripes" %>
<stripes:useActionBean beanclass="com.ecyrd.jspwiki.action.WorkflowActionBean" event="view" />

<%! 
    Logger log = LoggerFactory.getLogger("JSPWiki"); 
%>

<%
    WikiEngine wiki = WikiEngine.getInstance( getServletConfig() );
    // Create wiki context and check for authorization
    WikiContext wikiContext = wiki.createContext( request, WikiContext.WORKFLOW );
    
    // Extract the wiki session
    WikiSession wikiSession = wikiContext.getWikiSession();
    
    // Get the current decisions
    DecisionQueue dq = wiki.getWorkflowManager().getDecisionQueue();

    if( "decide".equals(request.getParameter("action")) )
    {
        try
        {
          // Extract parameters for decision ID & decision outcome
          int id = Integer.parseInt( request.getParameter( "id" ) );
          String outcomeKey = request.getParameter("outcome");
          Outcome outcome = Outcome.forName( outcomeKey );
          // Iterate through our actor decisions and see if we can find an ID match
          Collection decisions = dq.getActorDecisions(wikiSession);
          for (Iterator it = decisions.iterator(); it.hasNext();)
          {
            Decision d = (Decision)it.next();
            if (d.getId() == id)
            {
              // Cool, we found it. Now make the decision.
              dq.decide(d, outcome);
            }
          }
        }
        catch ( NumberFormatException e )
        {
           log.warn("Could not parse integer from parameter 'decision'. Somebody is being naughty.");
        }
        catch ( NoSuchOutcomeException e )
        {
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
          Collection workflows = wiki.getWorkflowManager().getOwnerWorkflows(wikiSession);
          for (Iterator it = workflows.iterator(); it.hasNext();)
          {
            Workflow w = (Workflow)it.next();
            if (w.getId() == id)
            {
              // Cool, we found it. Now kill the workflow.
              w.abort();
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
    request.setAttribute("workflows",   wiki.getWorkflowManager().getOwnerWorkflows(wikiSession));
    request.setAttribute("wikiSession", wikiSession);
    
    response.setContentType("text/html; charset="+wiki.getContentEncoding() );
    String contentPage = wiki.getTemplateManager().findJSP( pageContext,
                                                            wikiContext.getTemplate(),
                                                            "ViewTemplate.jsp" );
%><wiki:Include page="<%=contentPage%>" />

