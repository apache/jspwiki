/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2002 Janne Jalkanen (Janne.Jalkanen@iki.fi)

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation; either version 2.1 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.ecyrd.jspwiki.workflow;

import java.security.Principal;

/**
 * Decision subclass that includes two available Outcomes:
 * {@link Outcome#DECISION_APPROVE} or {@link Outcome#DECISION_DENY}.
 * The Decision is reassignable, and the default Outcome is 
 * {@link Outcome#DECISION_APPROVE}.
 * 
 * @author Andrew Jaquith
 */
public class SimpleDecision extends Decision
{

    /**
     * Constructs a new SimpleDecision assigned to a specified actor.
     * @param workflow the parent Workflow
     * @param messageKey the message key that describes the Decision, which
     * will be presented in the UI
     * @param actor the Principal (<em>e.g.</em>, WikiPrincipal,
     * GroupPrincipal, Role) who will decide
     */
    public SimpleDecision(Workflow workflow, String messageKey, Principal actor)
    {
        super(workflow, messageKey, actor, Outcome.DECISION_APPROVE);

        // Add the other default outcomes
        super.addSuccessor(Outcome.DECISION_DENY, null);
    }

}
