/* 
   JSPWiki - a JSP-based WikiWiki clone.

   Copyright (C) 2001-2002 Janne Jalkanen (Janne.Jalkanen@iki.fi)

   This program is free software; you can redistribute it and/or modify
   it under the terms of the GNU Lesser General Public License as published by
   the Free Software Foundation; either version 2.1 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU Lesser General Public License for more details.

   You should have received a copy of the GNU Lesser General Public License
   along with this program; if not, write to the Free Software
   Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package com.ecyrd.jspwiki.diff;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Properties;
import java.util.Vector;

import org.apache.log4j.Logger;

import bmsi.util.Diff;

import com.ecyrd.jspwiki.NoRequiredPropertyException;
import com.ecyrd.jspwiki.TextUtil;
import com.ecyrd.jspwiki.WikiEngine;


/**
 * This is the JSPWiki 'traditional' diff. 
 */
public class TraditionalDiffProvider implements DiffProvider
{
    private static final Logger log = Logger.getLogger(TraditionalDiffProvider.class);

    
    private static final char DIFF_ADDED_SYMBOL = '+';
    private static final char DIFF_REMOVED_SYMBOL = '-';
    private static final String CSS_DIFF_ADDED = "<tr><td bgcolor=\"#99FF99\" class=\"diffadd\">";
    private static final String CSS_DIFF_REMOVED = "<tr><td bgcolor=\"#FF9933\" class=\"diffrem\">";
    private static final String CSS_DIFF_UNCHANGED = "<tr><td class=\"diff\">";
    private static final String CSS_DIFF_CLOSE = "</td></tr>";


    public TraditionalDiffProvider()
    {
    }


    /**
     * @see com.ecyrd.jspwiki.WikiProvider#getProviderInfo()
     */
    public String getProviderInfo()
    {
        return "TraditionalDiffProvider";
    }

    /**
     * @see com.ecyrd.jspwiki.WikiProvider#initialize(com.ecyrd.jspwiki.WikiEngine, java.util.Properties)
     */
    public void initialize(WikiEngine engine, Properties properties)
        throws NoRequiredPropertyException, IOException
    {
    }
    
    /**
     * Makes a diff using the BMSI utility package. We use our own diff printer,
     * which makes things easier.
     */
    public String makeDiffHtml(String p1, String p2)
    {
        String diffResult = "";
        try
        {
            String[] first = stringToArray(p1);
            String[] second = stringToArray(p2);

            Diff diff = new Diff(first, second);
            Diff.change script = diff.diff_2(false);

            if (script != null)
            {
                StringWriter sw = new StringWriter();
                bmsi.util.DiffPrint.Base p = new WriterPrint(first, second, sw);
                p.print_script(script);

                String rawWikiDiff = sw.toString();
                
                String htmlWikiDiff = TextUtil.replaceEntities( rawWikiDiff );

                diffResult = colorizeDiff(htmlWikiDiff);
            }
        }
        catch (IOException e)
        {
            diffResult = "makeDiff failed with IOException";
            log.error(diffResult, e);
        }

        return diffResult;
    }


    /**
     * Writes a diff in a human-readable form, as opposed to your standard
     * average diff. Lifted from org.mahlen.hula.utils.VersionUtil.
     * @author Mahlen Morris
     * @author Janne Jalkanen
     */
    private class WriterPrint extends bmsi.util.DiffPrint.NormalPrint
    {

        public WriterPrint(String[] a, String[] b, Writer w)
        {
            super(a, b);
            outfile = new PrintWriter(w);
        }

        protected void print_range_length(int a, int b)
        {
            outfile.print(b - a + 1);
        }

        /**
         * This method no longer emulates any known diff format.
         */
        protected void print_hunk(bmsi.util.Diff.change hunk)
        {

            /* Determine range of line numbers involved in each file. */
            analyze_hunk(hunk);
            if (deletes == 0 && inserts == 0)
                return;

            /* Print out the line number header for this hunk */

            if (inserts != 0 && deletes == 0)
            {
                outfile.print("At line ");
                print_number_range('-', first0, last0);
                outfile.print(" added ");
                print_range_length(first1, last1);
                outfile.print(" line" + ( ( last1 - first1 == 0 ) ? "." : "s." ));
            }
            else if (deletes != 0 && inserts == 0)
            {
                outfile.print("Removed line" + ( ( last0 - first0 == 0 ) ? " " : "s " ));
                print_number_range('-', first0, last0);
                // outfile.print(" removed ");
                // print_range_length(first1, last1);
                // outfile.print(" line" + ((last1-first1 == 0)? "." : "s.") );
            }
            else
            {
                if (last0 - first0 == 0)
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

                outfile.print("line" + ( ( last1 - first1 == 0 ) ? " " : "s " ));

                print_number_range('-', first1, last1);
            }


            outfile.println();

            /* Print the lines that the first file has. */
            if (deletes != 0)
                for (int i = first0; i <= last0; i++)
                    print_1_line("- ", file0[i]);

            /*
             * if (inserts != 0 && deletes != 0) outfile.println("===");
             */

            /* Print the lines that the second file has. */
            if (inserts != 0)
                for (int i = first1; i <= last1; i++)
                    print_1_line("+ ", file1[i]);
        }

    }

    /**
     * Again, lifted from org.mahlen.hula.utils.VersionUtil.
     */
    private static String[] stringToArray(String str) throws IOException
    {
        BufferedReader rdr = new BufferedReader(new StringReader(str));
        Vector s = new Vector();
        for (;;)
        {
            String line = rdr.readLine();
            if (line == null)
                break;
            s.addElement(line);
        }
        String[] a = new String[s.size()];
        s.copyInto(a);
        return a;
    }


    /**
     * Goes through output provided by a diff command and inserts HTML tags to
     * make the result more legible. Currently colors lines starting with a +
     * green, those starting with - reddish (hm, got to think of color blindness
     * here...).
     */
    static String colorizeDiff(String diffText) throws IOException
    {
        if (diffText == null)
            return "Invalid diff - probably something wrong with server setup.";

        String line = null;
        String start = null;
        String stop = null;

        BufferedReader in = new BufferedReader(new StringReader(diffText));
        StringBuffer out = new StringBuffer();

        out.append("<table class=\"diff\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\">\n");
        while (( line = in.readLine() ) != null)
        {
            stop = CSS_DIFF_CLOSE;

            if (line.length() > 0)
            {
                switch (line.charAt(0))
                {
                  case DIFF_ADDED_SYMBOL :
                    start = CSS_DIFF_ADDED;
                    break;
                  case DIFF_REMOVED_SYMBOL :
                    start = CSS_DIFF_REMOVED;
                    break;
                  default :
                    start = CSS_DIFF_UNCHANGED;
                }
            }
            else
            {
                start = CSS_DIFF_UNCHANGED;
            }

            out.append(start);
            out.append(line.trim());
            out.append(stop + "\n");
        }

        out.append("</table>\n");
        return ( out.toString() );
    }

}
