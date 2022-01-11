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

import org.apache.wiki.pages.Page;
import org.apache.wiki.pages.haddock.ViewWikiPage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;

import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.not;


/**
 * Anonymous view related tests for Apache JSPWiki
 */
public class AnonymousViewIT extends WithIntegrationTestSetup {
    
    @Test
    void anonymousView() {
        final ViewWikiPage main = ViewWikiPage.open( "Main" );
        Assertions.assertEquals( "JSPWiki: Main", main.title() );
        Assertions.assertEquals( "Main", main.wikiTitle() );
        
        Assertions.assertTrue( main.wikiPageContent().contains( "You have successfully installed" ) );
        final ViewWikiPage about = main.navigateTo( "JSPWiki" );
        Assertions.assertTrue( about.wikiPageContent().contains( "This Wiki is done using" ) );
    }
    
    @Test
    void anonymousViewImage() throws Exception {
        final File file = Page.download( Page.baseUrl() + "/images/jspwiki_logo_s.png" );
        Assertions.assertTrue( file.exists() );
    }
    
    @Test
    void anonymousReaderView() {
        final ViewWikiPage main = ViewWikiPage.open( "Main" );
        Assertions.assertEquals( "JSPWiki: Main", main.title() );
        Assertions.assertEquals( "Main", main.wikiTitle() );
        main.sidebar().should( exist );
        
        main.hoverMoreArea()
            .clickOnShowReaderView()
            .sidebar().should( not( exist ) );
    }

}
