package com.example.filters;

import org.apache.wiki.WikiContext;
import org.apache.wiki.api.filters.BasicPageFilter;


public class TwoXFilter extends BasicPageFilter {

    /**
     *  {@inheritDoc}
     */
    @Override
    public String postTranslate( final WikiContext wikiContext, final String htmlContent ) {
        return "see how I care about yor content - hmmm...";
    }

}
