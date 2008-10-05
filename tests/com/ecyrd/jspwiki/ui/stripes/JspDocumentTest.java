package com.ecyrd.jspwiki.ui.stripes;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class JspDocumentTest extends TestCase
{
    public JspDocumentTest( String s )
    {
        super( s );
    }

    public void testSetName() throws Exception
    {
        // Parse <foo></foo>
        JspParser parser = new JspParser();
        String s = "<foo></foo>";
        JspDocument doc = parser.parse( s );

        assertEquals( 2, doc.getNodes().size() );
        assertEquals( s, doc.toString() );

        // Make sure 2 tags were parsed correctly.
        Node startTag = doc.getNodes().get( 0 );
        Node endTag = doc.getNodes().get( 1 );
        assertEquals( 0, startTag.getStart() );
        assertEquals( 5, startTag.getEnd() );
        assertEquals( "foo", startTag.getName() );
        assertEquals( 5, endTag.getStart() );
        assertEquals( 11, endTag.getEnd() );
        assertEquals( "foo", endTag.getName() );
    }

    public static Test suite()
    {
        return new TestSuite( JspDocumentTest.class );
    }
}
