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
package org.apache.wiki.its.environment;


import com.codeborne.selenide.Configuration;

/**
 * Tests' environment values that can be overwritten through System properties.
 */
public class Env {
    
    /** Base url on which the functional tests are run. Default value is {@code https://jspwiki-wiki.apache.org}. */
    public static final String TESTS_BASE_URL = System.getProperty( "it-jspwiki.base.url", "https://jspwiki-wiki.apache.org" );

    /** Selenide tests download's folder. Default value is {@code ./target/downloads}. */
    public static final String TESTS_CONFIG_DOWNLOADS_FOLDER = System.getProperty( "it-jspwiki.config.download-folder", "./target/downloads" );
    
    /** Should the browser start on headless mode? Only for Firefox / Chrome. Default value is {@code false}. */
    public static final boolean TESTS_CONFIG_HEADLESS = Boolean.parseBoolean( System.getProperty( "it-jspwiki.config.headless", "false" ) );

    /** Selenide tests reports' folder. Default value is {@code ./target/selenide}. */
    public static final String TESTS_CONFIG_REPORTS_FOLDER = System.getProperty( "it-jspwiki.config.reports", "./target/selenide" );

    /** Amount of time, in milliseconds, to wait for the search index tasks to complete. Default value is {@code 1200}. */
    public static final long TESTS_CONFIG_SEARCH_INDEX_WAIT = Long.parseLong( System.getProperty( "it-jspwiki.config.search-index-wait", "1200" ) );

    /** Which size should start the browser with?. Default value is {@code 1366x768}. */
    public static final String TESTS_CONFIG_BROWSER_SIZE = System.getProperty( "it-jspwiki.config.browser-size", "1366x768" );

    /** Folder where the WebDriver will be downloaded. Default value is {@code ./target/wdm}. */
    public static final String TESTS_CONFIG_WDM_TARGET_PATH = System.getProperty( "it-jspwiki.config.wdm.target-path", "./target/wdm" );

    /** Janne's username. Default value is {@code janne}. */
    public static final String LOGIN_JANNE_USERNAME = System.getProperty( "it-jspwiki.login.janne.username", "janne" );

    /** Janne's password. Default value is {@code myP@5sw0rd}. */
    public static final String LOGIN_JANNE_PASSWORD = System.getProperty( "it-jspwiki.login.janne.password", "myP@5sw0rd" );

    public static void setUp() {
        Configuration.headless = Env.TESTS_CONFIG_HEADLESS;
        Configuration.fastSetValue = true; // default value seems to not send `[` or `{` characters to input controls. weird.
        Configuration.reportsFolder = Env.TESTS_CONFIG_REPORTS_FOLDER;
        Configuration.browserSize = Env.TESTS_CONFIG_BROWSER_SIZE;
        Configuration.downloadsFolder = Env.TESTS_CONFIG_DOWNLOADS_FOLDER;
        System.setProperty( "wdm.targetPath", Env.TESTS_CONFIG_WDM_TARGET_PATH );
    }

}
