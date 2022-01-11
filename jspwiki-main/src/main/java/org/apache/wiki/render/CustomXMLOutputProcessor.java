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

import org.jdom2.output.Format;
import org.jdom2.output.support.AbstractXMLOutputProcessor;
import org.jdom2.output.support.FormatStack;

import java.io.IOException;
import java.io.Writer;

/**
 *  Override added to ensure attribute values including ampersands and quotes still get escaped even if
 *  disable-output-escaping processing instruction (meant to keep rest of HTML string as-is) set.
 *
 *  @since  2.10
 */
public class CustomXMLOutputProcessor extends AbstractXMLOutputProcessor {

    protected void attributeEscapedEntitiesFilter( final Writer out, final FormatStack fstack, final String value ) throws IOException {
        write( out, Format.escapeAttribute( fstack.getEscapeStrategy(), value ) );
    }

}
