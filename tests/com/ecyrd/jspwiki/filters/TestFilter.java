package com.ecyrd.jspwiki.filters;

import java.util.Properties;

import com.ecyrd.jspwiki.WikiEngine;

public class TestFilter
    extends BasicPageFilter
{
    public Properties m_properties;

    public void initialize( WikiEngine engine, Properties props )
    {
        m_properties = props;
    }
}
