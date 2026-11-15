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
package org.apache.wiki.auth;

import org.apache.commons.io.FileUtils;
import org.apache.wiki.TestEngine;
import org.apache.wiki.auth.user.UserDatabase;
import org.apache.wiki.auth.user.XMLUserDatabase;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.Duration;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

class AccountLifecycleThreadTest {

    private static final String THREAD_NAME = "JSPWiki Account Lifecycle";

    private static Set< Thread > lifecycleThreads() {
        return Thread.getAllStackTraces().keySet().stream().filter( thread -> THREAD_NAME.equals( thread.getName() ) ).collect( Collectors.toSet() );
    }

    private static TestEngine newEngine( final String lockDays ) throws Exception {
        final Properties props = TestEngine.getTestProperties();
        final File file = new File( "target/AccountLifecycleThreadTest" + UUID.randomUUID() + ".xml" );
        FileUtils.copyFile( new File( "src/test/resources/userdatabase.xml" ), file );
        props.put( XMLUserDatabase.PROP_USERDATABASE, file.getAbsolutePath() );
        if( lockDays != null ) {
            props.put( AccountLifecycle.PROP_LOCK_DAYS, lockDays );
        }
        return new TestEngine( props );
    }

    @Test
    void testNoThreadWhenThereIsNothingToDo() throws Exception {
        final Set< Thread > before = lifecycleThreads();

        final TestEngine engine = newEngine( null );
        try {
            final Set< Thread > started = new HashSet<>( lifecycleThreads() );
            started.removeAll( before );
            Assertions.assertTrue( started.isEmpty() );
        } finally {
            engine.stop();
        }
    }

    @Test
    void testTheThreadGoesThroughTheAccountsWhenItStartsAndStopsWithTheEngine() throws Exception {
        final Set< Thread > before = lifecycleThreads();
        final TestEngine engine = newEngine( "120" );
        final UserDatabase db = engine.getManager( UserManager.class ).getUserDatabase();
        final Set< Thread > started = new HashSet<>( lifecycleThreads() );
        started.removeAll( before );
        try {
            Assertions.assertEquals( 1, started.size() );

            // the first pass gives the passwords their date
            Awaitility.await( "first pass" ).atMost( Duration.ofSeconds( 30 ) )
                      .until( () -> db.findByLoginName( "janne" ).getPasswordChanged() != null );
        } finally {
            engine.stop();
        }

        final Thread thread = started.iterator().next();
        Awaitility.await( "the thread stops with the engine" ).atMost( Duration.ofSeconds( 30 ) ).until( () -> !thread.isAlive() );
    }

}
