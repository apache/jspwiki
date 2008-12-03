package com.ecyrd.jspwiki.ui.stripes;

import java.util.HashMap;
import java.util.Map;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class JspMigratorTest extends TestCase
{
    protected Map<String, Object> m_sharedState = new HashMap<String, Object>();

    protected JspTransformer m_transformer = new StripesJspTransformer();

    protected JspDocument m_doc = new JspDocument();

    public JspMigratorTest( String s )
    {
        super( s );
    }

    public static Test suite()
    {
        return new TestSuite( JspMigratorTest.class );
    }
}
