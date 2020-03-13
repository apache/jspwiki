package com.example.filters;

import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.api.exceptions.FilterException;
import org.apache.wiki.api.filters.BasicPageFilter;

import java.util.Properties;


public class TwoXFilter extends BasicPageFilter {

    String newContent = "";

    /** {@inheritDoc} */
    @Override
    public void initialize( final WikiEngine engine, final Properties properties ) throws FilterException {
        super.initialize( engine, properties );
        newContent = "see how I care about yor content - hmmm...";
    }

    /** {@inheritDoc} */
    @Override
    public String postTranslate( final WikiContext wikiContext, final String htmlContent ) {
        return newContent;
    }

}
