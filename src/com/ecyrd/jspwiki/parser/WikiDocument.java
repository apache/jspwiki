package com.ecyrd.jspwiki.parser;

import org.jdom.Document;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiPage;

public class WikiDocument extends Document
{
    private WikiPage m_page;
    private String   m_wikiText;

    private WikiContext m_context;
    
    public WikiDocument( WikiPage page )
    {
        m_page     = page;
    }
    
    public void setPageData( String data )
    {
        m_wikiText = data;
    }
    
    public String getPageData()
    {
        return m_wikiText;
    }
    
    public WikiPage getPage()
    {
        return m_page;
    }
    
    public void setContext( WikiContext ctx )
    {
        m_context = ctx;
    }
    
    public WikiContext getContext()
    {
        return m_context;
    }
}
