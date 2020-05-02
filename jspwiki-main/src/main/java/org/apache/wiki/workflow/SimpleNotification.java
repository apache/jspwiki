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

import org.apache.wiki.api.exceptions.WikiException;

import java.io.Serializable;
import java.security.Principal;
import java.util.Map;

/**
 * Decision subclass used for notifications that includes only one available Outcome: {@link Outcome#DECISION_ACKNOWLEDGE}. The Decision is
 * not reassignable, and the default Outcome is {@link Outcome#DECISION_ACKNOWLEDGE}.
 * 
 * @since 2.5
 */
public final class SimpleNotification extends Decision {

    private static final long serialVersionUID = -3392947495169819527L;

    /**
     * Constructs a new SimpleNotification object with a supplied message key, associated Workflow, and named actor who must acknowledge
     * the message. The notification is placed in the Principal's list of queued Decisions. Because the only available Outcome is
     * {@link Outcome#DECISION_ACKNOWLEDGE}, the actor can only acknowledge the message.
     * 
     * @param workflowId the parent workflow id to set
     * @param workflowContext the parent workflow context to set
     * @param messageKey the message key
     * @param actor the Principal who will acknowledge the message
     */
    public SimpleNotification( final int workflowId, final Map< String, Serializable > workflowContext, final String messageKey, final Principal actor ) {
        super( workflowId, workflowContext, messageKey, actor, Outcome.DECISION_ACKNOWLEDGE );
    }
    
    /**
     * Convenience method that simply calls {@link #decide(Outcome)} with the value {@link Outcome#DECISION_ACKNOWLEDGE}.
     *
     * @throws WikiException never
     */
    public void acknowledge() throws WikiException {
        this.decide( Outcome.DECISION_ACKNOWLEDGE  );
    }

    /**
     * Notifications cannot be re-assigned, so this method always returns <code>false</code>.
     *
     * @return <code>false</code> always.
     */
    public boolean isReassignable() {
        return false;
    }

}
