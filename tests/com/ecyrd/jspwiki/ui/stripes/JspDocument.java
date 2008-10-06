package com.ecyrd.jspwiki.ui.stripes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tree structure that represents a JSP document.
 */
class JspDocument
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
     * Returns the list of nodes parsed by {@link #parse(String)}, in the order
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
     * Returns the list of nodes of a specified type as parsed by
     * {@link #parse(String)}, in the order parsed.
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
        return typeNodes;
    }
    
    public void addTaglibDirective( String uri, String prefix )
    {
        // Create new directive
        Tag directive = new Tag( this, NodeType.JSP_DIRECTIVE );
        directive.setName( "taglib" );
        Attribute attribute = new Attribute( this );
        attribute.setName( "uri" );
        attribute.setValue( "/WEB-INF/stripes.tld" );
        directive.addAttribute( attribute );
        attribute = new Attribute( this );
        attribute.setName( "prefix" );
        attribute.setValue( "stripes" );
        directive.addAttribute( attribute );
        
        // Create linebreak
        Text linebreak = new Text( this );
        linebreak.setValue( System.getProperty( "line.separator" ) );
        linebreak.setParent( root );
        
        // Figure out where to put it
        List<Node> directives = getNodes( NodeType.JSP_DIRECTIVE );
        if ( directives.size() == 0 )
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
            Tag directive = (Tag)node;
            if ( "taglib".equals( directive.getName() ) )
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
