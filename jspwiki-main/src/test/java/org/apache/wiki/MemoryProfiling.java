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
package org.apache.wiki;

import org.apache.wiki.api.core.Acl;
import org.apache.wiki.api.core.AclEntry;
import org.apache.wiki.api.core.Attachment;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.core.Page;
import org.apache.wiki.api.core.Session;
import org.apache.wiki.api.spi.Wiki;
import org.github.jamm.MemoryMeter;
import org.junit.jupiter.api.Test;

import java.util.Properties;

/**
 * Some memory profiling checks. Needs jamm java agent set as jvm arg to run. This can be enabled manually (adding
 * {@code -javaagent=path-to-jamm.jar} as jvm arg when launching this test) or through the mem-profiling maven profile
 * ({@code mvn test -Dtest=MemoryProfiling}).
 *
 * note this deactivates other javaagents like f.ex., jacoco.
 */
class MemoryProfiling {

    @Test
    void memorySize() {
        final Properties props = TestEngine.getTestProperties();
        props.put( "jspwiki.fileSystemProvider.pageDir", "../jspwiki-wikipages/en/src/main/resources" );
        final Engine engine = Wiki.engine().find( TestEngine.createServletContext( "/test" ), TestEngine.getTestProperties() );
        final Engine engineWithDefaultPages = Wiki.engine().find( TestEngine.createServletContext( "/test" ), props );
        final Page main = Wiki.contents().page( engine, "Main" );
        final Attachment att = Wiki.contents().attachment( engine, "Main", "file" );
        final Session session = Wiki.session().guest( engine );
        final Acl acl = Wiki.acls().acl();
        final AclEntry aclEntry = Wiki.acls().entry();

        final MemoryMeter meter = MemoryMeter.builder().build();
        final long engineBytes = meter.measureDeep( engine );
        final long engineWithDefaultPagesBytes = meter.measureDeep( engineWithDefaultPages );
        final long mainBytes = meter.measureDeep( main );
        final long attBytes = meter.measureDeep( att );
        final long sessionBytes = meter.measureDeep( session );
        final long aclBytes = meter.measureDeep( acl );
        final long aclEntryBytes = meter.measureDeep( aclEntry );

        System.out.println( "" );
        System.out.println( "===========================================================================================" );
        System.out.println( "Plain Engine, without pages/attachments, search indexes, references, etc.: " + format( engineBytes ) );
        System.out.println( "Engine, with default set of wiki pages: .................................. " + format( engineWithDefaultPagesBytes ) );
        System.out.println( "Page: .................................................................... " + format( mainBytes - engineBytes) );
        System.out.println( "Attachment: .............................................................. " + format( attBytes - engineBytes ) );
        System.out.println( "Guest session on plain engine: ........................................... " + format( sessionBytes - engineBytes ) );
        System.out.println( "Acl: ..................................................................... " + format( aclBytes ) );
        System.out.println( "Acl entry: ............................................................... " + format( aclEntryBytes ) );
        System.out.println( "-------------------------------------------------------------------------------------------" );
        System.out.println( "" );
    }

    String format( final long bytes ) {
        return String.format( "%,10d bytes", bytes );
    }

}
