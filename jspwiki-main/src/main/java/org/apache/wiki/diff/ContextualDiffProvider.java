/*
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

package org.apache.wiki.diff;

import org.apache.log4j.Logger;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.api.exceptions.NoRequiredPropertyException;
import org.apache.wiki.util.TextUtil;
import org.suigeneris.jrcs.diff.Diff;
import org.suigeneris.jrcs.diff.DifferentiationFailedException;
import org.suigeneris.jrcs.diff.Revision;
import org.suigeneris.jrcs.diff.RevisionVisitor;
import org.suigeneris.jrcs.diff.delta.AddDelta;
import org.suigeneris.jrcs.diff.delta.ChangeDelta;
import org.suigeneris.jrcs.diff.delta.Chunk;
import org.suigeneris.jrcs.diff.delta.DeleteDelta;
import org.suigeneris.jrcs.diff.delta.Delta;
import org.suigeneris.jrcs.diff.myers.MyersDiff;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;


/**
 * A seriously better diff provider, which highlights changes word-by-word using CSS.
 *
 * Suggested by John Volkar.
 */
public class ContextualDiffProvider implements DiffProvider {

    private static final Logger log = Logger.getLogger( ContextualDiffProvider.class );

    /**
     *  A jspwiki.properties value to define how many characters are shown around the change context.
     *  The current value is <tt>{@value}</tt>.
     */
    public static final String PROP_UNCHANGED_CONTEXT_LIMIT = "jspwiki.contextualDiffProvider.unchangedContextLimit";

    //TODO all of these publics can become jspwiki.properties entries...
    //TODO span title= can be used to get hover info...

    public boolean m_emitChangeNextPreviousHyperlinks = true;

    //Don't use spans here the deletion and insertions are nested in this...
    public static String CHANGE_START_HTML = ""; //This could be a image '>' for a start marker
    public static String CHANGE_END_HTML = ""; //and an image for an end '<' marker
    public static String DIFF_START = "<div class=\"diff-wikitext\">";
    public static String DIFF_END = "</div>";

    // Unfortunately we need to do dumb HTML here for RSS feeds.

    public static String INSERTION_START_HTML = "<font color=\"#8000FF\"><span class=\"diff-insertion\">";
    public static String INSERTION_END_HTML = "</span></font>";
    public static String DELETION_START_HTML = "<strike><font color=\"red\"><span class=\"diff-deletion\">";
    public static String DELETION_END_HTML = "</span></font></strike>";
    private static final String ANCHOR_PRE_INDEX = "<a name=\"change-";
    private static final String ANCHOR_POST_INDEX = "\" />";
    private static final String BACK_PRE_INDEX = "<a class=\"diff-nextprev\" title=\"Go to previous change\" href=\"#change-";
    private static final String BACK_POST_INDEX = "\">&lt;&lt;</a>";
    private static final String FORWARD_PRE_INDEX = "<a class=\"diff-nextprev\" title=\"Go to next change\" href=\"#change-";
    private static final String FORWARD_POST_INDEX = "\">&gt;&gt;</a>";
    public static String ELIDED_HEAD_INDICATOR_HTML = "<br/><br/><b>...</b>";
    public static String ELIDED_TAIL_INDICATOR_HTML = "<b>...</b><br/><br/>";
    public static String LINE_BREAK_HTML = "<br />";
    public static String ALTERNATING_SPACE_HTML = "&nbsp;";

    // This one, I will make property file based...
    private static final int LIMIT_MAX_VALUE = (Integer.MAX_VALUE /2) - 1;
    private int m_unchangedContextLimit = LIMIT_MAX_VALUE;


    /**
     *  Constructs this provider.
     */
    public ContextualDiffProvider()
    {}

    /**
     * @see org.apache.wiki.WikiProvider#getProviderInfo()
     * 
     * {@inheritDoc}
     */
    public String getProviderInfo()
    {
        return "ContextualDiffProvider";
    }

    /**
     * @see org.apache.wiki.WikiProvider#initialize(org.apache.wiki.WikiEngine,
     *      java.util.Properties)
     *      
     * {@inheritDoc}
     */
    public void initialize( final WikiEngine engine, final Properties properties) throws NoRequiredPropertyException, IOException {
        final String configuredLimit = properties.getProperty( PROP_UNCHANGED_CONTEXT_LIMIT, Integer.toString( LIMIT_MAX_VALUE ) );
        int limit = LIMIT_MAX_VALUE;
        try {
            limit = Integer.parseInt( configuredLimit );
        } catch( final NumberFormatException e ) {
            log.warn("Failed to parseInt " + PROP_UNCHANGED_CONTEXT_LIMIT + "=" + configuredLimit + " Will use a huge number as limit.", e );
        }
        m_unchangedContextLimit = limit;
    }



    /**
     * Do a colored diff of the two regions. This. is. serious. fun. ;-)
     *
     * @see org.apache.wiki.diff.DiffProvider#makeDiffHtml(WikiContext, String, String)
     * 
     * {@inheritDoc}
     */
    public synchronized String makeDiffHtml( final WikiContext ctx, final String wikiOld, final String wikiNew ) {
        //
        // Sequencing handles lineterminator to <br /> and every-other consequtive space to a &nbsp;
        //
        final String[] alpha = sequence( TextUtil.replaceEntities( wikiOld ) );
        final String[] beta  = sequence( TextUtil.replaceEntities( wikiNew ) );

        final Revision rev;
        try {
            rev = Diff.diff( alpha, beta, new MyersDiff() );
        } catch( final DifferentiationFailedException dfe ) {
            log.error( "Diff generation failed", dfe );
            return "Error while creating version diff.";
        }

        final int revSize = rev.size();
        final StringBuffer sb = new StringBuffer();

        sb.append( DIFF_START );

        //
        // The MyersDiff is a bit dumb by converting a single line multi-word diff into a series
        // of Changes. The ChangeMerger pulls them together again...
        //
        final ChangeMerger cm = new ChangeMerger( sb, alpha, revSize );
        rev.accept( cm );
        cm.shutdown();
        sb.append( DIFF_END );
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
    private String[] sequence( final String wikiText ) {
        final String[] linesArray = Diff.stringToArray( wikiText );
        final List< String > list = new ArrayList<>();
        for( final String line : linesArray ) {

            String lastToken = null;
            String token;
            // StringTokenizer might be discouraged but it still is perfect here...
            for( final StringTokenizer st = new StringTokenizer( line, " ", true ); st.hasMoreTokens(); ) {
                token = st.nextToken();

                if( " ".equals( lastToken ) && " ".equals( token ) ) {
                    token = ALTERNATING_SPACE_HTML;
                }

                list.add( token );
                lastToken = token;
            }

            list.add( LINE_BREAK_HTML ); // Line Break
        }

        return list.toArray( new String[ 0 ] );
    }

    /**
     * This helper class does the housekeeping for merging
     * our various changes down and also makes sure that the
     * whole change process is threadsafe by encapsulating
     * all necessary variables.
     */
    private final class ChangeMerger implements RevisionVisitor {
        private StringBuffer m_sb;

        /** Keeping score of the original lines to process */
        private int m_max;

        private int m_index = 0;

        /** Index of the next element to be copied into the output. */
        private int m_firstElem = 0;

        /** Link Anchor counter */
        private int m_count = 1;

        /** State Machine Mode */
        private int m_mode = -1; /* -1: Unset, 0: Add, 1: Del, 2: Change mode */

        /** Buffer to coalesce the changes together */
        private StringBuffer m_origBuf;

        private StringBuffer m_newBuf;

        /** Reference to the source string array */
        private String[] m_origStrings;

        private ChangeMerger( final StringBuffer sb, final String[] origStrings, final int max ) {
            m_sb = sb;
            m_origStrings = origStrings != null ? origStrings.clone() : null;
            m_max = max;

            m_origBuf = new StringBuffer();
            m_newBuf = new StringBuffer();
        }

        private void updateState( final Delta delta ) {
            m_index++;
            final Chunk orig = delta.getOriginal();
            if( orig.first() > m_firstElem ) {
                // We "skip" some lines in the output.
                // So flush out the last Change, if one exists.
                flushChanges();

                // Allow us to "skip" large swaths of unchanged text, show a "limited" amound of
                // unchanged context so the changes are shown in
                if( ( orig.first() - m_firstElem ) > 2 * m_unchangedContextLimit ) {
                    if (m_firstElem > 0) {
                        final int endIndex = Math.min( m_firstElem + m_unchangedContextLimit, m_origStrings.length -1 );

                        for( int j = m_firstElem; j < endIndex; j++ ) {
                            m_sb.append( m_origStrings[ j ] );
                        }

                        m_sb.append( ELIDED_TAIL_INDICATOR_HTML );
                    }

                    m_sb.append( ELIDED_HEAD_INDICATOR_HTML );

                    final int startIndex = Math.max(orig.first() - m_unchangedContextLimit, 0);
                    for (int j = startIndex; j < orig.first(); j++) {
                        m_sb.append( m_origStrings[ j ] );
                    }

                } else {
                    // No need to skip anything, just output the whole range...
                    for( int j = m_firstElem; j < orig.first(); j++ ) {
                        m_sb.append( m_origStrings[ j ] );
                    }
                }
            }
            m_firstElem = orig.last() + 1;
        }

        public void visit( final Revision rev ) {
            // GNDN (Goes nowhere, does nothing)
        }

        public void visit( final AddDelta delta ) {
            updateState( delta );

            // We have run Deletes up to now. Flush them out.
            if( m_mode == 1 ) {
                flushChanges();
                m_mode = -1;
            }
            // We are in "neutral mode". Start a new Change
            if( m_mode == -1 ) {
                m_mode = 0;
            }

            // We are in "add mode".
            if( m_mode == 0 || m_mode == 2 ) {
                addNew( delta.getRevised() );
                m_mode = 1;
            }
        }

        public void visit( final ChangeDelta delta ) {
            updateState( delta );

            // We are in "neutral mode". A Change might be merged with an add or delete.
            if( m_mode == -1 ) {
                m_mode = 2;
            }

            // Add the Changes to the buffers.
            addOrig( delta.getOriginal() );
            addNew( delta.getRevised() );
        }

        public void visit( final DeleteDelta delta ) {
            updateState( delta );

            // We have run Adds up to now. Flush them out.
            if( m_mode == 0 ) {
                flushChanges();
                m_mode = -1;
            }
            // We are in "neutral mode". Start a new Change
            if( m_mode == -1 ) {
                m_mode = 1;
            }

            // We are in "delete mode".
            if( m_mode == 1 || m_mode == 2 ) {
                addOrig( delta.getOriginal() );
                m_mode = 1;
            }
        }

        public void shutdown() {
            m_index = m_max + 1; // Make sure that no hyperlink gets created
            flushChanges();

            if( m_firstElem < m_origStrings.length ) {
                // If there's more than the limit of the orginal left just emit limit and elided...
                if( ( m_origStrings.length - m_firstElem ) > m_unchangedContextLimit ) {
                    final int endIndex = Math.min( m_firstElem + m_unchangedContextLimit, m_origStrings.length -1 );
                    for (int j = m_firstElem; j < endIndex; j++) {
                        m_sb.append( m_origStrings[ j ] );
                    }

                    m_sb.append( ELIDED_TAIL_INDICATOR_HTML );
                } else {
                // emit entire tail of original...
                    for( int j = m_firstElem; j < m_origStrings.length; j++ ) {
                        m_sb.append( m_origStrings[ j ] );
                    }
                }
            }
        }

        private void addOrig( final Chunk chunk ) {
            if( chunk != null ) {
                chunk.toString( m_origBuf );
            }
        }

        private void addNew( final Chunk chunk ) {
            if( chunk != null ) {
                chunk.toString( m_newBuf );
            }
        }

        private void flushChanges() {
            if( m_newBuf.length() + m_origBuf.length() > 0 ) {
                // This is the span element which encapsulates anchor and the change itself
                m_sb.append( CHANGE_START_HTML );

                // Do we want to have a "back link"?
                if( m_emitChangeNextPreviousHyperlinks && m_count > 1 ) {
                    m_sb.append( BACK_PRE_INDEX );
                    m_sb.append( m_count - 1 );
                    m_sb.append( BACK_POST_INDEX );
                }

                // An anchor for the change.
                if (m_emitChangeNextPreviousHyperlinks) {
                    m_sb.append( ANCHOR_PRE_INDEX );
                    m_sb.append( m_count++ );
                    m_sb.append( ANCHOR_POST_INDEX );
                }

                // ... has been added
                if( m_newBuf.length() > 0 ) {
                    m_sb.append( INSERTION_START_HTML );
                    m_sb.append( m_newBuf );
                    m_sb.append( INSERTION_END_HTML );
                }

                // .. has been removed
                if( m_origBuf.length() > 0 ) {
                    m_sb.append( DELETION_START_HTML );
                    m_sb.append( m_origBuf );
                    m_sb.append( DELETION_END_HTML );
                }

                // Do we want a "forward" link?
                if( m_emitChangeNextPreviousHyperlinks && (m_index < m_max) ) {
                    m_sb.append( FORWARD_PRE_INDEX );
                    m_sb.append( m_count ); // Has already been incremented.
                    m_sb.append( FORWARD_POST_INDEX );
                }

                m_sb.append( CHANGE_END_HTML );

                // Nuke the buffers.
                m_origBuf = new StringBuffer();
                m_newBuf = new StringBuffer();
            }

            // After a flush, everything is reset.
            m_mode = -1;
        }
    }

}
