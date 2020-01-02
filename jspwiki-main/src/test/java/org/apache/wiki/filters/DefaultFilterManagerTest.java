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
import org.apache.wiki.TestEngine;
import org.apache.wiki.api.engine.FilterManager;
import org.apache.wiki.api.filters.PageFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.List;
import java.util.Properties;

public class DefaultFilterManagerTest {

    Properties props = TestEngine.getTestProperties();
    TestEngine engine = TestEngine.build();

    @Test
    public void testInitFilters() throws Exception {
        FilterManager m = new DefaultFilterManager( engine, props );

        List<PageFilter> l = m.getFilterList();

        Assertions.assertEquals( 2, l.size(), "Wrong number of filters" );

        Iterator<PageFilter> i = l.iterator();
        PageFilter f1 = i.next();

        Assertions.assertTrue( f1 instanceof ProfanityFilter, "Not a Profanityfilter" );

        PageFilter f2 = i.next();

        Assertions.assertTrue( f2 instanceof TestFilter, "Not a Testfilter" );
    }

    @Test
    public void testInitParams() throws Exception {
        FilterManager m = new DefaultFilterManager( engine, props );

        List<PageFilter> l = m.getFilterList();

        Iterator<PageFilter> i = l.iterator();
        i.next();
        TestFilter f2 = (TestFilter)i.next();

        Properties p = f2.m_properties;

        Assertions.assertEquals( "Zippadippadai", p.getProperty("foobar"), "no foobar" );

        Assertions.assertEquals( "5", p.getProperty( "blatblaa" ), "no blatblaa" );
    }

}
