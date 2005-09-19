/* 
   JSPWiki - a JSP-based WikiWiki clone.

   Copyright (C) 2001-2005 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.commons.jrcs.diff.*;
import org.apache.commons.jrcs.diff.myers.MyersDiff;
import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.NoRequiredPropertyException;
import com.ecyrd.jspwiki.TextUtil;
import com.ecyrd.jspwiki.WikiEngine;

/**
 * A seriously better diff provider, which highlights changes word-by-word using
 * CSS.
 *
 * Suggested by John Volkar.
 * 
 * @author John Volkar
 * @author Janne Jalkanen
 * @author <a href="mailto:hps@intermeta.de">Henning P. Schmiedehausen</a>
 */

public class ContextualDiffProvider implements DiffProvider
{

    private static final Logger log = Logger.getLogger( ContextualDiffProvider.class );

    public static final String PROP_UNCHANGED_CONTEXT_LIMIT = "jspwiki.contextualDiffProvider.unchangedContextLimit";

    //TODO all of these publics can become jspwiki.properties entries...
    //TODO span title= can be used to get hover info...

    public boolean m_emitChangeNextPreviousHyperlinks = true;

    //Don't use spans here the deletion and insertions are nested in this...
    public String m_changeStartHtml    = ""; //This could be a image '>' for a start marker
    public String m_changeEndHtml      = ""; //and an image for an end '<' marker
    public String m_diffStart          = "<div class=\"diff-wikitext\">";
    public String m_diffEnd            = "</div>";
    
    // Unfortunately we need to do dumb HTML here for RSS feeds.
    
    public String m_insertionStartHtml = "<font color=\"#8000FF\"><span class=\"diff-insertion\">";
    public String m_insertionEndHtml   = "</span></font>";
    public String m_deletionStartHtml  = "<strike><font color=\"red\"><span class=\"diff-deletion\">";
    public String m_deletionEndHtml    = "</span></font></strike>";
    private String m_anchorPreIndex    = "<a name=\"change-";
    private String m_anchorPostIndex   = "\" />";
    private String m_backPreIndex      = "<a class=\"diff-nextprev\" title=\"Go to previous change\" href=\"#change-";
    private String m_backPostIndex     = "\">&lt;&lt;</a>";
    private String m_forwardPreIndex   = "<a class=\"diff-nextprev\" title=\"Go to next change\" href=\"#change-";
    private String m_forwardPostIndex  = "\">&gt;&gt;</a>";
    public String m_elidedHeadIndicatorHtml = "<br/><br/><b>...</b>";
    public String m_elidedTailIndicatorHtml = "<b>...</b><br/><br/>";
    public String m_lineBreakHtml = "<br />";
    public String m_alternatingSpaceHtml = "&nbsp;";
    
    // This one, I will make property file based...
    private static final int LIMIT_MAX_VALUE = (Integer.MAX_VALUE /2) - 1;
    private int m_unchangedContextLimit = LIMIT_MAX_VALUE;



    public ContextualDiffProvider()
    {}

    /**
     * @see com.ecyrd.jspwiki.WikiProvider#getProviderInfo()
     */
    public String getProviderInfo()
    {
        return "ContextualDiffProvider";
    }

    /**
     * @see com.ecyrd.jspwiki.WikiProvider#initialize(com.ecyrd.jspwiki.WikiEngine,
     *      java.util.Properties)
     */
    public void initialize(WikiEngine engine, Properties properties) throws NoRequiredPropertyException, IOException
    {
        String configuredLimit = properties.getProperty(PROP_UNCHANGED_CONTEXT_LIMIT, Integer
            .toString(LIMIT_MAX_VALUE));
        int limit = LIMIT_MAX_VALUE;
        try
        {
            limit = Integer.parseInt(configuredLimit);
        }
        catch (NumberFormatException e)
        {
            log.warn("Failed to parseInt " + PROP_UNCHANGED_CONTEXT_LIMIT + "=" + configuredLimit
                + "   Will use a huge number as limit.", e);
        }
        m_unchangedContextLimit = limit;
    }



    /**
     * Do a colored diff of the two regions. This. is. serious. fun. ;-)
     * 
     * @see com.ecyrd.jspwiki.diff.DiffProvider#makeDiffHtml(java.lang.String,
     *      java.lang.String)
     */
    public synchronized String makeDiffHtml( String wikiOld, String wikiNew )
    {
        //
        // Sequencing handles lineterminator to <br /> and every-other consequtive space to a &nbsp;
        //
        String[] alpha = sequence( TextUtil.replaceEntities( wikiOld ) );
        String[] beta  = sequence( TextUtil.replaceEntities( wikiNew ) );

        Revision rev = null;
        try
        {
            rev = Diff.diff( alpha, beta, new MyersDiff() );
        }
        catch( DifferentiationFailedException dfe )
        {
            log.error( "Diff generation failed", dfe );
            return "Error while creating version diff.";
        }

        int revSize = rev.size();

        StringBuffer sb = new StringBuffer();

        sb.append( m_diffStart );

        //
        // The MyersDiff is a bit dumb by converting a single line multi-word diff into a series
        // of Changes. The ChangeMerger pulls them together again...
        //
        ChangeMerger cm = new ChangeMerger( sb, alpha, revSize );

        rev.accept( cm );

        cm.shutdown();

        sb.append( m_diffEnd );

        return sb.toString();
    }

    /**
     * Take the string and create an array from it, split it first on newlines, making 
     * sure to preserve the newlines in the elements, split each resulting element on 
     * spaces, preserving the spaces.
     * 
     * All this preseving of newlines and spaces is so the wikitext when diffed will have fidelity 
     * to it's original form.  As a side affect we see edits of purely whilespace.
     */
    private String[] sequence( String wikiText )
    {
        String[] linesArray = Diff.stringToArray( wikiText );

        List list = new ArrayList();

        for( int i = 0; i < linesArray.length; i++ )
        {
            String line = linesArray[i];

            String lastToken = null; String token=null;
            // StringTokenizer might be discouraged but it still is perfect here...
            for (StringTokenizer st = new StringTokenizer( line, " ", true ); st.hasMoreTokens();)
            {
                token = st.nextToken();

                if(" ".equals( lastToken) && " ".equals( token ))
                    token = m_alternatingSpaceHtml;
                
                list.add(token);
                lastToken = token;
            }
            
            list.add(m_lineBreakHtml); // Line Break
        }

        return (String[])list.toArray( new String[0] );
    }

    /**
     * This helper class does the housekeeping for merging
     * our various changes down and also makes sure that the
     * whole change process is threadsafe by encapsulating
     * all necessary variables.
     */
    private class ChangeMerger implements RevisionVisitor
    {
        private StringBuffer m_sb = null;

        /** Keeping score of the original lines to process */
        private int m_max = -1;

        private int m_index = 0;

        /** Index of the next element to be copied into the output. */
        private int m_firstElem = 0;

        /** Link Anchor counter */
        private int m_count = 1;

        /** State Machine Mode */
        private int m_mode = -1; /* -1: Unset, 0: Add, 1: Del, 2: Change mode */

        /** Buffer to coalesce the changes together */
        private StringBuffer m_origBuf = null;

        private StringBuffer m_newBuf = null;

        /** Reference to the source string array */
        private String[] m_origStrings = null;

        private ChangeMerger( final StringBuffer sb, final String[] origStrings, final int max )
        {
            m_sb = sb;
            m_origStrings = origStrings;
            m_max = max;

            m_origBuf = new StringBuffer();
            m_newBuf = new StringBuffer();
        }

        private void updateState( Delta delta )
        {
            m_index++;

            Chunk orig = delta.getOriginal();

            if (orig.first() > m_firstElem)
            {
                // We "skip" some lines in the output.
                // So flush out the last Change, if one exists.
                flushChanges();

                // Allow us to "skip" large swaths of unchanged text, show a "limited" amound of
                // unchanged context so the changes are shown in
                if ((orig.first() - m_firstElem) > 2 * m_unchangedContextLimit)
                {
                    if (m_firstElem > 0)
                    {
                        int endIndex = Math.min( m_firstElem + m_unchangedContextLimit, m_origStrings.length -1 );
                        
                        for (int j = m_firstElem; j < endIndex; j++)
                            m_sb.append(m_origStrings[j]);

                        m_sb.append(m_elidedTailIndicatorHtml);
                    }

                    m_sb.append(m_elidedHeadIndicatorHtml);

                    int startIndex = Math.max(orig.first() - m_unchangedContextLimit, 0);
                    for (int j = startIndex; j < orig.first(); j++)
                        m_sb.append(m_origStrings[j]);

                }
                else
                {
                    // No need to skip anything, just output the whole range...
                    for (int j = m_firstElem; j < orig.first(); j++)
                    m_sb.append( m_origStrings[j] );
                }
            }
            m_firstElem = orig.last() + 1;
        }

        public void visit( Revision rev )
        {
            // GNDN (Goes nowhere, does nothing)
        }

        public void visit( AddDelta delta )
        {
            updateState( delta );

            // We have run Deletes up to now. Flush them out.
            if( m_mode == 1 )
            {
                flushChanges();
                m_mode = -1;
            }
            // We are in "neutral mode". Start a new Change
            if( m_mode == -1 )
            {
                m_mode = 0;
            }

            // We are in "add mode".
            if( m_mode == 0 || m_mode == 2 )
            {
                addNew( delta.getRevised() );
                m_mode = 1;
            }
        }

        public void visit( ChangeDelta delta )
        {
            updateState( delta );

            // We are in "neutral mode". A Change might be merged with an add or delete.
            if( m_mode == -1 )
            {
                m_mode = 2;
            }

            // Add the Changes to the buffers. 
            addOrig( delta.getOriginal() );
            addNew( delta.getRevised() );
        }

        public void visit( DeleteDelta delta )
        {
            updateState( delta );

            // We have run Adds up to now. Flush them out.
            if( m_mode == 0 )
            {
                flushChanges();
                m_mode = -1;
            }
            // We are in "neutral mode". Start a new Change
            if( m_mode == -1 )
            {
                m_mode = 1;
            }

            // We are in "delete mode".
            if( m_mode == 1 || m_mode == 2 )
            {
                addOrig( delta.getOriginal() );
                m_mode = 1;
            }
        }

        public void shutdown()
        {
            m_index = m_max + 1; // Make sure that no hyperlink gets created
            flushChanges();

            if (m_firstElem < m_origStrings.length)
            {
                // If there's more than the limit of the orginal left just emit limit and elided...
                if ((m_origStrings.length - m_firstElem) > m_unchangedContextLimit)
                {
                    int endIndex = Math.min( m_firstElem + m_unchangedContextLimit, m_origStrings.length -1 );
                    
                    for (int j = m_firstElem; j < endIndex; j++)
                    m_sb.append( m_origStrings[j] );

                    m_sb.append(m_elidedTailIndicatorHtml);
                }
                else
                // emit entire tail of original...
                {
                    for (int j = m_firstElem; j < m_origStrings.length; j++)
                        m_sb.append(m_origStrings[j]);
                }
            }
        }

        private void addOrig( Chunk chunk )
        {
            if( chunk != null )
            {
                chunk.toString( m_origBuf );
            }
        }

        private void addNew( Chunk chunk )
        {
            if( chunk != null )
            {
                chunk.toString( m_newBuf );
            }
        }



        private void flushChanges()
        {

            if( m_newBuf.length() + m_origBuf.length() > 0 )
            {
                // This is the span element which encapsulates anchor and the change itself
                m_sb.append( m_changeStartHtml );

                // Do we want to have a "back link"?
                if( m_emitChangeNextPreviousHyperlinks && m_count > 1 )
                {
                    m_sb.append( m_backPreIndex );
                    m_sb.append( m_count - 1 );
                    m_sb.append( m_backPostIndex );
                }

                // An anchor for the change.
                if (m_emitChangeNextPreviousHyperlinks)
                {
                    m_sb.append( m_anchorPreIndex );
                    m_sb.append( m_count++ );
                    m_sb.append( m_anchorPostIndex );
                }

                // ... has been added
                if( m_newBuf.length() > 0 )
                {
                    m_sb.append( m_insertionStartHtml );
                    m_sb.append( m_newBuf );
                    m_sb.append( m_insertionEndHtml );
                }

                // .. has been removed
                if( m_origBuf.length() > 0 )
                {
                    m_sb.append( m_deletionStartHtml );
                    m_sb.append( m_origBuf );
                    m_sb.append( m_deletionEndHtml );
                }

                // Do we want a "forward" link?
                if( m_emitChangeNextPreviousHyperlinks && (m_index < m_max) )
                {
                    m_sb.append( m_forwardPreIndex );
                    m_sb.append( m_count ); // Has already been incremented.
                    m_sb.append( m_forwardPostIndex );
                }

                m_sb.append( m_changeEndHtml );

                // Nuke the buffers.
                m_origBuf = new StringBuffer();
                m_newBuf = new StringBuffer();
            }

            // After a flush, everything is reset.
            m_mode = -1;
        }
    }
}
