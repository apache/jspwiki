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
package org.apache.wiki.auth.authorize;
import java.security.Principal;
import java.util.Properties;

import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.auth.WikiPrincipal;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GroupTest
{
    Group m_group;
    String m_wiki;

    @BeforeEach
    public void setUp() throws Exception
    {
        Properties props = TestEngine.getTestProperties();
        WikiEngine engine  = new TestEngine( props );
        m_wiki = engine.getApplicationName();

        m_group = new Group( "TestGroup", m_wiki );
    }

    @Test
    public void testAdd1()
    {
        Principal u1 = new WikiPrincipal( "Alice" );
        m_group.add( u1 );
        Assertions.assertTrue( m_group.isMember( u1 ) );
    }

    @Test
    public void testAdd2()
    {
        Principal u1 = new WikiPrincipal( "Alice" );
        Principal u2 = new WikiPrincipal( "Bob" );

        Assertions.assertTrue( m_group.add( u1 ), "adding alice" );

        Assertions.assertTrue( m_group.add( u2 ), "adding bob" );

        Assertions.assertTrue( m_group.isMember( u1 ), "Alice" );
        Assertions.assertTrue( m_group.isMember( u2 ), "Bob" );
    }

    /**
     * Check that different objects match as well.
     */
    @Test
    public void testAdd3()
    {
        Principal u1 = new WikiPrincipal( "Alice" );
        Principal u2 = new WikiPrincipal( "Bob" );
        Principal u3 = new WikiPrincipal( "Bob" );

        Assertions.assertTrue( m_group.add( u1 ), "adding alice" );
        Assertions.assertTrue( m_group.add( u2 ), "adding bob" );

        Assertions.assertTrue( m_group.isMember( u1 ), "Alice" );
        Assertions.assertTrue( m_group.isMember( u3 ), "Bob" );
    }

    @Test
    public void testRemove()
    {
        Principal u1 = new WikiPrincipal( "Alice" );
        Principal u2 = new WikiPrincipal( "Bob" );
        Principal u3 = new WikiPrincipal( "Bob" );

        m_group.add( u1 );
        m_group.add( u2 );

        m_group.remove( u3 );

        Assertions.assertTrue( m_group.isMember( u1 ), "Alice" );
        Assertions.assertFalse( m_group.isMember( u2 ), "Bob" );
        Assertions.assertFalse( m_group.isMember( u3 ), "Bob 2" );
    }

    @Test
    public void testEquals1()
    {
        Principal u1 = new WikiPrincipal( "Alice" );
        Principal u2 = new WikiPrincipal( "Bob" );

        m_group.add( u1 );
        m_group.add( u2 );

        Group group2 = new Group( "TestGroup", m_wiki );
        Principal u3 = new WikiPrincipal( "Alice" );
        Principal u4 = new WikiPrincipal( "Bob" );

        group2.add( u3 );
        group2.add( u4 );

        Assertions.assertTrue( m_group.equals( group2 ) );
    }

    @Test
    public void testEquals2()
    {
        Principal u1 = new WikiPrincipal( "Alice" );
        Principal u2 = new WikiPrincipal( "Bob" );

        m_group.add( u1 );
        m_group.add( u2 );

        Group group2 = new Group( "Group2", m_wiki );
        Principal u3 = new WikiPrincipal( "Alice" );
        Principal u4 = new WikiPrincipal( "Charlie" );

        group2.add( u3 );
        group2.add( u4 );

        Assertions.assertFalse( m_group.equals( group2 ) );
    }

    @Test
    public void testEquals3()
    {
        Group group1 = new Group( "Blib", m_wiki );
        Principal u1 = new WikiPrincipal( "Alice" );
        Principal u2 = new WikiPrincipal( "Bob" );
        group1.add( u1 );
        group1.add( u2 );

        Group group2 = new Group( "Blib", m_wiki );
        Principal u3 = new WikiPrincipal( "Alice" );
        Principal u4 = new WikiPrincipal( "Bob" );
        group2.add( u3 );
        group2.add( u4 );

        Assertions.assertTrue( group1.equals( group2 ) );
    }

    @Test
    public void testEquals4()
    {
        Group group1 = new Group( "BlibBlab", m_wiki );
        Principal u1 = new WikiPrincipal( "Alice" );
        Principal u2 = new WikiPrincipal( "Bob" );
        group1.add( u1 );
        group1.add( u2 );

        Group group2 = new Group( "Blib", m_wiki );
        Principal u3 = new WikiPrincipal( "Alice" );
        Principal u4 = new WikiPrincipal( "Bob" );
        group2.add( u3 );
        group2.add( u4 );

        Assertions.assertFalse( m_group.equals( group2 ) );
    }

}