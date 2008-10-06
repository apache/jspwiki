/**
 * 
 */
package com.ecyrd.jspwiki.ui.stripes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Node implementation representing an HTML or XML tag.
 */
public class Tag extends AbstractNode
{
    private List<Attribute> m_attributes = new ArrayList<Attribute>();

    /**
     * @param doc the parent JspDocument
     * @param type
     */
    public Tag( JspDocument doc, NodeType type )
    {
        super( doc, type );
    }

    public void addAttribute( Attribute attribute )
    {
        m_attributes.add( attribute );
    }

    /**
     * Returns the attribute whose name matches a supplied string, or
     * <code>null</code> if not found.
     * 
     * @param name the named attribute to search for
     * @return the attribute if found, or <code>null</code>
     */
    public Attribute getAttribute( String name )
    {
        if( name == null )
        {
            throw new IllegalArgumentException( "Name cannot be null. " );
        }
        for( Attribute attribute : m_attributes )
        {
            if( name.equals( attribute.getName() ) )
            {
                return attribute;
            }
        }
        return null;
    }

    /**
     * Returns the attributes of this node, as a defensive copy of the
     * internally-cached list.
     * 
     * @return
     */
    public List<Attribute> getAttributes()
    {
        List<Attribute> attributesCopy = new ArrayList<Attribute>();
        attributesCopy.addAll( m_attributes );
        return Collections.unmodifiableList( attributesCopy );
    }

    public void removeAttribute( Attribute attribute )
    {
        m_attributes.remove( attribute );
    }

    /**
     * Adds a child to the current Node. If the Node is of type
     * {@link NodeType#HTML_COMBINED_TAG}, the tag will be split into two nodes
     * (start tag and end tag), with the child Node inserted between the two.
     * 
     * @param node the node to insert
     * @param value the node to insert in between the split nodes
     * @throws IllegalStateException if the current Node must be split, and does
     *             not have a parent.
     */
    public void addChild( Node node )
    {
        if( m_children.size() == 0 )
        {
            addChild( node, 0 );
        }
        else
        {
            super.addChild( node );
        }
    }

    /**
     * Adds a child to the current Node before a specified position in the list
     * of children. If the position is 0, the Node will be inserted before the
     * first child. If the Node is of type {@link NodeType#HTML_COMBINED_TAG},
     * the tag will be split into two nodes (start tag and end tag), with the
     * child Node inserted between the two.
     * 
     * @param node the node to insert
     * @param index the position to insert the Node into
     * @throws IllegalStateException if the current Node must be split, and does
     *             not have a parent.
     */
    public void addChild( Node node, int index )
    {
        // If this node is a "combined node," split it into two
        if( m_type == NodeType.HTML_COMBINED_TAG )
        {
            if( m_parent == null )
            {
                throw new IllegalStateException( "Node does not have a parent!" );
            }

            // Change node type to start tag
            m_type = NodeType.HTML_START_TAG;

            // Build new end tag & set its parent
            Tag endNode = new Tag( m_doc, NodeType.HTML_END_TAG );
            endNode.setName( m_name );
            endNode.setParent( m_parent );

            // Insert as sibling of this node
            List<Node> siblings = m_parent.getChildren();
            int startTagPos = siblings.indexOf( this );
            if( startTagPos == siblings.size() - 1 )
            {
                m_parent.addChild( endNode );
            }
            else
            {
                m_parent.addChild( endNode, startTagPos + 1 );
            }
        }
        
        // Finally add the child to the parent
        super.addChild( node, index );
    }

    public String getValue()
    {
        if( m_type != NodeType.HTML_START_TAG )
        {
            return null;
        }
        return super.getValue();
    }

    /**
     * Returns the string that represents the Tag, including the name and
     * attributes, but not any child nodes.
     */
    public String toString()
    {
        // Root node is easy!
        if ( m_type == NodeType.ROOT )
        {
            return "ROOT";
        }
        
        StringBuilder sb = new StringBuilder();
        
        // Calculate start and end nodes
        String tagStart= m_type.getTagStart();
        String tagEnd = m_type.getTagEnd();
        if ( tagStart == null ) tagStart = "?";
        if ( tagEnd == null ) tagEnd = "?";

        // Print tag start
        sb.append( tagStart );

        // If Tag, print start/end plus attributes.
        if( isHtmlNode() || m_type == NodeType.JSP_DIRECTIVE )
        {
            sb.append( m_name );
            if( m_attributes.size() > 0 )
            {
                sb.append( ' ' );
                NodeType lastType = null;
                for( Attribute attr : m_attributes )
                {
                    if ( attr.getType() == lastType )
                    {
                        sb.append( ' ' );
                    }
                    sb.append( attr.toString() );
                    lastType = attr.getType();
                }
                if ( lastType == NodeType.DYNAMIC_ATTRIBUTE  || m_type == NodeType.JSP_DIRECTIVE  )
                {
                    sb.append( ' ' );
                }
            }
        }

        // Everything else is just the start/end tags plus the children nodes
        else
        {
            for( Node child : m_children )
            {
                sb.append( child.toString() );
            }
        }

        // Print tag end
        sb.append( tagEnd );
        return sb.toString();
    }

}
