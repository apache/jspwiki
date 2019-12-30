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

import java.io.IOException;

/**
 *  Provides an interface to the basic rendering engine. This class is an abstract class instead of an interface because
 *  it is expected that rendering capabilities are increased at some point, and I would hate if renderers broke.
 *  This class allows some sane defaults to be implemented.
 *
 *  @since  2.4
 */
public abstract class WikiRenderer {

    protected WikiContext     m_context;
    protected WikiDocument    m_document;

    public static final String LINKS_TRANSLATION = "$1#$2";
    public static final String LINKS_SOURCE = "(.+)#section-.+-(.+)";

    /**
     *  Create a WikiRenderer.
     *
     *  @param context A WikiContext in which the rendering will take place.
     *  @param doc The WikiDocument which shall be rendered.
     */
    protected WikiRenderer( final WikiContext context, final WikiDocument doc ) {
        m_context = context;
        m_document = doc;
        doc.setContext( context ); // Make sure it is set
    }

    /**
     *  Renders and returns the end result.
     *
     *  @return A rendered string.
     *  @throws IOException If rendering fails.
     */
    public abstract String getString()
        throws IOException;

}
