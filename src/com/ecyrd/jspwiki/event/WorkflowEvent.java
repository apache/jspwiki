/*
 JSPWiki - a JSP-based WikiWiki clone.

 Copyright (C) 2001-2006 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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

package com.ecyrd.jspwiki.event;

import com.ecyrd.jspwiki.workflow.Workflow;

/**
 * <p>
 * WorkflowEvent indicates that a state change to a Workflow: started, running,
 * waiting, completed, aborted. These correspond exactly to the states described
 * in the {@link com.ecyrd.jspwiki.workflow.Workflow}. All events are logged
 * with priority INFO.
 * </p>
 * 
 * @author Andrew Jaquith
 * @since 2.3.79
 */
public final class WorkflowEvent extends WikiEvent
{

    private static final long serialVersionUID = 1L;
    
    /**
     * After Workflow instantiation.
     */
    public static final int CREATED = 0;

    /**
     * After the Workflow has been instantiated, but before it has been started
     * using the {@link com.ecyrd.jspwiki.workflow.Workflow#start()} method.
     */
    public static final int STARTED = 10;

    /**
     * fter the Workflow has been started (or re-started) using the
     * {@link com.ecyrd.jspwiki.workflow.Workflow#start()} method, 
     * but before it has finished processing all Steps.
     */
    public static final int RUNNING = 20;

    /**
     * When the Workflow has temporarily paused, for example because of a
     * pending Decision.
     */
    public static final int WAITING = 30;

    /** After the Workflow has finished processing all Steps, without errors. */
    public static final int COMPLETED = 40;

    /** If a Step has elected to abort the Workflow. */
    public static final int ABORTED = 50;

    /**
     * Constructs a new instance of this event type, which signals a security
     * event has occurred. The <code>source</code> parameter is required, and
     * may not be <code>null</code>. When the WikiSecurityEvent is
     * constructed, the security logger {@link WikiSecurityEvent#log} is notified.
     * 
     * @param source
     *            the source of the event, which can be any object: a wiki page,
     *            group or authentication/authentication/group manager.
     * @param type
     *            the type of event
     */
    public WorkflowEvent(Object source, int type)
    {
        super(source, type);
        if (source == null)
        {
            throw new IllegalArgumentException("Argument(s) cannot be null.");
        }
    }

    /**
     * Convenience method that returns the Workflow to which the event applied.
     * 
     * @return the Workflow
     */
    public final Workflow getWorkflow()
    {
        return (Workflow) super.getSource();
    }

    /**
     * Prints a String (human-readable) representation of this object.
     * 
     * @see java.lang.Object#toString()
     */
    public final String toString()
    {
        StringBuffer msg = new StringBuffer();
        msg.append("WorkflowEvent.");
        msg.append(eventName(getType()));
        msg.append(" [source=" + getSource().toString());
        msg.append("]");
        return msg.toString();
    }

    /**
     * Returns a textual representation of an event type.
     * 
     * @param type
     *            the type
     * @return the string representation
     */
    public final String eventName(int type)
    {
        switch (type)
        {
            case CREATED:
                return "CREATED";
            case ABORTED:
                return "ABORTED";
            case COMPLETED:
                return "COMPLETED";
            case RUNNING:
                return "RUNNING";
            case STARTED:
                return "STARTED";
            case WAITING:
                return "WAITING";
            default:
                return super.eventName();
        }
    }

}
