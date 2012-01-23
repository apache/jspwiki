/*
    JSPWiki - a JSP-based WikiWiki clone.

    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.    
 */
package org.apache.wiki.ui.migrator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        m_transformer.initialize( new JspMigrator(), JspMigrator.findBeanClasses(), m_sharedState );
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
    
    public void testChangeFindContext1() throws Exception
    {
        String s = "<% WikiContext ctx = WikiContext.findContext(pageContext); %>";
        JspDocument doc = new JspParser().parse( s );

        // Should be 1 node: scriptlet
        List<Node> nodes = doc.getNodes();
        assertEquals( 1, nodes.size() );
        Node node = nodes.get( 0 );
        assertEquals( NodeType.SCRIPTLET, node.getType() );

        // Run the transformer
        m_transformer.transform( m_sharedState, doc );

        // Should be 3 nodes: page import, plus CR, plus WikiContext.findContext() changed to WikiContextFactory.findContext();
        nodes = doc.getNodes();
        assertEquals( 3, nodes.size() );
        node = nodes.get( 0 );
        assertEquals( NodeType.JSP_DIRECTIVE, node.getType() );
        assertEquals( "page", node.getName() );
        assertEquals( "org.apache.wiki.action.WikiContextFactory", ( (Tag)node).getAttribute( "import" ).getValue() );
        node = nodes.get( 1 );
        assertEquals( NodeType.TEXT, node.getType() );
        node = nodes.get( 2 );
        assertEquals( NodeType.SCRIPTLET, node.getType() );
        assertEquals( " WikiContext ctx = WikiContextFactory.findContext( pageContext ); ", node.getValue() );
    }
    
    public void testChangeFindContext2() throws Exception
    {
        String s = "<%@ page import=\"org.apache.wiki.action.*\" %><% WikiContext ctx = WikiContext.findContext(pageContext); %>";
        JspDocument doc = new JspParser().parse( s );

        // Should be 2 nodes: directive (with wildcard) plus scriptlet
        List<Node> nodes = doc.getNodes();
        assertEquals( 2, nodes.size() );
        Node node = nodes.get( 0 );
        assertEquals( NodeType.JSP_DIRECTIVE, node.getType() );
        assertEquals( "page", node.getName() );
        assertEquals( "org.apache.wiki.action.*", ( (Tag)node).getAttribute( "import" ).getValue() );
        node = nodes.get( 1 );
        assertEquals( NodeType.SCRIPTLET, node.getType() );

        // Run the transformer
        m_transformer.transform( m_sharedState, doc );

        // Should be 3 nodes: page import, plus CR, plus WikiContext.findContext() changed to WikiContextFactory.findContext();
        nodes = doc.getNodes();
        assertEquals( 2, nodes.size() );
        node = nodes.get( 0 );
        assertEquals( NodeType.JSP_DIRECTIVE, node.getType() );
        assertEquals( "page", node.getName() );
        assertEquals( "org.apache.wiki.action.*", ( (Tag)node).getAttribute( "import" ).getValue() );
        node = nodes.get( 1 );
        assertEquals( NodeType.SCRIPTLET, node.getType() );
        assertEquals( " WikiContext ctx = WikiContextFactory.findContext( pageContext ); ", node.getValue() );
    }
    
    public void testRemoveGetName1() throws Exception
    {
        String s = "<% String pagereq = wikiContext.getName();%>";
        JspDocument doc = new JspParser().parse( s );

        // Should be 1 node: scriptlet
        assertEquals( 1, doc.getNodes().size() );
        Node node = doc.getNodes().get( 0 );
        assertEquals( NodeType.SCRIPTLET, node.getType() );

        // Run the transformer
        m_transformer.transform( m_sharedState, doc );

        // Should be 1 node with getName() changed to getPage().getName();
        assertEquals( 1, doc.getNodes().size() );
        assertEquals( " String pagereq = wikiContext.getPage().getName();", node.getValue() );
    }

    public void testRemoveGetName2() throws Exception
    {
        String s = "<% String pagereq = context.getName();%>";
        JspDocument doc = new JspParser().parse( s );

        // Should be 1 node: scriptlet
        assertEquals( 1, doc.getNodes().size() );
        Node node = doc.getNodes().get( 0 );
        assertEquals( NodeType.SCRIPTLET, node.getType() );

        // Run the transformer
        m_transformer.transform( m_sharedState, doc );

        // Should be 1 node with getName() changed to getPage().getName();
        assertEquals( 1, doc.getNodes().size() );
        assertEquals( " String pagereq = context.getPage().getName();", node.getValue() );
    }

    public void testRemoveHasAccess1() throws Exception
    {
        String s = "<% WikiContext context;   if(!wikiContext.hasAccess( response )) return;  %>";
        JspDocument doc = new JspParser().parse( s );

        // Should be 1 node: scriptlet
        assertEquals( 1, doc.getNodes().size() );
        Node node = doc.getNodes().get( 0 );
        assertEquals( NodeType.SCRIPTLET, node.getType() );

        // Run the transformer
        m_transformer.transform( m_sharedState, doc );

        // Should be 1 node with hasAccess() removed
        assertEquals( 1, doc.getNodes().size() );
        assertEquals( " WikiContext context;     ", node.getValue() );
    }
    
    public void testRemoveHasAccess2() throws Exception
    {
        String s = "<% WikiContext context;   if( !context.hasAccess( r ) ) return;  %>";
        JspDocument doc = new JspParser().parse( s );

        // Should be 1 node: scriptlet
        assertEquals( 1, doc.getNodes().size() );
        Node node = doc.getNodes().get( 0 );
        assertEquals( NodeType.SCRIPTLET, node.getType() );

        // Run the transformer
        m_transformer.transform( m_sharedState, doc );

        // Should be 1 node with hasAccess() removed
        assertEquals( 1, doc.getNodes().size() );
        assertEquals( " WikiContext context;     ", node.getValue() );
    }
    
    public void testRemoveHasAccess3() throws Exception
    {
        String s = "<% if(!wikiContext.hasAccess( response )) return; %>";
        JspDocument doc = new JspParser().parse( s );

        // Should be 1 node: scriptlet
        assertEquals( 1, doc.getNodes().size() );
        Node node = doc.getNodes().get( 0 );
        assertEquals( NodeType.SCRIPTLET, node.getType() );

        // Run the transformer
        m_transformer.transform( m_sharedState, doc );

        // Should be 1 node with hasAccess() removed
        assertEquals( 1, doc.getNodes().size() );
        assertEquals( "  ", node.getValue() );
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
        String s = "<% engine.createContext( request, WikiContext.COMMENT ); %>";
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
        assertEquals( "beanclass", tag.getAttributes().get( 0 ).getName() );
        assertEquals( "org.apache.wiki.action.EditActionBean", tag.getAttributes().get( 0 ).getValue() );
        assertEquals( "event", tag.getAttributes().get( 1 ).getName() );
        assertEquals( "comment", tag.getAttributes().get( 1 ).getValue() );
    }
    
    public static Test suite()
    {
        return new TestSuite( JSPWikiJspTransformerTest.class );
    }
}
