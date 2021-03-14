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

import java.util.HashMap;
import java.util.Map;

import org.apache.wiki.WikiPage;


/**
 *  Defines a search result.
 */
public interface SearchResult {

	/**
     *  Return the page.
     *
     *  @return the WikiPage object containing this result
     */
    WikiPage getPage();

    /**
     *  Returns the score.
     *
     *  @return A positive score value.  Note that there is no upper limit for the score.
     */
    int getScore();


    /**
     * Collection of XHTML fragments representing some contexts in which the match was made (a.k.a., "snippets").
     *
     * @return the search results
     * @since 2.4
     */
    String[] getContexts();

    /**
     * Map representation of this search result, usable for example for JSON serialization.
     *
     * @return a map version of this search result
     */
    default Map<String,Object> toMap() {
        HashMap<String,Object> jsonMap = new HashMap<String,Object>();
        jsonMap.put( "page", getPage().getName() );
        jsonMap.put( "score", getScore() );
        return  jsonMap;
    }

}
