/*
 * (C) Janne Jalkanen 2005
 * 
 */
package com.ecyrd.jspwiki.ui;

import java.util.Properties;
import java.util.regex.Pattern;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.ecyrd.jspwiki.TestEngine;
import com.ecyrd.jspwiki.TestHttpServletRequest;
import com.ecyrd.jspwiki.WikiSession;

public class InputValidatorTest extends TestCase
{
    TestEngine     testEngine;

    InputValidator val;

    String         TEST  = "test";

    protected void setUp() throws Exception
    {
        Properties props = new Properties();
        props.load( TestEngine.findTestProperties() );
        testEngine = new TestEngine( props );
        WikiSession session = WikiSession.getWikiSession( testEngine, new TestHttpServletRequest() );
        val = new InputValidator( TEST, session );
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
