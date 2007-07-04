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
import java.text.ChoiceFormat;
import java.text.Format;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.Properties;
import java.util.ResourceBundle;

import org.apache.commons.jrcs.diff.*;
import org.apache.commons.jrcs.diff.myers.MyersDiff;
import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.NoRequiredPropertyException;
import com.ecyrd.jspwiki.TextUtil;
import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.i18n.InternationalizationManager;


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
    public String makeDiffHtml( WikiContext ctx, String p1, String p2 )
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
            rev.accept( new RevisionPrint(ctx,ret) );
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


    public static final class RevisionPrint
        implements RevisionVisitor
    {
        private StringBuffer m_result = null;
        private WikiContext  m_context;
        private ResourceBundle m_rb;
        
        private RevisionPrint(WikiContext ctx,StringBuffer sb)
        {
            m_result = sb;
            m_context = ctx;
            m_rb = ctx.getBundle( InternationalizationManager.CORE_BUNDLE );
        }

        public void visit(Revision rev)
        {
            // GNDN (Goes nowhere, does nothing)
        }

        public void visit(AddDelta delta)
        {
            Chunk changed = delta.getRevised();
            print(changed, m_rb.getString( "diff.traditional.added" ) );
            changed.toString(m_result, CSS_DIFF_ADDED, CSS_DIFF_CLOSE);
        }

        public void visit(ChangeDelta delta)
        {
            Chunk changed = delta.getOriginal();
            print(changed, m_rb.getString( "diff.traditional.changed") );
            changed.toString(m_result, CSS_DIFF_REMOVED, CSS_DIFF_CLOSE);
            delta.getRevised().toString(m_result, CSS_DIFF_ADDED, CSS_DIFF_CLOSE);
        }

        public void visit(DeleteDelta delta)
        {
            Chunk changed = delta.getOriginal();
            print(changed, m_rb.getString( "diff.traditional.removed") );
            changed.toString(m_result, CSS_DIFF_REMOVED, CSS_DIFF_CLOSE);
        }

        private void print(Chunk changed, String type)
        {
            m_result.append(CSS_DIFF_UNCHANGED);
            
            String[] choiceString = 
            {
               m_rb.getString("diff.traditional.oneline"),
               m_rb.getString("diff.traditional.lines")
            };
            double[] choiceLimits = { 1, 2 };
            
            MessageFormat fmt = new MessageFormat("");
            fmt.setLocale( WikiContext.getLocale(m_context) );
            ChoiceFormat cfmt = new ChoiceFormat( choiceLimits, choiceString );
            fmt.applyPattern( type );
            Format[] formats = { NumberFormat.getInstance(), cfmt, NumberFormat.getInstance() };
            fmt.setFormats( formats );
            
            Object[] params = { new Integer(changed.first() + 1), 
                                new Integer(changed.size()),
                                new Integer(changed.size()) };
            m_result.append( fmt.format(params) );
            m_result.append(CSS_DIFF_CLOSE);
        }
    }
}
