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

import java.io.IOException;
import java.util.Properties;

import org.apache.commons.jrcs.diff.AddDelta;
import org.apache.commons.jrcs.diff.ChangeDelta;
import org.apache.commons.jrcs.diff.Chunk;
import org.apache.commons.jrcs.diff.DeleteDelta;
import org.apache.commons.jrcs.diff.Diff;
import org.apache.commons.jrcs.diff.DifferentiationFailedException;
import org.apache.commons.jrcs.diff.Revision;
import org.apache.commons.jrcs.diff.RevisionVisitor;
import org.apache.commons.jrcs.diff.myers.MyersDiff;
import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.NoRequiredPropertyException;
import com.ecyrd.jspwiki.TextUtil;
import com.ecyrd.jspwiki.WikiEngine;


/**
 * This is the JSPWiki 'traditional' diff.
 * @author Janne Jalkanen
 * @author Erik Bunn 
 * @author <a href="mailto:hps@intermeta.de">Henning P. Schmiedehausen</a>
 */

public class TraditionalDiffProvider implements DiffProvider
{
    private static final Logger log = Logger.getLogger(TraditionalDiffProvider.class);

    private static final String CSS_DIFF_ADDED = "<tr><td bgcolor=\"#99FF99\" class=\"diffadd\">";
    private static final String CSS_DIFF_REMOVED = "<tr><td bgcolor=\"#FF9933\" class=\"diffrem\">";
    private static final String CSS_DIFF_UNCHANGED = "<tr><td class=\"diff\">";
    private static final String CSS_DIFF_CLOSE = "</td></tr>" + Diff.NL;


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
            String[] first  = Diff.stringToArray(TextUtil.replaceEntities(p1));
            String[] second = Diff.stringToArray(TextUtil.replaceEntities(p2));
            Revision rev = Diff.diff(first, second, new MyersDiff());
            
            if( rev == null || rev.size() == 0 )
            {
                // No difference

                return "";
            }
            
            StringBuffer ret = new StringBuffer(rev.size() * 20); // Guessing how big it will become...

            ret.append("<table class=\"diff\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\">\n");
            rev.accept( new RevisionPrint(ret) );
            ret.append("</table>\n");

            return ret.toString();
        }
        catch( DifferentiationFailedException e )
        {
            diffResult = "makeDiff failed with DifferentiationFailedException";
            log.error(diffResult, e);
        }

        return diffResult;
    }


    public static class RevisionPrint
        implements RevisionVisitor
    {
        private StringBuffer m_result = null;
       
        private RevisionPrint(StringBuffer sb)
        {
            m_result = sb;
        }

        public void visit(Revision rev)
        {
            // GNDN (Goes nowhere, does nothing)
        }

        public void visit(AddDelta delta)
        {
            Chunk changed = delta.getRevised();
            print(changed, " added ");
            changed.toString(m_result, CSS_DIFF_ADDED, CSS_DIFF_CLOSE);
        }

        public void visit(ChangeDelta delta)
        {
            Chunk changed = delta.getOriginal();
            print(changed, " changed ");
            changed.toString(m_result, CSS_DIFF_REMOVED, CSS_DIFF_CLOSE);
            delta.getRevised().toString(m_result, CSS_DIFF_ADDED, CSS_DIFF_CLOSE);
        }
      
        public void visit(DeleteDelta delta)
        {
            Chunk changed = delta.getOriginal();
            print(changed, " removed ");
            changed.toString(m_result, CSS_DIFF_REMOVED, CSS_DIFF_CLOSE);
        }
        
        private void print(Chunk changed, String type)
        {
            m_result.append(CSS_DIFF_UNCHANGED);
            m_result.append("At line ");
            m_result.append(changed.first() + 1);
            m_result.append(type);
            m_result.append(changed.size());
            m_result.append(" line");
            m_result.append((changed.size() == 1) ? "." : "s.");
            m_result.append(CSS_DIFF_CLOSE);
        }
    }
}
