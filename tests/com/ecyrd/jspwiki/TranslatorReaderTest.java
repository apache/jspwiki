
package com.ecyrd.jspwiki;

import junit.framework.*;
import java.io.*;
import java.util.*;

public class TranslatorReaderTest extends TestCase
{
    Properties props = new Properties();

    public TranslatorReaderTest( String s )
    {
        super( s );
    }

    public void setUp()
        throws Exception
    {
        props.load( getClass().getClassLoader().getResourceAsStream("/jspwiki.properties") );
    }

    public void tearDown()
    {
    }

    private String translate( String src )
        throws IOException
    {
        Reader r = new TranslatorReader( new TestEngine( props ), 
                                         new BufferedReader( new StringReader(src)) );
        StringWriter out = new StringWriter();
        int c;

        while( ( c=r.read()) != -1 )
        {
            out.write( c );
        }

        return out.toString();
    }

    public void testHyperlinks2()
        throws Exception
    {
        String src = "This should be a [hyperlink]";

        assertEquals( "This should be a <A HREF=\"Wiki.jsp?page=Hyperlink\">hyperlink</A>\n",
                      translate(src) );
    }

    public void testHyperlinks3()
        throws Exception
    {
        String src = "This should be a [hyperlink too]";

        assertEquals( "This should be a <A HREF=\"Wiki.jsp?page=HyperlinkToo\">hyperlink too</A>\n",
                      translate(src) );
    }

    public void testHyperlinks4()
        throws Exception
    {
        String src = "This should be a [HyperLink]";

        assertEquals( "This should be a <A HREF=\"Wiki.jsp?page=HyperLink\">HyperLink</A>\n",
                      translate(src) );
    }

    public void testHyperlinks5()
        throws Exception
    {
        String src = "This should be a [here|HyperLink]";

        assertEquals( "This should be a <A HREF=\"Wiki.jsp?page=HyperLink\">here</A>\n",
                      translate(src) );
    }

    public void testHyperlinksExt()
        throws Exception
    {
        String src = "This should be a [http://www.regex.fi/]";

        assertEquals( "This should be a <A HREF=\"http://www.regex.fi/\">http://www.regex.fi/</A>\n",
                      translate(src) );
    }

    public void testHyperlinksExt2()
        throws Exception
    {
        String src = "This should be a [link|http://www.regex.fi/]";

        assertEquals( "This should be a <A HREF=\"http://www.regex.fi/\">link</A>\n",
                      translate(src) );
    }

    public void testParagraph()
        throws Exception
    {
        String src = "1\n\n2\n\n3";

        assertEquals( "1\n<P>\n2\n<P>\n3\n", translate(src) );
    }



    public void testLinebreak()
        throws Exception
    {
        String src = "1\\\\2";

        assertEquals( "1<BR>2\n", translate(src) );
    }

    public void testList1()
        throws Exception
    {
        String src = "A list:\n* One\n* Two\n* Three\n";

        assertEquals( "A list:\n<UL>\n<LI> One\n<LI> Two\n<LI> Three\n</UL>\n", 
                      translate(src) );
    }

    public void testHTML()
        throws Exception
    {
        String src = "<B>Test</B>";

        assertEquals( "&lt;B&gt;Test&lt;/B&gt;\n", translate(src) );
    }

    public void testHTML2()
        throws Exception
    {
        String src = "<P>";

        assertEquals( "&lt;P&gt;\n", translate(src) );
    }

    public static Test suite()
    {
        return new TestSuite( TranslatorReaderTest.class );
    }
}
