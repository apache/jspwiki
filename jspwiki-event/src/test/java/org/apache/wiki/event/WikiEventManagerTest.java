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
import org.junit.jupiter.api.Test;


public class WikiEventManagerTest {

    @Test
    public void shouldCheckRegisterUnregister() {
        final String client1 = "test1";
        final String client2 = "test2";
        final TestWikiEventListener listener = new TestWikiEventListener();
        WikiEventManager.addWikiEventListener( client1, listener );
        Assertions.assertEquals( 1, WikiEventManager.getWikiEventListeners( client1 ).size() );
        Assertions.assertTrue( WikiEventManager.isListening( client1 ) );

        WikiEventManager.removeWikiEventListener( client1, listener );
        Assertions.assertEquals( 0, WikiEventManager.getWikiEventListeners( client1 ).size() );
        Assertions.assertFalse( WikiEventManager.isListening( client1 ) );

        WikiEventManager.addWikiEventListener( client1, listener );
        WikiEventManager.addWikiEventListener( client2, listener );
        WikiEventManager.removeWikiEventListener( listener );
        Assertions.assertEquals( 0, WikiEventManager.getWikiEventListeners( client1 ).size() );
        Assertions.assertEquals( 0, WikiEventManager.getWikiEventListeners( client2 ).size() );
    }

    @Test
    public void shouldCheckAddingSameWikiEventListenerSeveralTimesOnlyGetsRegisteredOnce() {
        final String client = "test3";
        final TestWikiEventListener listener = new TestWikiEventListener();
        WikiEventManager.addWikiEventListener( client, listener );
        WikiEventManager.addWikiEventListener( client, listener );

        Assertions.assertEquals( 1, WikiEventManager.getWikiEventListeners( client ).size() );

        WikiEventManager.removeWikiEventListener( client, listener );
        Assertions.assertEquals( 0, WikiEventManager.getWikiEventListeners( client ).size() );
    }

    @Test
    public void shouldCheckEventsFiring() {
        final String client = "test4";
        final TestWikiEventListener listener = new TestWikiEventListener();
        WikiEventManager.addWikiEventListener( client, listener );
        WikiEventManager.fireEvent( client, new WikiPageEvent( "object which fires the event", WikiPageEvent.PAGE_REQUESTED, "page" ) );

        Assertions.assertEquals( 1, listener.getInvoked() );
        WikiEventManager.removeWikiEventListener( listener ); // dispose listener; if not done, listener would still be attached to test4 on other tests
    }

}
