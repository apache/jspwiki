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

package com.ecyrd.jspwiki;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class AllTests extends TestCase
{
 
    public AllTests( String s )
    {
        super( s );
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite("JSPWiki Unit Tests");

        suite.addTest( FileUtilTest.suite() );
        suite.addTest( PageManagerTest.suite() );
        suite.addTest( PropertyReaderTest.suite() );
        suite.addTest( ReferenceManagerTest.suite() );
        suite.addTest( ReleaseTest.suite() );
        suite.addTest( TextUtilTest.suite() );
        suite.addTest( VariableManagerTest.suite() );
        // This is obsolete and not maintained anymore
        // suite.addTest( TranslatorReaderTest.suite() );
        suite.addTest( WikiSessionTest.suite() );
        suite.addTest( WikiEngineTest.suite() );
        suite.addTest( com.ecyrd.jspwiki.action.AllTests.suite() );
        suite.addTest( com.ecyrd.jspwiki.content.AllTests.suite() );
        suite.addTest( com.ecyrd.jspwiki.attachment.AllTests.suite() );
        suite.addTest( com.ecyrd.jspwiki.auth.AllTests.suite() );
        suite.addTest( com.ecyrd.jspwiki.dav.AllTests.suite() );
        suite.addTest( com.ecyrd.jspwiki.diff.AllTests.suite() );
        suite.addTest( com.ecyrd.jspwiki.filters.AllTests.suite() );
        suite.addTest( com.ecyrd.jspwiki.htmltowiki.AllTests.suite() );
        suite.addTest( com.ecyrd.jspwiki.parser.AllTests.suite() );
        suite.addTest( com.ecyrd.jspwiki.plugin.AllTests.suite() );
        suite.addTest( com.ecyrd.jspwiki.providers.AllTests.suite() );
        suite.addTest( com.ecyrd.jspwiki.render.AllTests.suite() );
        suite.addTest( com.ecyrd.jspwiki.rss.AllTests.suite() );
        suite.addTest( com.ecyrd.jspwiki.search.AllTests.suite() );
        suite.addTest( com.ecyrd.jspwiki.ui.AllTests.suite() );
        suite.addTest( com.ecyrd.jspwiki.url.AllTests.suite() );
        suite.addTest( com.ecyrd.jspwiki.util.AllTests.suite() );
        // These are not runnable without a running tomcat
        //suite.addTest( com.ecyrd.jspwiki.web.AllTests.suite() );
        suite.addTest( com.ecyrd.jspwiki.workflow.AllTests.suite() );
        suite.addTest( com.ecyrd.jspwiki.xmlrpc.AllTests.suite() );
        
        return suite;
    }
}
