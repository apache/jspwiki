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
package org.apache.wiki.parser;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Properties;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class CreoleToJSPWikiTranslatorTest
{

    public static final String TEST_PROPERTIES = "src/test/java/org/apache/wiki/parser/creole.properties";

    @Test
    public void testBold() throws Exception
    {
        String src = "This is **bold**.";

        Assertions.assertEquals("This is __bold__.", translate(src));
    }

    @Test
    public void testBoldVersusList() throws Exception
    {
        String src = "**This is all bold.**";

        Assertions.assertEquals("__This is all bold.__", translate(src));
    }

    @Test
    public void testBoldAcrossLineBreaks() throws Exception
    {
        String src = "This is **bold\nand still bold**.";

        Assertions.assertEquals("This is __bold\nand still bold__.", translate(src));
    }

    @Test
    public void testBoldAcrossLineParagraphs() throws Exception
    {
        String src = "This is **bold\r\n\r\nand no more bold.";

        Assertions.assertEquals("This is __bold__\r\n\r\nand no more bold.", translate(src));
    }

    @Test
    public void testItalicAcrossLineBreaks() throws Exception
    {
        String src = "This is //italic\r\nand still italic//.";

        Assertions.assertEquals("This is ''italic\r\nand still italic''.", translate(src));
    }

    @Test
    public void testItalicAcrossLineParagraphs() throws Exception
    {
        String src = "This is //italic\r\n\r\nnand no more italic.";

        Assertions.assertEquals("This is ''italic''\r\n\r\nnand no more italic.", translate(src));
    }

    @Test
    public void testItalic() throws Exception
    {
        String src = "This is //italic//.";

        Assertions.assertEquals("This is ''italic''.", translate(src));
    }

    @Test
    public void testImage() throws Exception
    {
        String src = "This is {{Image}}.";

        Assertions.assertEquals("This is [{Image src='Image'}].", translate(src));
    }

    @Test
    public void testImageLink() throws Exception
    {
        String src = "This is [[http://www.wikicreole.org|{{Image}}]] with a link.";
        Assertions.assertEquals("This is [{Image src='Image' link='http://www.wikicreole.org'}] with a link.", translate(src));
    }

    @Test
    public void testImageDescription() throws Exception
    {
        String src = "This is {{Image|Description}}.";
        Assertions.assertEquals("This is [{Image src='Image' caption='Description'}].", translate(src));
    }

    @Test
    public void testImageLinkDescription() throws Exception
    {
        String src = "This is [[http://www.wikicreole.org|{{Image|Description}}]].";

        Assertions.assertEquals("This is [{Image src='Image' link='http://www.wikicreole.org' caption='Description'}].", translate(src));
    }

    @Test
    public void testHyperlinks2() throws Exception
    {
        String src = "This should be a [[hyperlink]]";

        Assertions.assertEquals("This should be a [hyperlink]", translate(src));
    }

    @Test
    public void testHyperlinks3() throws Exception
    {
        String src = "This should be a [[hyperlink too]]";

        Assertions.assertEquals("This should be a [hyperlink too]", translate(src));
    }

    @Test
    public void testHyperlinks4() throws Exception
    {
        String src = "This should be a [[HyperLink]]";

        Assertions.assertEquals("This should be a [HyperLink]", translate(src));
    }

    @Test
    public void testHyperlinks5() throws Exception
    {
        String src = "This should be a [[HyperLink|here]]";

        Assertions.assertEquals("This should be a [here|HyperLink]", translate(src));
    }

    @Test
    public void testHyperlinksNamed1() throws Exception
    {

        String src = "This should be a [[HyperLink#heading|here]]";

        Assertions.assertEquals("This should be a [here|HyperLink#heading]", translate(src));
    }

    @Test
    public void testHyperlinksNamed2() throws Exception
    {
        String src = "This should be a [[HyperLink#heading]]";

        Assertions.assertEquals("This should be a [HyperLink#heading]", translate(src));
    }

    //
    // Testing CamelCase hyperlinks
    //

    @Test
    public void testHyperLinks6() throws Exception
    {

        String src = "[[DiscussionAboutWiki]] [[WikiMarkupDevelopment]].";

        Assertions.assertEquals("[DiscussionAboutWiki] [WikiMarkupDevelopment].", translate(src));
    }

    /** ******* Stuff not in JSPWikiMarkupParserTest ************************* */
    /* these are test where errors occured in the Creole Wiki */

    @Test
    public void testHeadingsCreole1() throws Exception
    {
        String src = "=====Level 4 heading";

        Assertions.assertEquals("__Level 4 heading__", translate(src));
    }

    @Test
    public void testHyperLinksCreole1() throws Exception
    {

        String src = "Sponsored by the [Wiki Symposium|http://www.wikisym.org/] and [i3G Institute|http://www.i3g.hs-heilbronn.de].";

        Assertions.assertEquals(
                     "Sponsored by the [Wiki Symposium|http://www.wikisym.org/] and [i3G Institute|http://www.i3g.hs-heilbronn.de].",
                     translate(src));
    }

    @Test
    public void testHyperLinksJSPWiki() throws Exception
    {
        String src = "* [http://www.wikisym.org/cgi-bin/mailman/listinfo/wiki-research|Wiki research mailing list]";
        Assertions.assertEquals(src, translate(src));
    }

    @Test
    public void testHyperLinksCreole2() throws Exception
    {

        String src = "Sponsored by the [[http://www.wikisym.org/|Wiki Symposium]] and [[http://www.i3g.hs-heilbronn.de|i3G Institute]].";

        Assertions.assertEquals(
                     "Sponsored by the [Wiki Symposium|http://www.wikisym.org/] and [i3G Institute|http://www.i3g.hs-heilbronn.de].",
                     translate(src));
    }

    @Test
    public void testPreformattedCreole() throws Exception
    {
        String src = "{{{$$...$$}}}";

        Assertions.assertEquals(src, translate(src));
    }

    @Test
    public void testPreformattedCreole2() throws Exception
    {
        String src = "{{{\r\n" + "\r\n" + "[[http://en.wikipedia.org|wikipedia]]\r\n" + "}}}";
        Assertions.assertEquals(src, translate(src));
    }

    @Test
    public void testPreformattedCreole3() throws Exception
    {
        String src = "{{{\r\n" + "Guitar Chord C:\r\n" + "\r\n" + "||---|---|---|\r\n" + "||-0-|---|---|\r\n"
                     + "||---|-0-|---|\r\n" + "||---|---|-0-|\r\n" + "||---|---|---|\\n" + "}}}";

        Assertions.assertEquals(src, translate(src));
    }

    @Test
    public void testPreformattedCreole4() throws Exception
    {
        // don't interpret plugins
        String src = "{{{<<Test>>}}}";

        Assertions.assertEquals(src, translate(src));
    }

    @Test
    public void testPreformattedCreole5() throws Exception
    {
        String src = "{{{<<<Test>>>}}}";

        Assertions.assertEquals(src, translate(src));
    }

    @Test
    public void testPreformattedPlusLinks1() throws Exception
    {
        String preformatted = "{{{\r\n" + "Guitar Chord C:\r\n" + "\r\n" + "||---|---|---|\r\n" + "||-0-|---|---|\r\n"
                              + "||---|-0-|---|\r\n" + "||---|---|-0-|\r\n" + "||---|---|---|\r\n" + "}}}";

        String src = "[[http://www.wikicreole.org|external Links]]\r\n" + preformatted;

        String target = "[external Links|http://www.wikicreole.org]\r\n" + preformatted;
        Assertions.assertEquals(target, translate(src));
    }

    @Test
    public void testPreformattedPlusLinks2() throws Exception
    {
        String preformatted = "{{{\r\n" + "[[http://www.wikicreole.org]]\r\n" + "}}}";
        String src = "[[http://www.wikicreole.org]]\r\n" + preformatted;

        String target = "[http://www.wikicreole.org]\r\n" + preformatted;

        Assertions.assertEquals(target, translate(src));
    }

    @Test
    public void testListCreole() throws Exception
    {
        String src = "- 1\r\n" + "-- 2\r\n" + "--- 3\r\n" + "---- 4\r\n" + "----- 5";
        String target = "* 1\r\n" + "** 2\r\n" + "*** 3\r\n" + "**** 4\r\n" + "***** 5";

        Assertions.assertEquals(target, translate(src));
    }

    @Test
    public void testLineAmbiguity() throws Exception
    {
        String src = "Some text\r\n\r\n----\r\n\r\nMore text";
        Assertions.assertEquals(src, translate(src));
    }

    @Test
    public void testSignartureAmbiguity() throws Exception
    {
        String src = "Some **text**\r\n\r\n--Steff";
        String target = "Some __text__\r\n\r\n--Steff";
        Assertions.assertEquals(target, translate(src));
    }

    public void disabledTestLinebreakCreole() throws Exception
    {

        String src = "My contact dates:\n" + "Pone: xyz\r\n" + "Fax: +45\n" + "Mobile: abc";

        String target = "My contact dates:\\\\\n" + "Pone: xyz\\\\\r\n" + "Fax: +45\\\\\n" + "Mobile: abc";

        Assertions.assertEquals(target, translate(src));
    }

    public void disabledTestLinebreakCreoleShort() throws Exception
    {

        String src = "a\n" + "b\n" + "c\n";

        String target = "a\\\\\n" + "b\\\\\n" + "c\n";

        Assertions.assertEquals(target, translate(src));
    }

    public void disabledTestLinebreakCreoleWithLists() throws Exception
    {

        String src = "*This\n" + "*Is\n" + "*a\n" + "*list";

        Assertions.assertEquals(src, translate(src));
    }

    @Test
    public void testCSS() throws Exception
    {

        String src = "Some test\r\n" + "\r\n" + "%%commentbox\r\n" + "Aloha World!\r\n" + "%%\r\n" + "\r\n"
                     + "Does the pagefilter mess up special jspwiki css markup?";

        Assertions.assertEquals(src, translate(src));
    }

    @Test
    public void testSeparatorAfterHypenList() throws Exception
    {

        String src = "- 1\r\n" + "-- 1.1\r\n" + "-- 1.2\r\n" + "- 2\r\n" + "---------\r\n" + "test\r\n" + "Test";

        String target = "* 1\r\n" + "** 1.1\r\n" + "** 1.2\r\n" + "* 2\r\n" + "---------\r\n" + "test\r\n" + "Test";

        Assertions.assertEquals(target, translate(src));
    }

    /**
     * This might not work, users will have to resolve this ambiguity by hand...
     *
     * @throws Exception
     */
    @Test
    public void testBulletListBoldAmbiguity() throws Exception
    {

        String src = "* 1\r\n" + "** 1.1\r\n" + "** 1.2\r\n" + "* 2\r\n" + "---------\r\n" + "test";
        Assertions.assertEquals(src, translate(src));
    }

    @Test
    public void testEscapeHypens() throws Exception
    {

        String src = " 1\\\\\r\n" + "~- 3\\\\\r\n" + "~===\\\\\r\n" + "~- 2\\\\";

        String target = " 1\\\\\r\n" + "~- 3\\\\\r\n" + "~===\\\\\r\n" + "~- 2\\\\";

        Assertions.assertEquals(target, translate(src));
    }

    @Test
    public void testEscapeNowiki() throws Exception
    {

        String src = "{{{\r\n" + "{{{\r\n" + "{{Image}}\r\n" + "~}}}\r\n" + "}}}\r\n" + "Test";

        String target = "{{{\r\n" + "{{{\r\n" + "{{Image}}\r\n" + "~}}}\r\n" + "}}}\r\n" + "Test";

        Assertions.assertEquals(target, translate(src));
    }

    @Test
    public void testTables1() throws Exception
    {

        String src = "|a|b\r\n" + "|c|d";

        Assertions.assertEquals(src, translate(src));
    }

    @Test
    public void testTables2() throws Exception
    {

        String src = "|a|b|\r\n" + "|c|d|";

        String target = "|a|b\r\n" + "|c|d";

        Assertions.assertEquals(target, translate(src));
    }

    @Test
    public void testTables3() throws Exception
    {

        String src = "before\r\n" + "|a|b|   \r\n" + "|c|d|\r\n" + "after";

        String target = "before\r\n" + "|a|b\r\n" + "|c|d\r\n" + "after";

        Assertions.assertEquals(target, translate(src));
    }

    @Test
    public void testTables4() throws Exception
    {

        String src = "before\r\n" + "|a\\\\b|b|\r\n" + "|c|d|\r\n" + "after";

        String target = "before\r\n" + "|a\\\\b|b\r\n" + "|c|d\r\n" + "after";

        Assertions.assertEquals(target, translate(src));
    }

    @Test
    public void testTables5() throws Exception
    {

        // does a empty line between two tables get lost?
        String src = "|a|b|\r\n" + "\r\n" +  "|x|y|\r\nTest";

        String target = "|a|b\r\n" + "\r\n" +  "|x|y\r\nTest";

        Assertions.assertEquals(target, translate(src));
    }

    @Test
    public void testTableHeaders1() throws Exception
    {

        String src = "|=a|=b|\r\n" + "|c|d|";

        String target = "||a||b\r\n" + "|c|d";
        Assertions.assertEquals(target, translate(src));
    }

    @Test
    public void testTableHeaders2() throws Exception
    {

        String src = "|=a=|=b=|\r\n" + "|c|d|";

        String target = "||a||b\r\n" + "|c|d";

        Assertions.assertEquals(target, translate(src));
    }

    @Test
    public void testTableHeaders3() throws Exception
    {

        String src = "||a||b\r\n" + "|c|d";

        Assertions.assertEquals(src, translate(src));
    }

    @Test
    public void testExtensions1() throws Exception
    {

        String src = "<<ImagePlugin src='abc'>>";

        String target = "[{ImagePlugin src='abc'}]";

        Assertions.assertEquals(target, translate(src));
    }

    @Test
    public void testExtensions2() throws Exception
    {

        String src = "[{ImagePlugin src='abc'}]";

        Assertions.assertEquals(src, translate(src));
    }

    @Test
    public void testExtensions3() throws Exception
    {

        String src = "<This is HTML>";

        Assertions.assertEquals(src, translate(src));
    }

    @Test
    public void testExtensions4() throws Exception
    {
        String src = "<<FormOpen submit=\'http://jspwiki.apache.org\' >>";

        String target = "[{FormOpen submit=\'http://jspwiki.apache.org\' }]";

        Assertions.assertEquals(target, translate(src));
    }

    @Test
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

        Assertions.assertEquals(target, translate(src));
    }

    @Test
    public void testHeaderNotAtBeginning()
    {
        String src = "Hallo==Hallo";
        Assertions.assertEquals(src, translate(src));
    }

    @Test
    public void testTableLink()
    {
        String src = "|=a=|=b=|\r\n" + "|[[c]]|d|";

        String target = "||a||b\r\n" + "|[c]|d";

        Assertions.assertEquals(target, translate(src));
    }

    @Test
    public void testTableImage()
    {
        String src = "|=a=|=b=|\r\n" + "|[[c]]|{{Image.png}}|";

        String target = "||a||b\r\n" + "|[c]|[{Image src='Image.png'}]";

        Assertions.assertEquals(target, translate(src));
    }

    @Test
    public void testHeaderAfterLinebreak()
    {
        String src = "Hallo das ist super\r\n===Und jetzt\r\nGehts weiter";

        String target = "Hallo das ist super\r\n!!Und jetzt\r\nGehts weiter";

        Assertions.assertEquals(target, translate(src));
    }

    @Test
    public void testBulletMixedEnum()
    {
        String src = "# Hallo\r\n" + "-- Hallo\r\n" + "--- Hallo\r\n" + "Hi";

        String target = "# Hallo\r\n" + "** Hallo\r\n" + "*** Hallo\r\n" + "Hi";
        Assertions.assertEquals(target, translate(src));
    }

    @Test
    public void testBulletMixedEnum2()
    {
        String src = "- Hallo\r\n" + "## Hallo\r\n" + "### Hallo\r\n" + "Hi";

        String target = "* Hallo\r\n" + "## Hallo\r\n" + "### Hallo\r\n" + "Hi";
        Assertions.assertEquals(target, translate(src));
    }

    @Test
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

        Assertions.assertEquals(target, translate(src));
    }

    @Test
    public void testSignature()
    {
        String src = "Hallo\r\n--~~~";
        String target = "Hallo\r\n-- [[Hanno]]";
        Properties props = new Properties();
        props.put("creole.dateFormat", "dd/MM/yyyy");
        Assertions.assertEquals(target, new CreoleToJSPWikiTranslator().translateSignature(props, src, "Hanno"));
    }

    @Test
    public void testSignatureDate()
    {
        String src = "Hallo\r\n--~~~~";
        Calendar cal = Calendar.getInstance();
        String target = "Hallo\r\n-- [[Hanno]], " + (new SimpleDateFormat("dd/MM/yyyy")).format(cal.getTime());
        Properties props = new Properties();
        props.put("creole.dateFormat", "dd/MM/yyyy");
        Assertions.assertEquals(target, new CreoleToJSPWikiTranslator().translateSignature(props, src, "Hanno"));
    }

    @Test
    public void testSignatureDate2()
    {

        String format = "\n   yyyy-MM-dd HH:mm   ";
        String src = "Hallo\r\n--~~~~";
        Calendar cal = Calendar.getInstance();
        String target = "Hallo\r\n-- [[Hanno]], " + (new SimpleDateFormat(format)).format(cal.getTime());
        Properties props = new Properties();
        props.put("creole.dateFormat", format);
        Assertions.assertEquals(target, new CreoleToJSPWikiTranslator().translateSignature(props, src, "Hanno"));
    }

    @Test
    public void testHeaderAtStart()
    {
        String src = "Hallo\r\n=Hallo\r\nHallo";
        String target = "Hallo\r\n!!!Hallo\r\nHallo";
        Assertions.assertEquals(target, translate(src));
    }

    @Test
    public void testSignatureSourceCode()
    {
        String format = "\n   yyyy-MM-dd HH:mm   ";
        String src = "{{{Hallo\r\n" + "--~~~~\r\n" + "Hallo\r\n" + "}}}";
        Properties props = new Properties();
        props.put("creole.dateFormat", format);
        Assertions.assertEquals(src, new CreoleToJSPWikiTranslator().translateSignature(props, src, "Hanno"));
    }

    @Test
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
        Assertions.assertEquals(target, translate(src));
    }

    @Test
    public void testWWWToHTTP()
    {
        String src = "Hallo\r\nHallo[[ 	www.gmx.de]]Hallo\r\nHallo";
        String target = "Hallo\r\nHallo[http://www.gmx.de]Hallo\r\nHallo";
        Assertions.assertEquals(target, translate(src));

        String src2 = "Hallo\r\nHallo[[www.gmx.de]]Hallo\r\nHallo";
        String target2 = "Hallo\r\nHallo[http://www.gmx.de]Hallo\r\nHallo";
        Assertions.assertEquals(target2, translate(src2));

        String src3 = "Hallo\r\nHallo[[www.gmx.de|GMX]]Hallo\r\nHallo";
        String target3 = "Hallo\r\nHallo[GMX|http://www.gmx.de]Hallo\r\nHallo";
        Assertions.assertEquals(target3, translate(src3));
    }

    @Test
    public void testImageX()
    {
        String src = "Hallo {{Image.png|Caption|M,NB}}";
        String target = "Hallo [{ImageX src='Image.png' caption='Caption' width='250' border=0}]";
        Properties props = new Properties();
        props.put("creole.imagePlugin.para.M", "width='250'");
        props.put("creole.imagePlugin.para.NB", "border=0");
        props.put("creole.imagePlugin.name", "ImageX");

        Assertions.assertEquals(target, new CreoleToJSPWikiTranslator().translate(props, src));
    }

    @Test
    public void testImageX11()
    {
        String src = "Hallo {{Image.png|Caption|250}}";
        String target = "Hallo [{ImageX src='Image.png' caption='Caption' width='250px'}]";
        Properties props = new Properties();
        props.put("creole.imagePlugin.name", "ImageX");

        Assertions.assertEquals(target, new CreoleToJSPWikiTranslator().translate(props, src));
    }

    @Test
    public void testImageX2()
    {
        String src = "Hallo {{Image.png|Caption}}";
        String target = "Hallo [{ImageX src='Image.png' caption='Caption' }]";
        Properties props = new Properties();
        props.put("creole.imagePlugin.name", "ImageX");

        Assertions.assertEquals(target, new CreoleToJSPWikiTranslator().translate(props, src));
    }

    @Test
    public void testImageX3()
    {
        String src = "Hallo {{Image.png|Caption|M,NB,TEST}}";
        String target = "Hallo [{ImageX src='Image.png' caption='Caption' width='250' border=0}]";
        Properties props = new Properties();
        props.put("creole.imagePlugin.para.M", "width='250'");
        props.put("creole.imagePlugin.para.NB", "border=0");
        props.put("creole.imagePlugin.name", "ImageX");

        Assertions.assertEquals(target, new CreoleToJSPWikiTranslator().translate(props, src));
    }

    @Test
    public void testImageX4()
    {
        String src = "Hallo {{Image.png||M,NB,TEST}}";
        String target = "Hallo [{ImageX src='Image.png' width='250' border=0}]";
        Properties props = new Properties();
        props.put("creole.imagePlugin.para.M", "width='250'");
        props.put("creole.imagePlugin.para.NB", "border=0");
        props.put("creole.imagePlugin.name", "ImageX");

        Assertions.assertEquals(target, new CreoleToJSPWikiTranslator().translate(props, src));
    }

    @Test
    public void testImageX5()
    {
        String src = "Hallo [[http://www.google.de|{{Image.png||M,NB,TEST}}]]";
        String target = "Hallo [{ImageX src='Image.png' link='http://www.google.de' width='250' border=0}]";
        Properties props = new Properties();
        props.put("creole.imagePlugin.para.M", "width='250'");
        props.put("creole.imagePlugin.para.NB", "border=0");
        props.put("creole.imagePlugin.name", "ImageX");

        Assertions.assertEquals(target, new CreoleToJSPWikiTranslator().translate(props, src));
    }

    @Test
    public void testImageX6() throws Exception
    {
        Properties props = new Properties();
        props.load(new FileInputStream(TEST_PROPERTIES));
        String src = "Hallo {{Image.png|Caption|M,[-]}}";
        String target = "Hallo [{ImageX src='Image.png' caption='Caption' width='180' border=false}]";
        Assertions.assertEquals(target, new CreoleToJSPWikiTranslator().translate(props, src));
    }

    @Test
    public void testImageX7() throws Exception
    {
        Properties props = new Properties();
        props.load(new FileInputStream(TEST_PROPERTIES));
        String src = "Hallo [[http://www.gmx.de|{{Image.png||XL,+X,[-]}}]]";
        String target = "Hallo [{ImageX src='Image.png' link='http://www.gmx.de' width='540' float='right' border=false}]";
        Assertions.assertEquals(target, new CreoleToJSPWikiTranslator().translate(props, src));
    }

    @Test
    public void testImageX8() throws Exception
    {
        Properties props = new Properties();
        props.load(new FileInputStream(TEST_PROPERTIES));
        String src = "Hallo [[http://www.gmx.de|{{Image.png||XL,+X,X-,[-]}}]]";
        String target = "Hallo [{ImageX src='Image.png' link='http://www.gmx.de' width='540' float='right' align='left' border=false}]";
        Assertions.assertEquals(target, new CreoleToJSPWikiTranslator().translate(props, src));
    }

    @Test
    public void testImageX9() throws Exception
    {
        Properties props = new Properties();
        props.load(new FileInputStream(TEST_PROPERTIES));
        String src = "Hallo [[http://www.gmx.de|{{Image.png|Caption|XL,+X,X-,[-]}}]]";
        String target = "Hallo [{ImageX src='Image.png' link='http://www.gmx.de' caption='Caption' width='540' float='right' align='left' border=false}]";
        Assertions.assertEquals(target, new CreoleToJSPWikiTranslator().translate(props, src));
    }

    @Test
    public void testImageX10() throws Exception
    {
        Properties props = new Properties();
        props.load(new FileInputStream(TEST_PROPERTIES));
        String src = "Hallo [[http://www.gmx.de|{{Image.png|Caption|xL, +X , X-, [-]}}]]";
        String target = "Hallo [{ImageX src='Image.png' link='http://www.gmx.de' caption='Caption' width='540' float='right' align='left' border=false}]";
        Assertions.assertEquals(target, new CreoleToJSPWikiTranslator().translate(props, src));
    }

    @Test
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

        Assertions.assertEquals(target, new CreoleToJSPWikiTranslator().translate(props, src));
    }

    @Test
    public void testJuwi()
    {
        String src = "<<JudoScript\r\n" + "if this works then ok\r\n" + "else improve the programm\r\n" + ">>";
        String target = "[{JudoScript\r\n" + "if this works then ok\r\n" + "else improve the programm\r\n" + "}]";
        Assertions.assertEquals(target, translate(src));
    }

    @Test
    public void testPluginBold()
    {

        String src = "**<<CurrentTimePlugin format='HH:mm \'am\' dd-MMM-yyyy'>>**";
        String tar = "__[{CurrentTimePlugin format='HH:mm \'am\' dd-MMM-yyyy'}]__";

        Assertions.assertEquals(tar, translate(src));
    }

    @Test
    public void testPluginLinebreakPlugin()
    {

        String src = "<<CurrentTimePlugin format=zzzz>>\r\n" + "\r\n" + "<<RecentChangesPlugin since='30'>>";

        String tar = "[{CurrentTimePlugin format=zzzz}]\r\n" + "\r\n" + "[{RecentChangesPlugin since='30'}]";

        Assertions.assertEquals(tar, translate(src));
    }

    @Test
    public void testJuwi2()
    {
        String src = "<<JudoScript\r\n" + "if [[this]] works then ok\r\n" + "else improve the programm\r\n" + ">>";
        String target = "[{JudoScript\r\n" + "if [[this]] works then ok\r\n" + "else improve the programm\r\n" + "}]";
        Assertions.assertEquals(target, translate(src));

    }

    @Test
    public void testURL()
    {
        String src = "Hallo[[https://wiki.i3g.hs-heilbronn.de]]Hallo";
        String target = "Hallo[https://wiki.i3g.hs-heilbronn.de]Hallo";

        Assertions.assertEquals(target, translate(src));
    }

    @Test
    public void testSourcePlugin()
    {
        String src = "Hallo<<Hallo{{{Test}}}Hallo>>Hallo";
        String target = "Hallo[{Hallo{{{Test}}}Hallo}]Hallo";

        Assertions.assertEquals(target, translate(src));
    }

    @Test
    public void testMultilinePlugin3()
    {
        String src = "Hallo\r\n" + "<<Hallo\r\n" + "Hallo\r\n" + "Hallo\r\n" + ">>";

        String target = "Hallo\r\n" + "[{Hallo\r\n" + "Hallo\r\n" + "Hallo\r\n" + "}]";

        Assertions.assertEquals(target, translate(src));
    }

    /**
     * See issue JSPWIKI-688 for details.
     * (before the patch it took about 50 seconds on an Intel Core 2 to complete, after the patch : 0.2 seconds).
     */
    @Test
    public void testJSPWIKI688()
    {
        CreoleToJSPWikiTranslator translator = new CreoleToJSPWikiTranslator();

        Properties props = new Properties();
        props.setProperty( "creole.imagePlugin.para.XL", "width='540'" );
        props.setProperty( "creole.imagePlugin.para.XXL", "width='1024'" );
        props.setProperty( "creole.imagePlugin.para.S", "width='140'" );
        props.setProperty( "creole.imagePlugin.para.M", "width='180'" );
        props.setProperty( "creole.imagePlugin.para.L", "width='360'" );
        props.setProperty( "creole.imagePlugin.name", "Image" );
        props.setProperty( "creole.imagePlugin.para.%px", "width='%px\"" );

        String content = "//Note: Please see the [[http://liferay.com/community/100-papercuts|main landing page]] for the latest updates.//<<TableOfContents>>== Introduction ==This project's aim is to identify and fix high-visibility, easy to correct bugs in Liferay Portal. It is driven by the wider Liferay community, with volunteers working to identify, prioritize, assign, and resolve known issues.== The Process ==# **Identify issues.**  The community has always been encouraged to vote for issues that they would like to see fixed sooner rather than later.  This has allowed Liferay program managers to properly prioritize bugs based on community feedback.  With the 100 Paper Cuts program, voting has become even more important.  We use the same voting system to identify high visibility, easy-to-fix bugs.  The 100 Paper Cuts process begins with a period of time in which additional voting is encouraged.  If you wish to vote for issues, please read the voting process.  You can also [[http://issues.liferay.com/secure/IssueNavigator.jspa?mode=hide&requestId=12340|browse this filter for potential PaperCuts bugs]].  This fiter shows all open bugs that haven't been already selected for inclusion into the existing Liferay development pipeline, sorted by votes.# **Assign and Fix.**  During a 2-week period, approximately 10 issues are selected and fixed based on their impact and effort required to fix.  We aim for selecting bugs that take no more than 1 developer-day to fix.  This period is called a \"Community Sprint\" and represents a fixed time amount in which to fix the identified issues.  Once the sprint period is over, the process is repeated.# **Track progress.**  During and after the sprint, issues are tracked for progress, until the issue is merged with one or more Liferay releases.== Current Sprint Status ==|=Resolved|=In Progress|=Blocked on submitter|=Warning!|=Unfixable/Not a papercut||{{http://cdn.www.liferay.com/osb-theme/images/dock/my_place_current.png}}|{{../../../c/wiki/get_page_attachment?p_l_id=10171&nodeId=10304&title=&fileName=SharedImages/health-80plus.gif}}|{{../../../c/wiki/get_page_attachment?p_l_id=10171&nodeId=10304&title=&fileName=SharedImages/health-40to59.png}}|{{../../../c/wiki/get_page_attachment?p_l_id=10171&nodeId=10304&title=&fileName=SharedImages/warning.gif}}|{{../../../c/wiki/get_page_attachment?p_l_id=10171&nodeId=10304&title=&fileName=SharedImages/red.gif}}|=== Sprint 3: Mar 16, 2011 - Mar 30, 2011 ===|= Issue |= Summary |= Assigned To |= Status |= Indicator || [[http://issues.liferay.com/browse/LPS-15491 |LPS-15491]] | Automatic \"html linkification\" of text http links in message board posts: [[http://liferay.com/|http://liferay.com]] => <a href=\"[[http://lliferay.com\">http//lliferay.com</a>|http://lliferay.com\">http://lliferay.com</a>]] | Baptiste | Open | {{../../../c/wiki/get_page_attachment?p_l_id=10171&nodeId=10304&title=&fileName=SharedImages/warning.gif}}|| [[http://issues.liferay.com/browse/LPS-9157 |LPS-9157]] | Width of the panel shouldn't be changed| Maarten | Contributed Solution | {{http://cdn.www.liferay.com/osb-theme/images/dock/my_place_current.png}}|| [[http://issues.liferay.com/browse/LPS-15494 |LPS-15494]] | Showing search result content in inappropriate layout| Juan | Contributed Solution | {{../../../c/wiki/get_page_attachment?p_l_id=10171&nodeId=10304&title=&fileName=http://cdn.www.liferay.com/osb-theme/images/dock/my_place_current.png}}|| [[http://issues.liferay.com/browse/LPS-15791 |LPS-15791]] | Pagination is lost after editing permissions in the Define Permission action of Roles admin portlet| Rafal | Contributed Solution | {{http://cdn.www.liferay.com/osb-theme/images/dock/my_place_current.png}}|| [[http://issues.liferay.com/browse/LPS-8968 |LPS-8968]] | Web Proxy Assertions.fails with error.httpclient in Glassfish| Deb | Contributed Solution | {{../../../c/wiki/get_page_attachment?p_l_id=10171&nodeId=10304&title=&fileName=http://cdn.www.liferay.com/osb-theme/images/dock/my_place_current.png}}|| [[http://issues.liferay.com/browse/LPS-15677 |LPS-15677]] | Asset Publisher portlet does not display web content, When the content publish again.| Boubker | Contributed Solution | {{http://cdn.www.liferay.com/osb-theme/images/dock/my_place_current.png}}|| [[http://issues.liferay.com/browse/LPS-15225 |LPS-15225]] | String not internationalized in Enterprise Admin Organizations portlet.| Corne | Community Resolved| {{../../../c/wiki/get_page_attachment?p_l_id=10171&nodeId=10304&title=&fileName=http://cdn.www.liferay.com/osb-theme/images/dock/my_place_current.png}}|| [[http://issues.liferay.com/browse/LPS-15122 |LPS-15122]] | o language key for \"Table of Contents\" in the wiki | Corne | Contributed Solution| {{http://cdn.www.liferay.com/osb-theme/images/dock/my_place_current.png}}|| [[http://issues.liferay.com/browse/LPS-14789 |LPS-14789]] | Freemarker template processor has undefined variables | Tomas | Contributed Solution | {{../../../c/wiki/get_page_attachment?p_l_id=10171&nodeId=10304&title=&fileName=http://cdn.www.liferay.com/osb-theme/images/dock/my_place_current.png}}|== Previous Sprint Status ===== Sprint 2: Feb 23, 2011 - Mar 9, 2011 ===|= Issue |= Summary |= Assigned To |= Status |= Indicator || [[http://issues.liferay.com/browse/LEP-6973 |LEP-6973]] | Publish to Live is not properly configuring Page Permissions from Staging | Boubker | Contributed Solution | {{http://cdn.www.liferay.com/osb-theme/images/dock/my_place_current.png}}|| [[http://issues.liferay.com/browse/LPS-12988 |LPS-12988]] | Bad HTTP Content-Type for RSS feed | Jelmer | Contributed Solution | {{../../../c/wiki/get_page_attachment?p_l_id=10171&nodeId=10304&title=&fileName=http://cdn.www.liferay.com/osb-theme/images/dock/my_place_current.png}}|| [[http://issues.liferay.com/browse/LPS-10263 |LPS-10263]] | Remove the mandatory URL from announcements entries | Jaromir | Contributed Solution | {{http://cdn.www.liferay.com/osb-theme/images/dock/my_place_current.png}}|| [[http://issues.liferay.com/browse/LPS-11854 |LPS-11854]] | Web Form Portlet Configuration -- Changing type field does not appear to work at all in IE8. | Deb | Contributed Solution | {{../../../c/wiki/get_page_attachment?p_l_id=10171&nodeId=10304&title=&fileName=http://cdn.www.liferay.com/osb-theme/images/dock/my_place_current.png}}|| [[http://issues.liferay.com/browse/LPS-12918 |LPS-12918]] | Import lar with global \"Structures/Templates\" | Jelmer | Contributed Solution | {{http://cdn.www.liferay.com/osb-theme/images/dock/my_place_current.png}}|| [[http://issues.liferay.com/browse/LPS-11479 |LPS-11479]] | ServiceBuilder doesn't support one-to-one relationships out of the box | Tomas | Open | {{../../../c/wiki/get_page_attachment?p_l_id=10171&nodeId=10304&title=&fileName=SharedImages/red.gif}}|| [[http://issues.liferay.com/browse/LPS-7503 |LPS-7503]] | Give alternatives to <object> | Juan | Open | {{../../../c/wiki/get_page_attachment?p_l_id=10171&nodeId=10304&title=&fileName=SharedImages/red.gif}}|| [[http://issues.liferay.com/browse/LPS-14905 |LPS-14905]] | Unable to import group from LDAP in Liferay 6.0.5 with ldap.import.method=user | Baptiste | Resolved | {{http://cdn.www.liferay.com/osb-theme/images/dock/my_place_current.png}}|=== Sprint 1: Feb 2, 2011 - Feb 16, 2011 ===|= Issue |= Summary |= Assigned To |= Status |= Indicator || [[http://issues.liferay.com/browse/LPS-11003|LPS-11003]] | sample-struts-liferay-portlet can not be deployed to trunk | James | Closed (not reproducible) | {{../../../c/wiki/get_page_attachment?p_l_id=10171&nodeId=10304&title=&fileName=http://cdn.www.liferay.com/osb-theme/images/dock/my_place_current.png}}|| [[http://issues.liferay.com/browse/LPS-13422|LPS-13422]] | Preformatted URLS show with \"[ ]\" around them on a wiki page | Milan | Fixed | {{http://cdn.www.liferay.com/osb-theme/images/dock/my_place_current.png}}|| [[http://issues.liferay.com/browse/LPS-14911|LPS-14911]] | Unable to publish previously saved draft | Deb | Community Resolved | {{../../../c/wiki/get_page_attachment?p_l_id=10171&nodeId=10304&title=&fileName=http://cdn.www.liferay.com/osb-theme/images/dock/my_place_current.png}}|| [[http://issues.liferay.com/browse/LPS-14671|LPS-14671]] | When adding a document to the document library a file extension is required in the document title | Corne | Closed (already fixed) | {{http://cdn.www.liferay.com/osb-theme/images/dock/my_place_current.png}}|| [[http://issues.liferay.com/browse/LPS-14411|LPS-14411]] | complete_gradient.png is missing | Boubker | In Review | {{../../../c/wiki/get_page_attachment?p_l_id=10171&nodeId=10304&title=&fileName=http://cdn.www.liferay.com/osb-theme/images/dock/my_place_current.png}}|| [[http://issues.liferay.com/browse/LPS-14351|LPS-14351]] | Liferay Calendar, Duplicate Events Upon Import | Tomas | Contributed Solution | {{http://cdn.www.liferay.com/osb-theme/images/dock/my_place_current.png}}|| [[http://issues.liferay.com/browse/LPS-12988|LPS-12988]] | Bad HTTP Content-Type for RSS feed | Maarten | Open | {{../../../c/wiki/get_page_attachment?p_l_id=10171&nodeId=10304&title=&fileName=SharedImages/red.gif}}|| [[http://issues.liferay.com/browse/LPS-12810|LPS-12810]] | Error on Summary Tab of Calendar | Juan | Fixed | {{http://cdn.www.liferay.com/osb-theme/images/dock/my_place_current.png}}|| [[http://issues.liferay.com/browse/LPS-11859|LPS-11859]] | Categories Navigation with Wiki Broken | Juan | Contributed Solution | {{../../../c/wiki/get_page_attachment?p_l_id=10171&nodeId=10304&title=&fileName=http://cdn.www.liferay.com/osb-theme/images/dock/my_place_current.png}}|| [[http://issues.liferay.com/browse/LPS-11854|LPS-11854]] | Web Form Portlet Configuration -- Changing type field does not appear to work at all in IE8. | Szymon | Open | {{../../../c/wiki/get_page_attachment?p_l_id=10171&nodeId=10304&title=&fileName=SharedImages/warning.gif}}|";

        long startTime = System.currentTimeMillis();
        translator.translate( props, content );
        long testDuration = System.currentTimeMillis() - startTime;

        // even a very slow cpu should do this much faster
        Assertions.assertTrue( testDuration < 3000, "rendering takes too long" );
    }

    public String translate(String src)
    {
        CreoleToJSPWikiTranslator translator = new CreoleToJSPWikiTranslator();

        return translator.translate(new Properties(), src);
    }

}
