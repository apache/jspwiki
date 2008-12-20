package com.ecyrd.jspwiki.ui.migrator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tree structure that represents a JSP document.
 */
public class JspDocument
{
    private final AbstractNode root;

    /**
     * Constructs a new JspDocument.
     */
    public JspDocument()
    {
        super();
        root = new Tag( this, NodeType.ROOT );
    }

    /**
     * Returns the list of nodes contained in the JspDocument, in the order
     * parsed (depth-first search). The list returned is a defensive copy of the
     * internally cached list.
     * 
     * @return the list of nodes
     */
    public List<Node> getNodes()
    {
        List<Node> allNodes = new ArrayList<Node>();
        visitChildren( allNodes, root.getChildren() );
        return Collections.unmodifiableList( allNodes );
    }
    
    /**
     * Returns a list of nodes contained in the JspDocument that contain Java
     * code, in the order parsed (depth-first search). Nodes that are of type
     * {@link NodeType#JSP_DECLARATION}, {@link NodeType#SCRIPTLET},
     * {@link NodeType#JSP_EXPRESSION} or {@link NodeType#CDATA} are considered
     * to contain Java code. Attributes contained within Tags that are JSP expressions
     * are also returned. The list returned is a defensive copy of the
     * internally cached list.
     * 
     * @return the list of nodes
     */
    public List<Node> getScriptNodes()
    {
        List<Node> scriptNodes = new ArrayList<Node>();
        List<Node> nodes = getNodes();
        for ( Node node : nodes )
        {
            switch ( node.getType() )
            {
                case CDATA:
                case JSP_DECLARATION:
                case JSP_EXPRESSION:
                case SCRIPTLET:
                {
                    scriptNodes.add( node );
                    break;
                }
                case START_TAG:
                case EMPTY_ELEMENT_TAG:
                {
                    Tag tag = (Tag)node;
                    for ( Attribute attribute : tag.getAttributes() )
                    {
                        if ( attribute.getType() == NodeType.ATTRIBUTE )
                        {
                            for ( Node attributeNode : attribute.getChildren() )
                            {
                                if ( attributeNode.getType() == NodeType.JSP_EXPRESSION )
                                {
                                    scriptNodes.add( attributeNode );
                                }
                            }
                        }
                    }
                    break;
                }
            }
        }
        return scriptNodes;
    }

    /**
     * Returns the list of nodes contained in the JspDocument, of a specified
     * type, in the order parsed (depth-first search). The list returned is a
     * defensive copy of the internally cached list.
     * 
     * @return the list of nodes
     */
    public List<Node> getNodes( NodeType type )
    {
        List<Node> typeNodes = new ArrayList<Node>();
        for( Node node : getNodes() )
        {
            if( node.getType() == type )
            {
                typeNodes.add( node );
            }
        }
        return Collections.unmodifiableList( typeNodes );
    }

    /**
     * Convenience method that inserts a page-import directive for a supplied class
     * after the last directive in the document. If an import directive already exists
     * for this class (or for its containing package), it is not added.
     *
     * @param clazz the type to add, <em>e.g.,</em> <code>org.foo.Bar</code>
     *            or <code>org.foo.*</code>
     *  @return <code>true</code> if an import was actually added, <code>false</code>
     *  otherwise
     */
    public boolean addPageImportDirective( Class clazz )
    {
        // No need to add it if it's already there
        List<Tag> imports = getPageImport( clazz );
        if( imports.size() > 0 )
        {
            return false;
        }
        
        // Create new directive
        String type = clazz.getName();
        Tag directive = new Tag( this, NodeType.JSP_DIRECTIVE );
        directive.setName( "page" );
        directive.addAttribute( new Attribute( this, "import", type ) );

        // Create linebreak
        Text linebreak = new Text( this );
        linebreak.setValue( System.getProperty( "line.separator" ) );
        linebreak.setParent( root );

        // Figure out where to put it
        List<Node> directives = getNodes( NodeType.JSP_DIRECTIVE );
        if( directives.size() == 0 )
        {
            root.addChild( linebreak, 0 );
            root.addChild( directive, 0 );
        }
        else
        {
            Node lastDirective = directives.get( directives.size() - 1 );
            lastDirective.addSibling( directive );
            lastDirective.addSibling( linebreak );
        }
        return true;
    }

    /**
     * Convenience method that inserts a taglib directive after the last
     * directive in the document.
     * 
     * @param uri the URI of the taglib to add,
     *            <em>e.g.,</em><code>http://stripes.sourceforge.net/stripes.tld</code>
     * @param prefix the prefix for the tablib,
     *            <em>e.g.,</em><code>stripes</code>
     */
    public void addTaglibDirective( String uri, String prefix )
    {
        // Create new directive
        Tag directive = new Tag( this, NodeType.JSP_DIRECTIVE );
        directive.setName( "taglib" );
        directive.addAttribute( new Attribute( this, "uri", uri ) );
        directive.addAttribute( new Attribute( this, "prefix", prefix ) );

        // Create linebreak
        Text linebreak = new Text( this );
        linebreak.setValue( System.getProperty( "line.separator" ) );
        linebreak.setParent( root );

        // Figure out where to put it
        List<Node> directives = getNodes( NodeType.JSP_DIRECTIVE );
        if( directives.size() == 0 )
        {
            root.addChild( linebreak, 0 );
            root.addChild( directive, 0 );
        }
        else
        {
            Node lastDirective = directives.get( directives.size() - 1 );
            lastDirective.addSibling( directive );
            lastDirective.addSibling( linebreak );
        }
    }

    /**
     * <p>
     * Returns a list of JSP page-import directive Tags that match a supplied
     * type name. To be considered a match, the type named in the import must
     * match the supplied type exactly, or match be the wildcard import for the
     * package containing it. For example, if the type being searched for was
     * <code>org.bar.Foo</code>, these page imports would match:
     * </p>
     * <ul>
     * <li>&lt;%@ page import="org.bar.Foo" %&gt;</li>
     * <li>&lt;%@ page import="org.bar.*" %&gt;</li>
     * </ul>
     * 
     * @param clazz the class, interface or other type to match
     * @return a list of all matching tags, which may be a zero-length list
     */
    public List<Tag> getPageImport( Class clazz )
    {
        if( clazz == null )
        {
            throw new IllegalArgumentException( "Class cannot be null." );
        }
        String type = clazz.getName();
        int periodPosition = type.lastIndexOf( '.' );
        String wildcardType = periodPosition == -1 ? "*" : type.substring( 0, periodPosition ) + ".*";
        List<Node> directives = getNodes( NodeType.JSP_DIRECTIVE );
        List<Tag> matchingDirectives = new ArrayList<Tag>();
        for( Node node : directives )
        {
            Tag directive = (Tag) node;
            Attribute imported = directive.getAttribute( "import" );
            if( "page".equals( directive.getName() ) && imported != null )
            {
                if( type.equals( imported.getValue() ) || wildcardType.equals( imported.getValue() ) )
                {
                    matchingDirectives.add( directive );
                }
            }
        }
        return matchingDirectives;
    }

    /**
     * Returns a list of Tags that match a taglib URI and/or prefix. The tablib
     * directive searched for based on a supplied URI and prefix, which may be
     * "*" to denote any URI or prefix.
     * 
     * @param uri the URI to search for
     * @param prefix the prefix to search for
     * @return a list of all matching tags, which may be a zero-length list
     */
    public List<Tag> getTaglibDirective( String uri, String prefix )
    {
        if( uri == null || prefix == null )
        {
            throw new IllegalArgumentException( "URI or prefix cannot be null." );
        }
        List<Node> directives = getNodes( NodeType.JSP_DIRECTIVE );
        List<Tag> matchingDirectives = new ArrayList<Tag>();
        for( Node node : directives )
        {
            Tag directive = (Tag) node;
            if( "taglib".equals( directive.getName() ) )
            {
                String nodeUri = directive.getAttribute( "uri" ).getValue();
                String nodePrefix = directive.getAttribute( "prefix" ).getValue();
                boolean uriMatch = "*".equals( uri ) || nodeUri.equals( uri );
                boolean prefixMatch = "*".equals( prefix ) || nodePrefix.equals( prefix );
                if( uriMatch && prefixMatch )
                {
                    matchingDirectives.add( directive );
                }
            }
        }
        return matchingDirectives;
    }

    /**
     * Returns all of the child nodes for a supplied Tag, recursively.
     * 
     * @param start
     * @return the list of tags
     */
    public List<Node> getChildren( Tag start )
    {
        List<Node> allChildren = new ArrayList<Node>();
        visitChildren( allChildren, start.getChildren() );
        return allChildren;
    }

    public AbstractNode getRoot()
    {
        return root;
    }

    private void visitChildren( List<Node> collection, List<Node> children )
    {
        for( Node child : children )
        {
            collection.add( child );
            if( child.getChildren().size() >= 0 )
            {
                visitChildren( collection, child.getChildren() );
            }
        }
    }

    /**
     * Returns the JspDocument as a String, reconstructed from the Nodes it
     * contains.
     */
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        List<Node> allNodes = getNodes();
        for( Node node : allNodes )
        {
            builder.append( node.toString() );
        }
        return builder.toString();
    }
}
