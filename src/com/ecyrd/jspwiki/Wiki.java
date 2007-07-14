package com.ecyrd.jspwiki;

public class Wiki
{
    private String m_name;

    protected Wiki( String name )
    {
        m_name = name;
    }
    
    public String getName()
    {
        return m_name;
    }
}
