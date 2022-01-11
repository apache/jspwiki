/* 
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
 */
package org.apache.wiki.workflow;

import org.apache.wiki.api.core.Session;
import org.apache.wiki.api.engine.Initializable;
import org.apache.wiki.api.exceptions.WikiException;
import org.apache.wiki.event.WikiEventListener;

import java.security.Principal;
import java.util.List;
import java.util.Set;


/**
 * <p>
 * Monitor class that tracks running Workflows. The WorkflowManager also keeps track of the names of users or groups expected to approve
 * particular Workflows.
 * </p>
 */
public interface WorkflowManager extends WikiEventListener, Initializable {

    /** The name of the key from jspwiki.properties which defines who shall approve the workflow of storing a wikipage.  Value is <tt>{@value}</tt> */
    String WF_WP_SAVE_APPROVER = "workflow.saveWikiPage";
    /** The message key for storing the Decision text for saving a page.  Value is {@value}. */
    String WF_WP_SAVE_DECISION_MESSAGE_KEY = "decision.saveWikiPage";
    /** The message key for rejecting the decision to save the page.  Value is {@value}. */
    String WF_WP_SAVE_REJECT_MESSAGE_KEY = "notification.saveWikiPage.reject";
    /** Fact name for storing the page name.  Value is {@value}. */
    String WF_WP_SAVE_FACT_PAGE_NAME = "fact.pageName";
    /** Fact name for storing a diff text. Value is {@value}. */
    String WF_WP_SAVE_FACT_DIFF_TEXT = "fact.diffText";
    /** Fact name for storing the current text.  Value is {@value}. */
    String WF_WP_SAVE_FACT_CURRENT_TEXT = "fact.currentText";
    /** Fact name for storing the proposed (edited) text.  Value is {@value}. */
    String WF_WP_SAVE_FACT_PROPOSED_TEXT = "fact.proposedText";
    /** Fact name for storing whether the user is authenticated or not.  Value is {@value}. */
    String WF_WP_SAVE_FACT_IS_AUTHENTICATED = "fact.isAuthenticated";

    /** The workflow attribute which stores the user profile. */
    String WF_UP_CREATE_SAVE_ATTR_SAVED_PROFILE = "userProfile";
    /** The name of the key from jspwiki.properties which defines who shall approve the workflow of creating a user profile.  Value is <tt>{@value}</tt> */
    String WF_UP_CREATE_SAVE_APPROVER = "workflow.createUserProfile";
    /** The message key for storing the Decision text for saving a user profile.  Value is {@value}. */
    String WF_UP_CREATE_SAVE_DECISION_MESSAGE_KEY = "decision.createUserProfile";
    /** Fact name for storing a the submitter name. Value is {@value}. */
    String WF_UP_CREATE_SAVE_FACT_SUBMITTER = "fact.submitter";
    /** Fact name for storing the preferences' login name. Value is {@value}. */
    String WF_UP_CREATE_SAVE_FACT_PREFS_LOGIN_NAME = "prefs.loginname";
    /** Fact name for storing the preferences' full name. Value is {@value}. */
    String WF_UP_CREATE_SAVE_FACT_PREFS_FULL_NAME = "prefs.fullname";
    /** Fact name for storing the preferences' email. Value is {@value}. */
    String WF_UP_CREATE_SAVE_FACT_PREFS_EMAIL = "prefs.email";

    /** The prefix to use for looking up <code>jspwiki.properties</code> approval roles. */
    String PROPERTY_APPROVER_PREFIX = "jspwiki.approver.";

    /**
     * Returns a collection of the currently active workflows.
     *
     * @return the current workflows
     */
    Set< Workflow > getWorkflows();

    /**
     * Returns a collection of finished workflows; that is, those that have aborted or completed.
     *
     * @return the finished workflows
     */
    List< Workflow > getCompletedWorkflows();

    /**
     * Returns <code>true</code> if a workflow matching a particular key contains an approval step.
     *
     * @param messageKey the name of the workflow; corresponds to the value returned by {@link Workflow#getMessageKey()}.
     * @return the result
     */
    boolean requiresApproval( String messageKey );

    /**
     * Looks up and resolves the actor who approves a Decision for a particular Workflow, based on the Workflow's message key.
     * If not found, or if Principal is Unresolved, throws WikiException. This particular implementation always returns the
     * GroupPrincipal <code>Admin</code>
     *
     * @param messageKey the Decision's message key
     * @return the actor who approves Decisions
     * @throws WikiException if the message key was not found, or the
     * Principal value corresponding to the key could not be resolved
     */
    Principal getApprover( String messageKey ) throws WikiException;

    /**
     * Returns the DecisionQueue associated with this WorkflowManager
     *
     * @return the decision queue
     */
    DecisionQueue getDecisionQueue();

    /**
     * Returns the current workflows a wiki session owns. These are workflows whose {@link Workflow#getOwner()} method returns a Principal
     * also possessed by the wiki session (see {@link org.apache.wiki.api.core.Session#getPrincipals()}). If the wiki session is not
     * authenticated, this method returns an empty Collection.
     *
     * @param session the wiki session
     * @return the collection workflows the wiki session owns, which may be empty
     */
    List< Workflow > getOwnerWorkflows( Session session );

}
