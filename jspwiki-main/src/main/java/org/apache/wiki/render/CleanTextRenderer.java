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
package org.apache.wiki.render;

import org.apache.wiki.WikiContext;
import org.apache.wiki.parser.WikiDocument;
import org.apache.wiki.util.XmlUtil;


/**
 *  A simple renderer that just renders all the text() nodes from the DOM tree. This is very useful for cleaning away all of the XHTML.
 *
 *  @since  2.4
 */
public class CleanTextRenderer extends WikiRenderer {

    /**
     *  Create a renderer.
     *
     *  @param context A WikiContext in which the rendering will take place.
     *  @param doc The WikiDocument which shall be rendered.
     */
    public CleanTextRenderer( final WikiContext context, final WikiDocument doc ) {
        super( context, doc );
    }

    /**
     *  {@inheritDoc}
     */
    public String getString() {
    	return m_document != null ? XmlUtil.extractTextFromDocument( m_document.getDocument() ) : "";
    }

}
