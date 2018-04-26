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
package org.apache.wiki.htmltowiki;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * JUnit test cases for Converting Html to Wiki Markup.
 *
 */
public class HtmlStringToWikiTranslatorTest {
    HtmlStringToWikiTranslator html2wiki;

    @Before
    public void setUp() {
        html2wiki = new HtmlStringToWikiTranslator();
    }

    @Test
    public void testAnchor() throws Exception {
        Assert.assertEquals( "[startup.bat]", html2wiki.translate(
                " <a class=\"attachment\" href=\"attach?page=startup.bat\">startup.bat</a><a href=\"PageInfo.jsp?page=startup.bat\"><img src=\"images/attachment_small.png\" alt=\"(att)\" border=\"0\"></a>" ) );

        Assert.assertEquals( "[http://www.startup.de]", html2wiki.translate(
                "<a class=\"external\" href=\"http://www.startup.de\">http://www.startup.de</a><img class=\"outlink\" src=\"images/out.png\" alt=\"\">" ) );

        Assert.assertEquals( " [AbsolutelyTestNotExisting]\n\n",
                html2wiki.translate(
                        "<table class=\"imageplugin\" align=\"left\" border=\"0\">\r\n" + "<tbody><tr><td><br>\r\n" + "</td></tr>\r\n" + "</tbody></table>\r\n"
                                + "\r\n" + " [AbsolutelyTestNotExisting]<p>\r\n" + "<table class=\"imageplugin\" align=\"left\" border=\"0\">\r\n" + "\r\n"
                                + "<tbody>\r\n" + "</tbody></table>\r\n" + "\r\n" + "</p><p>\r\n" + "</p>" ) );

        Assert.assertEquals( "[ThisPageDoesNotExist]", html2wiki.translate( "<a href=\"Edit.jsp?page=ThisPageDoesNotExist\">ThisPageDoesNotExist</a>" ) );

        Assert.assertEquals( "[/JSPWiki/wysiwyg/FCKeditor/editor/images/smiley/msn/sad_smile.gif]",
                html2wiki.translate( "<img src=\"/JSPWiki/wysiwyg/FCKeditor/editor/images/smiley/msn/sad_smile.gif\" alt=\"\"/>" ) );

        Assert.assertEquals( "[AugumentedWikiLinks|AugumentedWikiLinks|title='my \"custom\" title' target='_blank']", html2wiki.translate(
                "<a class=\"wikipage\" href=\"Wiki.jsp?page=AugumentedWikiLinks\" target=\"_blank\" title=\"my 'custom' title\">AugumentedWikiLinks</a>" ) );

        // footnote links
        Assert.assertEquals( "[23]", html2wiki.translate( "<a class=\"footnoteref\" href=\"#ref-PageName-23\">[23]</a>" ) );
        Assert.assertEquals( "[something|23]", html2wiki.translate( "<a class=\"footnoteref\" href=\"#ref-PageName-23\">[something]</a>" ) );

    }

    @Test
    public void testTable() throws Exception {
        Assert.assertEquals( "a\n| erste\n", html2wiki.translate( "a <table border=\"1\"> <tbody><tr> <td> erste</td> </tr> </tbody> </table>" ) );

        Assert.assertEquals(
                "|| Throalisches Jahr || Ereignis\n" + "| 100 v. TH | Elianer Messias \u00fcbersetzt die tausendj\u00e4hrigen B\u00fccher von Harrow.\n"
                        + "| 50 v. TH | Gr\u00fcndung Nehr?eshams und der ewigen Bibliothek.\n",
                html2wiki.translate( "<table class=\"wikitable\" border=\"1\"><tbody><tr><th> Throalisches Jahr </th><th> Ereignis</th></tr>\n"
                        + "<tr><td> 100 v. TH</td><td> Elianer Messias \u00fcbersetzt die tausendj\u00e4hrigen B\u00fccher von Harrow.</td></tr>\n"
                        + "<tr><td> 50 v. TH</td><td> Gr\u00fcndung Nehr?eshams und der ewigen Bibliothek.</td></tr>\n" + "</tbody></table>" ) );

        Assert.assertEquals(
                "|| Throalisches Jahr || Ereignis\n" + "| 100 v. TH | Elianer Messias \u00fcbersetzt die tausendj\u00e4hrigen B\u00fccher von Harrow.\n"
                        + "| 50 v. TH | Gr\u00fcndung Nehr?eshams und der ewigen Bibliothek.\n\u00A0",
                html2wiki.translate( "<table class=\"wikitable\" border=\"1\"><tbody><tr><th> Throalisches Jahr </th><th> Ereignis</th></tr>\n"
                        + "<tr><td> 100 v. TH</td><td> Elianer Messias \u00fcbersetzt die tausendj\u00e4hrigen B\u00fccher von Harrow.</td></tr>\n"
                        + "<tr><td> 50 v. TH</td><td> Gr\u00fcndung Nehr?eshams und der ewigen Bibliothek.</td></tr>\n" + "</tbody></table> &nbsp;" ) );

        Assert.assertEquals( "| 3. Rang | Name des Helden, den der Bogen t\u00f6ten sollte.\n" + "| F\u00e4higkeit | Bonus auf die Initiative von 1\n",
                html2wiki.translate(
                        "<table class=\"wikitable\" border=\"1\"><tbody><tr><td> 3. Rang</td><td> Name des Helden, den der Bogen t\u00f6ten sollte.</td></tr>\n"
                                + "<tr><td> F\u00e4higkeit</td><td> Bonus auf die Initiative von 1</td></tr></tbody></table></p><p>" ) );

        Assert.assertEquals(
                "| Name: [Christian|ChristianS] \\\\ Geschicklichkeit: 2 \\\\ Hang zu perversen Sexorgien. Jongliert mit Worten und K\u00f6pfen. \\\\ [Berian Nachtschleicher|Berian] \\\\ [XLerul] \\\\ [Effifot Erif|EffifotErif]\n",
                html2wiki.translate(
                        "<table class=\"wikitable\" border=\"1\"><tbody><tr><td> Name: <a class=\"wikipage\" href=\"Wiki.jsp?page=ChristianS\">Christian</a> <br> Geschicklichkeit: 2 <br> Hang zu perversen Sexorgien. Jongliert mit Worten und K\u00f6pfen. <br> <a class=\"wikipage\" href=\"Wiki.jsp?page=Berian\">Berian Nachtschleicher</a> <br> <a class=\"wikipage\" href=\"Wiki.jsp?page=XLerul\">XLerul</a> <br> <a class=\"wikipage\" href=\"Wiki.jsp?page=EffifotErif\">Effifot Erif</a></td></tr> </tbody></table>" ) );

    }

    @Test
    public void testRulers() throws Exception {
        Assert.assertEquals( "a\n----\nb", html2wiki.translate( "a<hr/>b" ) );
        Assert.assertEquals( "by\n" + "----\n" + "Dies", html2wiki.translate( "by\n" + "<hr>\n" + "Dies" ) );

    }

    @Test
    public void testParagraphs() throws Exception {
        Assert.assertEquals( "a\nb\nc", html2wiki.translate( "a<p>b</p>c" ) );

        Assert.assertEquals( "ab", html2wiki.translate( "a<p></p>b" ) );

        Assert.assertEquals( "a\n\nb", html2wiki.translate( "a<p>\n</p>b" ) );

        Assert.assertEquals( "\n\\\\\n__Willkommen__ \\\\\n\\\\\nUnd niemand wird sie sehen \\\\\nEine Page ...\n\nAls Unterthema\n", html2wiki.translate(
                "<p><br />\n<strong>Willkommen</strong> <br />\n<br />\nUnd niemand wird sie sehen <br />\nEine Page ...</p>\n<p>Als Unterthema</p>" ) );

        Assert.assertEquals( "\n\u00A0\n\nTop\n\nBottom\n\n\n", html2wiki.translate( "<p>&nbsp;</p><p>Top</p>\n<p></p>\n<p></p>\n<p>Bottom</p> <p> </p>" ) );
    }

    @Test
    public void testWhitespace() throws Exception {
        Assert.assertEquals( "", html2wiki.translate( "" ) );
        Assert.assertEquals( "", html2wiki.translate( "   " ) );
        Assert.assertEquals( "", html2wiki.translate( "<div>\n\n\n</div>" ) );
        Assert.assertEquals( "a ", html2wiki.translate( "a\n  \n\n \t\r\n" ) );
    }

    @Test
    public void testLists() throws Exception {
        Assert.assertEquals( "\n* Punkt 1\n* Punkt 2\n", html2wiki.translate( "<ul><li>Punkt 1</li><li>Punkt 2</li></ul>" ) );
        Assert.assertEquals( "\n# Punkt 1\n# Punkt 2\n", html2wiki.translate( "<ol><li>Punkt 1</li><li>Punkt 2</li></ol>" ) );
        Assert.assertEquals( "\n# Punkt 1\n## Punkt 2\n\n", html2wiki.translate( "<ol><li>Punkt 1<ol><li>Punkt 2</li></ol></li></ol>" ) );
        Assert.assertEquals( "\n* Punkt 1\n** Punkt 2\n\n", html2wiki.translate( "<ul><li>Punkt 1<ul><li>Punkt 2</li></ul></li></ul>" ) );
        Assert.assertEquals( "\n* Punkt 1\n*# Punkt 2\n\n", html2wiki.translate( "<ul><li>Punkt 1<ol><li>Punkt 2</li></ol></li></ul>" ) );
        Assert.assertEquals(
                "\n# list item 1\n# list item 2\n## list item 2.1\n##* list item 2.1.1\n##* list item 2.1.2\n## list item 2.2\n# list item 3\n## list item 3.1\n##* list item 3.1.1\n## list item 3.2\n# list item 4\n",
                html2wiki.translate(
                        "<ol> <li>list item 1</li> <li>list item 2 <ol> <li>list item 2.1 <ul> <li>list item 2.1.1</li> <li>list item 2.1.2</li> </ul> </li> <li>list item 2.2</li> </ol> </li> <li>list item 3 <ol> <li>list item 3.1 <ul> <li>list item 3.1.1</li> </ul> </li> <li>list item 3.2</li> </ol> </li> <li>list item 4</li> </ol>" ) );

        Assert.assertEquals(
                "\n* Diese Karte kann von jedem editiert und um neue Links erweitert werden. \\\\Klickt einfach unten neben der Karte auf {{{[edit]}}}\n",
                html2wiki.translate(
                        "<ul><li> Diese Karte kann von jedem editiert und um neue Links erweitert werden.<br>Klickt einfach unten neben der Karte auf <span style=\"font-family: monospace; white-space: pre;\">[edit]</span></li></ul>" ) );

        Assert.assertEquals(
                "\n* Diese Karte kann von jedem editiert und um neue Links erweitert werden. \\\\Klickt einfach unten neben der Karte auf {{{[edit]}}}\n",
                html2wiki.translate(
                        "<ul><li> Diese Karte kann von jedem editiert und um neue Links erweitert werden.<br>Klickt einfach unten neben der Karte auf <span style=\"font-family: monospace; white-space: pre;\">[edit]</span></li></ul>" ) );

    }

    @Test
    public void testPre() throws Exception {
        Assert.assertEquals( "\n{{{hallo}}}\n", html2wiki.translate( "<pre>hallo</pre>" ) );
        Assert.assertEquals( "\n{{{Hallo\nWelt!\n\n}}}\n", html2wiki.translate( "<pre>Hallo<br>Welt!<br><br></pre>" ) );
        Assert.assertEquals( "\n{{{\n\n\n\nHallo\n\n\n\nWelt!\n\n\n\n}}}\n",
                html2wiki.translate( "\n\n\n\n<pre>\n\n\n\nHallo\n\n\n\nWelt!\n\n\n\n</pre>\n\n\n\n" ) );

        Assert.assertEquals( "\n{{{\n\n* Baltramon \n  lasdjfh\n\n}}}\n", html2wiki.translate( "<pre>\n\n* Baltramon \n  lasdjfh\n\n</pre>" ) );

        /*
         * // The style "font-family: courier new" is no longer translated as monospace text, so this test case is no longer needed. Assert.assertEquals(
         * "Diese Karte{{{ kann }}} von", html2wiki .translate( "Diese Karte<span style=\"font-family: courier new,courier,mono;\"> kann </span> von" ) );
         */

        Assert.assertEquals( "Fahrt einfac{{{h mit\u00A0\u00A0 \n der \u00A0 Maus}}} drueber", html2wiki
                .translate( "Fahrt einfac<span style=\"font-family: monospace; white-space: pre;\">h mit&nbsp;&nbsp; <br> der &nbsp; Maus</span> drueber" ) );

    }

    @Test
    public void testTT() throws Exception {
        Assert.assertEquals( "{{hallo}}", html2wiki.translate( "<tt>hallo</tt>" ) );
        Assert.assertEquals( "{{hallo}}", html2wiki.translate( "<code>hallo</code>" ) );
    }

    @Test
    public void testCenter() throws Exception {
        Assert.assertEquals( "\n%%( text-align: center; )\nHello \\\\\nWorld!\n/%\n",
                html2wiki.translate( "<div style=\"text-align: center;\">Hello<br>World!</div>" ) );

        Assert.assertEquals( "__%%( text-align: center; display: block; )Hello \\\\World!/%__",
                html2wiki.translate( "<span style=\"font-weight: bold; text-align: center; display: block;\">Hello<br>World!</span>" ) );

    }

    @Test
    public void testImage() throws Exception {
        Assert.assertEquals( "[{Image src='Homunkulus/homunculus4.jpg' align='left'}]", html2wiki.translate(
                "<table class=\"imageplugin\" align=\"left\" border=\"0\"> <tbody><tr><td><img src=\"attach?page=Homunkulus%2Fhomunculus4.jpg\"></td></tr> </tbody></table>" ) );

        Assert.assertEquals( "[{Image src=\'AbenteuerQuilpins/Quilpins.jpg\' align=\'left\'}]",
                html2wiki.translate( "<table class=\"imageplugin\" align=\"left\" border=\"0\">\r\n"
                        + "<tbody><tr><td><img src=\"attach?page=AbenteuerQuilpins%2FQuilpins.jpg\"></td></tr>\r\n</tbody>" + "</table>" ) );

        Assert.assertEquals( "[{Image src=\'AbenteuerQuilpins/Quilpins.jpg\' caption=\'Testing Image\' style=\'font-size: 120%; color: green;\'}]",
                html2wiki.translate( "<table class=\"imageplugin\" style=\"font-size: 120%; color: green;\" border=\"0\">\r\n"
                        + "<caption align=\"bottom\">Testing Image</caption>\r\n"
                        + "<tbody><tr><td><img src=\"attach?page=AbenteuerQuilpins%2FQuilpins.jpg\"></td></tr>\r\n</tbody>" + "</table>" ) );

        Assert.assertEquals(
                "[{Image src=\'http://opi.yahoo.com/online?u=YahooUser1234&m=g&t=2\' link=\'http://edit.yahoo.com/config/send_webmesg?.target=YahooUser1234&.src=pg\'}]",
                html2wiki.translate( "<table class=\"imageplugin\" border=\"0\">\r\n" + "<tbody><tr><td>"
                        + "<a href=\"http://edit.yahoo.com/config/send_webmesg?.target=YahooUser1234&amp;.src=pg\">"
                        + "<img src=\"http://opi.yahoo.com/online?u=YahooUser1234&amp;m=g&amp;t=2\">" + "</a></td></tr>\r\n" + "</tbody>" + "</table>" ) );

        Assert.assertEquals(
                "[{Image src=\'homunculus4.jpg\' align=\'left\' height=\'100px\' width=\'100px\' alt=\'alt text\' caption=\'caption text\' link=\'http://google.de\' border=\'1\'}]",
                html2wiki.translate( "<table class=\"imageplugin\" align=\"left\" border=\"0\"> \r\n"
                        + "  <caption align=\"bottom\">caption text</caption> \r\n" + "  <tbody><tr><td>\r\n"
                        + "    <a href=\"http://google.de\"><img src=\"homunculus4.jpg\" alt=\"alt text\" border=\"1\" height=\"100px\" width=\"100px\">\r\n"
                        + "    </a></td></tr> \r\n" + "  </tbody>  \r\n" + "</table>" ) );

        Assert.assertEquals(
                "[{Image src=\'http://opi.yahoo.com/online?u=YahooUser1234&m=g&t=2\' link=\'http://edit.yahoo.com/config/send_webmesg?.target=YahooUser1234&.src=pg\'}]",
                html2wiki.translate( "  <a href=\"http://edit.yahoo.com/config/send_webmesg?.target=YahooUser1234&amp;.src=pg\">\r\n"
                        + "  <img src=\"http://opi.yahoo.com/online?u=YahooUser1234&amp;m=g&amp;t=2\">\r\n" + "  </a" ) );

    }

    @Test
    public void testPlugin() throws Exception {
        Assert.assertEquals(
                "This is a private homepage done by\n" + "----\n" + "Dies ist eine private, nicht-kommerzielle Homepage von\n"
                        + "[{Text2gif width=\'150\' height=\'100\' \n" + " \n" + "Sebastian L. Baltes \n" + "Lange Str. 53 \n" + "44137 Dortmund \n" + " \n"
                        + "email: info@sorokan.de \n" + "}]\n",
                html2wiki.translate( "This is a private homepage done by\n" + "<hr>\n" + "Dies ist eine private, nicht-kommerzielle Homepage von\n" + "<p>\n"
                        + "[{Text2gif width=\'150\' height=\'100\'\n" + "<br> <br>Sebastian L. Baltes\n" + "<br>Lange Str. 53\n" + "<br>44137 Dortmund\n"
                        + "<br> <br>email: info@sorokan.de\n" + "<br>}]\n" + "</p><p>" ) );

    }

    @Test
    public void testCSS() throws Exception {
        Assert.assertEquals( "%%( color: rgb(255, 0, 0); )Und niemand wird sie sehen/%, die",
                html2wiki.translate( "<span style=\"color: rgb(255, 0, 0);\">Und niemand wird sie sehen</span>, die" ) );

        Assert.assertEquals(
                "\n%%information\nCSS class here\n/%\n\nFont %%( color: #ff9900; font-family: arial; font-size: large; )styling here./% Some %%( background-color: #ffff00; )more here/%.\n",
                html2wiki.translate(
                        "<div class=\"information\">CSS class here</div>\n" + "<p>Font <font face=\"Arial\" color=\"#ff9900\" size=\"5\">styling here.</font>"
                                + " Some <font style=\"background-color: #ffff00\">more here</font>.</p>" ) );

        Assert.assertEquals( "\n\n%%( text-align: center; )\nsome leading text %%strike line through this text/% some trailing text\n/%\n\n",
                html2wiki.translate( "<p align=\"center\">some leading text <strike>line through this text</strike> some trailing text</p>" ) );
    }

    @Test
    public void testParsing() throws Exception {
        Assert.assertEquals( "Hello World!", html2wiki.translate( "Hello World!" ) );

        Assert.assertEquals( "a", html2wiki.translate( "a" ) );

        Assert.assertEquals( "a \\\\b", html2wiki.translate( "a<br/>b" ) );

        Assert.assertEquals( "\\\\\\", html2wiki.translate( "\\\\\\" ) );

        Assert.assertEquals( "[{Test\nHello World!}]", html2wiki.translate( "[{Test\\\\Hello World!}]" ) );

        Assert.assertEquals( "{{{[{Test\\\\Hello World!}]}}}", html2wiki.translate( "{{{[{Test\\\\Hello World!}]}}}" ) );

        Assert.assertEquals( "{{{[{Test\\\\Hello World!}]}}}{{{[{Test\\\\\\\\Hello World!}]}}}[{Test\n\nHello World!}][{Test\n\nHello World!}]", html2wiki
                .translate( "{{{[{Test\\\\Hello World!}]}}}{{{[{Test\\\\\\\\Hello World!}]}}}[{Test\\\\\\\\Hello World!}][{Test\\\\\\\\Hello World!}]" ) );

    }

    @Test
    public void testBoldAndItalic() throws Exception {
        Assert.assertEquals( "Dies ist __bold__, ''italic'' und __''both''__.", html2wiki.translate(
                "Dies ist <span style=\"font-weight: bold;\">bold</span>, <span style=\"font-style: italic;\">italic</span> und <span style=\"font-style: italic; font-weight: bold;\">both</span>." ) );

        Assert.assertEquals( "Dies ist __bold__, ''italic'' und __''both''__ 2.",
                html2wiki.translate( "Dies ist <b>bold</b>, <i>italic</i> und <b><i>both</i></b> 2." ) );

        Assert.assertEquals( "Dies ist __bold__, ''italic'' und __''both''__ 3.",
                html2wiki.translate( "Dies ist <strong>bold</strong>, <em>italic</em> und <strong><em>both</em></strong> 3." ) );

        Assert.assertEquals( "Wilma: ''Ich m\u00f6chte hiermal in allerDeutlichkeit sagen! ''",
                html2wiki.translate( "Wilma: <i>Ich             m\u00f6chte hier\nmal in aller\nDeutlichkeit sagen! </i>" ) );

    }

    @Test
    public void testHeading() throws Exception {
        Assert.assertEquals( "\n!!! Heading 1 should be translated to large heading.\n",
                html2wiki.translate( "<h1>Heading 1 should be translated to large heading.</h1>" ) );

        Assert.assertEquals( "\n!!! Heading 2 should be translated to large heading.\n",
                html2wiki.translate( "<h2>Heading 2 should be translated to large heading.</h2>" ) );

        Assert.assertEquals( "\n!! Heading 3 should be translated to medium heading.\n",
                html2wiki.translate( "<h3>Heading 3 should be translated to medium heading.</h3>" ) );

        Assert.assertEquals( "\n! Heading 4 should be translated to small heading.\n",
                html2wiki.translate( "<h4>Heading 4 should be translated to small heading.</h4>" ) );
    }

    @Test
    public void testForm() throws Exception {
        Assert.assertEquals( "\n[{FormOpen form='myForm'}]\n\n[{FormClose}]\n",
                html2wiki.translate( "<div class=\"wikiform\">\n<form name=\"myForm\"><input name=\"formname\" value=\"myForm\" type=\"hidden\">\n</div>" ) );

        Assert.assertEquals( "[{FormInput type='hidden' name='myHiddenField' value='myHiddenField'}]myHiddenField",
                html2wiki.translate( "<input name=\"nbf_myHiddenField\" value=\"myHiddenField\" type=\"hidden\">myHiddenField" ) );

        Assert.assertEquals( "[{FormInput type='checkbox' name='myCheckbox' value='myCheckbox' checked='checked'}]myCheckbox",
                html2wiki.translate( "<input checked=\"checked\" value=\"myCheckbox\" name=\"nbf_myCheckbox\" type=\"checkbox\">myCheckbox" ) );

        Assert.assertEquals( "[{FormInput type='radio' name='myRadioButton' value='myRadioButton'}]myRadioButton",
                html2wiki.translate( "<input name=\"nbf_myRadioButton\" value=\"myRadioButton\" type=\"radio\">myRadioButton" ) );

        Assert.assertEquals( "[{FormInput type='button' name='myButton' value='myButton'}]myButton",
                html2wiki.translate( "<input name=\"nbf_myButton\" value=\"myButton\" type=\"button\">myButton" ) );

        Assert.assertEquals( "[{FormTextarea name='myTextarea' rows='6' cols='50'}]myTextarea",
                html2wiki.translate( "<textarea cols=\"50\" name=\"nbf_myTextarea\" rows=\"6\"></textarea>myTextarea" ) );

        Assert.assertEquals( "[{FormSelect name='mySelectionList' value='apple;*orange;pear'}]mySelectList",
                html2wiki.translate( "<select name=\"nbf_mySelectionList\">\n" + "<option value=\"apple\">apple</option>\n"
                        + "<option selected=\"selected\" value=\"orange\">orange</option>\n" + "<option value=\"pear\">pear</option>\n"
                        + "</select>mySelectList" ) );

    }

    @Test
    public void testDefinitionList() throws Exception {
        Assert.assertEquals( "\n;__Priority__:High\n\n;__TODO Name__:Initialization\n\n;__Requester__:John Smith\n",
                html2wiki.translate( "<dl><dt><b>Priority</b></dt><dd>High</dd></dl>\n" + "<dl><dt><b>TODO Name</b></dt><dd>Initialization</dd></dl>\n"
                        + "<dl><dt><b>Requester</b></dt><dd>John Smith</dd></dl>\n" ) );

        Assert.assertEquals( "Some text here\n;:(A)indented comment here\n\n;:(B)another comment here\n", html2wiki.translate(
                "Some text here\n<dl><dt></dt><dd>(A)indented comment here</dd></dl>\n" + "<dl><dt></dt><dd>(B)another comment here</dd></dl>\n" ) );

        Assert.assertEquals( "\n;__New Page Name__:[{FormInput type='text' name='newPageName'}]\n",
                html2wiki.translate( "\n<dl><dt><b>New Page Name</b></dt><dd><input name=\"nbf_newPageName\" type=\"text\"></dd></dl>\n" ) );
    }

}
