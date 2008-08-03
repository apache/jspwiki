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
import java.io.StringWriter;

import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.parser.WikiDocument;

/**
 *  Implements a WikiRendered that outputs XHTML.  Because the internal DOM
 *  representation is in XHTML already, this just basically dumps out everything
 *  out in a non-prettyprinted format.
 *  
 *  @since  2.4
 */
public class XHTMLRenderer
    extends WikiRenderer 
{
    private static final String LINEBREAK = "\n";

    /**
     *  Creates an XHTML 1.0 renderer.
     *  
     *  @param context {@inheritDoc}
     *  @param doc {@inheritDoc}
     */
    public XHTMLRenderer( WikiContext context, WikiDocument doc )
    {
        super( context, doc );
    }
    
    /**
     *  {@inheritDoc}
     */
    public String getString()
        throws IOException
    {
        m_document.setContext( m_context );

        XMLOutputter output = new XMLOutputter();
        
        StringWriter out = new StringWriter();
        
        Format fmt = Format.getRawFormat();
        fmt.setExpandEmptyElements( false );
        fmt.setLineSeparator( LINEBREAK );

        output.setFormat( fmt );
        output.outputElementContent( m_document.getRootElement(), out );
        
        return out.toString();
    }
}
