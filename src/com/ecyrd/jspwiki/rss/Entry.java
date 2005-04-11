/*
 * (C) Janne Jalkanen 2005
 * 
 */
package com.ecyrd.jspwiki.rss;

import com.ecyrd.jspwiki.WikiPage;

/**
 *  @author jalkanen
 *
 *  @since 
 */
public class Entry
{
    private String m_content;
    private String m_URL;
    private String m_title;
    private WikiPage m_page;
    private String m_author;
    
    public void setAuthor( String author )
    {
        m_author = author;
    }
    
    public String getAuthor()
    {
        return m_author;
    }
    
    public WikiPage getPage()
    {
        return m_page;
    }
    
    public void setPage( WikiPage p )
    {
        m_page = p;
    }
    
    public void setTitle( String title )
    {
        m_title = title;
    }
    
    public String getTitle()
    {
        return m_title;
    }
    
    public void setURL( String url )
    {
        m_URL = url;
    }
    
    public String getURL()
    {
        return m_URL;
    }
    
    public void setContent( String content )
    {
        m_content = content;
    }
    
    public String getContent()
    {
        return m_content;
    }
}
