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

package org.apache.wiki.variables;

import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiPage;
import org.apache.wiki.api.exceptions.NoSuchVariableException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


public class DefaultVariableManagerTest {

    static VariableManager m_variableManager;
    static WikiContext m_context;

    static final String PAGE_NAME = "TestPage";

    @BeforeAll
    public static void setUp() {
        final TestEngine testEngine = TestEngine.build();
        m_variableManager = new DefaultVariableManager( TestEngine.getTestProperties() );
        m_context = new WikiContext( testEngine, new WikiPage( testEngine, PAGE_NAME ) );
    }

    @Test
    public void testIllegalInsert1() {
        Assertions.assertThrows( IllegalArgumentException.class, () -> m_variableManager.parseAndGetValue( m_context, "" ) );
    }

    @Test
    public void testIllegalInsert2() {
        Assertions.assertThrows( IllegalArgumentException.class, () -> m_variableManager.parseAndGetValue( m_context, "{$" ) );
    }

    @Test
    public void testIllegalInsert3() {
        Assertions.assertThrows( IllegalArgumentException.class, () -> m_variableManager.parseAndGetValue( m_context, "{$pagename" ) );
    }

    @Test
    public void testIllegalInsert4() {
        Assertions.assertThrows( IllegalArgumentException.class, () -> m_variableManager.parseAndGetValue( m_context, "{$}" ) );
    }

    @Test
    public void testNonExistantVariable() {
        Assertions.assertThrows( NoSuchVariableException.class, () -> m_variableManager.parseAndGetValue( m_context, "{$no_such_variable}" ) );
    }

    @Test
    public void testPageName() throws Exception {
        final String res = m_variableManager.getValue( m_context, "pagename" );
        Assertions.assertEquals( PAGE_NAME, res );
    }

    @Test
    public void testPageName2() throws Exception {
        final String res =  m_variableManager.parseAndGetValue( m_context, "{$  pagename  }" );
        Assertions.assertEquals( PAGE_NAME, res );
    }

    @Test
    public void testMixedCase() throws Exception {
        final String res =  m_variableManager.parseAndGetValue( m_context, "{$PAGeNamE}" );
        Assertions.assertEquals( PAGE_NAME, res );
    }

    @Test
    public void testExpand1() {
        final String res = m_variableManager.expandVariables( m_context, "Testing {$pagename}..." );
        Assertions.assertEquals( "Testing "+PAGE_NAME+"...", res );
    }

    @Test
    public void testExpand2() {
        final String res = m_variableManager.expandVariables( m_context, "{$pagename} tested..." );
        Assertions.assertEquals( PAGE_NAME+" tested...", res );
    }

    @Test
    public void testExpand3() {
        final String res = m_variableManager.expandVariables( m_context, "Testing {$pagename}, {$applicationname}" );
        Assertions.assertEquals( "Testing "+PAGE_NAME+", JSPWiki", res );
    }

    @Test
    public void testExpand4() {
        final String res = m_variableManager.expandVariables( m_context, "Testing {}, {{{}" );
        Assertions.assertEquals( "Testing {}, {{{}", res );
    }

}
