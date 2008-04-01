/* 
    JSPWiki - a JSP-based WikiWiki clone.

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
package com.ecyrd.jspwiki.render;

import java.io.IOException;

import com.ecyrd.jspwiki.TextUtil;
import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.parser.MarkupParser;
import com.ecyrd.jspwiki.parser.WikiDocument;

/**
 *  Provides an interface to the basic rendering engine.
 *  This class is an abstract class instead of an interface because
 *  it is expected that rendering capabilities are increased at some
 *  point, and I would hate if renderers broke.  This class allows
 *  some sane defaults to be implemented.
 *  
 *  @since  2.4
 */
public abstract class WikiRenderer
{
    protected WikiContext     m_context;
    protected WikiDocument    m_document;
    protected boolean         m_enablePlugins = true;
    
    protected WikiRenderer( WikiContext context, WikiDocument doc )
    {
        m_context = context;
        m_document = doc;
        doc.setContext( context ); // Make sure it is set
        
        //
        //  Do some sane defaults
        //
        WikiEngine engine = m_context.getEngine();
        String runplugins = engine.getVariable( m_context, MarkupParser.PROP_RUNPLUGINS );
        if( runplugins != null ) enablePlugins( TextUtil.isPositive(runplugins));
    }

    /**
     *  Can be used to turn on plugin execution on a translator-reader basis
     */
    public void enablePlugins( boolean toggle )
    {
        m_enablePlugins = toggle;
    }

    public abstract String getString()
        throws IOException;

}
