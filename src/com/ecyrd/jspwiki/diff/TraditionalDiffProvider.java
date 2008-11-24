/*
   JSPWiki - a JSP-based WikiWiki clone.

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
import com.ecyrd.jspwiki.action.WikiActionBeanFactory;
import com.ecyrd.jspwiki.i18n.InternationalizationManager;


/**
 * This is the JSPWiki 'traditional' diff.  It uses an internal diff engine.
 * 
 * @author Erik Bunn
 * @author <a href="mailto:hps@intermeta.de">Henning P. Schmiedehausen</a>
 */

public class TraditionalDiffProvider implements DiffProvider
{
    private static final Logger log = Logger.getLogger(TraditionalDiffProvider.class);

    private static final String CSS_DIFF_ADDED = "<tr><td class=\"diffadd\">";
    private static final String CSS_DIFF_REMOVED = "<tr><td class=\"diffrem\">";
    private static final String CSS_DIFF_UNCHANGED = "<tr><td class=\"diff\">";
    private static final String CSS_DIFF_CLOSE = "</td></tr>" + Diff.NL;


    /**
     *  Constructs the provider.
     */
    public TraditionalDiffProvider()
    {
    }


    /**
     * {@inheritDoc}
     * @see com.ecyrd.jspwiki.WikiProvider#getProviderInfo()
     */
    public String getProviderInfo()
    {
        return "TraditionalDiffProvider";
    }

    /**
     * {@inheritDoc}
     * @see com.ecyrd.jspwiki.WikiProvider#initialize(com.ecyrd.jspwiki.WikiEngine, java.util.Properties)
     */
    public void initialize(WikiEngine engine, Properties properties)
        throws NoRequiredPropertyException, IOException
    {
    }

    /**
     * Makes a diff using the BMSI utility package. We use our own diff printer,
     * which makes things easier.
     * 
     * @param ctx The WikiContext in which the diff should be made.
     * @param p1 The first string
     * @param p2 The second string.
     * 
     * @return Full HTML diff.
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


    private static final class RevisionPrint
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
            fmt.setLocale( WikiActionBeanFactory.getLocale(m_context) );
            ChoiceFormat cfmt = new ChoiceFormat( choiceLimits, choiceString );
            fmt.applyPattern( type );
            Format[] formats = { NumberFormat.getInstance(), cfmt, NumberFormat.getInstance() };
            fmt.setFormats( formats );
            
            Object[] params = { changed.first() + 1, 
                                changed.size(),
                                changed.size() };
            m_result.append( fmt.format(params) );
            m_result.append(CSS_DIFF_CLOSE);
        }
    }
}
