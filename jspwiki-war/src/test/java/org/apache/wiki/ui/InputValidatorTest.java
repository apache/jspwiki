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
/*
 * (C) Janne Jalkanen 2005
 * 
 */
package org.apache.wiki.ui;

import java.util.Properties;
import java.util.regex.Pattern;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiPage;

public class InputValidatorTest extends TestCase
{
    TestEngine     testEngine;

    InputValidator val;

    String         TEST  = "test";

    protected void setUp() throws Exception
    {
        Properties props = TestEngine.getTestProperties();
        testEngine = new TestEngine( props );
        WikiContext context = new WikiContext( testEngine, new WikiPage(testEngine,"dummyPage"));
        val = new InputValidator( TEST, context );
    }

    public void testUnsafePattern()
    {
        Pattern unsafe = InputValidator.UNSAFE_PATTERN;
        assertFalse( unsafe.matcher( "a b c d e f g" ).find() );
        assertTrue( unsafe.matcher( "<a> b c d e f g" ).find() );
        assertTrue( unsafe.matcher( "foo$" ).find() );
    }

    public void testValidate()
    {
        assertTrue( val.validate( "Test string", "Name", InputValidator.STANDARD ) );
        assertFalse( val.validate( "Test $tring", "Name", InputValidator.STANDARD ) );
        assertFalse( val.validate( "Test <string>", "Name", InputValidator.STANDARD ) );
        assertFalse( val.validate( "Test & string", "Name", InputValidator.STANDARD ) );
        assertFalse( val.validate( "Test @ string", "Name", InputValidator.STANDARD ) );
        
        // Null or blank fields should validate
        assertTrue( val.validate( "", "Name", InputValidator.STANDARD ) );
        assertTrue( val.validate( null, "Name", InputValidator.STANDARD ) );
    }
    
    public void testValidateNotNull()
    {
        assertTrue( val.validateNotNull("Test string", "Name") );
        assertFalse( val.validateNotNull("Test $tring", "Name") );
        assertFalse( val.validateNotNull("", "Name") );
        assertFalse( val.validateNotNull(null, "Name") );
    }
    
    public void testValidateEmail()
    {
        assertTrue( val.validateNotNull("foo@bar.com", "E-mail", InputValidator.EMAIL) );
        assertTrue( val.validateNotNull("foo-bar@foo.com", "E-mail", InputValidator.EMAIL) );
        assertTrue( val.validateNotNull("foo-bar@foo.co.uk", "E-mail", InputValidator.EMAIL) );
        assertTrue( val.validateNotNull("foo+bar@foo.co.uk", "E-mail", InputValidator.EMAIL) );
        assertTrue( val.validateNotNull("foo.bar@foo.co.uk", "E-mail", InputValidator.EMAIL) );
        assertFalse( val.validateNotNull("foobar", "E-mail", InputValidator.EMAIL) );
        assertFalse( val.validateNotNull("foobar@foo", "E-mail", InputValidator.EMAIL) );
    }

    public static Test suite()
    {
        return new TestSuite( InputValidatorTest.class );
    }
}
