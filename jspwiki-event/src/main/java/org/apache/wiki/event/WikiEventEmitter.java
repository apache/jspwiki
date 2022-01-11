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

import java.util.Set;


/**
 * Emits all kind of {@link org.apache.wiki.event.WikiEvent}s.
 */
public enum WikiEventEmitter {

    INSTANCE;

    public static WikiEventEmitter get() {
        return INSTANCE;
    }

    /**
     * Fires a Workflow Event from provided source and workflow type.
     *
     * @param src the source of the event, which can be any object: a wiki page, group or authentication/authentication/group manager.
     * @param type the type of event
     * @return fired {@link WorkflowEvent} or {@code null} if the {@link WikiEventEmitter} instance hasn't listeners attached.
     */
    public static WorkflowEvent fireWorkflowEvent( final Object src, final int type ) {
        return fireEvent( new WorkflowEvent( src, type ) );
    }

    /**
     * Fires a Workflow Event from provided source and workflow type.
     *
     * @param src the source of the event, which can be any object: a wiki page, group or authentication/authentication/group manager.
     * @param type the type of event
     * @return fired {@link WorkflowEvent} or {@code null} if the {@link WikiEventEmitter} instance hasn't listeners attached.
     */
    public static WorkflowEvent fireWorkflowEvent( final Object src, final int type, final Object... args ) {
        return fireEvent( new WorkflowEvent( src, type, args ) );
    }

    static < T extends WikiEvent > T fireEvent( final T event ) {
        if( WikiEventManager.isListening( WikiEventEmitter.get() ) ) {
            WikiEventManager.fireEvent( WikiEventEmitter.get(), event );
            return event;
        }
        return null;
    }

    /**
     * Registers a {@link WikiEventListener} so it listens events fired from the {@link WikiEventEmitter} instance. Every other
     * {@link WikiEventListener} of the same type, listening events from the {@link WikiEventEmitter} instance will stop listening events
     * from it. This ensures events received by the {@link WikiEventListener} will only process the events once.
     *
     * @param listener {@link WikiEventListener}
     */
    public static void attach( final WikiEventListener listener ) {
        if( WikiEventManager.isListening( get() ) ) {
            final Set< WikiEventListener > attachedListeners = WikiEventManager.getWikiEventListeners( WikiEventEmitter.get() );
            attachedListeners.stream()
                             .filter( l -> listener.getClass().isAssignableFrom( l.getClass() ) )
                             .forEach( WikiEventManager::removeWikiEventListener );
        }
        register( listener );
    }

    /**
     * Registers a {@link WikiEventListener} so it listens events fired from the {@link WikiEventEmitter} instance. Events received by the
     * {@link WikiEventListener} could process the events more than once or, several instances of the same {@link WikiEventListener} would
     * be able to receive the same event.
     *
     * @param listener {@link WikiEventListener}
     */
    public static void register( final WikiEventListener listener ) {
        WikiEventManager.addWikiEventListener( WikiEventEmitter.get(), listener );
    }

}
