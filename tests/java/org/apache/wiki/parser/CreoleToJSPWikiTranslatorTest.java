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
package org.apache.wiki.parser;

import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Properties;

import org.apache.wiki.parser.CreoleToJSPWikiTranslator;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class CreoleToJSPWikiTranslatorTest extends TestCase
{

    public static final String TEST_PROPERTIES = "tests/com/ecyrd/jspwiki/parser/creole.properties";

    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(CreoleToJSPWikiTranslatorTest.class);
    }

    protected void setUp() throws Exception
    {
        super.setUp();
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

    public void testBold() throws Exception
    {
        String src = "This is **bold**.";

        assertEquals("This is __bold__.", translate(src));
    }

    public void testBoldVersusList() throws Exception
    {
        String src = "**This is all bold.**";

        assertEquals("__This is all bold.__", translate(src));
    }

    public void testBoldAcrossLineBreaks() throws Exception
    {
        String src = "This is **bold\nand still bold**.";

        assertEquals("This is __bold" + System.getProperty("line.separator") + "and still bold__.", translate(src));
    }

    public void testBoldAcrossLineParagraphs() throws Exception
    {
        String src = "This is **bold\r\n\r\nand no more bold.";

        assertEquals("This is __bold__\r\n\r\nand no more bold.", translate(src));
    }

    public void testItalicAcrossLineBreaks() throws Exception
    {
        String src = "This is //italic\r\nand still italic//.";

        assertEquals("This is ''italic\r\nand still italic''.", translate(src));
    }

    public void testItalicAcrossLineParagraphs() throws Exception
    {
        String src = "This is //italic\r\n\r\nnand no more italic.";

        assertEquals("This is ''italic''\r\n\r\nnand no more italic.", translate(src));
    }

    public void testItalic() throws Exception
    {
        String src = "This is //italic//.";

        assertEquals("This is ''italic''.", translate(src));
    }

    public void testImage() throws Exception
    {
        String src = "This is {{Image}}.";

        assertEquals("This is [{Image src='Image'}].", translate(src));
    }

    public void testImageLink() throws Exception
    {
        String src = "This is [[http://www.wikicreole.org|{{Image}}]] with a link.";
        assertEquals("This is [{Image src='Image' link='http://www.wikicreole.org'}] with a link.", translate(src));
    }

    public void testImageDescription() throws Exception
    {
        String src = "This is {{Image|Description}}.";
        assertEquals("This is [{Image src='Image' caption='Description'}].", translate(src));
    }

    public void testImageLinkDescription() throws Exception
    {
        String src = "This is [[http://www.wikicreole.org|{{Image|Description}}]].";

        assertEquals("This is [{Image src='Image' link='http://www.wikicreole.org' caption='Description'}].", translate(src));
    }

    public void testHyperlinks2() throws Exception
    {
        String src = "This should be a [[hyperlink]]";

        assertEquals("This should be a [hyperlink]", translate(src));
    }

    public void testHyperlinks3() throws Exception
    {
        String src = "This should be a [[hyperlink too]]";

        assertEquals("This should be a [hyperlink too]", translate(src));
    }

    public void testHyperlinks4() throws Exception
    {
        String src = "This should be a [[HyperLink]]";

        assertEquals("This should be a [HyperLink]", translate(src));
    }

    public void testHyperlinks5() throws Exception
    {
        String src = "This should be a [[HyperLink|here]]";

        assertEquals("This should be a [here|HyperLink]", translate(src));
    }

    public void testHyperlinksNamed1() throws Exception
    {

        String src = "This should be a [[HyperLink#heading|here]]";

        assertEquals("This should be a [here|HyperLink#heading]", translate(src));
    }

    public void testHyperlinksNamed2() throws Exception
    {
        String src = "This should be a [[HyperLink#heading]]";

        assertEquals("This should be a [HyperLink#heading]", translate(src));
    }

    //
    // Testing CamelCase hyperlinks
    //

    public void testHyperLinks6() throws Exception
    {

        String src = "[[DiscussionAboutWiki]] [[WikiMarkupDevelopment]].";

        assertEquals("[DiscussionAboutWiki] [WikiMarkupDevelopment].", translate(src));
    }

    /** ******* Stuff not in JSPWikiMarkupParserTest ************************* */
    /* these are test where errors occured in the Creole Wiki */

    public void testHeadingsCreole1() throws Exception
    {
        String src = "=====Level 4 heading";

        assertEquals("__Level 4 heading__", translate(src));
    }

    public void testHyperLinksCreole1() throws Exception
    {

        String src = "Sponsored by the [Wiki Symposium|http://www.wikisym.org/] and [i3G Institute|http://www.i3g.hs-heilbronn.de].";

        assertEquals(
                     "Sponsored by the [Wiki Symposium|http://www.wikisym.org/] and [i3G Institute|http://www.i3g.hs-heilbronn.de].",
                     translate(src));
    }

    public void testHyperLinksJSPWiki() throws Exception
    {
        String src = "* [http://www.wikisym.org/cgi-bin/mailman/listinfo/wiki-research|Wiki research mailing list]";
        assertEquals(src, translate(src));
    }

    public void testHyperLinksCreole2() throws Exception
    {

        String src = "Sponsored by the [[http://www.wikisym.org/|Wiki Symposium]] and [[http://www.i3g.hs-heilbronn.de|i3G Institute]].";

        assertEquals(
                     "Sponsored by the [Wiki Symposium|http://www.wikisym.org/] and [i3G Institute|http://www.i3g.hs-heilbronn.de].",
                     translate(src));
    }

    public void testPreformattedCreole() throws Exception
    {
        String src = "{{{$$...$$}}}";

        assertEquals(src, translate(src));
    }

    public void testPreformattedCreole2() throws Exception
    {
        String src = "{{{\r\n" + "\r\n" + "[[http://en.wikipedia.org|wikipedia]]\r\n" + "}}}";
        assertEquals(src, translate(src));
    }

    public void testPreformattedCreole3() throws Exception
    {
        String src = "{{{\r\n" + "Guitar Chord C:\r\n" + "\r\n" + "||---|---|---|\r\n" + "||-0-|---|---|\r\n"
                     + "||---|-0-|---|\r\n" + "||---|---|-0-|\r\n" + "||---|---|---|\\n" + "}}}";

        assertEquals(src, translate(src));
    }

    public void testPreformattedCreole4() throws Exception
    {
        // don't interpret plugins
        String src = "{{{<<Test>>}}}";

        assertEquals(src, translate(src));
    }

    public void testPreformattedCreole5() throws Exception
    {
        String src = "{{{<<<Test>>>}}}";

        assertEquals(src, translate(src));
    }

    public void testPreformattedPlusLinks1() throws Exception
    {
        String preformatted = "{{{\r\n" + "Guitar Chord C:\r\n" + "\r\n" + "||---|---|---|\r\n" + "||-0-|---|---|\r\n"
                              + "||---|-0-|---|\r\n" + "||---|---|-0-|\r\n" + "||---|---|---|\r\n" + "}}}";

        String src = "[[http://www.wikicreole.org|external Links]]\r\n" + preformatted;

        String target = "[external Links|http://www.wikicreole.org]\r\n" + preformatted;
        assertEquals(target, translate(src));
    }

    public void testPreformattedPlusLinks2() throws Exception
    {
        String preformatted = "{{{\r\n" + "[[http://www.wikicreole.org]]\r\n" + "}}}";
        String src = "[[http://www.wikicreole.org]]\r\n" + preformatted;

        String target = "[http://www.wikicreole.org]\r\n" + preformatted;

        assertEquals(target, translate(src));
    }

    public void testListCreole() throws Exception
    {
        String src = "- 1\r\n" + "-- 2\r\n" + "--- 3\r\n" + "---- 4\r\n" + "----- 5";
        String target = "* 1\r\n" + "** 2\r\n" + "*** 3\r\n" + "**** 4\r\n" + "***** 5";

        assertEquals(target, translate(src));
    }

    public void testLineAmbiguity() throws Exception
    {
        String src = "Some text\r\n\r\n----\r\n\r\nMore text";
        assertEquals(src, translate(src));
    }

    public void testSignartureAmbiguity() throws Exception
    {
        String src = "Some **text**\r\n\r\n--Steff";
        String target = "Some __text__\r\n\r\n--Steff";
        assertEquals(target, translate(src));
    }

    public void disabledTestLinebreakCreole() throws Exception
    {

        String src = "My contact dates:\n" + "Pone: xyz\r\n" + "Fax: +45\n" + "Mobile: abc";

        String target = "My contact dates:\\\\\n" + "Pone: xyz\\\\\r\n" + "Fax: +45\\\\\n" + "Mobile: abc";

        assertEquals(target, translate(src));
    }

    public void disabledTestLinebreakCreoleShort() throws Exception
    {

        String src = "a\n" + "b\n" + "c\n";

        String target = "a\\\\\n" + "b\\\\\n" + "c\n";

        assertEquals(target, translate(src));
    }

    public void disabledTestLinebreakCreoleWithLists() throws Exception
    {

        String src = "*This\n" + "*Is\n" + "*a\n" + "*list";

        assertEquals(src, translate(src));
    }

    public void testCSS() throws Exception
    {

        String src = "Some test\r\n" + "\r\n" + "%%commentbox\r\n" + "Aloha World!\r\n" + "%%\r\n" + "\r\n"
                     + "Does the pagefilter mess up special jspwiki css markup?";

        assertEquals(src, translate(src));
    }

    public void testSeparatorAfterHypenList() throws Exception
    {

        String src = "- 1\r\n" + "-- 1.1\r\n" + "-- 1.2\r\n" + "- 2\r\n" + "---------\r\n" + "test\r\n" + "Test";

        String target = "* 1\r\n" + "** 1.1\r\n" + "** 1.2\r\n" + "* 2\r\n" + "---------\r\n" + "test\r\n" + "Test";

        assertEquals(target, translate(src));
    }

    /**
     * This might not work, users will have to resolve this ambiguity by hand...
     *
     * @throws Exception
     */
    public void testBulletListBoldAmbiguity() throws Exception
    {

        String src = "* 1\r\n" + "** 1.1\r\n" + "** 1.2\r\n" + "* 2\r\n" + "---------\r\n" + "test";
        assertEquals(src, translate(src));
    }

    public void testEscapeHypens() throws Exception
    {

        String src = " 1\\\\\r\n" + "~- 3\\\\\r\n" + "~===\\\\\r\n" + "~- 2\\\\";

        String target = " 1\\\\\r\n" + "~- 3\\\\\r\n" + "~===\\\\\r\n" + "~- 2\\\\";

        assertEquals(target, translate(src));
    }

    public void testEscapeNowiki() throws Exception
    {

        String src = "{{{\r\n" + "{{{\r\n" + "{{Image}}\r\n" + "~}}}\r\n" + "}}}\r\n" + "Test";

        String target = "{{{\r\n" + "{{{\r\n" + "{{Image}}\r\n" + "~}}}\r\n" + "}}}\r\n" + "Test";

        assertEquals(target, translate(src));
    }

    public void testTables1() throws Exception
    {

        String src = "|a|b\r\n" + "|c|d";

        assertEquals(src, translate(src));
    }

    public void testTables2() throws Exception
    {

        String src = "|a|b|\r\n" + "|c|d|";

        String target = "|a|b\r\n" + "|c|d";

        assertEquals(target, translate(src));
    }

    public void testTables3() throws Exception
    {

        String src = "before\r\n" + "|a|b|   \r\n" + "|c|d|\r\n" + "after";

        String target = "before\r\n" + "|a|b\r\n" + "|c|d\r\n" + "after";

        assertEquals(target, translate(src));
    }

    public void testTables4() throws Exception
    {

        String src = "before\r\n" + "|a\\\\b|b|\r\n" + "|c|d|\r\n" + "after";

        String target = "before\r\n" + "|a\\\\b|b\r\n" + "|c|d\r\n" + "after";

        assertEquals(target, translate(src));
    }
    
    public void testTables5() throws Exception
    {

        // does a empty line between two tables get lost?
        String src = "|a|b|\r\n" + "\r\n" +  "|x|y|\r\nTest";

        String target = "|a|b\r\n" + "\r\n" +  "|x|y\r\nTest";

        assertEquals(target, translate(src));
    }

    public void testTableHeaders1() throws Exception
    {

        String src = "|=a|=b|\r\n" + "|c|d|";

        String target = "||a||b\r\n" + "|c|d";
        assertEquals(target, translate(src));
    }

    public void testTableHeaders2() throws Exception
    {

        String src = "|=a=|=b=|\r\n" + "|c|d|";

        String target = "||a||b\r\n" + "|c|d";

        assertEquals(target, translate(src));
    }

    public void testTableHeaders3() throws Exception
    {

        String src = "||a||b\r\n" + "|c|d";

        assertEquals(src, translate(src));
    }

    public void testExtensions1() throws Exception
    {

        String src = "<<ImagePlugin src='abc'>>";

        String target = "[{ImagePlugin src='abc'}]";

        assertEquals(target, translate(src));
    }

    public void testExtensions2() throws Exception
    {

        String src = "[{ImagePlugin src='abc'}]";

        assertEquals(src, translate(src));
    }

    public void testExtensions3() throws Exception
    {

        String src = "<This is HTML>";

        assertEquals(src, translate(src));
    }

    public void testExtensions4() throws Exception
    {
        String src = "<<FormOpen submit=\'http://www.jspwiki.org\' >>";

        String target = "[{FormOpen submit=\'http://www.jspwiki.org\' }]";
        
        assertEquals(target, translate(src));
    }

    public void testExtensions5() 
    {
        
        String src =
            "<<Script\r\n" +
            "\r\n" +
            "//Comment\r\n" +
            ">>\r\n" +
            "\r\n" +
            "[[http://www.xyz.com/]]\r\n";

        String target = 
            "[{Script\r\n" +
            "\r\n" +
            "//Comment\r\n" +
            "}]\r\n" +
            "\r\n" +
            "[http://www.xyz.com/]\r\n";
        
        //System.out.println(src);
        //System.out.println(translate(src));
        
        assertEquals(target, translate(src));
    }
    
    public void testHeaderNotAtBeginning()
    {
        String src = "Hallo==Hallo";
        assertEquals(src, translate(src));
    }

    public void testTableLink()
    {
        String src = "|=a=|=b=|\r\n" + "|[[c]]|d|";

        String target = "||a||b\r\n" + "|[c]|d";

        assertEquals(target, translate(src));
    }

    public void testTableImage()
    {
        String src = "|=a=|=b=|\r\n" + "|[[c]]|{{Image.png}}|";

        String target = "||a||b\r\n" + "|[c]|[{Image src='Image.png'}]";

        assertEquals(target, translate(src));
    }

    public void testHeaderAfterLinebreak()
    {
        String src = "Hallo das ist super\r\n===Und jetzt\r\nGehts weiter";

        String target = "Hallo das ist super\r\n!!Und jetzt\r\nGehts weiter";

        assertEquals(target, translate(src));
    }

    public void testBulletMixedEnum()
    {
        String src = "# Hallo\r\n" + "-- Hallo\r\n" + "--- Hallo\r\n" + "Hi";

        String target = "# Hallo\r\n" + "** Hallo\r\n" + "*** Hallo\r\n" + "Hi";
        assertEquals(target, translate(src));
    }

    public void testBulletMixedEnum2()
    {
        String src = "- Hallo\r\n" + "## Hallo\r\n" + "### Hallo\r\n" + "Hi";

        String target = "* Hallo\r\n" + "## Hallo\r\n" + "### Hallo\r\n" + "Hi";
        assertEquals(target, translate(src));
    }

    public void testBulletMixedEnum3()
    {
        String src = "#Headings\r\n" + "#Links (with optional title)\r\n" + "#Lists (like this one)\r\n"
                     + "--including nested lists\r\n" + "#Tables\r\n" + "--caption\r\n" + "--headers\r\n" + "--summary\r\n"
                     + "#Language information\r\n" + "#Acronyms and abbreviations\r\n" + "#Emphasis and strong emphasis\r\n"
                     + "#Quotes, inline and block\r\n" + "#Images";

        String target = "#Headings\r\n" + "#Links (with optional title)\r\n" + "#Lists (like this one)\r\n"
                        + "**including nested lists\r\n" + "#Tables\r\n" + "**caption\r\n" + "**headers\r\n" + "**summary\r\n"
                        + "#Language information\r\n" + "#Acronyms and abbreviations\r\n" + "#Emphasis and strong emphasis\r\n"
                        + "#Quotes, inline and block\r\n" + "#Images";

        assertEquals(target, translate(src));
    }

    public void testSignature()
    {
        String src = "Hallo\r\n--~~~";
        String target = "Hallo\r\n-- [[Hanno]]";
        Properties props = new Properties();
        props.put("creole.dateFormat", "dd/MM/yyyy");
        assertEquals(target, new CreoleToJSPWikiTranslator().translateSignature(props, src, "Hanno"));
    }

    public void testSignatureDate()
    {
        String src = "Hallo\r\n--~~~~";
        Calendar cal = Calendar.getInstance();
        String target = "Hallo\r\n-- [[Hanno]], " + (new SimpleDateFormat("dd/MM/yyyy")).format(cal.getTime());
        Properties props = new Properties();
        props.put("creole.dateFormat", "dd/MM/yyyy");
        assertEquals(target, new CreoleToJSPWikiTranslator().translateSignature(props, src, "Hanno"));
    }

    public void testSignatureDate2()
    {

        String format = "\n   yyyy-MM-dd HH:mm   ";
        String src = "Hallo\r\n--~~~~";
        Calendar cal = Calendar.getInstance();
        String target = "Hallo\r\n-- [[Hanno]], " + (new SimpleDateFormat(format)).format(cal.getTime());
        Properties props = new Properties();
        props.put("creole.dateFormat", format);
        assertEquals(target, new CreoleToJSPWikiTranslator().translateSignature(props, src, "Hanno"));
    }

    public void testHeaderAtStart()
    {
        String src = "Hallo\r\n=Hallo\r\nHallo";
        String target = "Hallo\r\n!!!Hallo\r\nHallo";
        assertEquals(target, translate(src));
    }

    public void testSignatureSourceCode()
    {
        String format = "\n   yyyy-MM-dd HH:mm   ";
        String src = "{{{Hallo\r\n" + "--~~~~\r\n" + "Hallo\r\n" + "}}}";
        Properties props = new Properties();
        props.put("creole.dateFormat", format);
        assertEquals(src, new CreoleToJSPWikiTranslator().translateSignature(props, src, "Hanno"));
    }

    public void testTilde()
    {
        String src = "==Willkommen zum WikiWizardScript\r\n" + "~~ sdfsdf\r\n" + "\r\n" + "now what happens?\r\n" + "\r\n"
                     + "- nothing I hope\r\n" + "- maybe something\r\n" + "- we will soon see!\r\n" + "\r\n"
                     + "== and this is a big title =================\r\n" + "\r\n" + "What can we put here?\r\n" + "\r\n"
                     + "{{Web2.png}}";
        String target = "!!!Willkommen zum WikiWizardScript\r\n" + "~~ sdfsdf\r\n" + "\r\nnow what happens?\r\n"
                        + "\r\n* nothing I hope\r\n" + "* maybe something\r\n" + "* we will soon see!\r\n"
                        + "\r\n!!! and this is a big title ===============\r\n" + "\r\nWhat can we put here?\r\n"
                        + "\r\n[{Image src='Web2.png'}]";
        assertEquals(target, translate(src));
    }

    public void testWWWToHTTP()
    {
        String src = "Hallo\r\nHallo[[ 	www.gmx.de]]Hallo\r\nHallo";
        String target = "Hallo\r\nHallo[http://www.gmx.de]Hallo\r\nHallo";
        assertEquals(target, translate(src));

        String src2 = "Hallo\r\nHallo[[www.gmx.de]]Hallo\r\nHallo";
        String target2 = "Hallo\r\nHallo[http://www.gmx.de]Hallo\r\nHallo";
        assertEquals(target2, translate(src2));

        String src3 = "Hallo\r\nHallo[[www.gmx.de|GMX]]Hallo\r\nHallo";
        String target3 = "Hallo\r\nHallo[GMX|http://www.gmx.de]Hallo\r\nHallo";
        assertEquals(target3, translate(src3));
    }

    public void testImageX()
    {
        String src = "Hallo {{Image.png|Caption|M,NB}}";
        String target = "Hallo [{ImageX src='Image.png' caption='Caption' width='250' border=0}]";
        Properties props = new Properties();
        props.put("creole.imagePlugin.para.M", "width='250'");
        props.put("creole.imagePlugin.para.NB", "border=0");
        props.put("creole.imagePlugin.name", "ImageX");

        assertEquals(target, new CreoleToJSPWikiTranslator().translate(props, src));
    }

    public void testImageX11()
    {
        String src = "Hallo {{Image.png|Caption|250}}";
        String target = "Hallo [{ImageX src='Image.png' caption='Caption' width='250px'}]";
        Properties props = new Properties();
        props.put("creole.imagePlugin.name", "ImageX");

        assertEquals(target, new CreoleToJSPWikiTranslator().translate(props, src));
    }

    public void testImageX2()
    {
        String src = "Hallo {{Image.png|Caption}}";
        String target = "Hallo [{ImageX src='Image.png' caption='Caption' }]";
        Properties props = new Properties();
        props.put("creole.imagePlugin.name", "ImageX");

        assertEquals(target, new CreoleToJSPWikiTranslator().translate(props, src));
    }

    public void testImageX3()
    {
        String src = "Hallo {{Image.png|Caption|M,NB,TEST}}";
        String target = "Hallo [{ImageX src='Image.png' caption='Caption' width='250' border=0}]";
        Properties props = new Properties();
        props.put("creole.imagePlugin.para.M", "width='250'");
        props.put("creole.imagePlugin.para.NB", "border=0");
        props.put("creole.imagePlugin.name", "ImageX");

        assertEquals(target, new CreoleToJSPWikiTranslator().translate(props, src));
    }

    public void testImageX4()
    {
        String src = "Hallo {{Image.png||M,NB,TEST}}";
        String target = "Hallo [{ImageX src='Image.png' width='250' border=0}]";
        Properties props = new Properties();
        props.put("creole.imagePlugin.para.M", "width='250'");
        props.put("creole.imagePlugin.para.NB", "border=0");
        props.put("creole.imagePlugin.name", "ImageX");

        assertEquals(target, new CreoleToJSPWikiTranslator().translate(props, src));
    }

    public void testImageX5()
    {
        String src = "Hallo [[http://www.google.de|{{Image.png||M,NB,TEST}}]]";
        String target = "Hallo [{ImageX src='Image.png' link='http://www.google.de' width='250' border=0}]";
        Properties props = new Properties();
        props.put("creole.imagePlugin.para.M", "width='250'");
        props.put("creole.imagePlugin.para.NB", "border=0");
        props.put("creole.imagePlugin.name", "ImageX");

        assertEquals(target, new CreoleToJSPWikiTranslator().translate(props, src));
    }

    public void testImageX6() throws Exception
    {
        Properties props = new Properties();
        props.load(new FileInputStream(TEST_PROPERTIES));
        String src = "Hallo {{Image.png|Caption|M,[-]}}";
        String target = "Hallo [{ImageX src='Image.png' caption='Caption' width='180' border=false}]";
        assertEquals(target, new CreoleToJSPWikiTranslator().translate(props, src));
    }

    public void testImageX7() throws Exception
    {
        Properties props = new Properties();
        props.load(new FileInputStream(TEST_PROPERTIES));
        String src = "Hallo [[http://www.gmx.de|{{Image.png||XL,+X,[-]}}]]";
        String target = "Hallo [{ImageX src='Image.png' link='http://www.gmx.de' width='540' float='right' border=false}]";
        assertEquals(target, new CreoleToJSPWikiTranslator().translate(props, src));
    }

    public void testImageX8() throws Exception
    {
        Properties props = new Properties();
        props.load(new FileInputStream(TEST_PROPERTIES));
        String src = "Hallo [[http://www.gmx.de|{{Image.png||XL,+X,X-,[-]}}]]";
        String target = "Hallo [{ImageX src='Image.png' link='http://www.gmx.de' width='540' float='right' align='left' border=false}]";
        assertEquals(target, new CreoleToJSPWikiTranslator().translate(props, src));
    }

    public void testImageX9() throws Exception
    {
        Properties props = new Properties();
        props.load(new FileInputStream(TEST_PROPERTIES));
        String src = "Hallo [[http://www.gmx.de|{{Image.png|Caption|XL,+X,X-,[-]}}]]";
        String target = "Hallo [{ImageX src='Image.png' link='http://www.gmx.de' caption='Caption' width='540' float='right' align='left' border=false}]";
        assertEquals(target, new CreoleToJSPWikiTranslator().translate(props, src));
    }

    public void testImageX10() throws Exception
    {
        Properties props = new Properties();
        props.load(new FileInputStream(TEST_PROPERTIES));
        String src = "Hallo [[http://www.gmx.de|{{Image.png|Caption|xL, +X , X-, [-]}}]]";
        String target = "Hallo [{ImageX src='Image.png' link='http://www.gmx.de' caption='Caption' width='540' float='right' align='left' border=false}]";
        assertEquals(target, new CreoleToJSPWikiTranslator().translate(props, src));
    }

    public void testImageX12()
    {
        String src = "Hallo [[http://www.google.de|{{Image.png||120px}}]]\r\n[[http://www.google.de|{{Image.png||120cm}}]]";
        String target = "Hallo [{ImageX src='Image.png' link='http://www.google.de' width='120'}]\r\n"
                        + "[{ImageX src='Image.png' link='http://www.google.de' widthInCM='120'}]";
        ;
        Properties props = new Properties();
        props.put("creole.imagePlugin.para.%px", "width='%'");
        props.put("creole.imagePlugin.para.%cm", "widthInCM='%'");
        props.put("creole.imagePlugin.name", "ImageX");

        assertEquals(target, new CreoleToJSPWikiTranslator().translate(props, src));
    }

    public void testJuwi()
    {
        String src = "<<JudoScript\r\n" + "if this works then ok\r\n" + "else improve the programm\r\n" + ">>";
        String target = "[{JudoScript\r\n" + "if this works then ok\r\n" + "else improve the programm\r\n" + "}]";
        assertEquals(target, translate(src));
    }

    public void testPluginBold()
    {

        String src = "**<<CurrentTimePlugin format='HH:mm \'am\' dd-MMM-yyyy'>>**";
        String tar = "__[{CurrentTimePlugin format='HH:mm \'am\' dd-MMM-yyyy'}]__";

        assertEquals(tar, translate(src));
    }

    public void testPluginLinebreakPlugin()
    {

        String src = "<<CurrentTimePlugin format=zzzz>>\r\n" + "\r\n" + "<<RecentChangesPlugin since='30'>>";

        String tar = "[{CurrentTimePlugin format=zzzz}]\r\n" + "\r\n" + "[{RecentChangesPlugin since='30'}]";

        assertEquals(tar, translate(src));
    }

    public void testJuwi2()
    {
        String src = "<<JudoScript\r\n" + "if [[this]] works then ok\r\n" + "else improve the programm\r\n" + ">>";
        String target = "[{JudoScript\r\n" + "if [[this]] works then ok\r\n" + "else improve the programm\r\n" + "}]";
        assertEquals(target, translate(src));

    }

    public void testURL()
    {
        String src = "Hallo[[https://wiki.i3g.hs-heilbronn.de]]Hallo";
        String target = "Hallo[https://wiki.i3g.hs-heilbronn.de]Hallo";

        assertEquals(target, translate(src));
    }

    public void testSourcePlugin()
    {
        String src = "Hallo<<Hallo{{{Test}}}Hallo>>Hallo";
        String target = "Hallo[{Hallo{{{Test}}}Hallo}]Hallo";

        assertEquals(target, translate(src));
    }

    public void testMultilinePlugin3()
    {
        String src = "Hallo\r\n" + "<<Hallo\r\n" + "Hallo\r\n" + "Hallo\r\n" + ">>";

        String target = "Hallo\r\n" + "[{Hallo\r\n" + "Hallo\r\n" + "Hallo\r\n" + "}]";

        assertEquals(target, translate(src));
    }

    public String translate(String src)
    {
        CreoleToJSPWikiTranslator translator = new CreoleToJSPWikiTranslator();

        return translator.translate(new Properties(), src);
    }

    public static Test suite()
    {
        return new TestSuite( CreoleToJSPWikiTranslatorTest.class );
    }
}
