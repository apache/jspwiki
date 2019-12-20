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

import org.apache.wiki.WikiContext;


/**
 * Load, initialize and delegate to the DiffProvider that will actually do the work.
 */
public interface DifferenceManager {

    /** Property value for storing a diff provider.  Value is {@value}. */
    String PROP_DIFF_PROVIDER = "jspwiki.diffProvider";

    /**
     * Returns valid XHTML string to be used in any way you please.
     *
     * @param context        The Wiki Context
     * @param firstWikiText  The old text
     * @param secondWikiText the new text
     * @return XHTML, or empty string, if no difference detected.
     */
    String makeDiff( WikiContext context, String firstWikiText, String secondWikiText );

    /**
     *  Returns a diff of two versions of a page.
     *  <p>
     *  Note that the API was changed in 2.6 to provide a WikiContext object!
     *
     *  @param context The WikiContext of the page you wish to get a diff from
     *  @param version1 Version number of the old page.  If WikiPageProvider.LATEST_VERSION (-1), then uses current page.
     *  @param version2 Version number of the new page.  If WikiPageProvider.LATEST_VERSION (-1), then uses current page.
     *
     *  @return A HTML-ized difference between two pages.  If there is no difference, returns an empty string.
     */
    String getDiff( WikiContext context, int version1, int version2 );

}

