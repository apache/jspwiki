package com.ecyrd.jspwiki;

import java.util.Date;

public class WikiPage
{
    private String m_name;
    private Date   m_lastModified;

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

    public String toString()
    {
        return "WikiPage ["+m_name+",mod="+m_lastModified+"]";
    }
}
