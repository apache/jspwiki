package com.ecyrd.jspwiki.filters;

import com.ecyrd.jspwiki.WikiContext;
import java.util.Properties;

/**
 *  Provides a base implementation of a PageFilter.  None of the methods
 *  do anything, so it is a good idea for you to extend from this class
 *  and implement only methods that you need.
 *
 *  @author Janne Jalkanen
 */
public class BasicPageFilter
    implements PageFilter
{
    public void initialize( Properties properties )
        throws FilterException
    {
    }

    public String preTranslate( WikiContext wikiContext, String content )
        throws FilterException
    {
        return content;
    }

    public String postTranslate( WikiContext wikiContext, String htmlContent )
        throws FilterException
    {
        return htmlContent;
    }

    public String preSave( WikiContext wikiContext, String content )
        throws FilterException
    {
        return content;
    }

    public void postSave( WikiContext wikiContext, String content )
        throws FilterException
    {
    }
}
