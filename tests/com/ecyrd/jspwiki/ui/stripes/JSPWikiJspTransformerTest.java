package com.ecyrd.jspwiki.ui.stripes;

import java.util.HashMap;
import java.util.Map;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;


public class JSPWikiJspTransformerTest extends TestCase
{
    protected Map<String,Object> m_sharedState = new HashMap<String,Object>();
    protected JspTransformer m_transformer = new JSPWikiJspTransformer();
    protected JspDocument m_doc = new JspDocument();
    
    public JSPWikiJspTransformerTest( String s )
    {
        super( s );
    }

    public void testAcceptCharset() throws Exception
    {
        String s = "<form accept-charset=\"ISO\" method=\"POST\" />";
        JspDocument doc = new JspParser().parse( s );
        m_transformer.transform( m_sharedState, doc );

        assertEquals( 1, doc.getNodes().size() );
        Node node = doc.getNodes().get( 0 );
        assertEquals( "form", node.getName() );
        
        assertEquals( 2, ((Tag)node).getAttributes().size() );
        Node attribute = ((Tag)node).getAttributes().get( 0 );
        assertEquals( "accept-charset", attribute.getName() );
        assertEquals( "UTF-8", attribute.getValue() );
    }

    public void testOnSubmit() throws Exception
    {
        String s = "<form method=\"POST\" onsubmit=\"return Wiki.submitOnce(this);\" />";
        JspDocument doc = new JspParser().parse( s );
        m_transformer.transform( m_sharedState, doc );

        assertEquals( 1, doc.getNodes().size() );
        Node node = doc.getNodes().get( 0 );
        assertEquals( "form", node.getName() );
        
        assertEquals( 1, ((Tag)node).getAttributes().size() );
        Node attribute = ((Tag)node).getAttributes().get( 0 );
        assertEquals( "method", attribute.getName() );
        assertEquals( "POST", attribute.getValue() );
    }

    public static Test suite()
    {
        return new TestSuite( JSPWikiJspTransformerTest.class );
    }
}
