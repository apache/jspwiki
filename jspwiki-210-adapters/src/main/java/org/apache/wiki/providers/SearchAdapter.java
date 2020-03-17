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
