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
package org.apache.wiki.pages.haddock;

import com.codeborne.selenide.Selenide;
import org.apache.wiki.its.environment.Env;
import org.openqa.selenium.By;

/**
 * Actions available on the Login page.
 */
public class LoginPage implements HaddockPage {

    /**
     * Logs in using Janne username and password.
     *
     * @return {@link ViewWikiPage} instance, to allow chaining of actions.
     */
    public ViewWikiPage performLogin() {
        return performLogin( Env.LOGIN_JANNE_USERNAME, Env.LOGIN_JANNE_PASSWORD );
    }

    /**
     * Logs in using the supplied username and password.
     *
     * @param login user login.
     * @param password user password.
     * @return {@link ViewWikiPage} instance, to allow chaining of actions.
     */
    public ViewWikiPage performLogin(final String login, final String password ) {
        Selenide.$( By.id( "j_username" ) ).val( login );
        Selenide.$( By.id( "j_password" ) ).val( password );
        Selenide.$( By.name( "submitlogin" ) ).click();
        
        return new ViewWikiPage();
    }

}
