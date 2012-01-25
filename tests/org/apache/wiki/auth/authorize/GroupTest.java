/* 
    JSPWiki - a JSP-based WikiWiki clone.

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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.auth.WikiPrincipal;

public class GroupTest extends TestCase
{
    Group m_group;
    String m_wiki;
    
    public GroupTest( String s )
    {
        super( s );
    }

    public void setUp() throws Exception
    {
        Properties props = new Properties();
        props.load( TestEngine.findTestProperties() );
        WikiEngine engine  = new TestEngine( props );
        m_wiki = engine.getApplicationName();
        
        m_group = new Group( "TestGroup", m_wiki );
    }
    
    public void testAdd1()
    {
        Principal u1 = new WikiPrincipal( "Alice" );
        m_group.add( u1 );
        assertTrue( m_group.isMember( u1 ) );
    }

    public void testAdd2()
    {
        Principal u1 = new WikiPrincipal( "Alice" );
        Principal u2 = new WikiPrincipal( "Bob" );

        assertTrue( "adding alice", m_group.add( u1 ) );

        assertTrue( "adding bob", m_group.add( u2 ) );

        assertTrue( "Alice", m_group.isMember( u1 ) );
        assertTrue( "Bob", m_group.isMember( u2 ) );
    }

    /**
     * Check that different objects match as well.
     */
    public void testAdd3()
    {
        Principal u1 = new WikiPrincipal( "Alice" );
        Principal u2 = new WikiPrincipal( "Bob" );
        Principal u3 = new WikiPrincipal( "Bob" );

        assertTrue( "adding alice", m_group.add( u1 ) );
        assertTrue( "adding bob", m_group.add( u2 ) );

        assertTrue( "Alice", m_group.isMember( u1 ) );
        assertTrue( "Bob", m_group.isMember( u3 ) );
    }

    public void testRemove()
    {
        Principal u1 = new WikiPrincipal( "Alice" );
        Principal u2 = new WikiPrincipal( "Bob" );
        Principal u3 = new WikiPrincipal( "Bob" );

        m_group.add( u1 );
        m_group.add( u2 );

        m_group.remove( u3 );

        assertTrue( "Alice", m_group.isMember( u1 ) );
        assertFalse( "Bob", m_group.isMember( u2 ) );
        assertFalse( "Bob 2", m_group.isMember( u3 ) );
    }

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

        assertTrue( m_group.equals( group2 ) );
    }

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

        assertFalse( m_group.equals( group2 ) );
    }

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

        assertTrue( group1.equals( group2 ) );
    }

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

        assertFalse( m_group.equals( group2 ) );
    }

    public static Test suite()
    {
        return new TestSuite( GroupTest.class );
    }

}