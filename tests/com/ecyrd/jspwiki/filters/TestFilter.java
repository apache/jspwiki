package com.ecyrd.jspwiki.filters;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.TextUtil;
import java.util.*;

public class TestFilter
    extends BasicPageFilter
{
    public Properties m_properties;

    public void initialize( Properties props )
    {
        m_properties = props;
    }
}
