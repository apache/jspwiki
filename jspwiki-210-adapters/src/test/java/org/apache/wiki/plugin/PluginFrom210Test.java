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
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiPage;
import org.apache.wiki.api.core.Engine;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Properties;


public class PluginFrom210Test {

    @Test
    public void testPluginNotUsingPublicApiStillWorks() throws Exception {
        final Properties props = TestEngine.getTestProperties();
        props.setProperty( Engine.PROP_SEARCHPATH, "com.example.plugins" );
        final WikiEngine engine = TestEngine.build( props );
        final PluginManager pm = engine.getManager( PluginManager.class );
        final WikiContext context = new WikiContext( engine, new WikiPage( engine, "Testpage" ) );

        final String res = pm.execute( context,"{TwoXPlugin}" );
        Assertions.assertEquals( "hakuna matata", res );
    }

}
