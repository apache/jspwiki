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

import org.apache.wiki.event.WikiEventListener;
import org.apache.wiki.event.WikiEventManager;
import org.apache.wiki.event.WorkflowEvent;

import java.util.Set;


/**
 * Emits all kind of {@link WorkflowEvent}s.
 */
public enum WorkflowEventEmitter {

    INSTANCE;

    public static WorkflowEventEmitter get() {
        return INSTANCE;
    }

    public static void fireEvent( final Object src, final int type ) {
        if ( WikiEventManager.isListening( get() ) ) {
            WikiEventManager.fireEvent( get(), new WorkflowEvent( src, type ) );
        }
    }

    public static void registerListener( final WikiEventListener listener ) {
        if ( WikiEventManager.isListening( get() ) ) {
            final Set< WikiEventListener > attachedListeners = WikiEventManager.getWikiEventListeners( get() );
            attachedListeners.stream()
                             .filter( l -> listener.getClass().isAssignableFrom( l.getClass() ) )
                             .forEach( WikiEventManager::removeWikiEventListener );
        }
        WikiEventManager.addWikiEventListener( WorkflowEventEmitter.get(), listener );
    }

}
