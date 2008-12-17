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
package com.ecyrd.jspwiki.parser;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class MarkupParserTest extends TestCase
{

    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(MarkupParserTest.class);
    }

    protected void setUp() throws Exception
    {
        super.setUp();
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

    public void testCleanLink1()
    {
        assertEquals( "--CleanLink--", MarkupParser.cleanLink("--CleanLink--") );
    }

    public void testCleanLink2()
    {
        assertEquals( "CleanLink", MarkupParser.cleanLink("??CleanLink??") );
    }
    
    public void testCleanLink3()
    {
        assertEquals( "Clean (link)", MarkupParser.cleanLink("Clean (link)") );
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(MarkupParserTest.class);

        return suite;
    }
}
