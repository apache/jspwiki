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


/**
 * Tests' environment values that can be overwritten through System properties.
 */
public class Env {
    
    /** Base url on which the functional tests are run. Default value is {@code https://jspwiki-wiki.apache.org}. */
    public static String TESTS_BASE_URL = System.getProperty( "it-jspwiki.base.url", "https://jspwiki-wiki.apache.org" );
    
    /** Should the browser start on headless mode? Only for Firefox / Chrome. Default value is {@code false}. */
    public static boolean TESTS_CONFIG_HEADLESS = Boolean.valueOf( System.getProperty( "it-jspwiki.config.headless", "false" ) );

    /** Selenide tests reports' folder. Default value is {@code ./target/selenide}. */
    public static String TESTS_CONFIG_REPORTS_FOLDER = System.getProperty( "it-jspwiki.config.headless", "./target/selenide" );

    /** Should the browser start maximized?. Default value is {@code true}. */
    public static boolean TESTS_CONFIG_START_MAXIMIZED = Boolean.valueOf( System.getProperty( "it-jspwiki.config.start-maximized", "true" ) );

    /** Folder where the WebDriver will be downloaded. Default value is {@code ./target/wdm}. */
    public static String TESTS_CONFIG_WDM_TARGET_PATH = System.getProperty( "it-jspwiki.config.wdm.target-path", "./target/wdm" );

    /** Janne's username. Default value is {@code janne}. */
    public static String LOGIN_JANNE_USERNAME = System.getProperty( "it-jspwiki.login.janne.username", "janne" );

    /** Janne's password. Default value is {@code myP@5sw0rd}. */
    public static String LOGIN_JANNE_PASSWORD = System.getProperty( "it-jspwiki.login.janne.password", "myP@5sw0rd" );

}
