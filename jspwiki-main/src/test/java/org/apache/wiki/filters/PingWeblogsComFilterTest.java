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
package org.apache.wiki.filters;

import com.sun.net.httpserver.HttpServer;
import org.apache.wiki.TestEngine;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.Page;
import org.apache.wiki.api.spi.Wiki;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

class PingWeblogsComFilterTest {

    private static final String ANSWER = "<?xml version=\"1.0\"?><methodResponse><params><param><value><struct>"
                                       + "<member><name>flerror</name><value><boolean>0</boolean></value></member>"
                                       + "<member><name>message</name><value><string>ok</string></value></member>"
                                       + "</struct></value></param></params></methodResponse>";

    private final BlockingQueue< String > bodies = new LinkedBlockingQueue<>();
    private final BlockingQueue< String > contentTypes = new LinkedBlockingQueue<>();
    private HttpServer server;
    private TestEngine engine;

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create( new InetSocketAddress( "127.0.0.1", 0 ), 0 );
        server.createContext( "/RPC2", exchange -> {
            bodies.add( new String( exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8 ) );
            contentTypes.add( exchange.getRequestHeaders().getFirst( "Content-Type" ) );
            final byte[] answer = ANSWER.getBytes( StandardCharsets.UTF_8 );
            exchange.sendResponseHeaders( 200, answer.length );
            exchange.getResponseBody().write( answer );
            exchange.close();
        } );
        server.start();
        engine = TestEngine.build();
    }

    @AfterEach
    void stopServer() {
        server.stop( 0 );
        engine.shutdown();
    }

    private PingWeblogsComFilter filter() {
        final Properties props = new Properties();
        props.setProperty( PingWeblogsComFilter.PROP_PINGURL, "http://127.0.0.1:" + server.getAddress().getPort() + "/RPC2" );
        final PingWeblogsComFilter filter = new PingWeblogsComFilter();
        filter.initialize( engine, props );
        return filter;
    }

    private Context context( final String pageName ) {
        final Page page = Wiki.contents().page( engine, pageName );
        return Wiki.context().create( engine, page );
    }

    @Test
    void postsTheWeblogUpdatesPingOfAWeblogEntry() throws Exception {
        filter().postSave( context( "MyBlog_blogentry_20260101_1" ), "text" );

        final String body = bodies.poll( 10, TimeUnit.SECONDS );
        Assertions.assertNotNull( body, "the ping was not received" );
        Assertions.assertTrue( body.contains( "<methodName>weblogUpdates.ping</methodName>" ), body );
        Assertions.assertTrue( body.contains( "page=MyBlog" ), body );
        Assertions.assertEquals( "text/xml; charset=UTF-8", contentTypes.poll() );
    }

    @Test
    void doesNotPingForAPageThatIsNotAWeblogEntry() throws Exception {
        filter().postSave( context( "JustAPage" ), "text" );

        Assertions.assertNull( bodies.poll( 2, TimeUnit.SECONDS ) );
    }

    @Test
    void escapesTheValuesOfTheCall() {
        final String body = PingWeblogsComFilter.pingRequest( "a<b>&c", "http://x/?a=1&b=<2>" );

        Assertions.assertTrue( body.contains( "<string>a&lt;b&gt;&amp;c</string>" ), body );
        Assertions.assertTrue( body.contains( "<string>http://x/?a=1&amp;b=&lt;2&gt;</string>" ), body );
    }

}
