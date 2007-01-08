package com.ecyrd.jspwiki.workflow.impl;

import java.security.Principal;
import java.util.Collection;
import java.util.Properties;

import junit.framework.TestCase;

import com.ecyrd.jspwiki.TestEngine;
import com.ecyrd.jspwiki.WikiException;
import com.ecyrd.jspwiki.WikiSession;
import com.ecyrd.jspwiki.auth.GroupPrincipal;
import com.ecyrd.jspwiki.workflow.*;

public class SaveWikiPageWorkflowTest extends TestCase
{
    TestEngine m_engine;
    WorkflowManager wm;
    Workflow w;
    DecisionQueue dq;
    Principal admin;
    WikiSession adminSession;
    WikiSession janneSession;

    protected void setUp() throws Exception
    {
        super.setUp();
        Properties props = new Properties();
        props.load(TestEngine.findTestProperties());
        
        // Explicitly turn on Admin approvals for page saves
        props.put("jspwiki.approver.workflow.saveWikiPage", "Admin");
        
        // Start the wiki engine
        m_engine = new TestEngine(props);
        wm = m_engine.getWorkflowManager();
        adminSession = m_engine.adminSession();
        janneSession = m_engine.janneSession();
        dq = wm.getDecisionQueue();
        admin = new GroupPrincipal(m_engine.getApplicationName(), "Admin");
    }
    
    public void testSaveWithApproval() throws WikiException {
        // Create a sample test page and try to save it
        String pageName = "SaveWikiPageWorkflow-Test" + System.currentTimeMillis();
        String text = "This is a test!";
        m_engine.saveText(pageName, text);
        
        // How do we know the workflow works? Well, first of all the page shouldn't exist yet...
        assertFalse( m_engine.pageExists(pageName));
        
        // Second, GroupPrincipal Admin should see a Decision in its queue
        Collection decisions = dq.getActorDecisions(adminSession);
        assertEquals(1, decisions.size());
        
        // Now, approve the decision and it should go away, and page should apppear.
        Decision decision = (Decision)decisions.iterator().next();
        decision.decide(Outcome.DECISION_APPROVE);
        assertTrue( m_engine.pageExists(pageName));
        decisions = dq.getActorDecisions(adminSession);
        assertEquals(0, decisions.size());
    }
    
    public void testSaveWithRejection() throws WikiException {
        // Create a sample test page and try to save it
        String pageName = "SaveWikiPageWorkflow-Test" + System.currentTimeMillis();
        String text = "This is a test!";
        m_engine.saveTextAsJanne(pageName, text);
        
        // How do we know the workflow works? Well, first of all the page shouldn't exist yet...
        assertFalse( m_engine.pageExists(pageName));
        
        // ...and there should be a Decision in GroupPrincipal Admin's queue
        Collection decisions = dq.getActorDecisions(adminSession);
        assertEquals(1, decisions.size());
        
        // Now, DENY the decision and the page should still not exist...
        Decision decision = (Decision)decisions.iterator().next();
        decision.decide(Outcome.DECISION_DENY);
        assertFalse( m_engine.pageExists(pageName) );
        
        // ...but there should also be a notification decision in Janne's queue
        decisions = dq.getActorDecisions(janneSession);
        assertEquals(1, decisions.size());
        decision = (Decision)decisions.iterator().next();
        assertEquals(SaveWikiPageWorkflow.EDIT_REJECT, decision.getMessageKey());
        
        // Once Janne disposes of the notification, his queue should be empty
        decision.decide(Outcome.DECISION_ACKNOWLEDGE);
        decisions = dq.getActorDecisions(janneSession);
        assertEquals(0, decisions.size());
    }
}
