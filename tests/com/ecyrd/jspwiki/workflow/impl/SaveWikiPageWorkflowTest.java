package com.ecyrd.jspwiki.workflow.impl;

import java.security.Principal;
import java.util.Properties;

import junit.framework.TestCase;

import com.ecyrd.jspwiki.TestEngine;
import com.ecyrd.jspwiki.WikiException;
import com.ecyrd.jspwiki.auth.GroupPrincipal;
import com.ecyrd.jspwiki.auth.WikiPrincipal;
import com.ecyrd.jspwiki.workflow.*;

public class SaveWikiPageWorkflowTest extends TestCase
{
    TestEngine m_engine;
    WorkflowManager wm;
    Workflow w;
    DecisionQueue dq;
    Principal admin;

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
        
        // Second, we will see a Decision sitting in the DecisionQueue...
        assertEquals(1, dq.decisions().length);
        
        // ...and it should be intended for the GroupPrincipal Admin
        Decision[] decisions = dq.actorDecisions(admin);
        assertEquals(1, dq.decisions().length);
        
        // Now, approve the decision and it should go away, and page should apppear.
        decisions[0].decide(Outcome.DECISION_APPROVE);
        assertTrue( m_engine.pageExists(pageName));
        decisions = dq.decisions();
        assertEquals(0, decisions.length);
    }
    
    public void testSaveWithRejection() throws WikiException {
        // Create a sample test page and try to save it
        String pageName = "SaveWikiPageWorkflow-Test" + System.currentTimeMillis();
        String text = "This is a test!";
        m_engine.saveText(pageName, text);
        
        // How do we know the workflow works? Well, first of all the page shouldn't exist yet...
        assertFalse( m_engine.pageExists(pageName));
        
        // Second, we will see a Decision sitting in the DecisionQueue...
        assertEquals(1, dq.decisions().length);
        
        // ...and it should be intended for the GroupPrincipal Admin
        Decision[] decisions = dq.actorDecisions(admin);
        assertEquals(1, dq.decisions().length);
        
        // Now, DENY the decision and the page should still not exist...
        decisions[0].decide(Outcome.DECISION_DENY);
        assertFalse( m_engine.pageExists(pageName));
        
        // ...but there should also be a notification decision in the queue
        decisions = dq.decisions();
        assertEquals(1, dq.decisions().length);
        assertEquals(SaveWikiPageWorkflow.EDIT_REJECT, decisions[0].getMessageKey());
        
        // ...and it should be intended for Guest
        decisions = dq.actorDecisions(WikiPrincipal.GUEST);
        assertEquals(1, decisions.length);
        decisions[0].decide(Outcome.DECISION_ACKNOWLEDGE);
        
        // Once Guest disposes of the notification, the queue should be empty
        decisions = dq.decisions();
        assertEquals(0, dq.decisions().length);
    }
}
