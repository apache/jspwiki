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
package org.apache.wiki.util;

import java.io.File;
import java.util.Properties;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.wiki.api.exceptions.NoRequiredPropertyException;
import org.apache.wiki.providers.AbstractFileProvider;

public class TextUtilTest extends TestCase
{
    public TextUtilTest( String s )
    {
        super( s );
    }

    public void testGenerateRandomPassword()
    {
        for (int i=0; i<1000; i++)
        {
            assertEquals("pw", TextUtil.PASSWORD_LENGTH, TextUtil.generateRandomPassword().length());
        }
    }

    public static Test suite()
    {
        return new TestSuite( TextUtilTest.class );
    }
    
    public void testEncodeName_1()
    {
        String name = "Hello/World";

        assertEquals( "Hello/World",
                      TextUtil.urlEncode(name,"ISO-8859-1") );
    }

    public void testEncodeName_2()
    {
        String name = "Hello~World";

        assertEquals( "Hello%7EWorld",
                      TextUtil.urlEncode(name,"ISO-8859-1") );
    }

    public void testEncodeName_3()
    {
        String name = "Hello/World ~";

        assertEquals( "Hello/World+%7E",
                      TextUtil.urlEncode(name,"ISO-8859-1") );
    }

    public void testDecodeName_1()
         throws Exception
    {
        String name = "Hello/World+%7E+%2F";

        assertEquals( "Hello/World ~ /",
                      TextUtil.urlDecode(name,"ISO-8859-1") );
    }

    public void testEncodeNameUTF8_1()
    {
        String name = "\u0041\u2262\u0391\u002E";

        assertEquals( "A%E2%89%A2%CE%91.",
                      TextUtil.urlEncodeUTF8(name) );
    }

    public void testEncodeNameUTF8_2()
    {
        String name = "\uD55C\uAD6D\uC5B4";

        assertEquals( "%ED%95%9C%EA%B5%AD%EC%96%B4",
                      TextUtil.urlEncodeUTF8(name) );
    }

    public void testEncodeNameUTF8_3()
    {
        String name = "\u65E5\u672C\u8A9E";

        assertEquals( "%E6%97%A5%E6%9C%AC%E8%AA%9E",
                      TextUtil.urlEncodeUTF8(name) );
    }

    public void testEncodeNameUTF8_4()
    {
        String name = "Hello World";

        assertEquals( "Hello+World",
                      TextUtil.urlEncodeUTF8(name) );
    }

    public void testDecodeNameUTF8_1()
    {
        String name = "A%E2%89%A2%CE%91.";

        assertEquals( "\u0041\u2262\u0391\u002E",
                      TextUtil.urlDecodeUTF8(name) );
    }

    public void testDecodeNameUTF8_2()
    {
        String name = "%ED%95%9C%EA%B5%AD%EC%96%B4";

        assertEquals( "\uD55C\uAD6D\uC5B4",
                      TextUtil.urlDecodeUTF8(name) );
    }

    public void testDecodeNameUTF8_3()
    {
        String name = "%E6%97%A5%E6%9C%AC%E8%AA%9E";

        assertEquals( "\u65E5\u672C\u8A9E",
                      TextUtil.urlDecodeUTF8(name) );
    }

    public void testReplaceString1()
    {
        String text = "aabacaa";

        assertEquals( "ddbacdd", TextUtil.replaceString( text, "aa", "dd" ) ); 
    }

    public void testReplaceString4()
    {
        String text = "aabacaafaa";

        assertEquals( "ddbacddfdd", TextUtil.replaceString( text, "aa", "dd" ) ); 
    }

    public void testReplaceString5()
    {
        String text = "aaabacaaafaa";

        assertEquals( "dbacdfaa", TextUtil.replaceString( text, "aaa", "d" ) );     
    }

    public void testReplaceString2()
    {
        String text = "abcde";

        assertEquals( "fbcde", TextUtil.replaceString( text, "a", "f" ) ); 
    }

    public void testReplaceString3()
    {
        String text = "ababab";

        assertEquals( "afafaf", TextUtil.replaceString( text, "b", "f" ) ); 
    }
    
    public void testReplaceStringCaseUnsensitive1()
    {
        String text = "aABcAa";

        assertEquals( "ddBcdd", TextUtil.replaceStringCaseUnsensitive( text, "aa", "dd" ) ); 
    }

    public void testReplaceStringCaseUnsensitive2()
    {
        String text = "Abcde";

        assertEquals( "fbcde", TextUtil.replaceStringCaseUnsensitive( text, "a", "f" ) ); 
    }

    public void testReplaceStringCaseUnsensitive3()
    {
        String text = "aBAbab";

        assertEquals( "afAfaf", TextUtil.replaceStringCaseUnsensitive( text, "b", "f" ) ); 
    }
    
    public void testReplaceStringCaseUnsensitive4()
    {
        String text = "AaBAcAAfaa";

        assertEquals( "ddBAcddfdd", TextUtil.replaceStringCaseUnsensitive( text, "aa", "dd" ) ); 
    }

    public void testReplaceStringCaseUnsensitive5()
    {
        String text = "aAaBaCAAafaa";

        assertEquals( "dBaCdfaa", TextUtil.replaceStringCaseUnsensitive( text, "aaa", "d" ) );     
    }

    // Pure UNIX.
    public void testNormalizePostdata1()
    {
        String text = "ab\ncd";

        assertEquals( "ab\r\ncd\r\n", TextUtil.normalizePostData( text ) );
    }

    // Pure MSDOS.
    public void testNormalizePostdata2()
    {
        String text = "ab\r\ncd";

        assertEquals( "ab\r\ncd\r\n", TextUtil.normalizePostData( text ) );
    }

    // Pure Mac
    public void testNormalizePostdata3()
    {
        String text = "ab\rcd";

        assertEquals( "ab\r\ncd\r\n", TextUtil.normalizePostData( text ) );
    }

    // Mixed, ending correct.
    public void testNormalizePostdata4()
    {
        String text = "ab\ncd\r\n\r\n\r";

        assertEquals( "ab\r\ncd\r\n\r\n\r\n", TextUtil.normalizePostData( text ) );
    }

    // Multiple newlines
    public void testNormalizePostdata5()
    {
        String text = "ab\ncd\n\n\n\n";

        assertEquals( "ab\r\ncd\r\n\r\n\r\n\r\n", TextUtil.normalizePostData( text ) );
    }

    // Empty.
    public void testNormalizePostdata6()
    {
        String text = "";

        assertEquals( "\r\n", TextUtil.normalizePostData( text ) );
    }

    // Just a newline.
    public void testNormalizePostdata7()
    {
        String text = "\n";

        assertEquals( "\r\n", TextUtil.normalizePostData( text ) );
    }

    public void testGetBooleanProperty()
    {
        Properties props = new Properties();

        props.setProperty("foobar.0", "YES");
        props.setProperty("foobar.1", "true");
        props.setProperty("foobar.2", "false");
        props.setProperty("foobar.3", "no");
        props.setProperty("foobar.4", "on");
        props.setProperty("foobar.5", "OFF");
        props.setProperty("foobar.6", "gewkjoigew");

        assertTrue( "foobar.0", 
                    TextUtil.getBooleanProperty( props, "foobar.0", false ) );
        assertTrue( "foobar.1", 
                    TextUtil.getBooleanProperty( props, "foobar.1", false ) );

        assertFalse( "foobar.2", 
                     TextUtil.getBooleanProperty( props, "foobar.2", true ) );
        assertFalse( "foobar.3", 
                    TextUtil.getBooleanProperty( props, "foobar.3", true ) );
        assertTrue( "foobar.4", 
                    TextUtil.getBooleanProperty( props, "foobar.4", false ) );

        assertFalse( "foobar.5", 
                     TextUtil.getBooleanProperty( props, "foobar.5", true ) );

        assertFalse( "foobar.6", 
                     TextUtil.getBooleanProperty( props, "foobar.6", true ) );


    }

    public void testGetSection1()
        throws Exception
    {
        String src = "Single page.";

        assertEquals( "section 1", src, TextUtil.getSection(src,1) );

        try
        {
            TextUtil.getSection( src, 5 );
            fail("Did not get exception for 2");
        }
        catch( IllegalArgumentException e ) {}

        try
        {
            TextUtil.getSection( src, -1 );
            fail("Did not get exception for -1");
        }
        catch( IllegalArgumentException e ) {}
    }

    public void testGetSection2()
        throws Exception
    {
        String src = "First section\n----\nSecond section\n\n----\n\nThird section";

        assertEquals( "section 1", "First section\n", TextUtil.getSection(src,1) );
        assertEquals( "section 2", "\nSecond section\n\n", TextUtil.getSection(src,2) );
        assertEquals( "section 3", "\n\nThird section", TextUtil.getSection(src,3) );

        try
        {
            TextUtil.getSection( src, 4 );
            fail("Did not get exception for section 4");
        }
        catch( IllegalArgumentException e ) {}
    }

    public void testGetSection3()
        throws Exception
    {
        String src = "----\nSecond section\n----";

        
        assertEquals( "section 1", "", TextUtil.getSection(src,1) );
        assertEquals( "section 2", "\nSecond section\n", TextUtil.getSection(src,2) );
        assertEquals( "section 3", "", TextUtil.getSection(src,3) );

        try
        {
            TextUtil.getSection( src, 4 );
            fail("Did not get exception for section 4");
        }
        catch( IllegalArgumentException e ) {}
    }

    public void testGetSectionWithMoreThanFourDashes() throws Exception
    {
        String src = "----------------\nSecond section\n----";

        assertEquals("section 2", "\nSecond section\n", TextUtil.getSection(src, 2));
    }

    public void testBooleanParameter()
       throws Exception
    {
        assertEquals( "1", true, TextUtil.isPositive(" true ") );
        assertEquals( "2", false, TextUtil.isPositive(" fewqkfow kfpokwe ") );
        assertEquals( "3", true, TextUtil.isPositive("on") );
        assertEquals( "4", true, TextUtil.isPositive("\t\ton") );
    }
    
    public void testTrimmedProperty()
    {
        String[] vals = { "foo", " this is a property ", "bar", "60" };
        
        Properties props = TextUtil.createProperties(vals);
        
        assertEquals( "foo", "this is a property", TextUtil.getStringProperty(props,"foo","") );
        assertEquals( "bar", 60, TextUtil.getIntegerProperty(props,"bar",0) );
    }
    
    public void testGetRequiredProperty() throws Exception 
    {
        String[] vals = { "foo", " this is a property ", "bar", "60" };
        Properties props = TextUtil.createProperties(vals);
        assertEquals( "60", TextUtil.getRequiredProperty( props, "bar" ) );
    }
    
    public void testGetRequiredPropertyNRPE() 
    {
        String[] vals = { "foo", " this is a property ", "bar", "60" };
        Properties props = TextUtil.createProperties(vals);
        try
        {
            TextUtil.getRequiredProperty( props, "ber" );
            fail( "NoRequiredPropertyException should've been thrown!" );
        }
        catch (NoRequiredPropertyException nrpe) {}
    }
    
    public void testGetStringProperty() 
    {
        String[] vals = { "foo", " this is a property " };
        Properties props = TextUtil.createProperties(vals);
        assertEquals( "this is a property", TextUtil.getStringProperty( props, "foo", "err" ) );
    }
    
    public void testGetStringPropertyDefaultValue() 
    {
        String defaultValue = System.getProperty( "user.home" ) + File.separator + "jspwiki-files";
        String[] vals = { "foo", " this is a property " };
        Properties props = TextUtil.createProperties(vals);
        assertEquals( defaultValue, TextUtil.getStringProperty( props, "bar", defaultValue ) );
    }

    public void testGetCanonicalFilePathProperty()
    {
        String[] values = { AbstractFileProvider.PROP_PAGEDIR, " ." + File.separator + "data" + File.separator + "private " };
        Properties props = TextUtil.createProperties(values);
        String path = TextUtil.getCanonicalFilePathProperty(props, AbstractFileProvider.PROP_PAGEDIR, "NA");
        assertTrue( path.endsWith( File.separator + "data" + File.separator + "private" ) );
        assertFalse( path.endsWith( "." + File.separator + "data" + File.separator + "private" ) );
    }

    public void testGetCanonicalFilePathPropertyDefaultValue()
    {
        String defaultValue = System.getProperty( "user.home" ) + File.separator + "jspwiki-files";
        String[] values = {};
        Properties props = TextUtil.createProperties(values);
        String path = TextUtil.getCanonicalFilePathProperty(props, AbstractFileProvider.PROP_PAGEDIR, defaultValue);
        assertTrue(path.endsWith("jspwiki-files"));
    }

}


