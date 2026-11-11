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
package org.apache.wiki.plugin;

import org.apache.wiki.TestEngine;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.spi.Wiki;
import org.apache.wiki.pages.PageLock;
import org.apache.wiki.pages.PageManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;


public class ListLocksPluginTest {

    private final TestEngine engine = TestEngine.build();

    @AfterEach
    public void tearDown() throws Exception {
        engine.deleteTestPage( "LockedPage" );
        engine.stop();
    }

    /** The name of whoever holds a lock comes from the asserted name of the user, so it must be written escaped. */
    @Test
    public void testLockHolderIsWrittenEscaped() throws Exception {
        engine.saveText( "LockedPage", "text" );
        final PageManager pageManager = engine.getManager( PageManager.class );
        final PageLock lock = pageManager.lockPage( pageManager.getPage( "LockedPage" ), "holder<b>1</b>" );
        try {
            final Context context = Wiki.context().create( engine, pageManager.getPage( "LockedPage" ) );

            final String html = new ListLocksPlugin().execute( context, Map.of() );

            Assertions.assertTrue( html.contains( "<td>holder&lt;b&gt;1&lt;/b&gt;</td>" ), html );
            Assertions.assertFalse( html.contains( "<b>" ), html );
        } finally {
            pageManager.unlockPage( lock );
        }
    }

}
