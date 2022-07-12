package org.apache.wiki.markdown.migration.filters;

import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.exceptions.FilterException;
import org.apache.wiki.api.filters.BasePageFilter;


/**
 * Tidy up wiki markup in order to ease html -> markdown conversion.
 */
public class TidyMarkupFilter extends BasePageFilter {

    @Override
    public String preTranslate( final Context context, final String content ) throws FilterException {
        //content.replace( "", "" )
        return super.preTranslate( context, content );
    }
}
