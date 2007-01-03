package com.ecyrd.jspwiki.workflow.impl;

import java.security.Principal;

import com.ecyrd.jspwiki.*;
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
    public static final String ATTR_CURRENT_TEXT = "currentText";
    public static final String ATTR_PROPOSED_TEXT = "proposedText";
    public static final String ATTR_WIKI_CONTEXT = "wikiContext";
    public static final String EDIT_DECISION = "decision.editWikiApproval";

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
            String proposedText = (String) getWorkflow().getAttribute(SaveWikiPageWorkflow.ATTR_PROPOSED_TEXT);
            WikiEngine engine = context.getEngine();

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
            
            // Save old and new text for the approver
            getWorkflow().setAttribute(ATTR_CURRENT_TEXT, oldText);
            getWorkflow().setAttribute(ATTR_PROPOSED_TEXT, proposedText);
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
            String proposedText = (String) getWorkflow().getAttribute(SaveWikiPageWorkflow.ATTR_PROPOSED_TEXT);

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
        this.setWorkflowManager(context.getEngine().getWorkflowManager());
        setAttribute(ATTR_WIKI_CONTEXT, context);
        setAttribute(ATTR_PROPOSED_TEXT, proposedText);

        // Create pre-save task
        Step preSaveTask = new PreSaveWikiPageTask(this);

        // Create save/post-save task
        Step saveTask = new SaveWikiPageTask(this);

        // Create an intermediate decision step if we need to
        WorkflowManager mgr = getWorkflowManager();
        boolean decisionRequired = mgr.requiresApproval(getMessageKey());
        if (decisionRequired)
        {
            // Approvals simply go to the actor's decision cue
            Principal actor = mgr.getApprover(EDIT_WORKFLOW);
            Step decision = new SimpleDecision(this, EDIT_DECISION, actor);
            preSaveTask.addSuccessor(Outcome.STEP_COMPLETE, decision);
            decision.addSuccessor(Outcome.DECISION_APPROVE, saveTask);
            
            // If the approval is rejected, sent a notification
            Step reject = new SimpleDecision(this, EDIT_REJECT, context.getCurrentUser());
            decision.addSuccessor(Outcome.DECISION_DENY, reject);
        }
        else
        {
            preSaveTask.addSuccessor(Outcome.STEP_COMPLETE, saveTask);
        }

        // Add to workflow
        this.setFirstStep(preSaveTask);
    }
}
