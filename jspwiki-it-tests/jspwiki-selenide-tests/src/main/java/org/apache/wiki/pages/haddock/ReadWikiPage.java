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

import org.openqa.selenium.By;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;

public class ReadWikiPage implements HaddockPage {
    
    public String authenticatedText() {
        return Selenide.$( By.className( "wikipage" ) ).text().trim();
    }
    
    public LoginPage clickOnLogin() {
        Selenide.$( By.className( "icon-signin" ) ).click(); 
        return new LoginPage();
    }
    
    public ReadWikiPage clickOnShowReaderView() {
        Selenide.$( By.linkText( "Show Reader View" ) ).click();
        return new ReadWikiPage();
    }

    public ReadWikiPage hoverLoginArea() {
        Selenide.$( By.className( "icon-user" ) ).hover();
        return this;
    }
    
    public ReadWikiPage hoverMoreArea() {
        Selenide.$( By.id( "more" ) ).hover();
        return this;
    }
    
    public ReadWikiPage logout() {
        Selenide.$( By.linkText( "Log out" ) ).click();
        Selenide.$( By.className( "btn-success" ) ).waitUntil( Condition.visible, 1_000L ).click();
        return this;
    }
    
    public ReadWikiPage navigateTo( String wikiPageName ) {
        Selenide.$( By.linkText( wikiPageName ) ).click(); 
        return new ReadWikiPage();
    }
    
    public SelenideElement sidebar() {
        return Selenide.$( By.className( "sidebar" ) );
    }

}
