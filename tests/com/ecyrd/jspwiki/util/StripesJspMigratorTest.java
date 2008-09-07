package com.ecyrd.jspwiki.util;

import java.io.File;
import java.util.List;

import junit.framework.TestCase;

public class StripesJspMigratorTest extends TestCase
{
    public StripesJspMigratorTest( String s )
    {
        super( s );
    }

    public void testParse() throws Exception
    {
        StripesJspMigrator m = new StripesJspMigrator();
        File src = new File( "src/webdocs/LoginForm.jsp" );
        String s = m.readSource( src );

        // Parse the contents of the file
        JspDocument doc = new JspDocument();
        doc.parse( s.toString() );

        // Should result in 19 nodes parsed (11 tags/directives + 8 text/whitespace nodes
        JspDocument.Node node;
        JspDocument.Attribute attribute;
        List<JspDocument.Node> nodes = doc.getNodes();
        assertEquals( 19, nodes.size() );
        int i = 0;

        // Test line 1 aka nodes 0+1
        node = nodes.get( i );
        assertEquals( 1, node.getLine() );
        assertEquals( 1, node.getColumn() );
        assertEquals( 0, node.getStartChar() );
        assertEquals( 39, node.getEndChar() );
        assertEquals( 0, node.getChildren().size() );
        assertEquals( JspDocument.Node.Type.JSP_DIRECTIVE, node.getType() );
        assertEquals( null, node.getParent() );
        assertEquals( 1, node.getLevel() );
        assertEquals( 17, node.getSiblings().size() );
        assertEquals( "page", node.getName() );
        assertEquals( 1, node.getAttributes().size() );
        attribute = node.getAttributes().get( 0 );
        assertEquals( "import", attribute.getName() );
        assertEquals( "org.apache.log4j.*", attribute.getValue().getValue() );
        i++;
        node = nodes.get( i );
        assertEquals( 1, node.getLine() );
        assertEquals( 40, node.getColumn() );
        assertEquals( 39, node.getStartChar() );
        assertEquals( 40, node.getEndChar() );
        assertEquals( 0, node.getChildren().size() );
        assertEquals( JspDocument.Node.Type.TEXT, node.getType() );
        assertEquals( null, node.getParent() );
        assertEquals( 1, node.getLevel() );
        assertEquals( 17, node.getSiblings().size() );
        assertEquals( null, node.getName() );
        assertEquals( 0, node.getAttributes().size() );
        i++;
        
        // Test line 2 aka nodes 2+3
        node = nodes.get( i );
        assertEquals( 2, node.getLine() );
        assertEquals( 1, node.getColumn() );
        assertEquals( 40, node.getStartChar() );
        assertEquals( 80, node.getEndChar() );
        assertEquals( 0, node.getChildren().size() );
        assertEquals( JspDocument.Node.Type.JSP_DIRECTIVE, node.getType() );
        assertEquals( null, node.getParent() );
        assertEquals( 1, node.getLevel() );
        assertEquals( 17, node.getSiblings().size() );
        assertEquals( "page", node.getName() );
        assertEquals( 1, node.getAttributes().size() );
        attribute = node.getAttributes().get( 0 );
        assertEquals( "import", attribute.getName() );
        assertEquals( "com.ecyrd.jspwiki.*", attribute.getValue().getValue() );
        i++;
        node = nodes.get( i );
        assertEquals( 2, node.getLine() );
        assertEquals( 41, node.getColumn() );
        assertEquals( 80, node.getStartChar() );
        assertEquals( 81, node.getEndChar() );
        assertEquals( 0, node.getChildren().size() );
        assertEquals( JspDocument.Node.Type.TEXT, node.getType() );
        assertEquals( null, node.getParent() );
        assertEquals( 1, node.getLevel() );
        assertEquals( 17, node.getSiblings().size() );
        assertEquals( null, node.getName() );
        assertEquals( 0, node.getAttributes().size() );
        i++;

        // Test line 3 aka nodes 4+5
        node = nodes.get( i );
        assertEquals( 3, node.getLine() );
        assertEquals( 1, node.getColumn() );
        assertEquals( 81, node.getStartChar() );
        assertEquals( 128, node.getEndChar() );
        assertEquals( 0, node.getChildren().size() );
        assertEquals( JspDocument.Node.Type.JSP_DIRECTIVE, node.getType() );
        assertEquals( null, node.getParent() );
        assertEquals( 1, node.getLevel() );
        assertEquals( 17, node.getSiblings().size() );
        assertEquals( "page", node.getName() );
        assertEquals( 1, node.getAttributes().size() );
        attribute = node.getAttributes().get( 0 );
        assertEquals( "import", attribute.getName() );
        assertEquals( "com.ecyrd.jspwiki.action.*", attribute.getValue().getValue() );
        i++;
        node = nodes.get( i );
        assertEquals( 3, node.getLine() );
        assertEquals( 48, node.getColumn() );
        assertEquals( 128, node.getStartChar() );
        assertEquals( 129, node.getEndChar() );
        assertEquals( 0, node.getChildren().size() );
        assertEquals( JspDocument.Node.Type.TEXT, node.getType() );
        assertEquals( null, node.getParent() );
        assertEquals( 1, node.getLevel() );
        assertEquals( 17, node.getSiblings().size() );
        assertEquals( null, node.getName() );
        assertEquals( 0, node.getAttributes().size() );
        i++;
        
        // Test line 4 aka nodes 6+7
        node = nodes.get( i );
        assertEquals( 4, node.getLine() );
        assertEquals( 1, node.getColumn() );
        assertEquals( 129, node.getStartChar() );
        assertEquals( 163, node.getEndChar() );
        assertEquals( 0, node.getChildren().size() );
        assertEquals( JspDocument.Node.Type.JSP_DIRECTIVE, node.getType() );
        assertEquals( null, node.getParent() );
        assertEquals( 1, node.getLevel() );
        assertEquals( 17, node.getSiblings().size() );
        assertEquals( "page", node.getName() );
        assertEquals( 1, node.getAttributes().size() );
        attribute = node.getAttributes().get( 0 );
        assertEquals( "errorPage", attribute.getName() );
        assertEquals( "/Error.jsp", attribute.getValue().getValue() );
        i++;
        node = nodes.get( i );
        assertEquals( 4, node.getLine() );
        assertEquals( 35, node.getColumn() );
        assertEquals( 163, node.getStartChar() );
        assertEquals( 164, node.getEndChar() );
        assertEquals( 0, node.getChildren().size() );
        assertEquals( JspDocument.Node.Type.TEXT, node.getType() );
        assertEquals( null, node.getParent() );
        assertEquals( 1, node.getLevel() );
        assertEquals( 17, node.getSiblings().size() );
        assertEquals( null, node.getName() );
        assertEquals( 0, node.getAttributes().size() );
        i++;
        
        // Test line 5 aka nodes 8+9
        node = nodes.get( i );
        assertEquals( 5, node.getLine() );
        assertEquals( 1, node.getColumn() );
        assertEquals( 164, node.getStartChar() );
        assertEquals( 218, node.getEndChar() );
        assertEquals( 0, node.getChildren().size() );
        assertEquals( JspDocument.Node.Type.JSP_DIRECTIVE, node.getType() );
        assertEquals( null, node.getParent() );
        assertEquals( 1, node.getLevel() );
        assertEquals( 17, node.getSiblings().size() );
        assertEquals( "taglib", node.getName() );
        assertEquals( 2, node.getAttributes().size() );
        attribute = node.getAttributes().get( 0 );
        assertEquals( "uri", attribute.getName() );
        assertEquals( "/WEB-INF/jspwiki.tld", attribute.getValue().getValue() );
        attribute = node.getAttributes().get( 1 );
        assertEquals( "prefix", attribute.getName() );
        assertEquals( "wiki", attribute.getValue().getValue() );
        i++;
        node = nodes.get( i );
        assertEquals( 5, node.getLine() );
        assertEquals( 55, node.getColumn() );
        assertEquals( 218, node.getStartChar() );
        assertEquals( 219, node.getEndChar() );
        assertEquals( 0, node.getChildren().size() );
        assertEquals( JspDocument.Node.Type.TEXT, node.getType() );
        assertEquals( null, node.getParent() );
        assertEquals( 1, node.getLevel() );
        assertEquals( 17, node.getSiblings().size() );
        assertEquals( null, node.getName() );
        assertEquals( 0, node.getAttributes().size() );
        i++;

        // Test line 6 aka nodes 10+11
        node = nodes.get( i );
        assertEquals( 6, node.getLine() );
        assertEquals( 1, node.getColumn() );
        assertEquals( 219, node.getStartChar() );
        assertEquals( 276, node.getEndChar() );
        assertEquals( 0, node.getChildren().size() );
        assertEquals( JspDocument.Node.Type.JSP_DIRECTIVE, node.getType() );
        assertEquals( null, node.getParent() );
        assertEquals( 1, node.getLevel() );
        assertEquals( 17, node.getSiblings().size() );
        assertEquals( "taglib", node.getName() );
        assertEquals( 2, node.getAttributes().size() );
        attribute = node.getAttributes().get( 0 );
        assertEquals( "uri", attribute.getName() );
        assertEquals( "/WEB-INF/stripes.tld", attribute.getValue().getValue() );
        attribute = node.getAttributes().get( 1 );
        assertEquals( "prefix", attribute.getName() );
        assertEquals( "stripes", attribute.getValue().getValue() );
        i++;
        node = nodes.get( i );
        assertEquals( 6, node.getLine() );
        assertEquals( 58, node.getColumn() );
        assertEquals( 276, node.getStartChar() );
        assertEquals( 277, node.getEndChar() );
        assertEquals( 0, node.getChildren().size() );
        assertEquals( JspDocument.Node.Type.TEXT, node.getType() );
        assertEquals( null, node.getParent() );
        assertEquals( 1, node.getLevel() );
        assertEquals( 17, node.getSiblings().size() );
        assertEquals( null, node.getName() );
        assertEquals( 0, node.getAttributes().size() );
        i++;
        
        // Test line 7 aka nodes 12+13
        node = nodes.get( i );
        assertEquals( 7, node.getLine() );
        assertEquals( 1, node.getColumn() );
        assertEquals( 277, node.getStartChar() );
        assertEquals( 354, node.getEndChar() );
        assertEquals( 0, node.getChildren().size() );
        assertEquals( JspDocument.Node.Type.HTML_COMBINED_TAG, node.getType() );
        assertEquals( null, node.getParent() );
        assertEquals( 1, node.getLevel() );
        assertEquals( 17, node.getSiblings().size() );
        assertEquals( "stripes:useActionBean", node.getName() );
        
        // Node 12 should have 1 attribute: beanclass="com.ecyrd.jspwiki.action.LoginActionBean"
        assertEquals( 1, node.getAttributes().size() );
        attribute = node.getAttributes().get( 0 );
        assertEquals( "beanclass", attribute.getName() );
        assertEquals ( JspDocument.Node.Type.TEXT, attribute.getValue().getType());
        assertEquals( "com.ecyrd.jspwiki.action.LoginActionBean", attribute.getValue().getValue() );
        assertEquals( '"', node.getAttributes().get( 0 ).getQuoteChar() );
        i++;
        
        // Test line 7, node 13 (line break)
        node = nodes.get( i );
        assertEquals( 7, node.getLine() );
        assertEquals( 78, node.getColumn() );
        assertEquals( 354, node.getStartChar() );
        assertEquals( 355, node.getEndChar() );
        assertEquals( 0, node.getChildren().size() );
        assertEquals( JspDocument.Node.Type.TEXT, node.getType() );
        assertEquals( null, node.getParent() );
        assertEquals( 1, node.getLevel() );
        assertEquals( 17, node.getSiblings().size() );
        assertEquals( null, node.getName() );
        assertEquals( 0, node.getAttributes().size() );
        i++;

        // Test lines 8-19 aka nodes 14+15
        node = nodes.get( i );
        assertEquals( 8, node.getLine() );
        assertEquals( 1, node.getColumn() );
        assertEquals( 355, node.getStartChar() );
        assertEquals( 767, node.getEndChar() );
        assertEquals( JspDocument.Node.Type.JSP_DECLARATION, node.getType() );
        assertEquals( null, node.getParent() );
        assertEquals( 1, node.getLevel() );
        assertEquals( 17, node.getSiblings().size() );
        assertEquals( null, node.getName() );
        assertEquals( 0, node.getAttributes().size() );
        i++;
        node = nodes.get( i );
        assertEquals( 19, node.getLine() );
        assertEquals( 3, node.getColumn() );
        assertEquals( 767, node.getStartChar() );
        assertEquals( 768, node.getEndChar() );
        assertEquals( 0, node.getChildren().size() );
        assertEquals( JspDocument.Node.Type.TEXT, node.getType() );
        assertEquals( null, node.getParent() );
        assertEquals( 1, node.getLevel() );
        assertEquals( 17, node.getSiblings().size() );
        assertEquals( null, node.getName() );
        assertEquals( 0, node.getAttributes().size() );
        i++;
        
        // Test line 20-33 aka node 16
        node = nodes.get( i );
        assertEquals( 20, node.getLine() );
        assertEquals( 1, node.getColumn() );
        assertEquals( 768, node.getStartChar() );
        assertEquals( 1513, node.getEndChar() );
        assertEquals( 0, node.getChildren().size() );
        assertEquals( JspDocument.Node.Type.SCRIPTLET, node.getType() );
        assertEquals( null, node.getParent() );
        assertEquals( 1, node.getLevel() );
        assertEquals( 17, node.getSiblings().size() );
        assertEquals( null, node.getName() );
        assertEquals( 0, node.getAttributes().size() );
        i++;
        
        // Test second tag on line 33 aka node 17
        node = nodes.get( i );
        assertEquals( 33, node.getLine() );
        assertEquals( 3, node.getColumn() );
        assertEquals( 1513, node.getStartChar() );
        assertEquals( 1553, node.getEndChar() );
        assertEquals( 1, node.getChildren().size() );
        assertEquals( JspDocument.Node.Type.HTML_COMBINED_TAG, node.getType() );
        assertEquals( null, node.getParent() );
        assertEquals( 1, node.getLevel() );
        assertEquals( 17, node.getSiblings().size() );
        assertEquals( "wiki:Include", node.getName() );
        
        // Node 17 should have 1 attribute: page="<%=contentPage%>" 
        assertEquals( 1, node.getAttributes().size() );
        attribute = node.getAttributes().get( 0 );
        assertEquals( "page", attribute.getName() );
        assertEquals ( JspDocument.Node.Type.JSP_EXPRESSION, attribute.getValue().getType());
        assertEquals( "contentPage", attribute.getValue().getValue() );
        assertEquals( '"', node.getAttributes().get( 0 ).getQuoteChar() );
        i++;
        
        // Test child tag of second tag on line 33 aka node 18
        node = nodes.get( i );
        assertEquals( 33, node.getLine() );
        assertEquals( 23, node.getColumn() );
        assertEquals( 1533, node.getStartChar() );
        assertEquals( 1549, node.getEndChar() );
        assertEquals( 0, node.getChildren().size() );
        assertEquals( JspDocument.Node.Type.JSP_EXPRESSION, node.getType() );
        assertEquals( nodes.get( i - 1 ), node.getParent() );
        assertEquals( 2, node.getLevel() );
        assertEquals( 0, node.getSiblings().size() );
        assertEquals( null, node.getName() );
        assertEquals( 0, node.getAttributes().size() );
        i++;
    }
    
    public void testParseNested()
    {
        JspDocument doc = new JspDocument();
        doc.parse( "  <wiki:Include page=\"<%=contentPage%>\" var=\'Foo\' />  " );
        
        List<JspDocument.Node> nodes = doc.getNodes();
        JspDocument.Node node;
        assertEquals( 4, nodes.size());
        
        // Node 1: text node
        node = nodes.get( 0 );
        assertEquals( 1, node.getLine() );
        assertEquals( 1, node.getColumn() );
        assertEquals( 0, node.getStartChar() );
        assertEquals( 2, node.getEndChar() );
        assertEquals( JspDocument.Node.Type.TEXT, node.getType() );
        assertEquals( "  ", node.getValue() );
        assertEquals( null, node.getName() );
        assertEquals( null, node.getParent() );
        assertEquals( 1, node.getLevel() );
        assertEquals( 2, node.getSiblings().size() );
        assertEquals( 0, node.getChildren().size() );
        
        // Node 2: HTML tag with nested child
        node = nodes.get( 1 );
        assertEquals( 1, node.getLine() );
        assertEquals( 3, node.getColumn() );
        assertEquals( 2, node.getStartChar() );
        assertEquals( 52, node.getEndChar() );
        assertEquals( JspDocument.Node.Type.HTML_COMBINED_TAG, node.getType() );
        assertEquals( "wiki:Include page=\"<%=contentPage%>\" var=\'Foo\' ", node.getValue() );
        assertEquals( "wiki:Include", node.getName() );
        assertEquals( null, node.getParent() );
        assertEquals( 1, node.getLevel() );
        assertEquals( 2, node.getSiblings().size() );
        assertEquals( 1, node.getChildren().size() );
        assertEquals( nodes.get( 2 ), node.getChildren().get( 0 ) );
        
        // Node 2: test attributes
        assertEquals( 2, node.getAttributes().size() );
        JspDocument.Attribute attribute;
        attribute = node.getAttributes().get( 0 );
        assertEquals( "page", attribute.getName() );
        assertEquals( '"', attribute.getQuoteChar() );
        assertEquals( JspDocument.Node.Type.JSP_EXPRESSION, attribute.getValue().getType() );
        assertEquals( "contentPage", attribute.getValue().getValue() );
        attribute = node.getAttributes().get( 1 );
        assertEquals( "var", attribute.getName() );
        assertEquals( '\'', attribute.getQuoteChar() );
        assertEquals( JspDocument.Node.Type.TEXT, attribute.getValue().getType() );
        assertEquals( "Foo", attribute.getValue().getValue() );
        
        // Node 3: nested JSP expression
        node = nodes.get( 2 );
        assertEquals( 1, node.getLine() );
        assertEquals( 23, node.getColumn() );
        assertEquals( 22, node.getStartChar() );
        assertEquals( 38, node.getEndChar() );
        assertEquals( JspDocument.Node.Type.JSP_EXPRESSION, node.getType() );
        assertEquals( "contentPage", node.getValue() );
        assertEquals( null, node.getName() );
        assertEquals( nodes.get( 1 ), node.getParent() );
        assertEquals( 2, node.getLevel() );
        assertEquals( 0, node.getSiblings().size() );
        assertEquals( 0, node.getChildren().size() );
        
        // Node 4: text
        node = nodes.get( 3 );
        assertEquals( 1, node.getLine() );
        assertEquals( 53, node.getColumn() );
        assertEquals( 52, node.getStartChar() );
        assertEquals( 54, node.getEndChar() );
        assertEquals( JspDocument.Node.Type.TEXT, node.getType() );
        assertEquals( "  ", node.getValue() );
        assertEquals( null, node.getName() );
        assertEquals( null, node.getParent() );
        assertEquals( 1, node.getLevel() );
        assertEquals( 2, node.getSiblings().size() );
        assertEquals( 0, node.getChildren().size() );
    }

}
