/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2002 Janne Jalkanen (Janne.Jalkanen@iki.fi)

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.ecyrd.jspwiki;

import java.util.Properties;
import java.util.Vector;
import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.io.Writer;

import org.apache.log4j.Category;

// import org.suigeneris.diff.*;

/**
 *  Provides access to making a 'diff' between two Strings.
 *  Can be commanded to use a diff program or to use an internal diff.
 *
 *  @author Janne Jalkanen
 *  @author Erik Bunn
 */
public class DifferenceEngine
{
    private static final Category   log = Category.getInstance(DifferenceEngine.class);

    /** Determines the command to be used for 'diff'.  This program must
        be able to output diffs in the unified format. It defaults to
        'diff -u %s1 %s2'.*/
    public  static final String PROP_DIFFCOMMAND     = "jspwiki.diffCommand";

    private static final char   DIFF_ADDED_SYMBOL    = '+';
    private static final char   DIFF_REMOVED_SYMBOL  = '-';
    private static final String CSS_DIFF_ADDED       = "<TR><TD BGCOLOR=#99FF99 class=\"diffadd\">";
    private static final String CSS_DIFF_REMOVED     = "<TR><TD BGCOLOR=#FF9933 class=\"diffrem\">";
    private static final String CSS_DIFF_UNCHANGED   = "<TR><TD class=\"diff\">";
    private static final String CSS_DIFF_CLOSE       = "</TD></TR>";

    /** Default diff command */
    private String         m_diffCommand     = null; 

    private String         m_encoding;

    private boolean        m_useInternalDiff = true;

    /**
     *  Creates a new DifferenceEngine.
     *
     *  @param props The contents of jspwiki.properties
     *  @param encoding The character encoding used for making the diff.
     */
    public DifferenceEngine( Properties props, String encoding )
    {
        m_diffCommand = props.getProperty( PROP_DIFFCOMMAND );
        
        m_useInternalDiff = (m_diffCommand == null);

        m_encoding    = encoding;
    }

    private String getContentEncoding()
    {
        return m_encoding;
    }

    /**
     *  Returns a raw, text format diff of its arguments.  This diff can then
     *  be fed to the <TT>colorizeDiff()</TT>, below.
     *
     *  @see #colorizeDiff
     */
    public String makeDiff( String p1, String p2 )
    {
        if( m_useInternalDiff )
        {
            return makeDiffWithBMSI( p1, p2 );
        }
        else
        {
            return makeDiffWithProgram( p1, p2 );
        }
    }
    /*
     // Makes a diff with JRCS routines, but BMSI is slightly better.
    private String makeDiffWithJRCS( String p1, String p2 )        
    {
        try
        {
            Object[] first  = Diff.stringToArray(p1);
            Object[] second = Diff.stringToArray(p2);

            Revision diff = Diff.diff( first, second );
        
            return diff.toUnifiedString();
        }
        catch( DifferentiationFailedException e )
        {
            log.error("Diff failed", e);
        }

        return null;
    }
    */

    /**
     *  Makes a diff using the BMSI utility package.
     *  We use our own diff printer, which makes things
     *  easier.
     */
    private String makeDiffWithBMSI( String p1, String p2 )        
    {
        try
        {
            String[] first  = stringToArray(p1);
            String[] second = stringToArray(p2);

            bmsi.util.Diff diff = new bmsi.util.Diff( first, second );

            bmsi.util.Diff.change script = diff.diff_2(false);

            if( script == null )
            {
                // No differences.
                return "";
            }

            StringWriter sw = new StringWriter();
            bmsi.util.DiffPrint.Base p = new WriterPrint( first, second, sw );
            p.print_script( script );
            
            return sw.toString();
        }
        catch( IOException e )
        {
            log.error("Diff failed", e);
        }

        return null;
    }

    /**
     *  Writes a diff in a human-readable form, as opposed to your
     *  standard average diff.
     *
     *  Lifted from org.mahlen.hula.utils.VersionUtil.
     *  @author Mahlen Morris
     *  @author Janne Jalkanen
     */
    // FIXME: Must somehow add contextual diffs as well.
    private class WriterPrint extends bmsi.util.DiffPrint.NormalPrint
    {
        public WriterPrint( String[] a, String[] b, Writer w )
        {
            super( a, b );
            outfile = new PrintWriter( w );
        }

        protected void print_range_length( int a, int b )
        {
            outfile.print( b-a+1 );
        }

        /**
         *  This method no longer emulates any known diff format.
         */
        protected void print_hunk(bmsi.util.Diff.change hunk) {

            /* Determine range of line numbers involved in each file.  */
            analyze_hunk(hunk);
            if (deletes == 0 && inserts == 0)
                return;

            /* Print out the line number header for this hunk */

            if( inserts != 0 && deletes == 0 )
            {
                outfile.print("At line ");
                print_number_range('-', first0, last0);
                outfile.print(" added ");
                print_range_length(first1, last1);
                outfile.print(" line" + ((last1-first1 == 0)? "." : "s.") );
            }
            else if( deletes != 0 && inserts == 0 )
            {
                outfile.print("Removed line"+((last0-first0 == 0)? " " : "s "));
                print_number_range('-', first0, last0);
                // outfile.print(" removed ");
                // print_range_length(first1, last1);                
                // outfile.print(" line" + ((last1-first1 == 0)? "." : "s.") );
            }
            else
            {
                if( last0-first0 == 0 )
                {
                    outfile.print("Line ");
                    print_number_range('-', first0, last0);
                    outfile.print(" was replaced by ");
                }
                else
                {
                    outfile.print("Lines ");
                    print_number_range('-', first0, last0);
                    outfile.print(" were replaced by ");
                }

                outfile.print( "line"+((last1-first1 == 0) ? " " : "s "));

                print_number_range('-', first1, last1);                
            }


            outfile.println();

            /* Print the lines that the first file has.  */
            if (deletes != 0)
                for (int i = first0; i <= last0; i++)
                    print_1_line("- ", file0[i]);

            /*
            if (inserts != 0 && deletes != 0)
                outfile.println("===");
            */

            /* Print the lines that the second file has.  */
            if (inserts != 0)
                for (int i = first1; i <= last1; i++)
                    print_1_line("+ ", file1[i]);
        }

    }

    /**
     *  Again, lifted from org.mahlen.hula.utils.VersionUtil.
     */
    private static String[] stringToArray(String str) 
        throws IOException 
    {
        BufferedReader rdr = new BufferedReader(new StringReader(str));
        Vector s = new Vector();
        for(;;) 
        {
            String line = rdr.readLine();
            if (line == null) break;
            s.addElement(line);
        }
        String[] a = new String[s.size()];
        s.copyInto(a);
        return a;
    }
    /**
     *  Makes the diff by calling "diff" program.
     */
    private String makeDiffWithProgram( String p1, String p2 )
    {
        File f1 = null, f2 = null;
        String diff = null;

        try
        {
            f1 = FileUtil.newTmpFile( p1, getContentEncoding() );
            f2 = FileUtil.newTmpFile( p2, getContentEncoding() );

            String cmd = TextUtil.replaceString( m_diffCommand,
                                                 "%s1",
                                                 f1.getPath() );
            cmd = TextUtil.replaceString( cmd,
                                          "%s2",
                                          f2.getPath() );

            String output = FileUtil.runSimpleCommand( cmd, f1.getParent() );

            // FIXME: Should this rely on the system default encoding?
            diff = new String(output.getBytes("ISO-8859-1"),
                              getContentEncoding() );
        }
        catch( IOException e )
        {
            log.error("Failed to do file diff",e);
        }
        catch( InterruptedException e )
        {
            log.error("Interrupted",e);
        }
        finally
        {
            if( f1 != null ) f1.delete();
            if( f2 != null ) f2.delete();
        }

        return diff;
    }

    /**
     * Goes through output provided by a diff command and inserts
     * HTML tags to make the result more legible.
     * Currently colors lines starting with a + green,
     * those starting with - reddish (hm, got to think of
     * color blindness here...).
     */
    public String colorizeDiff( String diffText )
        throws IOException
    {
        String line = null;
        String start = null;
        String stop = null;

        if( diffText == null )
        {
            return "Invalid diff - probably something wrong with server setup.";
        }

        BufferedReader in = new BufferedReader( new StringReader( diffText ) );
        StringBuffer out = new StringBuffer();

        out.append("<TABLE BORDER=0 CELLSPACING=0 CELLPADDING=0>");
        while( ( line = in.readLine() ) != null )
        {
            stop  = CSS_DIFF_CLOSE;

            if( line.length() > 0 )
            {
                switch( line.charAt( 0 ) )
                {
                  case DIFF_ADDED_SYMBOL:
                    start = CSS_DIFF_ADDED;
                    break;
                  case DIFF_REMOVED_SYMBOL:
                    start = CSS_DIFF_REMOVED;
                    break;
                  default:
                    start = CSS_DIFF_UNCHANGED;
                }
            }
            else
            {
                start = CSS_DIFF_UNCHANGED;
            }
            
            out.append( start );
            out.append( line.trim() );
            out.append( stop + "\n" );

        }
        out.append("</TABLE>");
        return( out.toString() );
    }

}
