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

import com.codeborne.selenide.CollectionCondition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import org.apache.wiki.pages.Page;
import org.openqa.selenium.By;

import java.util.List;

/**
 * Actions available on the Search Results page.
 */
public class SearchResultsPage implements HaddockPage {

    private static final String SEARCH_PAGE_NAME_RESULTS = ".wikitable.table-striped tr:not(:first-child) td:first-child";

    /**
     * Open the search results page with a given query text to search for.
     *
     * @param pageName Wiki page name to View.
     * @return {@link ReadWikiPage} instance, to allow chaining of actions.
     */
    public static SearchResultsPage open( final String pageName ) {
        return Page.withUrl( Page.baseUrl() + "/Search.jsp?query=" + pageName ).openAs( new SearchResultsPage() );
    }

    /**
     * Returns the search result page names.
     *
     * @return the list of page names returned by the search query.
     */
    public List< String > pagesFound() {
        return Selenide.$$( By.cssSelector( SEARCH_PAGE_NAME_RESULTS ) ).texts();
    }

    /**
     * Ensures that the given page names are present on the search results.
     *
     * @param pageNames page names to look for.
     * @return {@link SearchResultsPage} instance, to allow chaining of actions.
     */
    public SearchResultsPage shouldContain( final String... pageNames ) {
        final ElementsCollection resultsTableRows = Selenide.$$( By.cssSelector( SEARCH_PAGE_NAME_RESULTS ) );
        for( final String pageName : pageNames ) {
            resultsTableRows.shouldHave( CollectionCondition.itemWithText( pageName ) );
        }

        return this;
    }

    /**
     * Navigates to a view page from the search results.
     *
     * @param result wikipage name to navigate to.
     * @return {@link ReadWikiPage} instance, to allow chaining of actions.
     */
    public ReadWikiPage navigateTo( final String result ) {
        Selenide.$( By.cssSelector( ".wikitable.table-striped" ) ).find( By.linkText( result ) ).click();
        return new ReadWikiPage();
    }

}
