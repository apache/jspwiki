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
package org.apache.wiki.its;

import org.apache.wiki.pages.haddock.LoginPage;
import org.apache.wiki.pages.haddock.ViewWikiPage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * Login-related tests for Apache JSPWiki
 */
public class LoginIT extends WithIntegrationTestSetup {
    
    @Test
    void loginAndLogout() {
        ViewWikiPage main = ViewWikiPage.open( "Main" );
        Assertions.assertEquals( "JSPWiki: Main", main.title() );
        Assertions.assertEquals( "Main", main.wikiTitle() );
        Assertions.assertEquals( "G’day (anonymous guest)", main.hoverLoginArea().authenticatedText() );
        
        final LoginPage login = main.clickOnLogin();
        Assertions.assertEquals( "JSPWiki: Login", login.title() );
        Assertions.assertEquals( "Login", login.wikiTitle() );
        
        main = login.performLogin();
        Assertions.assertEquals( "JSPWiki: Main", main.title() );
        Assertions.assertEquals( "G’day, Janne Jalkanen (authenticated)", main.hoverLoginArea().authenticatedText() );
        
        main.clickOnLogout();
        Assertions.assertEquals( "G’day (anonymous guest)", main.hoverLoginArea().authenticatedText() );
    }
    
    @Test
    void loginKO() {
        ViewWikiPage main = ViewWikiPage.open( "Main" );
        Assertions.assertEquals( "JSPWiki: Main", main.title() );
        Assertions.assertEquals( "Main", main.wikiTitle() );
        Assertions.assertEquals( "G’day (anonymous guest)", main.hoverLoginArea().authenticatedText() );
        
        final LoginPage login = main.clickOnLogin();
        Assertions.assertEquals( "JSPWiki: Login", login.title() );
        Assertions.assertEquals( "Login", login.wikiTitle() );
        
        main = login.performLogin( "perry", "mason" );
        Assertions.assertEquals( "JSPWiki: Login", main.title() );
        Assertions.assertEquals( "G’day (anonymous guest)", main.hoverLoginArea().authenticatedText() );
    }

}
