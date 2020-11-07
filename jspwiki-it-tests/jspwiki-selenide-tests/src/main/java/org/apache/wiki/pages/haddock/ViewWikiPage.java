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

import org.apache.wiki.pages.Page;
import org.openqa.selenium.By;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;

/**
 * Actions available on the View page.
 */
public class ViewWikiPage implements HaddockPage {

    /**
     * Open a given page for view.
     *
     * @param pageName Wiki page name to View.
     * @return {@link ViewWikiPage} instance, to allow chaining of actions.
     */
    public static ViewWikiPage open(final String pageName ) {
        return Page.withUrl( Page.baseUrl() + "/Wiki.jsp?page=" + pageName ).openAs( new ViewWikiPage() );
    }

    /**
     * Returns the authenticated user text.
     *
     * @return the authenticated user text.
     */
    public String authenticatedText() {
        return Selenide.$( By.className( "wikipage" ) ).text().trim();
    }

    /**
     * Clicks on the login button.
     *
     * @return {@link LoginPage} instance, to allow chaining of actions.
     */
    public LoginPage clickOnLogin() {
        hoverLoginArea();
        Selenide.$( By.className( "icon-signin" ) ).click(); 
        return new LoginPage();
    }

    /**
     * Clicks on the show reader view link.
     *
     * @return {@link ViewWikiPage} instance, to allow chaining of actions.
     */
    public ViewWikiPage clickOnShowReaderView() {
        Selenide.$( By.linkText( "Show Reader View" ) ).click();
        return this;
    }

    /**
     * Hover's the user's icon, so the login area gets visible.
     *
     * @return {@link ViewWikiPage} instance, to allow chaining of actions.
     */
    public ViewWikiPage hoverLoginArea() {
        Selenide.$( By.className( "icon-user" ) ).hover();
        return this;
    }

    /**
     * Hover's the More tab, making its associated pane visible.
     *
     * @return {@link ViewWikiPage} instance, to allow chaining of actions.
     */
    public ViewWikiPage hoverMoreArea() {
        Selenide.$( By.id( "more" ) ).hover();
        return this;
    }

    /**
     * Clicks the edit link.
     *
     * @return {@link EditWikiPage} instance, to allow chaining of actions.
     */
    public EditWikiPage editPage() {
        Selenide.$( By.cssSelector( "li#edit a" ) ).waitUntil( Condition.visible, 1_000L ).click();
        return new EditWikiPage();
    }

    /**
     * Searches for a given text.
     *
     * @param text text to search for.
     * @return {@link SearchResultsPage} instance, to allow chaining of actions.
     */
    public SearchResultsPage searchFor( final String text ) {
        Selenide.$( By.id( "searchForm" ) ).hover();
        Selenide.$( By.id( "query" ) ).click();
        Selenide.$( By.id( "query" ) ).val( text );
        Selenide.$( By.id( "searchSubmit" ) ).click();
        return new SearchResultsPage();
    }

    /**
     * Logs the user out.
     *
     * @return {@link ViewWikiPage} instance, to allow chaining of actions.
     */
    public ViewWikiPage clickOnLogout() {
        hoverLoginArea();
        Selenide.$( By.linkText( "Log out" ) ).click();
        Selenide.$( By.className( "btn-success" ) ).waitUntil( Condition.visible, 1_000L ).click();
        return this;
    }

    /**
     * Navigates to a given view page.
     *
     * @param wikiPageName wikipage name to navigate to.
     * @return {@link ViewWikiPage} instance, to allow chaining of actions.
     */
    public ViewWikiPage navigateTo(final String wikiPageName ) {
        Selenide.$( By.linkText( wikiPageName ) ).click(); 
        return this;
    }

    /**
     * Returns the sidebar element.
     *
     * @return the sidebar element.
     */
    public SelenideElement sidebar() {
        return Selenide.$( By.className( "sidebar" ) );
    }

}
