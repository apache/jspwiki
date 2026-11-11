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
package org.apache.wiki.diff;

import org.apache.wiki.TestEngine;
import org.apache.wiki.api.core.Engine;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

class ExternalDiffProviderTest {

    @Test
    void testDiffKeepsCharactersOutsideLatin1() throws Exception {
        Assumptions.assumeTrue( Files.isExecutable( Paths.get( "/usr/bin/diff" ) ), "diff is not available" );
        final Properties props = TestEngine.getTestProperties();
        props.setProperty( ExternalDiffProvider.PROP_DIFFCOMMAND, "/usr/bin/diff %s1 %s2" );
        props.setProperty( Engine.PROP_ENCODING, "UTF-8" );
        final TestEngine engine = TestEngine.build( props );
        try {
            final ExternalDiffProvider provider = new ExternalDiffProvider();
            provider.initialize( engine, props );

            final String diff = provider.makeDiffHtml( null, "price 10€", "price 20€" );

            Assertions.assertTrue( diff.contains( "price 10€" ), diff );
            Assertions.assertTrue( diff.contains( "price 20€" ), diff );
        } finally {
            engine.stop();
        }
    }

}
