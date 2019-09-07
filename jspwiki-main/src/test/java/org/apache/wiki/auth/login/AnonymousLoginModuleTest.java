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
package org.apache.wiki.auth.login;
import java.security.Principal;
import java.util.HashMap;
import java.util.Properties;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import javax.servlet.http.HttpServletRequest;

import org.apache.wiki.TestEngine;
import org.apache.wiki.api.exceptions.NoRequiredPropertyException;
import org.apache.wiki.auth.WikiPrincipal;
import org.apache.wiki.auth.authorize.Role;
import org.apache.wiki.auth.user.UserDatabase;
import org.apache.wiki.auth.user.XMLUserDatabase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;


/**
 */
public class AnonymousLoginModuleTest
{
    UserDatabase m_db;

    Subject      m_subject;

    private TestEngine m_engine;

    @Test
    public final void testLogin()
    {
        HttpServletRequest request = m_engine.newHttpRequest();
        try
        {
            // Test using IP address (AnonymousLoginModule succeeds)
            CallbackHandler handler = new WebContainerCallbackHandler( m_engine, request );
            LoginModule module = new AnonymousLoginModule();
            module.initialize( m_subject, handler, new HashMap<String, Object>(), new HashMap<String, Object>() );
            module.login();
            module.commit();
            Set< Principal > principals = m_subject.getPrincipals();
            Assertions.assertEquals( 1, principals.size() );
            Assertions.assertTrue( principals.contains( new WikiPrincipal( "127.0.0.1" ) ) );
            Assertions.assertFalse( principals.contains( Role.ANONYMOUS ) );
            Assertions.assertFalse( principals.contains( Role.ALL ) );
        }
        catch( LoginException e )
        {
            System.err.println( e.getMessage() );
            Assertions.assertTrue( false );
        }
    }

    @Test
    public final void testLogout()
    {
        HttpServletRequest request = m_engine.newHttpRequest();
        try
        {
            CallbackHandler handler = new WebContainerCallbackHandler( m_engine, request );
            LoginModule module = new AnonymousLoginModule();
            module.initialize( m_subject, handler,
                              new HashMap<String, Object>(),
                              new HashMap<String, Object>() );
            module.login();
            module.commit();
            Set< Principal > principals = m_subject.getPrincipals();
            Assertions.assertEquals( 1, principals.size() );
            Assertions.assertTrue( principals.contains( new WikiPrincipal( "127.0.0.1" ) ) );
            Assertions.assertFalse( principals.contains( Role.ANONYMOUS ) );
            Assertions.assertFalse( principals.contains( Role.ALL ) );
            module.logout();
            Assertions.assertEquals( 0, principals.size() );
        }
        catch( LoginException e )
        {
            System.err.println( e.getMessage() );
            Assertions.assertTrue( false );
        }
    }

    /**
     *
     */
    @BeforeEach
    public void setUp() throws Exception
    {
        Properties props = TestEngine.getTestProperties();
        props.put(XMLUserDatabase.PROP_USERDATABASE, "target/test-classes/userdatabase.xml" );
        m_engine = new TestEngine(props);
        m_db = new XMLUserDatabase();
        m_subject = new Subject();
        try
        {
            m_db.initialize( m_engine, props );
        }
        catch( NoRequiredPropertyException e )
        {
            System.err.println( e.getMessage() );
            Assertions.assertTrue( false );
        }
    }

}
