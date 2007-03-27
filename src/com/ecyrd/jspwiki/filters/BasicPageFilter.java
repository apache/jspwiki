package com.ecyrd.jspwiki.filters;

import java.util.Properties;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;

/**
 *  Provides a base implementation of a PageFilter.  None of the callbacks
 *  do anything, so it is a good idea for you to extend from this class
 *  and implement only methods that you need.
 *
 *  @author Janne Jalkanen
 */
public class BasicPageFilter
    implements PageFilter
{
    protected WikiEngine m_engine;
  
    /**
     *  If you override this, you should call super.initialize() first.
     */
    public void initialize( WikiEngine engine, Properties properties )
        throws FilterException
    {
        m_engine = engine;
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
    
    public void destroy( WikiEngine engine ) 
    {
    }
}
