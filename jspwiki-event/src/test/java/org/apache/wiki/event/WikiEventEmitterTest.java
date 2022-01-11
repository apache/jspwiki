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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


public class WikiEventEmitterTest {

    @BeforeEach
    public void setUp() {
        // Ensure no listeners are attached to WikiEventEmitter instance
        WikiEventManager.getWikiEventListeners( WikiEventEmitter.get() ).forEach( WikiEventManager::removeWikiEventListener );
    }

    @Test
    public void shouldCheckFireWorkflowEvent() {
        Assertions.assertNull( WikiEventEmitter.fireWorkflowEvent( "test", WorkflowEvent.CREATED ) );

        WikiEventEmitter.attach( new TestWikiEventListener() );
        final WorkflowEvent we = WikiEventEmitter.fireWorkflowEvent( "test", WorkflowEvent.CREATED );
        Assertions.assertNotNull( we );
        Assertions.assertEquals( WorkflowEvent.CREATED, we.getType() );
        Assertions.assertEquals( "test", we.getSource() );
    }

    @Test
    public void shouldCheckAttach() {
        Assertions.assertEquals( 0, WikiEventManager.getWikiEventListeners( WikiEventEmitter.get() ).size() );
        WikiEventEmitter.attach( new TestWikiEventListener() );
        WikiEventEmitter.attach( new TestWikiEventListener() );
        Assertions.assertEquals( 1, WikiEventManager.getWikiEventListeners( WikiEventEmitter.get() ).size() );
    }

    @Test
    public void shouldCheckRegister() {
        Assertions.assertEquals( 0, WikiEventManager.getWikiEventListeners( WikiEventEmitter.get() ).size() );
        WikiEventEmitter.register( new TestWikiEventListener() );
        WikiEventEmitter.register( new TestWikiEventListener() );
        Assertions.assertEquals( 2, WikiEventManager.getWikiEventListeners( WikiEventEmitter.get() ).size() );
    }

}
