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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;
import org.apache.wiki.api.exceptions.NoSuchVariableException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

public class VariableManagerTest
{
    VariableManager m_variableManager;
    WikiContext     m_context;

    static final String PAGE_NAME = "TestPage";

    @BeforeEach
    public void setUp()
        throws Exception
    {
        Properties props = TestEngine.getTestProperties();
        PropertyConfigurator.configure(props);

        m_variableManager = new VariableManager( props );
        TestEngine testEngine = new TestEngine( props );
        m_context = new WikiContext( testEngine,
                                     new WikiPage( testEngine, PAGE_NAME ) );
    }

    @AfterEach
    public void tearDown()
    {
    }

    @Test
    public void testIllegalInsert1()
        throws Exception
    {
        try
        {
            m_variableManager.parseAndGetValue( m_context, "" );
            Assertions.fail( "Did not Assertions.fail" );
        }
        catch( IllegalArgumentException e )
        {
            // OK.
        }
    }

    @Test
    public void testIllegalInsert2()
        throws Exception
    {
        try
        {
            m_variableManager.parseAndGetValue( m_context, "{$" );
            Assertions.fail( "Did not Assertions.fail" );
        }
        catch( IllegalArgumentException e )
        {
            // OK.
        }
    }

    @Test
    public void testIllegalInsert3()
        throws Exception
    {
        try
        {
            m_variableManager.parseAndGetValue( m_context, "{$pagename" );
            Assertions.fail( "Did not Assertions.fail" );
        }
        catch( IllegalArgumentException e )
        {
            // OK.
        }
    }

    @Test
    public void testIllegalInsert4()
        throws Exception
    {
        try
        {
            m_variableManager.parseAndGetValue( m_context, "{$}" );
            Assertions.fail( "Did not Assertions.fail" );
        }
        catch( IllegalArgumentException e )
        {
            // OK.
        }
    }

    @Test
    public void testNonExistantVariable()
    {
        try
        {
            m_variableManager.parseAndGetValue( m_context, "{$no_such_variable}" );
            Assertions.fail( "Did not Assertions.fail" );
        }
        catch( NoSuchVariableException e )
        {
            // OK.
        }
    }

    @Test
    public void testPageName()
        throws Exception
    {
        String res = m_variableManager.getValue( m_context, "pagename" );

        Assertions.assertEquals( PAGE_NAME, res );
    }

    @Test
    public void testPageName2()
        throws Exception
    {
        String res =  m_variableManager.parseAndGetValue( m_context, "{$  pagename  }" );

        Assertions.assertEquals( PAGE_NAME, res );
    }

    @Test
    public void testMixedCase()
        throws Exception
    {
        String res =  m_variableManager.parseAndGetValue( m_context, "{$PAGeNamE}" );

        Assertions.assertEquals( PAGE_NAME, res );
    }

    @Test
    public void testExpand1()
        throws Exception
    {
        String res = m_variableManager.expandVariables( m_context, "Testing {$pagename}..." );

        Assertions.assertEquals( "Testing "+PAGE_NAME+"...", res );
    }

    @Test
    public void testExpand2()
        throws Exception
    {
        String res = m_variableManager.expandVariables( m_context, "{$pagename} tested..." );

        Assertions.assertEquals( PAGE_NAME+" tested...", res );
    }

    @Test
    public void testExpand3()
        throws Exception
    {
        String res = m_variableManager.expandVariables( m_context, "Testing {$pagename}, {$applicationname}" );

        Assertions.assertEquals( "Testing "+PAGE_NAME+", JSPWiki", res );
    }

    @Test
    public void testExpand4()
        throws Exception
    {
        String res = m_variableManager.expandVariables( m_context, "Testing {}, {{{}" );

        Assertions.assertEquals( "Testing {}, {{{}", res );
    }

}
