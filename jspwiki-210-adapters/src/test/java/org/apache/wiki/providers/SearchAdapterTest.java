package org.apache.wiki.providers;

import org.apache.wiki.WikiPage;
import org.apache.wiki.api.search.QueryItem;
import org.apache.wiki.api.search.SearchResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class SearchAdapterTest {

    @Test
    public void testOldQueryItemfrom() {
        final QueryItem qi = new QueryItem();
        qi.type = 1;
        qi.word = "word";
        final org.apache.wiki.search.QueryItem old = SearchAdapter.oldQueryItemfrom( qi );
        Assertions.assertEquals( qi.type, old.type );
        Assertions.assertEquals( qi.word, old.word );
    }

    @Test
    public void testNewSearchResultFrom() {
        final org.apache.wiki.search.SearchResult old = new org.apache.wiki.search.SearchResult() {

            @Override
            public WikiPage getPage() {
                return null;
            }

            @Override
            public int getScore() {
                return 0;
            }

            @Override
            public String[] getContexts() {
                return new String[ 0 ];
            }
        };
        final SearchResult sr = SearchAdapter.newSearchResultFrom( old );
        Assertions.assertEquals( old.getPage(), sr.getPage() );
        Assertions.assertEquals( old.getScore(), sr.getScore() );
        Assertions.assertArrayEquals( old.getContexts(), sr.getContexts() );
    }

}
