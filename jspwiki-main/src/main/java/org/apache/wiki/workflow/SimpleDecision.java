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

import java.security.Principal;
import java.util.Map;


/**
 * Decision subclass that includes two available Outcomes: {@link Outcome#DECISION_APPROVE} or {@link Outcome#DECISION_DENY}.
 * The Decision is reassignable, and the default Outcome is {@link Outcome#DECISION_APPROVE}.
 */
public class SimpleDecision extends Decision {

    private static final long serialVersionUID = 8192213077644617341L;

    /**
     * Constructs a new SimpleDecision assigned to a specified actor.
     *
     * @param workflowId the parent workflow id to set
     * @param workflowContext the parent workflow context to set
     * @param messageKey the message key that describes the Decision, which will be presented in the UI
     * @param actor the Principal (<em>e.g.</em>, WikiPrincipal, GroupPrincipal, Role) who will decide
     */
    public SimpleDecision( final int workflowId, final Map< String, Object > workflowContext, final String messageKey, final Principal actor ) {
        super( workflowId, workflowContext, messageKey, actor, Outcome.DECISION_APPROVE );

        // Add the other default outcomes
        super.addSuccessor( Outcome.DECISION_DENY, null );
    }

}
