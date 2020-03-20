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
package org.apache.wiki.providers;

import org.apache.wiki.api.core.Page;
import org.apache.wiki.api.search.QueryItem;
import org.apache.wiki.api.search.SearchResult;


class SearchAdapter {

    static org.apache.wiki.search.QueryItem oldQueryItemfrom( final QueryItem item ) {
        final org.apache.wiki.search.QueryItem qi = new org.apache.wiki.search.QueryItem();
        qi.type = item.type;
        qi.word = item.word;
        return qi;
    }

    static SearchResult newSearchResultFrom( final org.apache.wiki.search.SearchResult result ) {
        return new SearchResult() {

            /** {@inheritDoc} */
            @Override
            public Page getPage() {
                return result.getPage();
            }

            /** {@inheritDoc} */
            @Override
            public int getScore() {
                return result.getScore();
            }

            /** {@inheritDoc} */
            @Override
            public String[] getContexts() {
                return result.getContexts();
            }
        };
    }

}
