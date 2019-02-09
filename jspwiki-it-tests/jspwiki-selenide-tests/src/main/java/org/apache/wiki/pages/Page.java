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
package org.apache.wiki.pages;

import java.io.File;
import java.io.IOException;

import org.apache.wiki.its.environment.Env;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebDriverRunner;


/**
 * Common operations for Page Objects.
 * 
 * @see https://selenide.gitbooks.io/user-guide/content/en/pageobjects.html
 */
public interface Page {

    /**
     * Creates a new {@link PageBuilder} in order to customize page creation.
     *
     * @param url url to open in the browser.
     * @return {@link PageBuilder} instance to allow page creation customization.
     */
    static PageBuilder withUrl( final String url ) {
        return new PageBuilder( url, null );
    }

    /**
     * returns the base URL on which the tests are run.
     *
     * @return the base URL on which the tests are run.
     */
    static String baseUrl() {
        return Env.TESTS_BASE_URL;
    }
    
    static File download( String url ) throws IOException {
        return Selenide.download( url );
    }

    /**
     * returns the actual page title.
     *
     * @return the actual page title.
     */
    default String title() {
        return Selenide.title();
    }

    /**
     * returns the actual URL.
     *
     * @return the actual URL.
     */
    default String url() {
        return WebDriverRunner.url();
    }
    
    /**
     * returns page's wiki title.
     * 
     * @return page's wiki title.
     */
    String wikiTitle();
    
    /**
     * returns page's wiki content.
     * 
     * @return page's wiki content.
     */
    String wikiPageContent();

}
