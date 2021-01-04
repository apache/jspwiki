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

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import org.apache.wiki.pages.Page;
import org.openqa.selenium.By;

import java.time.Duration;

/**
 * Actions available on the Edit page.
 */
public class EditWikiPage implements HaddockPage {

    private static final String EDIT_TEXTAREA = "textarea.editor.form-control.snipeable";

    /**
     * Open a given page for edition.
     *
     * @param pageName Wiki page name to Edit.
     * @return {@link EditWikiPage} instance, to allow chaining of actions.
     */
    public static EditWikiPage open( final String pageName ) {
        return Page.withUrl( Page.baseUrl() + "/Edit.jsp?page=" + pageName ).openAs( new EditWikiPage() );
    }

    /**
     * Press the cancel button and disacrd page Edit.
     *
     * @return {@link ViewWikiPage} instance, to allow chaining of actions.
     */
    public ViewWikiPage cancel() {
        Selenide.$( By.name( "cancel" ) ).click();
        return new ViewWikiPage();
    }

    /**
     * Edits the page with the given text. Ensures edition is complete by ensuring the preview pane shows the edited text.
     *
     * @param text text to edit.
     * @return {@link ViewWikiPage} instance, to allow chaining of actions.
     */
    public ViewWikiPage saveText(final String text ) {
        return saveText( text, text );
    }

    /**
     * Edits the page with the given text. Ensures edition is complete by ensuring the preview pane shows the preview text.
     *
     * @param text text text to edit.
     * @param preview expected text to hsow up on the preview pane (i.e., page directives on edit pane shouldn't show up here).
     * @return {@link ViewWikiPage} instance, to allow chaining of actions.
     */
    public ViewWikiPage saveText(final String text, final String preview ) {
        Selenide.$( By.cssSelector( EDIT_TEXTAREA ) ).clear();
        Selenide.$( By.cssSelector( EDIT_TEXTAREA ) ).val( text );
        Selenide.$( By.className( "ajaxpreview" ) ).shouldBe( Condition.text( preview ), Duration.ofSeconds( 1L ) );
        Selenide.$( By.name( "ok" ) ).click();
        return new ViewWikiPage();
    }

}
