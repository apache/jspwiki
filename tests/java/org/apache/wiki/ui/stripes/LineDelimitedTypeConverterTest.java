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
package org.apache.wiki.ui.stripes;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class LineDelimitedTypeConverterTest extends TestCase
{
    public void testLinefeedSplitLines() throws Exception
    {
        LineDelimitedTypeConverter c = new LineDelimitedTypeConverter();
        String r = c.getSplitRegex();
        String[] splits;
               
        splits = "Andrew Jaquith\r\nBeka".split( r );
        assertEquals( 2, splits.length );
        assertEquals( "Andrew Jaquith", splits[0] );
        assertEquals( "Beka", splits[1] );

        splits = "Andrew Jaquith  \r\n  Beka".split( r );
        assertEquals( 2, splits.length );
        assertEquals( "Andrew Jaquith", splits[0] );
        assertEquals( "Beka", splits[1] );
        
        splits = "Andrew Jaquith\nBeka".split( r );
        assertEquals( 2, splits.length );
        assertEquals( "Andrew Jaquith", splits[0] );
        assertEquals( "Beka", splits[1] );
        
        splits = "Andrew Jaquith  \n   Beka".split( r );
        assertEquals( 2, splits.length );
        assertEquals( "Andrew Jaquith", splits[0] );
        assertEquals( "Beka", splits[1] );
        
        splits = "Andrew Jaquith  \n\n   Beka".split( r );
        assertEquals( 2, splits.length );
        assertEquals( "Andrew Jaquith", splits[0] );
        assertEquals( "Beka", splits[1] );

        splits = "Andrew Jaquith \t\n   Beka".split( r );
        assertEquals( "Andrew Jaquith", splits[0] );
        assertEquals( 2, splits.length );
        assertEquals( "Beka", splits[1] );
    }

    public void testCommaSplitLines() throws Exception
    {
        LineDelimitedTypeConverter c = new LineDelimitedTypeConverter();
        String r = c.getSplitRegex();
        String[] splits;
               
        splits = "Andrew Jaquith,Beka".split( r );
        assertEquals( 2, splits.length );
        assertEquals( "Andrew Jaquith", splits[0] );
        assertEquals( "Beka", splits[1] );

        splits = "Andrew Jaquith  ,  Beka".split( r );
        assertEquals( 2, splits.length );
        assertEquals( "Andrew Jaquith", splits[0] );
        assertEquals( "Beka", splits[1] );
        
        splits = "Andrew Jaquith  ,,  Beka".split( r );
        assertEquals( 2, splits.length );
        assertEquals( "Andrew Jaquith", splits[0] );
        assertEquals( "Beka", splits[1] );

        splits = "Andrew Jaquith \t,   Beka".split( r );
        assertEquals( 2, splits.length );
        assertEquals( "Andrew Jaquith", splits[0] );
        assertEquals( "Beka", splits[1] );
    }

    public static Test suite()
    {
        return new TestSuite( LineDelimitedTypeConverterTest.class );
    }
}
