package com.ecyrd.jspwiki;

import java.util.*;

public class SearchResultComparator
    implements Comparator
{
    public int compare( Object o1, Object o2 )
    {
        SearchResult s1 = (SearchResult)o1;
        SearchResult s2 = (SearchResult)o2;

        // Bigger scores are first.

        int res = s2.getScore() - s1.getScore();

        if( res == 0 )
            res = s1.getName().compareTo(s2.getName());

        return res;
    }
}
