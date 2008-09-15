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

    /**
     * Returns all of the child nodes for a supplied Tag, recursively.
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
     * Returns the JspDocument as a String, reconstructed from the Nodes it contains.
     */
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        List<Node> allNodes = getNodes();
        for ( Node node : allNodes )
        {
            builder.append( node.toString() );
        }
        return builder.toString();
    }
}
