package com.ecyrd.jspwiki.workflow;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiException;

/**
 * Factory class that creates common Workflow instances such as a standard approval workflow.
 * @author Andrew Jaquith
 *
 */
public class WorkflowBuilder
{
    private static final Map c_builders = new HashMap();
    private final WikiEngine m_engine;
    
    /**
     * Private constructor that creates a new WorkflowBuilder for the supplied WikiEngine.
     * @param engine the wiki engine
     */
    private WorkflowBuilder( WikiEngine engine ) {
        m_engine = engine;
    }
    
    /**
     * Returns the WorkflowBuilder instance for a WikiEngine. Only one WorkflowBuilder
     * exists for a given engine.
     * @param engine the wiki engine
     * @return the workflow builder
     */
    public static WorkflowBuilder getBuilder( WikiEngine engine )
    {
        WorkflowBuilder builder = (WorkflowBuilder)c_builders.get( engine );
        if ( builder == null ) {
            builder = new WorkflowBuilder( engine );
            c_builders.put( engine, builder );
        }
        return builder;
    }
    
    /**
     * <p>Builds an approval workflow that requests approval from a named 
     * user, {@link com.ecyrd.jspwiki.auth.authorize.Group} or
     * {@link com.ecyrd.jspwiki.auth.authorize.Role} before running a Task.</p>
     * <p>The Principal who approves the activity is determined by looking up 
     * the property <code>jspwiki.approver.<var>workflowApproverKey</var></code>
     * in <code>jspwiki.properties</code>. If that Principal resolves to a known user, Group
     * Role, a Decision will be placed in the respective workflow queue (or multiple queues,
     * if necessary). Only one approver needs to make the Decision, and if the request is
     * approved, the completion task will be executed. If the request is denied, a 
     * {@link SimpleNotification} with a message corresponding to the <code>rejectedMessage</code>
     * message key will be placed in the submitter's workflow queue.</p>
     * <p>To help approvers determine how to make the Decision, callers can supply an
     * array of Fact objects to this method, which will be added to the Decision in the order
     * they appear in the array. These items will be displayed in the web UI.
     * In addition, the value of the first Fact will also be added as the third message
     * argument for the workflow (the first two are always the submitter and the approver). 
     * For example, the PageManager code that creates the "save page approval" workflow 
     * adds the name of the page as its first Fact; this results in the page name being
     * substituted correctly into the resulting message: 
     * "Save wiki page &lt;strong&gt;{2}&lt;/strong&gt;".</p>
     * @param submitter the user submitting the request
     * @param workflowApproverKey the key that names the user, Group or Role who must approve 
     * the request. The key is looked up in <code>jspwiki.properties</code>, and is derived
     * by prepending <code>jspwiki.approver</code> to the value of <code>workflowApproverKey</code>
     * @param prepTask the initial task that should run before the Decision step is processed.
     * If this parameter is <code>null</code>, the Decision will run as the first Step instead
     * @param decisionKey the message key in <code>default.properties</code> that contains
     * the text that will appear in approvers' workflow queues indicating they need to make
     * a Decision; for example, <code>decision.saveWikiPage</code>. In the i18n message bundle
     * file, this key might return text that reads "Approve page &lt;strong&gt;{2}&lt;/strong&gt;"
     * @param facts an array of {@link Fact} objects that will be shown to the approver
     * to aid decision-making. The facts will be displayed in the order supplied in the array
     * @param completionTask the Task that will run if the Decision is approved
     * @param rejectedMessageKey the message key in <code>default.properties</code> that contains
     * the text that will appear in the submitter's workflow queue if request was
     * not approved; for example, <code>notification.saveWikiPage.reject</code>. In the 
     * i18n message bundle file, this key might might return
     * text that reads "Your request to save page &lt;strong&gt;{2}&lt;/strong&gt; was rejected."
     * If this parameter is <code>null</code>, no message will be sent
     * @return the created workflow
     * @throws WikiException if the name of the approving user, Role or Group cannot be determined
     */
    public Workflow buildApprovalWorkflow( Principal submitter, 
                                           String workflowApproverKey, 
                                           Task prepTask, 
                                           String decisionKey,
                                           Fact[] facts,
                                           Task completionTask,
                                           String rejectedMessageKey ) throws WikiException
    {
        WorkflowManager mgr = m_engine.getWorkflowManager();
        Workflow workflow = new Workflow( workflowApproverKey, submitter );

        // Is a Decision required to run the approve task?
        boolean decisionRequired = mgr.requiresApproval( workflowApproverKey );
        
        // If Decision required, create a simple approval workflow 
        if ( decisionRequired )
        {
            // Look up the name of the approver (user or group) listed in jspwiki.properties;
            // approvals go to the approver's decision cue
            Principal approverPrincipal = mgr.getApprover( workflowApproverKey );
            Decision decision = new SimpleDecision( workflow, decisionKey, approverPrincipal );
            
            // Add facts to the Decision, if any were supplied
            if ( facts != null ) {
                for ( int i = 0; i < facts.length; i++ )
                {
                    decision.addFact( facts[i] );
                }
                // Add the first one as a message key
                if ( facts.length > 0 )
                {
                    workflow.addMessageArgument( facts[0].getValue() );
                }
            }
            
            // If rejected, sent a notification
            if ( rejectedMessageKey != null ) 
            {
                SimpleNotification rejectNotification = new SimpleNotification( workflow, rejectedMessageKey, submitter );
                decision.addSuccessor( Outcome.DECISION_DENY, rejectNotification );
            }
            
            // If approved, run the 'approved' task
            decision.addSuccessor( Outcome.DECISION_APPROVE, completionTask );
            
            // Set the first step
            if ( prepTask == null ) {
                workflow.setFirstStep( decision );
            }
            else
            {
                workflow.setFirstStep( prepTask );
                prepTask.addSuccessor( Outcome.STEP_COMPLETE, decision );
            }
        }
        
        // If Decision not required, just run the prep + approved tasks in succession
        else
        {
            // Set the first step
            if ( prepTask == null ) {
                workflow.setFirstStep( completionTask );
            }
            else
            {
                workflow.setFirstStep( prepTask );
                prepTask.addSuccessor( Outcome.STEP_COMPLETE, completionTask );
            }
        }

        // Make sure our tasks have this workflow as the parent, then return
        if ( prepTask != null ) 
        {
            prepTask.setWorkflow( workflow );
        }
        completionTask.setWorkflow( workflow );
        return workflow;
    }

}
