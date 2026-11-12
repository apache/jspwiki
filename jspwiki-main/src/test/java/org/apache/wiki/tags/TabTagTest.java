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
package org.apache.wiki.tags;

import jakarta.servlet.jsp.JspWriter;
import jakarta.servlet.jsp.PageContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;


public class TabTagTest {

    /** The url of a tab is written in a single quoted attribute, so a quote in it must not end the attribute. */
    @Test
    public void testSingleQuoteOfTheUrlIsEscaped() throws Exception {
        final TabbedSectionTag parent = Mockito.mock( TabbedSectionTag.class );
        Mockito.when( parent.isStateFindDefaultTab() ).thenReturn( false );
        Mockito.when( parent.isStateGenerateTabBody() ).thenReturn( false );
        Mockito.when( parent.isStateGenerateTabMenu() ).thenReturn( true );
        Mockito.when( parent.validateDefaultTab( "tab" ) ).thenReturn( true );
        final JspWriter out = Mockito.mock( JspWriter.class );
        final PageContext pageContext = Mockito.mock( PageContext.class );
        Mockito.when( pageContext.getOut() ).thenReturn( out );

        final TabTag tag = new TabTag();
        tag.setPageContext( pageContext );
        tag.setParent( parent );
        tag.setId( "tab" );
        tag.setTitle( "Title" );
        tag.setAccesskey( "e" );
        tag.setUrl( "EditGroup.jsp?group=' x='y" );
        tag.doEndTag();

        final ArgumentCaptor< String > html = ArgumentCaptor.forClass( String.class );
        Mockito.verify( out ).write( html.capture() );
        Assertions.assertTrue( html.getValue().contains( " href='EditGroup.jsp?group=&#39; x=&#39;y' accesskey=" ), html.getValue() );
    }

    /** A tab without a url, which a scriptlet expression may pass as null, is written without a link target. */
    @Test
    public void testNullUrlIsLeftOut() throws Exception {
        final TabbedSectionTag parent = Mockito.mock( TabbedSectionTag.class );
        Mockito.when( parent.isStateFindDefaultTab() ).thenReturn( false );
        Mockito.when( parent.isStateGenerateTabBody() ).thenReturn( false );
        Mockito.when( parent.isStateGenerateTabMenu() ).thenReturn( true );
        Mockito.when( parent.validateDefaultTab( "tab" ) ).thenReturn( true );
        final JspWriter out = Mockito.mock( JspWriter.class );
        final PageContext pageContext = Mockito.mock( PageContext.class );
        Mockito.when( pageContext.getOut() ).thenReturn( out );

        final TabTag tag = new TabTag();
        tag.setPageContext( pageContext );
        tag.setParent( parent );
        tag.setId( "tab" );
        tag.setTitle( "Title" );
        tag.setUrl( null );
        tag.doEndTag();

        final ArgumentCaptor< String > html = ArgumentCaptor.forClass( String.class );
        Mockito.verify( out ).write( html.capture() );
        Assertions.assertFalse( html.getValue().contains( "href=" ), html.getValue() );
    }

}
