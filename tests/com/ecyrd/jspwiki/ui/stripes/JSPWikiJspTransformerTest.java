package com.ecyrd.jspwiki.ui.stripes;

import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;


public class JSPWikiJspTransformerTest extends TestCase
{
    protected Map<String,Object> m_sharedState = new HashMap<String,Object>();
    protected JspTransformer m_transformer = new JSPWikiJspTransformer();
    protected JspDocument m_doc = new JspDocument();
    
    public void setUp()
    {
        m_transformer.initialize( JspMigrator.findBeanClasses(), m_sharedState );
    }

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
    
    public void testSetBundle() throws Exception
    {
        String s = "<fmt:setBundle basename=\"templates.default\"/>\n<form method=\"POST\" onsubmit=\"return Wiki.submitOnce(this);\" />";
        JspDocument doc = new JspParser().parse( s );
   
        // Should be 3 nodes: 2 HTML + 1 text
        assertEquals( 3, doc.getNodes().size() );
        assertEquals( 3, doc.getRoot().getChildren().size() );
        
        // Run the transformer
        m_transformer.transform( m_sharedState, doc );
        
        // Now, should be only 2 nodes now because the <fmt:setBundle> tag was removed
        assertEquals( 2, doc.getNodes().size() );
        assertEquals( 2, doc.getRoot().getChildren().size() );
        Node node = doc.getNodes().get( 0 );
        assertEquals( NodeType.TEXT, node.getType() );
        node = doc.getNodes().get( 1 );
        assertEquals( "form", node.getName() );
    }
    
    public void testUseActionBean()
    {
        String s = "<% engine.createContext( request, WikiContext.EDIT ); %>";
        JspDocument doc = new JspParser().parse( s );

        // Should be 1 node: scriptlet
        assertEquals( 1, doc.getNodes().size() );
        Node node = doc.getNodes().get( 0 );
        assertEquals( NodeType.SCRIPTLET, node.getType() );

        // Run the transformer
        m_transformer.transform( m_sharedState, doc );

        // Should be 5 nodes: Stripes taglib + <useActionBean> tag + scriptlet + 2 whitespace nodes
        assertEquals( 5, doc.getNodes().size() );
        Tag tag = (Tag)doc.getNodes().get( 2 );
        assertEquals( "stripes:useActionBean", tag.getName() );
        assertEquals( "beanClass", tag.getAttributes().get( 0 ).getName() );
        assertEquals( "com.ecyrd.jspwiki.action.EditActionBean", tag.getAttributes().get( 0 ).getValue() );
        assertEquals( "event", tag.getAttributes().get( 1 ).getName() );
        assertEquals( "edit", tag.getAttributes().get( 1 ).getValue() );
    }
    
    public static Test suite()
    {
        return new TestSuite( JSPWikiJspTransformerTest.class );
    }
}
