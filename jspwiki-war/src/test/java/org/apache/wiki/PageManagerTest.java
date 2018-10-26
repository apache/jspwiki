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
import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;
import org.apache.wiki.providers.CachingProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PageManagerTest
{
    Properties props = TestEngine.getTestProperties();

    TestEngine engine;

    @BeforeEach
    public void setUp()
        throws Exception
    {
        PropertyConfigurator.configure(props);
        engine = new TestEngine(props);
    }

    @AfterEach
    public void tearDown()
    {
    }

    @Test
    public void testPageCacheExists()
        throws Exception
    {
        props.setProperty( "jspwiki.usePageCache", "true" );
        PageManager m = new PageManager( engine, props );

        Assertions.assertTrue( m.getProvider() instanceof CachingProvider );
    }

    @Test
    public void testPageCacheNotInUse()
        throws Exception
    {
        props.setProperty( "jspwiki.usePageCache", "false" );
        PageManager m = new PageManager( engine, props );

        Assertions.assertTrue( !(m.getProvider() instanceof CachingProvider) );
    }

}
