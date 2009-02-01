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

package org.apache.wiki.util;


import org.apache.wiki.ui.migrator.JspParserTest;

import junit.framework.*;

public class AllTests extends TestCase
{
    public AllTests( String s )
    {
        super( s );
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite("Utility suite tests");

        suite.addTest( ClassUtilTest.suite() );
        suite.addTest( CommentedPropertiesTest.suite() );
        suite.addTest( CryptoUtilTest.suite() );
        suite.addTest( JspParserTest.suite() );
        suite.addTest( MailUtilTest.suite() );
        suite.addTest( PriorityListTest.suite() );
        suite.addTest( SerializerTest.suite() );
        suite.addTest( TextUtilTest.suite() );
        suite.addTest( TimedCounterListTest.suite() );
        suite.addTest( UtilJ2eeCompatTest.suite() );
        
        return suite;
    }
}
