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
package org.apache.wiki.i18n;
import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import org.junit.jupiter.api.Assertions;

public class InternationalizationManagerTest
{
    InternationalizationManager i18n = new InternationalizationManager( null );
    
    @BeforeEach
    public void setUp() throws Exception
    {
    	// enforce english locale as the default one. Otherwise, if your default locale is one
    	// of the given translations, ResourceBundle.getBundle(String, Locale.ENGLISH) will 
    	// return the bundle of your locale, rather than returning the default -english- one
    	Locale.setDefault( Locale.ENGLISH );
    }
    
    @Test
    public void testGetFromCoreWithArgs() 
    {
        String str = i18n.get( InternationalizationManager.CORE_BUNDLE, 
                               Locale.ENGLISH, 
                               "security.error.cannot.rename", 
                               "Test User" );
        Assertions.assertEquals( "Cannot rename: the login name 'Test User' is already taken.", str );
    }
    
    @Test
    public void testGetFromDefTemplateWithArgs() 
    {
        String str = i18n.get( InternationalizationManager.DEF_TEMPLATE, 
                               Locale.ENGLISH, 
                               "notification.createUserProfile.accept.content", 
                               "JSPWiki", "testUser", "Test User", "test@user.com", "www.foo.com" );
        Assertions.assertEquals( "Congratulations! Your new profile on JSPWiki has been created. " +
        		      "Your profile details are as follows: \n\n" +
                      "Login name: testUser \n" +
                      "Your name : Test User \n" +
                      "E-mail    : test@user.com \n\n" +
                      "If you forget your password, you can reset it at www.foo.com", 
                      str );
    }
    
}
