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
import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.StringReader;
import org.apache.log4j.Category;

/**
 *  Provides access to making a 'diff' between two Strings.
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
    private String         m_diffCommand = "diff -u %s1 %s2"; 

    private String         m_encoding;

    /**
     *  Creates a new DifferenceEngine.
     *
     *  @param props The contents of jspwiki.properties
     *  @param encoding The character encoding used for making the diff.
     */
    public DifferenceEngine( Properties props, String encoding )
    {
        m_diffCommand = props.getProperty( PROP_DIFFCOMMAND, m_diffCommand );

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
        return makeDiffWithProgram( p1, p2 );
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
            
            out.append( start );
            out.append( line.trim() );
            out.append( stop + "\n" );

        }
        out.append("</TABLE>");
        return( out.toString() );
    }

}
