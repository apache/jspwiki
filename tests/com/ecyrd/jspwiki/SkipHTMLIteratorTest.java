
package com.ecyrd.jspwiki;

import junit.framework.*;
import java.io.*;
import java.text.*;

public class SkipHTMLIteratorTest extends TestCase
{
    SkipHTMLIterator iter;

    public SkipHTMLIteratorTest( String s )
    {
        super( s );
    }

    public void setUp()
    {
        iter = new SkipHTMLIterator();
    }

    public void tearDown()
    {
    }

    public void testFirst()
    {
        String orig = "one two three";

        iter.setText( orig );

        assertEquals( 0, iter.first() ); 
    }

    public void testFirst2()
    {
        String orig = "  one two three";

        iter.setText( orig );

        assertEquals( 2, iter.first() ); 
    }

    public void testOne()
        throws Exception
    {
        String orig = "one two three";
        String res[]= { "one", "two", "three" };

        iter.setText( orig );

        int start = iter.first();
        int count = 0;

        for (int end = iter.next();
             end != BreakIterator.DONE;
             start = end, end = iter.next()) 
        {
            String part = orig.substring( start, end );

            assertEquals( "Count="+count, res[count], part );
            ++count;
        }


    }

    public void testTwo()
        throws Exception
    {
        String orig = "  one    ..,two  .,.   three   ";
        String res[]= { "one", "two", "three" };

        iter.setText( orig );

        int start = iter.first();
        int count = 0;

        for (int end = iter.next();
             end != BreakIterator.DONE;
             start = end, end = iter.next()) 
        {
            String part = orig.substring( start, end );

            assertEquals( "Count="+count, res[count], part );
            ++count;
        }
    }


    public void testThree()
        throws Exception
    {
        String orig = "  one    ..,two  .\n,.   three   \n";
        String res[]= { "one", "two", "three" };

        iter.setText( orig );

        int start = iter.first();
        int count = 0;

        for (int end = iter.next();
             end != BreakIterator.DONE;
             start = end, end = iter.next()) 
        {
            String part = orig.substring( start, end );

            assertEquals( "Count="+count, res[count], part );
            ++count;
        }

        assertEquals( 3, count );
    }


    public void testHTML()
        throws Exception
    {
        String orig = "\none <A HREF=\"testi.jsp\">This is a link</A> two three";
        String res[]= { "one", "two", "three" };

        iter.setText( orig );

        int start = iter.first();
        int count = 0;

        for (int end = iter.next();
             end != BreakIterator.DONE;
             start = end, end = iter.next()) 
        {
            String part = orig.substring( start, end );

            assertEquals( "Count="+count, res[count], part );
            ++count;
        }
    }


    public void testHTML2()
    {
        String orig = "This is an InterCapping test";
        String res[]= { "This", "is", "an", "InterCapping", "test" };

        iter.setText( orig );

        int start = iter.first();
        int count = 0;

        for (int end = iter.next();
             end != BreakIterator.DONE;
             start = end, end = iter.next()) 
        {
            String part = orig.substring( start, end );

            assertEquals( "Count="+count, res[count], part );
            ++count;
        }        
    }

    public static Test suite()
    {
        return new TestSuite( SkipHTMLIteratorTest.class );
    }
}
