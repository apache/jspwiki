package com.ecyrd.jspwiki;

import java.util.Date;

public class WikiPage
{
    private String m_name;
    private Date   m_lastModified;

    private int    m_version = 0;

    private String m_author = "unknown";

    public WikiPage( String name )
    {
        m_name = name;
    }

    public String getName()
    {
        return m_name;
    }

    public Date getLastModified()
    {
        return m_lastModified;
    }

    protected void setLastModified( Date date )
    {
        m_lastModified = date;
    }

    public void setVersion( int version )
    {
        m_version = version;
    }

    public int getVersion()
    {
        return m_version;
    }

    public void setAuthor( String author )
    {
        m_author = author;
    }

    public String getAuthor()
    {
        return m_author;
    }

    public String toString()
    {
        return "WikiPage ["+m_name+",mod="+m_lastModified+"]";
    }
}
