package com.ecyrd.jspwiki.htmltowiki;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit test cases for Converting Html to Wiki Markup.
 * 
 * @author Sebastian Baltes (sbaltes@gmx.com)
 */
public class TestHtmlStringToWikiTranslator extends TestCase
{
    HtmlStringToWikiTranslator html2wiki;

    public static void main( String[] args )
    {
        junit.textui.TestRunner.run( TestHtmlStringToWikiTranslator.class );
    }

    public TestHtmlStringToWikiTranslator( String arg0 )
    {
        super( arg0 );
    }

    public void setUp()
    {
        html2wiki = new HtmlStringToWikiTranslator();
    }
    
    public void testAnchor() throws Exception
    {
        assertEquals(
                     " [startup.bat] ",
                     html2wiki
                             .translate( "<a class=\"attachment\" href=\"attach?page=startup.bat\">startup.bat</a><a href=\"PageInfo.jsp?page=startup.bat\"><img src=\"images/attachment_small.png\" alt=\"(att)\" border=\"0\"></a>" ) );

       assertEquals(
                     " [http://www.startup.de] ",
                     html2wiki
                             .translate( "<a class=\"external\" href=\"http://www.startup.de\">http://www.startup.de</a><img class=\"outlink\" src=\"images/out.png\" alt=\"\">" ) );

       assertEquals( " [AbsolutelyTestNotExisting]\n\n", html2wiki
                     .translate( "<table class=\"imageplugin\" align=\"left\" border=\"0\">\r\n" + "<tbody><tr><td><br>\r\n"
                                 + "</td></tr>\r\n" + "</tbody></table>\r\n" + "\r\n" + "[AbsolutelyTestNotExisting]<p>\r\n"
                                 + "<table class=\"imageplugin\" align=\"left\" border=\"0\">\r\n" + "\r\n" + "<tbody>\r\n"
                                 + "</tbody></table>\r\n" + "\r\n" + "</p><p>\r\n" + "</p>" ) );

       assertEquals( "[ThisPageDoesNotExist] ", html2wiki
                     .translate( "<u>ThisPageDoesNotExist</u><a href=\"Edit.jsp?page=ThisPageDoesNotExist\">?</a>" ) );

       assertEquals( "[/JSPWiki/wysiwyg/FCKeditor/editor/images/smiley/msn/sad_smile.gif] ", html2wiki
                     .translate( "<img src=\"/JSPWiki/wysiwyg/FCKeditor/editor/images/smiley/msn/sad_smile.gif\" alt=\"\"/>" ) );


    }
    
    public void testTable() throws Exception
    {
        assertEquals( "a\n| erste\n", html2wiki
                      .translate( "a <table border=\"1\"> <tbody><tr> <td> erste</td> </tr> </tbody> </table>" ) );

        assertEquals(
                     "|| Throalisches Jahr || Ereignis\n"
                                                                                       + "| 100 v. TH | Elianer Messias übersetzt die tausendjährigen Bücher von Harrow.\n"
                                                                                       + "| 50 v. TH | Gründung Nehr?eshams und der ewigen Bibliothek.\n",
                     html2wiki
                             .translate( "<table class=\"wikitable\" border=\"1\"> <tbody><tr><th> Throalisches Jahr </th><th> Ereignis</th></tr> <tr><td> 100 v. TH</td><td> Elianer Messias übersetzt die tausendjährigen Bücher von Harrow.</td></tr> <tr><td> 50 v. TH</td><td> Gründung Nehr?eshams und der ewigen Bibliothek.</td></tr> </tbody></table>" ) );

       assertEquals(
                     "|| Throalisches Jahr || Ereignis\n"
                                                                                       + "| 100 v. TH | Elianer Messias übersetzt die tausendjährigen Bücher von Harrow.\n"
                                                                                       + "| 50 v. TH | Gründung Nehr?eshams und der ewigen Bibliothek.\n\u00A0",
                     html2wiki
                             .translate( "<table class=\"wikitable\" border=\"1\"> <tbody><tr><th> Throalisches Jahr </th><th> Ereignis</th></tr> <tr><td> 100 v. TH</td><td> Elianer Messias übersetzt die tausendjährigen Bücher von Harrow.</td></tr> <tr><td> 50 v. TH</td><td> Gründung Nehr?eshams und der ewigen Bibliothek.</td></tr> </tbody></table> &nbsp;" ) );

       assertEquals(
                    "| 3. Rang | Name des Helden, den der Bogen töten sollte.\n"
                                                                                      + "| Fähigkeit | Bonus auf die Initiative von 1\n\n",
                    html2wiki
                            .translate( "<table class=\"wikitable\" border=\"1\"> <tbody><tr><td> 3. Rang</td><td> Name des Helden, den der Bogen töten sollte.</td></tr> <tr><td> Fähigkeit</td><td> Bonus auf die Initiative von 1</td></tr> </tbody></table> </p><p>" ) );

       assertEquals(
                    "| Name: [Christian|ChristianS] \\\\ Geschicklichkeit: 2 \\\\ Hang zu perversen Sexorgien. Jongliert mit Worten und Köpfen. \\\\ [Berian Nachtschleicher|Berian] \\\\ [XLerul] \\\\ [Effifot Erif]\n",
                    html2wiki
                            .translate( "<table class=\"wikitable\" border=\"1\"> <tbody><tr><td> Name: <a class=\"wikipage\" href=\"Wiki.jsp?page=ChristianS\">Christian</a> <br> Geschicklichkeit: 2 <br> Hang zu perversen Sexorgien. Jongliert mit Worten und Köpfen. <br> <a class=\"wikipage\" href=\"Wiki.jsp?page=Berian\">Berian Nachtschleicher</a> <br> <a class=\"wikipage\" href=\"Wiki.jsp?page=XLerul\">XLerul</a> <br> <a class=\"wikipage\" href=\"Wiki.jsp?page=EffifotErif\">Effifot Erif</a></td></tr> </tbody></table>" ) );

        
    }

    public void testRulers() throws Exception
    {
        assertEquals( "a\n----\nb", html2wiki.translate( "a<hr/>b" ) );        
        assertEquals( "by\n" + "----\n" + "Dies", html2wiki.translate( "by\n" + "<hr>\n" + "Dies" ) );

    }
    
    public void testParagraphs() throws Exception
    {
        assertEquals( "a\n\nb\n\nc", html2wiki.translate( "a<p>b</p>c" ) );

        assertEquals( "a\n\nb", html2wiki.translate( "a<p></p>b" ) );

        assertEquals( "a\n\nb", html2wiki.translate( "a<p>\n</p>b" ) );        

        assertEquals(
                     "\n\n\\\\__Willkommen__ \\\\ \\\\ Und niemand wird sie sehen \\\\ Eine Page ... \\\\ \\\\\n\nAls Unterthema\n\n",
                     html2wiki
                             .translate( "<p> <br><b>Willkommen</b> <br> <br> Und niemand wird sie sehen <br> Eine Page ... <br> <br> </p><p> Als Unterthema</p><p>" ) );

    }
    
    public void testWhitespace() throws Exception
    {
        assertEquals( "", html2wiki.translate( "" ) );

        assertEquals( "", html2wiki.translate( "   " ) );

        assertEquals( "", html2wiki.translate( "<div>\n\n\n</div>" ) );        

        assertEquals( "a ", html2wiki.translate( "a\n  \n\n" ) );        
    }

    public void testLists() throws Exception
    {
        assertEquals( "\n* Punkt 1\n* Punkt 2\n", html2wiki.translate( "<ul><li>Punkt 1</li><li>Punkt 2</li></ul>" ) );

        assertEquals( "\n# Punkt 1\n# Punkt 2\n", html2wiki.translate( "<ol><li>Punkt 1</li><li>Punkt 2</li></ol>" ) );

        assertEquals( "\n# Punkt 1\n## Punkt 2\n\n", html2wiki.translate( "<ol><li>Punkt 1<ol><li>Punkt 2</li></ol></li></ol>" ) );

        assertEquals( "\n* Punkt 1\n** Punkt 2\n\n", html2wiki.translate( "<ul><li>Punkt 1<ul><li>Punkt 2</li></ul></li></ul>" ) );

        assertEquals( "\n* Punkt 1\n*# Punkt 2\n\n", html2wiki.translate( "<ul><li>Punkt 1<ol><li>Punkt 2</li></ol></li></ul>" ) );

        assertEquals(
                      "\n* eins\n* zwei\n* drei\n\n# eins\n# zwei\n# drei\n\n* ober\n\n** unter\n\n*** unter-unter\n\n**** unter-unter-unter\n\n# ober\n\n## unter\n\n### unter-unter\n\n#### unter-unter-unter\n#### unter-unter-unter\n### unter-unter\n\n* ober\n\n*# unter\n\n*## unter\n\n# ober\n\n#* unter\n\n#** unter\n\n",
                      html2wiki
                              .translate( "<ul> <li> eins </li> <li> zwei </li> <li> drei </li> </ul> <p> </p><ol> <li> eins </li> <li> zwei </li> <li> drei </li> </ol> <p></p><p> </p><ul> <li> ober </li> <ul> <li> unter </li> <ul> <li> unter-unter </li> <ul> <li> unter-unter-unter </li> </ul> </ul> </ul> </ul> <p></p><p> </p><ol> <li> ober </li> <ol> <li> unter </li> <ol> <li> unter-unter </li> <ol> <li> unter-unter-unter </li> <li> unter-unter-unter </li> </ol> <li> unter-unter </li> </ol> </ol> </ol> <p></p><p> </p><p> </p><ul> <li> ober </li> <ol> <li> unter </li> <ol> <li> unter </li> </ol> </ol> </ul> <p></p><p> </p><p> </p><ol> <li> ober </li> <ul> <li> unter </li> <ul> <li> unter </li> </ul> </ul> </ol> <p></p><p> </p>" ) );

        assertEquals(
                     "\n* Diese Karte kann von jedem editiert und um neue Links erweitert werden. \\\\Klickt einfach unten neben der Karte auf {{{[edit]}}}\n",
                     html2wiki
                             .translate( "<ul><li> Diese Karte kann von jedem editiert und um neue Links erweitert werden.<br>Klickt einfach unten neben der Karte auf <span style=\"font-family: monospace;\">[edit]</span></li></ul>" ) );

       assertEquals(
                     "\n* Diese Karte kann von jedem editiert und um neue Links erweitert werden. \\\\Klickt einfach unten neben der Karte auf {{{[edit]}}}\n",
                     html2wiki
                             .translate( "<ul><li> Diese Karte kann von jedem editiert und um neue Links erweitert werden.<br>Klickt einfach unten neben der Karte auf <span style=\"font-family: monospace;\">[edit]</span></li></ul>" ) );


    }
    
    public void testPre() throws Exception
    {
        
        assertEquals( "{{{hallo}}}\n", html2wiki.translate( "<pre>hallo</pre>" ) );

        assertEquals( "{{{Hallo\nWelt!\n\n}}}\n", html2wiki.translate( "<pre>Hallo<br>Welt!<br><br></pre>" ) );

        assertEquals( "{{{\n\n\n\nHallo\n\n\n\nWelt!\n\n\n\n}}}\n", html2wiki
                .translate( "\n\n\n\n<pre>\n\n\n\nHallo\n\n\n\nWelt!\n\n\n\n</pre>\n\n\n\n" ) );

        assertEquals( "{{{\n\n* Baltramon \n  lasdjfh\n\n}}}\n", html2wiki.translate( "<pre>\n\n* Baltramon \n  lasdjfh\n\n</pre>" ) );

        assertEquals( "Diese Karte{{{ kann }}} von", html2wiki
                      .translate( "Diese Karte<span style=\"font-family: courier new,courier,mono;\"> kann </span> von" ) );

        assertEquals(
                     "Fahrt einfac{{{h mit\u00A0\u00A0 \n der \u00A0 Maus}}} drueber",
                     html2wiki
                             .translate( "Fahrt einfac<span style=\"font-family: monospace;\">h mit&nbsp;&nbsp; <br> der &nbsp; Maus</span> drueber" ) );


    }

    public void testTT() throws Exception
    {
        assertEquals( "{{hallo}}\n", html2wiki.translate( "<tt>hallo</tt>" ) );

        assertEquals( "{{hallo}}\n", html2wiki.translate( "<code>hallo</code>" ) );        
    }
    
    public void testCenter() throws Exception
    {
        assertEquals( "%%( text-align: center; display: block; )Hello \\\\World!%%", html2wiki
                      .translate( "<div style=\"text-align: center;\">Hello<br>World!</div>" ) );

              assertEquals( "__%%( text-align: center; display: block; )Hello \\\\World!%%__", html2wiki
                      .translate( "<span style=\"font-weight: bold; text-align: center;\">Hello<br>World!</span>" ) );

        
    }

    public void testImage() throws Exception
    {
        assertEquals(
                     "[{Image src='Homunkulus/homunculus4.jpg' align='left'}]",
                     html2wiki
                             .translate( "<table class=\"imageplugin\" align=\"left\" border=\"0\"> <tbody><tr><td><img src=\"attach?page=Homunkulus%2Fhomunculus4.jpg\"></td></tr> </tbody></table>" ) );

       assertEquals( "[{Image src=\'AbenteuerQuilpins/Quilpins.jpg\' align=\'left\'}]", html2wiki
               .translate( "<table class=\"imageplugin\" align=\"left\" border=\"0\"> \r\n"
                           + "  <tbody><tr><td><img src=\"attach?page=AbenteuerQuilpins%2FQuilpins.jpg\"></td></tr> </tbody>\r\n"
                           + "</table> " ) );

       assertEquals(
                     "[{Image src=\'AbenteuerQuilpins/Quilpins.jpg\' caption=\'Testing Image\' style=\'font-size: 120%; color: green;\'}]",
                     html2wiki
                             .translate( "<table class=\"imageplugin\" style=\"font-size: 120%; color: green;\" border=\"0\"> \r\n"
                                         + "  <caption align=\"bottom\">Testing Image</caption> \r\n"
                                         + "  <tbody><tr><td><img src=\"attach?page=AbenteuerQuilpins%2FQuilpins.jpg\"></td></tr> </tbody>\r\n"
                                         + "</table> " ) );


       assertEquals(
                     "[{Image src=\'http://opi.yahoo.com/online?u=YahooUser1234&m=g&t=2\' link=\'http://edit.yahoo.com/config/send_webmesg?.target=YahooUser1234&.src=pg\'}]",
                     html2wiki
                             .translate( "<table class=\"imageplugin\" border=\"0\"> \r\n"
                                         + "  <tbody><tr><td>\r\n"
                                         + "  <a href=\"http://edit.yahoo.com/config/send_webmesg?.target=YahooUser1234&amp;.src=pg\">\r\n"
                                         + "  <img src=\"http://opi.yahoo.com/online?u=YahooUser1234&amp;m=g&amp;t=2\">\r\n"
                                         + "  </a></td></tr> \r\n" + "  </tbody>\r\n" + "</table> " ) );

       assertEquals(
                     "[{Image src=\'homunculus4.jpg\' align=\'left\' height=\'100px\' width=\'100px\' alt=\'alt text\' caption=\'caption text\' link=\'http://google.de\' border=\'1\'}]",
                     html2wiki
                             .translate( "<table class=\"imageplugin\" align=\"left\" border=\"0\"> \r\n"
                                         + "  <caption align=\"bottom\">caption text</caption> \r\n"
                                         + "  <tbody><tr><td>\r\n"
                                         + "    <a href=\"http://google.de\"><img src=\"homunculus4.jpg\" alt=\"alt text\" border=\"1\" height=\"100px\" width=\"100px\">\r\n"
                                         + "    </a></td></tr> \r\n" + "  </tbody>  \r\n" + "</table>" ) );

       assertEquals(
                     "[{Image src=\'http://opi.yahoo.com/online?u=YahooUser1234&m=g&t=2\' link=\'http://edit.yahoo.com/config/send_webmesg?.target=YahooUser1234&.src=pg\'}]",
                     html2wiki
                             .translate( "  <a href=\"http://edit.yahoo.com/config/send_webmesg?.target=YahooUser1234&amp;.src=pg\">\r\n"
                                         + "  <img src=\"http://opi.yahoo.com/online?u=YahooUser1234&amp;m=g&amp;t=2\">\r\n"
                                         + "  </a" ) );
        
    }
    
    public void testPlugin() throws Exception
    {
        assertEquals( "This is a private homepage done by\n" + "----\n"
                      + "Dies ist eine private, nicht-kommerzielle Homepage von\n" + "\n"
                      + "[{Text2gif width=\'150\' height=\'100\' \n" + " \n" + "Sebastian L. Baltes \n" + "Lange Str. 53 \n"
                      + "44137 Dortmund \n" + " \n" + "email: info@sorokan.de \n" + "}]\n" + "\n", html2wiki
                .translate( "This is a private homepage done by\n" + "<hr>\n"
                            + "Dies ist eine private, nicht-kommerzielle Homepage von\n" + "<p>\n"
                            + "[{Text2gif width=\'150\' height=\'100\'\n" + "<br> <br>Sebastian L. Baltes\n"
                            + "<br>Lange Str. 53\n" + "<br>44137 Dortmund\n" + "<br> <br>email: info@sorokan.de\n" + "<br>}]\n"
                            + "</p><p>" ) );
        
    }

    public void testCSS() throws Exception
    {
        assertEquals( "%%( color: rgb(255, 0, 0); )Und niemand wird sie sehen%%, die", html2wiki
                      .translate( "<span style=\"color: rgb(255, 0, 0);\">Und niemand wird sie sehen</span>, die" ) );

        
    }

    public void testParsing() throws Exception
    {
        assertEquals( "Hello World!", html2wiki.translate( "Hello World!" ) );

        assertEquals( "a", html2wiki.translate( "a" ) );


        assertEquals( "a \\\\b", html2wiki.translate( "a<br/>b" ) );

        assertEquals( "\\\\\\", html2wiki.translate( "\\\\\\" ) );

        assertEquals( "[{Test\nHello World!}]", html2wiki.translate( "[{Test\\\\Hello World!}]" ) );

        assertEquals( "{{{[{Test\\\\Hello World!}]}}}", html2wiki.translate( "{{{[{Test\\\\Hello World!}]}}}" ) );

        assertEquals(
                      "{{{[{Test\\\\Hello World!}]}}}{{{[{Test\\\\\\\\Hello World!}]}}}[{Test\n\nHello World!}][{Test\n\nHello World!}]",
                      html2wiki
                              .translate( "{{{[{Test\\\\Hello World!}]}}}{{{[{Test\\\\\\\\Hello World!}]}}}[{Test\\\\\\\\Hello World!}][{Test\\\\\\\\Hello World!}]" ) );

    }

    public void testBoldAndItalic()
        throws Exception
    {
        assertEquals(
                     "Dies ist __bold__, ''italic'' und __''both''__.",
                     html2wiki.translate( "Dies ist <span style=\"font-weight: bold;\">bold</span>, <span style=\"font-style: italic;\">italic</span> und <span style=\"font-style: italic; font-weight: bold;\">both</span>." ) );

        assertEquals(
                     "Dies ist __bold__, ''italic'' und __''both''__ 2.",
                     html2wiki.translate( "Dies ist <b>bold</b>, <i>italic</i> und <b><i>both</i></b> 2." ) );

        assertEquals(
                     "Dies ist __bold__, ''italic'' und __''both''__ 3.",
                     html2wiki.translate( "Dies ist <strong>bold</strong>, <em>italic</em> und <strong><em>both</em></strong> 3." ) );

        assertEquals( "Wilma: ''Ich möchte hier mal in aller Deutlichkeit sagen! ''", html2wiki
                      .translate( "Wilma: <i>Ich             möchte hier\nmal in aller\nDeutlichkeit sagen! </i>" ) );


    }
    
    public static Test suite()
    {
        return new TestSuite( TestHtmlStringToWikiTranslator.class );
    }

}