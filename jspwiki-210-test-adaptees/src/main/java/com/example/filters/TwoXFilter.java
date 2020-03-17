package com.example.filters;

import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.api.exceptions.FilterException;
import org.apache.wiki.api.filters.BasicPageFilter;

import java.util.Properties;


public class TwoXFilter extends BasicPageFilter {

    String newContent = "";
    int invocations = 0;

    /** {@inheritDoc} */
    @Override
    public void initialize( final WikiEngine engine, final Properties properties ) throws FilterException {
        super.initialize( engine, properties );
        invocations++;
    }

    @Override
    public String preTranslate( final WikiContext wikiContext, final String content ) throws FilterException {
        invocations++;
        return content;
    }

    /** {@inheritDoc} */
    @Override
    public String postTranslate( final WikiContext wikiContext, final String htmlContent ) {
        invocations++;
        newContent = "see how I care about yor content - hmmm...";
        return newContent;
    }

    @Override
    public String preSave( final WikiContext wikiContext, final String content ) throws FilterException {
        invocations++;
        return content;
    }

    @Override
    public void postSave( final WikiContext wikiContext, final String content ) throws FilterException {
        invocations++;
    }

    public int invocations() {
        return invocations;
    }

}
