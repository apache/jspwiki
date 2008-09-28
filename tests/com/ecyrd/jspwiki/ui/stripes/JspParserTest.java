package com.ecyrd.jspwiki.ui.stripes;

import java.io.File;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class JspParserTest extends TestCase
{
    public JspParserTest( String s )
    {
        super( s );
    }
    
    public void testCombinedTag() throws Exception
    {
        String s = "<foo />";
        
        // Parse the contents
        JspParser parser = new JspParser();
        JspDocument doc = parser.parse( s );
        
        // Results in one node
        List<Node> nodes = doc.getNodes();
        assertEquals( 1, nodes.size() );
        Tag node;
        
        // Verify HTML combined tag
        node = (Tag) nodes.get( 0 );
        assertEquals( "foo", node.getName() );
        assertEquals( null, node.getValue() );
        assertEquals( 0, node.getChildren().size() );
        assertEquals( NodeType.HTML_COMBINED_TAG, node.getType() );
        assertEquals( "<foo/>", node.toString() );
    }

    public void testParseHtmlComment() throws Exception
    {
        String s = "<!-- This is a comment -->";

        // Parse the contents
        JspParser parser = new JspParser();
        JspDocument doc = parser.parse( s );

        // Results in one node
        List<Node> nodes = doc.getNodes();
        assertEquals( 1, nodes.size() );
        Markup node;

        // Verify comment
        node = (Markup) nodes.get( 0 );
        assertEquals( "(MARKUP)", node.getName() );
        assertEquals( " This is a comment ", node.getValue() );
        assertEquals( 0, node.getChildren().size() );
    }
    
    public void testParseDirective() throws Exception
    {
        String s = "<%@ page import=\"org.apache.log4j.*\" %>";

        // Parse the contents of the file
        JspParser parser = new JspParser();
        JspDocument doc = parser.parse( s );

        // Results in one node
        List<Node> nodes = doc.getNodes();
        assertEquals( 1, nodes.size() );
        JspDirective node;
        Node attribute;

        // Verify directive
        node = (JspDirective) nodes.get( 0 );
        assertEquals( "page", node.getName() );
        assertEquals( 1, node.getAttributes().size() );
        attribute = node.getAttributes().get( 0 );
        assertEquals( "import", attribute.getName() );
        assertEquals( "org.apache.log4j.*", attribute.getValue() );
    }

    public void testParse() throws Exception
    {
        JspMigrator m = new JspMigrator();
        File src = new File( "src/webdocs/LoginForm.jsp" );
        String s = m.readSource( src );

        // Parse the contents of the file
        JspParser parser = new JspParser();
        JspDocument doc = parser.parse( s );

        // Should result in 18 nodes parsed (10 tags/directives + 8
        // text/whitespace nodes
        Node node;
        Node attribute;
        List<Node> nodes = doc.getNodes();
        assertEquals( 18, nodes.size() );
        int i = 0;

        // Test line 1 aka nodes 0+1
        node = nodes.get( i );
        assertEquals( 1, node.getLine() );
        assertEquals( 1, node.getColumn() );
        assertEquals( 0, node.getStart() );
        assertEquals( 39, node.getEnd() );
        assertEquals( 0, node.getChildren().size() );
        assertEquals( NodeType.JSP_DIRECTIVE, node.getType() );
        assertEquals( NodeType.ROOT, node.getParent().getType() );
        assertEquals( 1, node.getLevel() );
        assertEquals( 17, node.getSiblings().size() );
        assertEquals( "page", node.getName() );
        assertEquals( 1, ((JspDirective) node).getAttributes().size() );
        attribute = ((JspDirective) node).getAttributes().get( 0 );
        assertEquals( "import", attribute.getName() );
        assertEquals( "org.apache.log4j.*", attribute.getValue() );
        i++;
        node = nodes.get( i );
        assertEquals( 1, node.getLine() );
        assertEquals( 40, node.getColumn() );
        assertEquals( 39, node.getStart() );
        assertEquals( 40, node.getEnd() );
        assertEquals( 0, node.getChildren().size() );
        assertEquals( NodeType.TEXT, node.getType() );
        assertEquals( NodeType.ROOT, node.getParent().getType() );
        assertEquals( 1, node.getLevel() );
        assertEquals( 17, node.getSiblings().size() );
        assertEquals( "(TEXT)", node.getName() );
        i++;

        // Test line 2 aka nodes 2+3
        node = nodes.get( i );
        assertEquals( 2, node.getLine() );
        assertEquals( 1, node.getColumn() );
        assertEquals( 40, node.getStart() );
        assertEquals( 80, node.getEnd() );
        assertEquals( 0, node.getChildren().size() );
        assertEquals( NodeType.JSP_DIRECTIVE, node.getType() );
        assertEquals( NodeType.ROOT, node.getParent().getType() );
        assertEquals( 1, node.getLevel() );
        assertEquals( 17, node.getSiblings().size() );
        assertEquals( "page", node.getName() );
        assertEquals( 1, ((JspDirective) node).getAttributes().size() );
        attribute = ((JspDirective) node).getAttributes().get( 0 );
        assertEquals( "import", attribute.getName() );
        assertEquals( "com.ecyrd.jspwiki.*", attribute.getValue() );
        i++;
        node = nodes.get( i );
        assertEquals( 2, node.getLine() );
        assertEquals( 41, node.getColumn() );
        assertEquals( 80, node.getStart() );
        assertEquals( 81, node.getEnd() );
        assertEquals( 0, node.getChildren().size() );
        assertEquals( NodeType.TEXT, node.getType() );
        assertEquals( NodeType.ROOT, node.getParent().getType() );
        assertEquals( 1, node.getLevel() );
        assertEquals( 17, node.getSiblings().size() );
        assertEquals( "(TEXT)", node.getName() );
        i++;

        // Test line 3 aka nodes 4+5
        node = nodes.get( i );
        assertEquals( 3, node.getLine() );
        assertEquals( 1, node.getColumn() );
        assertEquals( 81, node.getStart() );
        assertEquals( 128, node.getEnd() );
        assertEquals( 0, node.getChildren().size() );
        assertEquals( NodeType.JSP_DIRECTIVE, node.getType() );
        assertEquals( NodeType.ROOT, node.getParent().getType() );
        assertEquals( 1, node.getLevel() );
        assertEquals( 17, node.getSiblings().size() );
        assertEquals( "page", node.getName() );
        assertEquals( 1, ((JspDirective) node).getAttributes().size() );
        attribute = ((JspDirective) node).getAttributes().get( 0 );
        assertEquals( "import", attribute.getName() );
        assertEquals( "com.ecyrd.jspwiki.action.*", attribute.getValue() );
        i++;
        node = nodes.get( i );
        assertEquals( 3, node.getLine() );
        assertEquals( 48, node.getColumn() );
        assertEquals( 128, node.getStart() );
        assertEquals( 129, node.getEnd() );
        assertEquals( 0, node.getChildren().size() );
        assertEquals( NodeType.TEXT, node.getType() );
        assertEquals( NodeType.ROOT, node.getParent().getType() );
        assertEquals( 1, node.getLevel() );
        assertEquals( 17, node.getSiblings().size() );
        assertEquals( "(TEXT)", node.getName() );
        i++;

        // Test line 4 aka nodes 6+7
        node = nodes.get( i );
        assertEquals( 4, node.getLine() );
        assertEquals( 1, node.getColumn() );
        assertEquals( 129, node.getStart() );
        assertEquals( 163, node.getEnd() );
        assertEquals( 0, node.getChildren().size() );
        assertEquals( NodeType.JSP_DIRECTIVE, node.getType() );
        assertEquals( NodeType.ROOT, node.getParent().getType() );
        assertEquals( 1, node.getLevel() );
        assertEquals( 17, node.getSiblings().size() );
        assertEquals( "page", node.getName() );
        assertEquals( 1, ((JspDirective) node).getAttributes().size() );
        attribute = ((JspDirective) node).getAttributes().get( 0 );
        assertEquals( "errorPage", attribute.getName() );
        assertEquals( "/Error.jsp", attribute.getValue() );
        i++;
        node = nodes.get( i );
        assertEquals( 4, node.getLine() );
        assertEquals( 35, node.getColumn() );
        assertEquals( 163, node.getStart() );
        assertEquals( 164, node.getEnd() );
        assertEquals( 0, node.getChildren().size() );
        assertEquals( NodeType.TEXT, node.getType() );
        assertEquals( NodeType.ROOT, node.getParent().getType() );
        assertEquals( 1, node.getLevel() );
        assertEquals( 17, node.getSiblings().size() );
        assertEquals( "(TEXT)", node.getName() );
        i++;

        // Test line 5 aka nodes 8+9
        node = nodes.get( i );
        assertEquals( 5, node.getLine() );
        assertEquals( 1, node.getColumn() );
        assertEquals( 164, node.getStart() );
        assertEquals( 218, node.getEnd() );
        assertEquals( 0, node.getChildren().size() );
        assertEquals( NodeType.JSP_DIRECTIVE, node.getType() );
        assertEquals( NodeType.ROOT, node.getParent().getType() );
        assertEquals( 1, node.getLevel() );
        assertEquals( 17, node.getSiblings().size() );
        assertEquals( "taglib", node.getName() );
        assertEquals( 2, ((JspDirective) node).getAttributes().size() );
        attribute = ((JspDirective) node).getAttributes().get( 0 );
        assertEquals( "uri", attribute.getName() );
        assertEquals( "/WEB-INF/jspwiki.tld", attribute.getValue() );
        attribute = ((JspDirective) node).getAttributes().get( 1 );
        assertEquals( "prefix", attribute.getName() );
        assertEquals( "wiki", attribute.getValue() );
        i++;
        node = nodes.get( i );
        assertEquals( 5, node.getLine() );
        assertEquals( 55, node.getColumn() );
        assertEquals( 218, node.getStart() );
        assertEquals( 219, node.getEnd() );
        assertEquals( 0, node.getChildren().size() );
        assertEquals( NodeType.TEXT, node.getType() );
        assertEquals( NodeType.ROOT, node.getParent().getType() );
        assertEquals( 1, node.getLevel() );
        assertEquals( 17, node.getSiblings().size() );
        assertEquals( "(TEXT)", node.getName() );
        i++;

        // Test line 6 aka nodes 10+11
        node = nodes.get( i );
        assertEquals( 6, node.getLine() );
        assertEquals( 1, node.getColumn() );
        assertEquals( 219, node.getStart() );
        assertEquals( 276, node.getEnd() );
        assertEquals( 0, node.getChildren().size() );
        assertEquals( NodeType.JSP_DIRECTIVE, node.getType() );
        assertEquals( NodeType.ROOT, node.getParent().getType() );
        assertEquals( 1, node.getLevel() );
        assertEquals( 17, node.getSiblings().size() );
        assertEquals( "taglib", node.getName() );
        assertEquals( 2, ((JspDirective) node).getAttributes().size() );
        attribute = ((JspDirective) node).getAttributes().get( 0 );
        assertEquals( "uri", attribute.getName() );
        assertEquals( "/WEB-INF/stripes.tld", attribute.getValue() );
        attribute = ((JspDirective) node).getAttributes().get( 1 );
        assertEquals( "prefix", attribute.getName() );
        assertEquals( "stripes", attribute.getValue() );
        i++;
        node = nodes.get( i );
        assertEquals( 6, node.getLine() );
        assertEquals( 58, node.getColumn() );
        assertEquals( 276, node.getStart() );
        assertEquals( 277, node.getEnd() );
        assertEquals( 0, node.getChildren().size() );
        assertEquals( NodeType.TEXT, node.getType() );
        assertEquals( NodeType.ROOT, node.getParent().getType() );
        assertEquals( 1, node.getLevel() );
        assertEquals( 17, node.getSiblings().size() );
        assertEquals( "(TEXT)", node.getName() );
        i++;

        // Test line 7 aka nodes 12+13
        node = nodes.get( i );
        assertEquals( 7, node.getLine() );
        assertEquals( 1, node.getColumn() );
        assertEquals( 277, node.getStart() );
        assertEquals( 354, node.getEnd() );
        assertEquals( 0, node.getChildren().size() );
        assertEquals( NodeType.HTML_COMBINED_TAG, node.getType() );
        assertEquals( NodeType.ROOT, node.getParent().getType() );
        assertEquals( 1, node.getLevel() );
        assertEquals( 17, node.getSiblings().size() );
        assertEquals( "stripes:useActionBean", node.getName() );

        // AbstractNode 12 should have 1 attribute:
        // beanclass="com.ecyrd.jspwiki.action.LoginActionBean"
        assertEquals( 1, ((Tag) node).getAttributes().size() );
        attribute = ((Tag) node).getAttributes().get( 0 );
        assertEquals( "beanclass", attribute.getName() );
        assertEquals( NodeType.ATTRIBUTE, attribute.getType() );
        assertEquals( "com.ecyrd.jspwiki.action.LoginActionBean", attribute.getValue() );
        assertEquals( 'c', ((Tag) node).getAttributes().get( 0 ).getValue().charAt( 0 ) );
        i++;

        // Test line 7, node 13 (line break)
        node = nodes.get( i );
        assertEquals( 7, node.getLine() );
        assertEquals( 78, node.getColumn() );
        assertEquals( 354, node.getStart() );
        assertEquals( 355, node.getEnd() );
        assertEquals( 0, node.getChildren().size() );
        assertEquals( NodeType.TEXT, node.getType() );
        assertEquals( NodeType.ROOT, node.getParent().getType() );
        assertEquals( 1, node.getLevel() );
        assertEquals( 17, node.getSiblings().size() );
        assertEquals( "(TEXT)", node.getName() );
        i++;

        // Test lines 8-19 aka nodes 14+15
        node = nodes.get( i );
        assertEquals( 8, node.getLine() );
        assertEquals( 1, node.getColumn() );
        assertEquals( 355, node.getStart() );
        assertEquals( 767, node.getEnd() );
        assertEquals( NodeType.JSP_DECLARATION, node.getType() );
        assertEquals( NodeType.ROOT, node.getParent().getType() );
        assertEquals( 1, node.getLevel() );
        assertEquals( 17, node.getSiblings().size() );
        assertEquals( "(MARKUP)", node.getName() );
        i++;
        node = nodes.get( i );
        assertEquals( 19, node.getLine() );
        assertEquals( 3, node.getColumn() );
        assertEquals( 767, node.getStart() );
        assertEquals( 768, node.getEnd() );
        assertEquals( 0, node.getChildren().size() );
        assertEquals( NodeType.TEXT, node.getType() );
        assertEquals( NodeType.ROOT, node.getParent().getType() );
        assertEquals( 1, node.getLevel() );
        assertEquals( 17, node.getSiblings().size() );
        assertEquals( "(TEXT)", node.getName() );
        i++;

        // Test line 20-33 aka node 16
        node = nodes.get( i );
        assertEquals( 20, node.getLine() );
        assertEquals( 1, node.getColumn() );
        assertEquals( 768, node.getStart() );
        assertEquals( 1513, node.getEnd() );
        assertEquals( 0, node.getChildren().size() );
        assertEquals( NodeType.SCRIPTLET, node.getType() );
        assertEquals( NodeType.ROOT, node.getParent().getType() );
        assertEquals( 1, node.getLevel() );
        assertEquals( 17, node.getSiblings().size() );
        assertEquals( "(MARKUP)", node.getName() );
        i++;

        // Test second tag on line 33 aka node 17
        node = nodes.get( i );
        assertEquals( 33, node.getLine() );
        assertEquals( 3, node.getColumn() );
        assertEquals( 1513, node.getStart() );
        assertEquals( 1553, node.getEnd() );
        assertEquals( 0, node.getChildren().size() );
        assertEquals( NodeType.HTML_COMBINED_TAG, node.getType() );
        assertEquals( NodeType.ROOT, node.getParent().getType() );
        assertEquals( 1, node.getLevel() );
        assertEquals( 17, node.getSiblings().size() );
        assertEquals( "wiki:Include", node.getName() );

        // AbstractNode 17 should have 1 attribute: page="<%=contentPage%>"
        assertEquals( 1, ((Tag) node).getAttributes().size() );
        attribute = ((Tag) node).getAttributes().get( 0 );
        assertEquals( "page", attribute.getName() );
        assertEquals( NodeType.ATTRIBUTE, attribute.getType() );
        assertEquals( "<%=contentPage%>", attribute.getValue() );
        assertEquals( 1, attribute.getChildren().size() );
        assertEquals( NodeType.JSP_EXPRESSION, attribute.getChildren().get( 0 ).getType() );
        assertEquals( "contentPage", attribute.getChildren().get( 0 ).getValue() );
        i++;
    }

    public void testParseNestedTags()
    {
        String s = "  <foo attribute1=\"1\">  <bar attribute2=\"2\" attribute3=\"3\"/>  </foo>  ";
        JspDocument doc = new JspParser().parse( s );

        // Total number of nodes (depth-first search) is 7
        List<Node> nodes = doc.getNodes();
        Node node;
        Node attribute;
        assertEquals( 7, nodes.size() );

        // First, check the root node. Should have 4 children (2 text nodes + 2
        // html nodes)
        node = doc.getRoot();
        assertEquals( 4, node.getChildren().size() );

        // AbstractNode 0 is whitespace
        node = nodes.get( 0 );
        assertEquals( NodeType.TEXT, node.getType() );
        assertEquals( "(TEXT)", node.getName() );
        assertEquals( "  ", node.getValue() );
        assertEquals( doc.getRoot(), node.getParent() );
        assertEquals( 0, node.getStart() );
        assertEquals( 2, node.getEnd() );

        // AbstractNode 1 is <foo> with 1 attribute
        node = nodes.get( 1 );
        assertEquals( NodeType.HTML_START_TAG, node.getType() );
        assertEquals( "<foo attribute1=\"1\">", node.toString() );
        assertEquals( "foo", node.getName() );
        assertEquals( "  <bar attribute2=\"2\" attribute3=\"3\"/>  ", node.getValue() );
        assertEquals( 2, node.getStart() );
        assertEquals( 22, node.getEnd() );

        // AbstractNode 1: attributes test
        assertEquals( 1, ((Tag) node).getAttributes().size() );
        attribute = ((Tag) node).getAttributes().get( 0 );
        assertEquals( "attribute1", attribute.getName() );
        assertEquals( "1", attribute.getValue() );
        assertEquals( 7, attribute.getStart() );
        assertEquals( 21, attribute.getEnd() );

        // AbstractNode 1 also has 3 child elements: <bar> plus two whitespace
        // nodes
        assertEquals( 3, node.getChildren().size() );

        // Check AbstractNode 1, child 0 -- should be whitespace
        node = nodes.get( 1 ).getChildren().get( 0 );
        assertEquals( NodeType.TEXT, node.getType() );
        assertEquals( "(TEXT)", node.getName() );
        assertEquals( "  ", node.getValue() );
        assertEquals( nodes.get( 1 ), node.getParent() );
        assertEquals( 22, node.getStart() );
        assertEquals( 24, node.getEnd() );

        // Check AbstractNode 1, child 1 -- should be <bar>
        node = nodes.get( 1 ).getChildren().get( 1 );
        assertEquals( NodeType.HTML_COMBINED_TAG, node.getType() );
        assertEquals( "<bar attribute2=\"2\" attribute3=\"3\"/>", node.toString() );
        assertEquals( "bar", node.getName() );
        assertEquals( 0, node.getChildren().size() );
        assertEquals( 24, node.getStart() );
        assertEquals( 60, node.getEnd() );
        assertEquals( 2, ((Tag) node).getAttributes().size() );
        assertEquals( "attribute2", ((Tag) node).getAttributes().get( 0 ).getName() );
        assertEquals( "2", ((Tag) node).getAttributes().get( 0 ).getValue() );
        assertEquals( "attribute3", ((Tag) node).getAttributes().get( 1 ).getName() );
        assertEquals( "3", ((Tag) node).getAttributes().get( 1 ).getValue() );

        // Check AbstractNode 1, child 2 -- should be whitespace
        node = nodes.get( 1 ).getChildren().get( 2 );
        assertEquals( NodeType.TEXT, node.getType() );
        assertEquals( "(TEXT)", node.getName() );
        assertEquals( "  ", node.getValue() );
        assertEquals( nodes.get( 1 ), node.getParent() );
        assertEquals( 60, node.getStart() );
        assertEquals( 62, node.getEnd() );

        // AbstractNode 5 (</foo) has no attributes
        node = nodes.get( 5 );
        assertEquals( NodeType.HTML_END_TAG, node.getType() );
        assertEquals( null, node.getValue() );
        assertEquals( "foo", node.getName() );
        assertEquals( 0, ((Tag) node).getAttributes().size() );
        assertEquals( 0, node.getChildren().size() );
        assertEquals( 62, node.getStart() );
        assertEquals( 68, node.getEnd() );

        // AbstractNode 6 is whitespace
        node = nodes.get( 6 );
        assertEquals( NodeType.TEXT, node.getType() );
        assertEquals( "(TEXT)", node.getName() );
        assertEquals( "  ", node.getValue() );
        assertEquals( doc.getRoot(), node.getParent() );
        assertEquals( 68, node.getStart() );
        assertEquals( 70, node.getEnd() );

        // The children of AbstractNode 1 == Nodes 2, 3 and 4 from
        // doc.getNodes()
        node = nodes.get( 1 );
        assertEquals( nodes.get( 2 ), node.getChildren().get( 0 ) );
        assertEquals( nodes.get( 3 ), node.getChildren().get( 1 ) );
        assertEquals( nodes.get( 4 ), node.getChildren().get( 2 ) );

    }

    public void testParseNestedExpression()
    {
        String s = "  <wiki:Include page=\"<%=contentPage%>\" var=\'Foo\' />  ";
        JspDocument doc = new JspParser().parse( s );

        List<Node> nodes = doc.getNodes();
        Node node;
        assertEquals( 3, nodes.size() );

        // AbstractNode 1: text node
        node = nodes.get( 0 );
        assertEquals( 1, node.getLine() );
        assertEquals( 1, node.getColumn() );
        assertEquals( 0, node.getStart() );
        assertEquals( 2, node.getEnd() );
        assertEquals( NodeType.TEXT, node.getType() );
        assertEquals( "  ", node.getValue() );
        assertEquals( "(TEXT)", node.getName() );
        assertEquals( NodeType.ROOT, node.getParent().getType() );
        assertEquals( 1, node.getLevel() );
        assertEquals( 2, node.getSiblings().size() );
        assertEquals( 0, node.getChildren().size() );

        // AbstractNode 2: HTML tag with attribute containing JSP expression
        node = nodes.get( 1 );
        assertEquals( 1, node.getLine() );
        assertEquals( 3, node.getColumn() );
        assertEquals( 2, node.getStart() );
        assertEquals( 52, node.getEnd() );
        assertEquals( NodeType.HTML_COMBINED_TAG, node.getType() );
        assertEquals( "<wiki:Include page=\"<%=contentPage%>\" var=\'Foo\'/>", node.toString() );
        assertEquals( "wiki:Include", node.getName() );
        assertEquals( null, node.getValue() );
        assertEquals( NodeType.ROOT, node.getParent().getType() );
        assertEquals( 1, node.getLevel() );
        assertEquals( 2, node.getSiblings().size() );
        assertEquals( 0, node.getChildren().size() );

        // AbstractNode 2: test attributes
        assertEquals( 2, ((Tag) node).getAttributes().size() );
        Node attribute;
        attribute = ((Tag) node).getAttributes().get( 0 );
        assertEquals( NodeType.ATTRIBUTE, attribute.getType() );
        assertEquals( "page", attribute.getName() );
        assertEquals( "<%=contentPage%>", attribute.getValue() );
        assertEquals( 1, attribute.getChildren().size() );
        assertEquals( NodeType.JSP_EXPRESSION, attribute.getChildren().get( 0 ).getType() );
        assertEquals( "contentPage", attribute.getChildren().get( 0 ).getValue() );

        attribute = ((Tag) node).getAttributes().get( 1 );
        assertEquals( NodeType.ATTRIBUTE, attribute.getType() );
        assertEquals( "var", attribute.getName() );
        assertEquals( "Foo", attribute.getValue() );
        assertEquals( 1, attribute.getChildren().size() );
        assertEquals( NodeType.TEXT, attribute.getChildren().get( 0 ).getType() );
        assertEquals( "Foo", attribute.getChildren().get( 0 ).getValue() );

        // AbstractNode 3: text
        node = nodes.get( 2 );
        assertEquals( 1, node.getLine() );
        assertEquals( 53, node.getColumn() );
        assertEquals( 52, node.getStart() );
        assertEquals( 54, node.getEnd() );
        assertEquals( NodeType.TEXT, node.getType() );
        assertEquals( "  ", node.getValue() );
        assertEquals( "(TEXT)", node.getName() );
        assertEquals( NodeType.ROOT, node.getParent().getType() );
        assertEquals( 1, node.getLevel() );
        assertEquals( 2, node.getSiblings().size() );
        assertEquals( 0, node.getChildren().size() );
    }

    public static Test suite()
    {
        return new TestSuite( JspParserTest.class );
    }
}
