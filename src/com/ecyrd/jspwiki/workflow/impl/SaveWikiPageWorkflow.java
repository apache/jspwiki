package com.ecyrd.jspwiki.workflow.impl;

import java.security.Principal;

import com.ecyrd.jspwiki.*;
import com.ecyrd.jspwiki.diff.DifferenceManager;
import com.ecyrd.jspwiki.workflow.*;

/**
 * Workflow steps for saving a wiki page, including intermediate approvals if required by the wiki.
 * @author Andreq Jaquith
 *
 */
public class SaveWikiPageWorkflow extends Workflow
{

    public static final String EDIT_WORKFLOW = "workflow.saveWikiPage";
    public static final String EDIT_REJECT = "notification.saveWikiPage.reject";
    public static final String FACT_DIFF_TEXT = "fact.diffText";
    public static final String FACT_CURRENT_TEXT = "fact.currentText";
    public static final String FACT_PROPOSED_TEXT = "fact.proposedText";
    public static final String ATTR_WIKI_CONTEXT = "wikiContext";
    public static final String EDIT_DECISION = "decision.saveWikiPage";

    /**
     * Inner class that handles the page pre-save actions. If the proposed page
     * text is the same as the current version, the {@link #execute()} method
     * returns <code>false</code>.
     * 
     * @author Andrew Jaquith
     */
    public static class PreSaveWikiPageTask extends Task
    {
        public static final String MESSAGE_KEY = "task.preSaveWikiPage";

        public PreSaveWikiPageTask(Workflow workflow)
        {
            super(workflow, MESSAGE_KEY);
        }

        public Outcome execute() throws WikiException
        {
            // Retrieve attributes
            WikiContext context = (WikiContext) getWorkflow().getAttribute(SaveWikiPageWorkflow.ATTR_WIKI_CONTEXT);
            String proposedText = (String) getWorkflow().getAttribute(SaveWikiPageWorkflow.FACT_PROPOSED_TEXT);
            WikiEngine engine = context.getEngine();
            Workflow workflow = getWorkflow();
            WorkflowManager mgr = workflow.getWorkflowManager();

            // Get the wiki page
            WikiPage page = context.getPage();

            // Figure out who the author was. Prefer the author
            // set programmatically; otherwise get from the
            // current logged in user
            if (page.getAuthor() == null)
            {
                Principal wup = context.getCurrentUser();

                if (wup != null)
                    page.setAuthor(wup.getName());
            }

            // Prepare text for saving
            proposedText = TextUtil.normalizePostData(proposedText);
            proposedText = engine.getFilterManager().doPreSaveFiltering(context, proposedText);

            // Check if page data actually changed; ABORT if not
            String oldText = engine.getPureText(page);
            if (oldText != null && oldText.equals(proposedText))
            {
                return Outcome.STEP_ABORT;
            }
            
            // Save old and new text for the successor steps
            workflow.setAttribute(FACT_CURRENT_TEXT, oldText);
            workflow.setAttribute(FACT_PROPOSED_TEXT, proposedText);

            // Figure out what our next Step should be: save page, or get approval?
            Step saveTask = new SaveWikiPageTask(workflow);
            boolean decisionRequired = mgr.requiresApproval(workflow.getMessageKey());
            if (decisionRequired)
            {
                // Approvals go to the actor's decision cue
                Principal actor = mgr.getApprover(EDIT_WORKFLOW);
                Decision decision = new SimpleDecision(workflow, EDIT_DECISION, actor);
                addSuccessor(Outcome.STEP_COMPLETE, decision);
                
                // Add the diffed, proposed and old text versions as Facts
                DifferenceManager differ = engine.getDifferenceManager();
                String diffText = differ.makeDiff(oldText, proposedText);
                decision.addSuccessor(Outcome.DECISION_APPROVE, saveTask);
                decision.addFact(new Fact(FACT_DIFF_TEXT, diffText));
                decision.addFact(new Fact(FACT_PROPOSED_TEXT, proposedText));
                decision.addFact(new Fact(FACT_CURRENT_TEXT, oldText));
                
                // If the approval is rejected, sent a notification
                Step reject = new SimpleNotification(workflow, EDIT_REJECT, context.getCurrentUser());
                decision.addSuccessor(Outcome.DECISION_DENY, reject);
            }
            else
            {
                addSuccessor(Outcome.STEP_COMPLETE, saveTask);
            }

            return Outcome.STEP_COMPLETE;
        }
    }

    /**
     * Inner class that handles the actual page save and post-save actions.
     * 
     * @author Andrew Jaquith
     */
    public static class SaveWikiPageTask extends Task
    {
        public static final String MESSAGE_KEY = "task.saveWikiPage";

        public SaveWikiPageTask(Workflow workflow)
        {
            super(workflow, MESSAGE_KEY);
        }

        public Outcome execute() throws WikiException
        {
            // Retrieve attributes
            WikiContext context = (WikiContext) getWorkflow().getAttribute(SaveWikiPageWorkflow.ATTR_WIKI_CONTEXT);
            String proposedText = (String) getWorkflow().getAttribute(SaveWikiPageWorkflow.FACT_PROPOSED_TEXT);

            WikiEngine engine = context.getEngine();
            WikiPage page = context.getPage();

            // Let the rest of the engine handle actual saving.
            engine.getPageManager().putPageText(page, proposedText);

            // Refresh the context for post save filtering.
            page = engine.getPage(page.getName());
            context.setPage(page);
            engine.textToHTML(context, proposedText);
            engine.getFilterManager().doPostSaveFiltering(context, proposedText);

            return Outcome.STEP_COMPLETE;
        }
    }

    public SaveWikiPageWorkflow(WikiContext context, String proposedText) throws WikiException
    {
        // Create workflow and stash attributes we'll need later; owner is current user
        super(EDIT_WORKFLOW, context.getCurrentUser());
        setWorkflowManager(context.getEngine().getWorkflowManager());
        addMessageArgument(context.getPage().getName());
        setAttribute(ATTR_WIKI_CONTEXT, context);
        setAttribute(FACT_PROPOSED_TEXT, proposedText);

        // Create pre-save task
        Step preSaveTask = new PreSaveWikiPageTask(this);
        this.setFirstStep(preSaveTask);
    }
}
