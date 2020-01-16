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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.NoSuchElementException;
import java.util.Properties;


public class TextUtilTest {

    @Test
    public void testGenerateRandomPassword() {
        for( int i = 0; i < 1000; i++ ) {
            Assertions.assertEquals( TextUtil.PASSWORD_LENGTH, TextUtil.generateRandomPassword().length(), "pw" );
        }
    }

    @Test
    public void testEncodeName_1() {
        final String name = "Hello/World";
        Assertions.assertEquals( "Hello/World", TextUtil.urlEncode(name,"ISO-8859-1") );
    }

    @Test
    public void testEncodeName_2() {
        final String name = "Hello~World";
        Assertions.assertEquals( "Hello%7EWorld", TextUtil.urlEncode(name,"ISO-8859-1") );
    }

    @Test
    public void testEncodeName_3() {
        final String name = "Hello/World ~";
        Assertions.assertEquals( "Hello/World+%7E", TextUtil.urlEncode(name,"ISO-8859-1") );
    }

    @Test
    public void testDecodeName_1() {
        final String name = "Hello/World+%7E+%2F";
        Assertions.assertEquals( "Hello/World ~ /", TextUtil.urlDecode(name,"ISO-8859-1") );
    }

    @Test
    public void testEncodeNameUTF8_1() {
        final String name = "\u0041\u2262\u0391\u002E";
        Assertions.assertEquals( "A%E2%89%A2%CE%91.", TextUtil.urlEncodeUTF8(name) );
    }

    @Test
    public void testEncodeNameUTF8_2() {
        final String name = "\uD55C\uAD6D\uC5B4";
        Assertions.assertEquals( "%ED%95%9C%EA%B5%AD%EC%96%B4", TextUtil.urlEncodeUTF8(name) );
    }

    @Test
    public void testEncodeNameUTF8_3() {
        final String name = "\u65E5\u672C\u8A9E";
        Assertions.assertEquals( "%E6%97%A5%E6%9C%AC%E8%AA%9E", TextUtil.urlEncodeUTF8(name) );
    }

    @Test
    public void testEncodeNameUTF8_4() {
        final String name = "Hello World";
        Assertions.assertEquals( "Hello+World", TextUtil.urlEncodeUTF8(name) );
    }

    @Test
    public void testDecodeNameUTF8_1() {
        final String name = "A%E2%89%A2%CE%91.";
        Assertions.assertEquals( "\u0041\u2262\u0391\u002E", TextUtil.urlDecodeUTF8(name) );
    }

    @Test
    public void testDecodeNameUTF8_2() {
        final String name = "%ED%95%9C%EA%B5%AD%EC%96%B4";
        Assertions.assertEquals( "\uD55C\uAD6D\uC5B4", TextUtil.urlDecodeUTF8(name) );
    }

    @Test
    public void testDecodeNameUTF8_3() {
        final String name = "%E6%97%A5%E6%9C%AC%E8%AA%9E";
        Assertions.assertEquals( "\u65E5\u672C\u8A9E", TextUtil.urlDecodeUTF8(name) );
    }

    @Test
    public void testReplaceString1() {
        final String text = "aabacaa";
        Assertions.assertEquals( "ddbacdd", TextUtil.replaceString( text, "aa", "dd" ) );
    }

    @Test
    public void testReplaceString4() {
        final String text = "aabacaafaa";
        Assertions.assertEquals( "ddbacddfdd", TextUtil.replaceString( text, "aa", "dd" ) );
    }

    @Test
    public void testReplaceString5() {
        final String text = "aaabacaaafaa";
        Assertions.assertEquals( "dbacdfaa", TextUtil.replaceString( text, "aaa", "d" ) );
    }

    @Test
    public void testReplaceString2() {
        final String text = "abcde";
        Assertions.assertEquals( "fbcde", TextUtil.replaceString( text, "a", "f" ) );
    }

    @Test
    public void testReplaceString3() {
        final String text = "ababab";
        Assertions.assertEquals( "afafaf", TextUtil.replaceString( text, "b", "f" ) );
    }

    @Test
    public void testReplaceStringCaseUnsensitive1() {
        final String text = "aABcAa";
        Assertions.assertEquals( "ddBcdd", TextUtil.replaceStringCaseUnsensitive( text, "aa", "dd" ) );
    }

    @Test
    public void testReplaceStringCaseUnsensitive2() {
        final String text = "Abcde";
        Assertions.assertEquals( "fbcde", TextUtil.replaceStringCaseUnsensitive( text, "a", "f" ) );
    }

    @Test
    public void testReplaceStringCaseUnsensitive3() {
        final String text = "aBAbab";
        Assertions.assertEquals( "afAfaf", TextUtil.replaceStringCaseUnsensitive( text, "b", "f" ) );
    }

    @Test
    public void testReplaceStringCaseUnsensitive4() {
        final String text = "AaBAcAAfaa";
        Assertions.assertEquals( "ddBAcddfdd", TextUtil.replaceStringCaseUnsensitive( text, "aa", "dd" ) );
    }

    @Test
    public void testReplaceStringCaseUnsensitive5() {
        final String text = "aAaBaCAAafaa";
        Assertions.assertEquals( "dBaCdfaa", TextUtil.replaceStringCaseUnsensitive( text, "aaa", "d" ) );
    }

    // Pure UNIX.
    @Test
    public void testNormalizePostdata1() {
        final String text = "ab\ncd";
        Assertions.assertEquals( "ab\r\ncd\r\n", TextUtil.normalizePostData( text ) );
    }

    // Pure MSDOS.
    @Test
    public void testNormalizePostdata2() {
        final String text = "ab\r\ncd";
        Assertions.assertEquals( "ab\r\ncd\r\n", TextUtil.normalizePostData( text ) );
    }

    // Pure Mac
    @Test
    public void testNormalizePostdata3() {
        final String text = "ab\rcd";
        Assertions.assertEquals( "ab\r\ncd\r\n", TextUtil.normalizePostData( text ) );
    }

    // Mixed, ending correct.
    @Test
    public void testNormalizePostdata4()
    {
        final String text = "ab\ncd\r\n\r\n\r";
        Assertions.assertEquals( "ab\r\ncd\r\n\r\n\r\n", TextUtil.normalizePostData( text ) );
    }

    // Multiple newlines
    @Test
    public void testNormalizePostdata5() {
        final String text = "ab\ncd\n\n\n\n";
        Assertions.assertEquals( "ab\r\ncd\r\n\r\n\r\n\r\n", TextUtil.normalizePostData( text ) );
    }

    // Empty.
    @Test
    public void testNormalizePostdata6() {
        final String text = "";
        Assertions.assertEquals( "\r\n", TextUtil.normalizePostData( text ) );
    }

    // Just a newline.
    @Test
    public void testNormalizePostdata7() {
        final String text = "\n";
        Assertions.assertEquals( "\r\n", TextUtil.normalizePostData( text ) );
    }

    @Test
    public void testGetBooleanProperty() {
        final Properties props = new Properties();
        props.setProperty("foobar.0", "YES");
        props.setProperty("foobar.1", "true");
        props.setProperty("foobar.2", "false");
        props.setProperty("foobar.3", "no");
        props.setProperty("foobar.4", "on");
        props.setProperty("foobar.5", "OFF");
        props.setProperty("foobar.6", "gewkjoigew");

        Assertions.assertTrue( TextUtil.getBooleanProperty( props, "foobar.0", false ), "foobar.0" );
        Assertions.assertTrue( TextUtil.getBooleanProperty( props, "foobar.1", false ), "foobar.1" );
        Assertions.assertFalse( TextUtil.getBooleanProperty( props, "foobar.2", true ), "foobar.2" );
        Assertions.assertFalse( TextUtil.getBooleanProperty( props, "foobar.3", true ), "foobar.3" );
        Assertions.assertTrue( TextUtil.getBooleanProperty( props, "foobar.4", false ), "foobar.4" );
        Assertions.assertFalse( TextUtil.getBooleanProperty( props, "foobar.5", true ), "foobar.5" );
        Assertions.assertFalse( TextUtil.getBooleanProperty( props, "foobar.6", true ), "foobar.6" );
    }

    @Test
    public void testGetSection1() {
        final String src = "Single page.";

        Assertions.assertEquals( src, TextUtil.getSection(src,1), "section 1" );
        Assertions.assertThrows( IllegalArgumentException.class, () -> TextUtil.getSection( src, 5 ) );
        Assertions.assertThrows( IllegalArgumentException.class, () -> TextUtil.getSection( src, -1 ) );
    }

    @Test
    public void testGetSection2() {
        final String src = "First section\n----\nSecond section\n\n----\n\nThird section";

        Assertions.assertEquals( "First section\n", TextUtil.getSection(src,1), "section 1" );
        Assertions.assertEquals( "\nSecond section\n\n", TextUtil.getSection(src,2), "section 2" );
        Assertions.assertEquals( "\n\nThird section", TextUtil.getSection(src,3), "section 3" );
        Assertions.assertThrows( IllegalArgumentException.class, () -> TextUtil.getSection( src, 4 ) );
    }

    @Test
    public void testGetSection3() {
        final String src = "----\nSecond section\n----";

        Assertions.assertEquals( "", TextUtil.getSection(src,1), "section 1" );
        Assertions.assertEquals( "\nSecond section\n", TextUtil.getSection(src,2), "section 2" );
        Assertions.assertEquals( "", TextUtil.getSection(src,3), "section 3" );
        Assertions.assertThrows( IllegalArgumentException.class, () -> TextUtil.getSection( src, 4 ) );
    }

    @Test
    public void testGetSectionWithMoreThanFourDashes() {
        final String src = "----------------\nSecond section\n----";
        Assertions.assertEquals( "\nSecond section\n", TextUtil.getSection(src, 2), "section 2" );
    }

    @Test
    public void testBooleanParameter() {
        Assertions.assertTrue( TextUtil.isPositive(" true "), "1" );
        Assertions.assertFalse( TextUtil.isPositive(" fewqkfow kfpokwe "), "2" );
        Assertions.assertTrue( TextUtil.isPositive("on"), "3" );
        Assertions.assertTrue( TextUtil.isPositive("\t\ton"), "4" );
    }

    @Test
    public void testTrimmedProperty() {
        final String[] vals = { "foo", " this is a property ", "bar", "60" };
        final Properties props = TextUtil.createProperties(vals);

        Assertions.assertEquals( "this is a property", TextUtil.getStringProperty(props,"foo",""), "foo" );
        Assertions.assertEquals( 60, TextUtil.getIntegerProperty(props,"bar",0), "bar" );
    }

    @Test
    public void testGetStringProperty() {
        final String[] vals = { "foo", " this is a property " };
        final Properties props = TextUtil.createProperties(vals);
        Assertions.assertEquals( "this is a property", TextUtil.getStringProperty( props, "foo", "err" ) );
    }

    @Test
    public void testGetStringPropertyDefaultValue() {
        final String defaultValue = System.getProperty( "user.home" ) + File.separator + "jspwiki-files";
        final String[] vals = { "foo", " this is a property " };
        final Properties props = TextUtil.createProperties(vals);
        Assertions.assertEquals( defaultValue, TextUtil.getStringProperty( props, "bar", defaultValue ) );
    }

    @Test
    public void testGetCanonicalFilePathProperty() {
        final String[] values = { "jspwiki.fileSystemProvider.pageDir", " ." + File.separator + "data" + File.separator + "private " };
        final Properties props = TextUtil.createProperties(values);
        final String path = TextUtil.getCanonicalFilePathProperty(props, "jspwiki.fileSystemProvider.pageDir", "NA");
        Assertions.assertTrue( path.endsWith( File.separator + "data" + File.separator + "private" ) );
        Assertions.assertFalse( path.endsWith( "." + File.separator + "data" + File.separator + "private" ) );
    }

    @Test
    public void testGetCanonicalFilePathPropertyDefaultValue() {
        final String defaultValue = System.getProperty( "user.home" ) + File.separator + "jspwiki-files";
        final String[] values = {};
        final Properties props = TextUtil.createProperties(values);
        final String path = TextUtil.getCanonicalFilePathProperty(props, "jspwiki.fileSystemProvider.pageDir", defaultValue);
        Assertions.assertTrue(path.endsWith("jspwiki-files"));
    }

    @Test
    public void testGetRequiredProperty() {
        final String[] vals = { "foo", " this is a property ", "bar", "60" };
        final Properties props = TextUtil.createProperties( vals );
        Assertions.assertEquals( "60", TextUtil.getRequiredProperty( props, "bar" ) );
    }

    @Test
    public void testGetRequiredPropertyNSEE() {
        final String[] vals = { "foo", " this is a property ", "bar", "60" };
        final Properties props = TextUtil.createProperties(vals);
        Assertions.assertThrows( NoSuchElementException.class, () -> TextUtil.getRequiredProperty( props, "ber" ) );
    }

    @Test
    public void testCleanString() {
        Assertions.assertNull( TextUtil.cleanString( null, TextUtil.PUNCTUATION_CHARS_ALLOWED ) );
        Assertions.assertEquals( " This is a link ", TextUtil.cleanString( " [ This is a link ] ", TextUtil.PUNCTUATION_CHARS_ALLOWED ) );
        Assertions.assertEquals( "ThisIsALink", TextUtil.cleanString( " [ This is a link ] ", TextUtil.LEGACY_CHARS_ALLOWED ) );
    }

}
