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
package org.apache.wiki.htmltowiki.syntax;

import org.jdom2.Text;

import java.io.PrintWriter;
import java.util.Deque;


/**
 * Translates to wiki syntax from a {@link Text} element.
 */
public class TextElementDecorator {

    final protected PrintWriter out;
    final protected Deque< String > preStack;

    public TextElementDecorator( final PrintWriter out, final Deque< String > preStack ) {
        this.out = out;
        this.preStack = preStack;
    }

    /**
     * Translates the given XHTML element into wiki markup.
     *
     * @param element XHTML element being translated.
     */
    public void decorate( final Text element ) {
        String s = element.getText();
        if( preStack.isEmpty() ) {
            // remove all "line terminator" characters
            s = s.replaceAll( "[\\r\\n\\f\\u0085\\u2028\\u2029]", "" );
        }
        out.print( s );
    }

}
