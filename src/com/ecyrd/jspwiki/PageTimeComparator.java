package com.ecyrd.jspwiki;

import java.util.*;

public class PageTimeComparator
    implements Comparator
{
    public int compare( Object o1, Object o2 )
    {
        WikiPage w1 = (WikiPage)o1;
        WikiPage w2 = (WikiPage)o2;
            
        // This gets most recent on top
        int timecomparison = w2.getLastModified().compareTo( w1.getLastModified() );

        if( timecomparison == 0 )
        {
            return w1.getName().compareTo( w2.getName() );
        }

        return timecomparison;
    }
}
