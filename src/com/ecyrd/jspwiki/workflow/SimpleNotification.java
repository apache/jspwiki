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

import com.ecyrd.jspwiki.WikiException;

/**
 * Decision subclass used for notifications that includes only one available Outcome:
 * {@link Outcome#DECISION_ACKNOWLEDGE}. The Decision is not reassignable, and
 * the default Outcome is {@link Outcome#DECISION_ACKNOWLEDGE}.
 * 
 * @author Andrew Jaquith
 * @since 2.5
 */
public final class SimpleNotification extends Decision
{

    /**
     * Constructs a new SimpleNotification object with a supplied message key,
     * associated Workflow, and named actor who must acknowledge the message.
     * The notification is placed in the Principal's list of queued Decisions.
     * Because the only available Outcome is
     * {@link Outcome#DECISION_ACKNOWLEDGE}, the actor can only acknowledge the
     * message.
     * 
     * @param workflow
     *            the Workflow to associate this notification with
     * @param messageKey
     *            the message key
     * @param actor
     *            the Principal who will acknowledge the message
     */
    public SimpleNotification(Workflow workflow, String messageKey, Principal actor)
    {
        super(workflow, messageKey, actor, Outcome.DECISION_ACKNOWLEDGE);
    }
    
    /**
     * Convenience method that simply calls {@link #decide(Outcome)} 
     * with the value {@link Outcome#DECISION_ACKNOWLEDGE}.
     * @throws WikiException never
     */
    public void acknowledge() throws WikiException
    {
        this.decide( Outcome.DECISION_ACKNOWLEDGE );
    }

    /**
     * Notifications cannot be re-assigned, so this method always returns
     * <code>false</code>.
     * @return <code>false</code> always
     */
    public final boolean isReassignable()
    {
        return false;
    }

}
