package com.ecyrd.jspwiki.parser;

import org.jdom.Document;

import com.ecyrd.jspwiki.WikiPage;

public class WikiDocument
{
    private Document m_document;
    private WikiPage m_page;
    private String   m_wikiText;
    
    public WikiDocument( WikiPage page, Document doc )
    {
        m_document = doc;
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
    
    public Document getDocument()
    {
        return m_document;
    }
    
    public WikiPage getPage()
    {
        return m_page;
    }
}
