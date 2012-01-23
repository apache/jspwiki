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
package org.apache.wiki.migration;

import java.util.Properties;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.wiki.TestEngine;
import org.apache.wiki.api.WikiPage;
import org.apache.wiki.content.PageNotFoundException;
import org.apache.wiki.providers.ProviderException;


/**
 * Unit tests associated to {@link MigrationManager}.
 */
public class MigrationManagerTest extends TestCase
{
    
    Properties props = new Properties();

    private TestEngine m_engine = null;

    public MigrationManagerTest( String s )
    {
        super( s );
    }

    public void setUp()
        throws Exception
    {
        props.load( TestEngine.findTestProperties() );
        m_engine = new TestEngine( props );
    }

    public void tearDown()
    {
        m_engine.shutdown();
    }
    
    public void testMigration() 
    {
        MigrationManager m = new DefaultJSPWikiPagesLoader();
        m.migrate( m_engine, new MigrationVO().setRepoDir( "build/tests/migration/testrepo" ) );
        WikiPage page = null;
        
        try
        {
            page = m_engine.getPage( "Main" );
            assertNotNull( page );
            assertEquals( 1, page.getVersion() );
        }
        catch( ProviderException pe )
        {
            fail( pe.getMessage() );
        }
        catch( PageNotFoundException pnfe )
        {
            fail( pnfe.getMessage() );
        }
        try
        {
            page = m_engine.getPage( "Blurb" );
            fail( "should not get here" );
        }
        catch( ProviderException pe )
        {
            fail( pe.getMessage() );
        }
        catch( PageNotFoundException pnfe )
        {
            // Blurb does not exist
            assertEquals( "Main", page.getName() );
        }
    }

    public static Test suite()
    {
        return new TestSuite( MigrationManagerTest.class );
    }
    
}
