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

package org.apache.wiki.event;


/**
 * <p>
 * WorkflowEvent indicates that a state change to a Workflow: started, running, waiting, completed, aborted. 
 * These correspond exactly to the states described in the {@link org.apache.wiki.workflow.Workflow}. All events 
 * are logged with priority INFO.
 * </p>
 * 
 * @since 2.3.79
 */
public final class WorkflowEvent extends WikiEvent {

    private static final long serialVersionUID = 1L;
    
    /** After Workflow instantiation. */
    public static final int CREATED = 0;

    /**
     * After the Workflow has been instantiated, but before it has been started
     * using the {@link org.apache.wiki.workflow.Workflow#start()} method.
     */
    public static final int STARTED = 10;

    /**
     * fter the Workflow has been started (or re-started) using the
     * {@link org.apache.wiki.workflow.Workflow#start()} method, 
     * but before it has finished processing all Steps.
     */
    public static final int RUNNING = 20;

    /** When the Workflow has temporarily paused, for example because of a pending Decision. */
    public static final int WAITING = 30;

    /** After the Workflow has finished processing all Steps, without errors. */
    public static final int COMPLETED = 40;

    /** If a Step has elected to abort the Workflow. */
    public static final int ABORTED = 50;

    /**
     * Constructs a new instance of this event type, which signals a security event has occurred. 
     * The <code>source</code> parameter is required, and may not be <code>null</code>. When the 
     * WikiSecurityEvent is constructed, the security logger {@link WikiSecurityEvent#log} is notified.
     * 
     * @param src the source of the event, which can be any object: a wiki page, group or authentication/authentication/group manager.
     * @param type the type of event
     */
    public WorkflowEvent( final Object src, final int type ) {
        super( src, type );
        if( src == null ) {
            throw new IllegalArgumentException( "Argument(s) cannot be null." );
        }
    }

    /**
     * Prints a String (human-readable) representation of this object.
     * 
     * @see java.lang.Object#toString()
     */
    public String toString() {
        final StringBuilder msg = new StringBuilder();
        msg.append( "WorkflowEvent." );
        msg.append( eventName( getType() ) );
        final Object obj = getSrc(); // cfr. https://forums.oracle.com/forums/thread.jspa?threadID=1184115
        msg.append( " [source=" ).append( obj.toString() );
        msg.append( "]" );
        return msg.toString();
    }

    /**
     * Returns a textual representation of an event type.
     * 
     * @param type the type
     * @return the string representation
     */
    public String eventName( final int type ) {
        switch( type ) {
            case CREATED:   return "CREATED";
            case ABORTED:   return "ABORTED";
            case COMPLETED: return "COMPLETED";
            case RUNNING:   return "RUNNING";
            case STARTED:   return "STARTED";
            case WAITING:   return "WAITING";
            default:        return super.eventName();
        }
    }

}
