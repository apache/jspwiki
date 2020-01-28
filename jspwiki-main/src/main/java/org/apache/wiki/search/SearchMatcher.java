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
package org.apache.wiki.search;

import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiPage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;


/**
 * SearchMatcher performs the task of matching a search query to a page's contents. This utility class is isolated to simplify
 * WikiPageProvider implementations and to offer an easy target for upgrades. The upcoming(?) TranslatorReader rewrite will
 * presumably invalidate this, among other things.
 *
 * @since 2.1.5
 */
public class SearchMatcher {
	
    private QueryItem[] m_queries;
    private WikiEngine m_engine;

    /**
     *  Creates a new SearchMatcher.
     *  
     *  @param engine The WikiEngine
     *  @param queries A list of queries
     */
    public SearchMatcher( final WikiEngine engine, final QueryItem[] queries ) {
        m_engine = engine;
        m_queries = queries != null ? queries.clone() : null;
    }

    /**
     * Compares the page content, available through the given stream, to the query items of this matcher. Returns a search result
     * object describing the quality of the match.
     *
     * <p>This method would benefit of regexps (1.4) and streaming. FIXME!
     * 
     * @param wikiname The name of the page
     * @param pageText The content of the page
     * @return A SearchResult item, or null, there are no queries
     * @throws IOException If reading page content fails
     */
    public SearchResult matchPageContent( final String wikiname, final String pageText ) throws IOException {
        if( m_queries == null ) {
            return null;
        }

        final int[] scores = new int[ m_queries.length ];
        final BufferedReader in = new BufferedReader( new StringReader( pageText ) );
        String line;

        while( (line = in.readLine() ) != null ) {
            line = line.toLowerCase();

            for( int j = 0; j < m_queries.length; j++ ) {
                int index = -1;

                while( (index = line.indexOf( m_queries[j].word, index + 1 ) ) != -1 ) {
                    if( m_queries[j].type != QueryItem.FORBIDDEN ) {
                        scores[j]++; // Mark, found this word n times
                    } else {
                        // Found something that was forbidden.
                        return null;
                    }
                }
            }
        }

        //  Check that we have all required words.
        int totalscore = 0;

        for( int j = 0; j < scores.length; j++ ) {
            // Give five points for each occurrence of the word in the wiki name.
            if( wikiname.toLowerCase().contains( m_queries[ j ].word ) && m_queries[j].type != QueryItem.FORBIDDEN ) {
                scores[j] += 5;
            }

            //  Filter out pages if the search word is marked 'required' but they have no score.
            if( m_queries[j].type == QueryItem.REQUIRED && scores[j] == 0 ) {
                return null;
            }

            //  Count the total score for this page.
            totalscore += scores[j];
        }

        if( totalscore > 0 ) {
            return new SearchResultImpl( wikiname, totalscore );
        }

        return null;
    }

    /**
     *  A local search result.
     */
    public class SearchResultImpl implements SearchResult {
    	
        int      m_score;
        WikiPage m_page;

        /**
         *  Create a new SearchResult with a given name and a score.
         *  
         *  @param name Page Name
         *  @param score A score from 0+
         */
        public SearchResultImpl( final String name, final int score ) {
            m_page  = new WikiPage( m_engine, name );
            m_score = score;
        }

        /**
         *  Returns Wikipage for this result.
         *  @return WikiPage
         */
        public WikiPage getPage() {
            return m_page;
        }

        /**
         *  Returns a score for this match.
         *  
         *  @return Score from 0+
         */
        public int getScore() {
            return m_score;
        }

        /**
         *  Returns an empty array, since BasicSearchProvider does not support context matching.
         *  
         *  @return an empty array
         */
        public String[] getContexts() {
            // Unimplemented
            return new String[0];
        }
    }

}
