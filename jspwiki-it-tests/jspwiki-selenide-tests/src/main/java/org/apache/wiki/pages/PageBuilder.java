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

import com.codeborne.selenide.Selenide;


/**
 * Builder for {@link Page} Objects.
 */
public class PageBuilder {

    final String url;
    final Page page;

    PageBuilder( final String url, final Page page ) {
        this.url = url;
        this.page = page;
    }

    /**
     * Sets up Selenide and opens the requested URL, allowing chaining additional operations over the given {@link Page} instance.
     *
     * @param page {@link Page} instance, in order to allow chaining operations.
     * @return {@link Page} instance given on the {@link PageBuilder} constructor.
     */
    public < T extends Page > T openAs( final T page ) {
        /* Configuration.headless = Env.TESTS_CONFIG_HEADLESS;
        Configuration.fastSetValue = true; // default value seems to not send `[` or `{` characters to input controls. weird.
        Configuration.reportsFolder = Env.TESTS_CONFIG_REPORTS_FOLDER;
        Configuration.browserSize = Env.TESTS_CONFIG_BROWSER_SIZE;
        Configuration.downloadsFolder = Env.TESTS_CONFIG_DOWNLOADS_FOLDER;
        System.setProperty( "wdm.targetPath", Env.TESTS_CONFIG_WDM_TARGET_PATH ); */
        
        Selenide.open( url );
        return page;
    }

}
