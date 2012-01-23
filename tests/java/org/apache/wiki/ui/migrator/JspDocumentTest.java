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
package org.apache.wiki.ui.migrator;


import org.apache.wiki.ui.migrator.JspDocument;
import org.apache.wiki.ui.migrator.JspParser;
import org.apache.wiki.ui.migrator.Node;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class JspDocumentTest extends TestCase
{
    public JspDocumentTest( String s )
    {
        super( s );
    }

    public void testSetName() throws Exception
    {
        // Parse <foo></foo>
        JspParser parser = new JspParser();
        String s = "<foo></foo>";
        JspDocument doc = parser.parse( s );

        assertEquals( 2, doc.getNodes().size() );
        assertEquals( s, doc.toString() );

        // Make sure 2 tags were parsed correctly.
        Node startTag = doc.getNodes().get( 0 );
        Node endTag = doc.getNodes().get( 1 );
        assertEquals( 0, startTag.getStart() );
        assertEquals( 5, startTag.getEnd() );
        assertEquals( "foo", startTag.getName() );
        assertEquals( 5, endTag.getStart() );
        assertEquals( 11, endTag.getEnd() );
        assertEquals( "foo", endTag.getName() );
    }

    public static Test suite()
    {
        return new TestSuite( JspDocumentTest.class );
    }
}
