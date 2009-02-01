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

/**
 * 
 */
package org.apache.wiki.ui.migrator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Node implementation representing an HTML tag, XML tag or JSP tag.
 */
public class Tag extends AbstractNode
{
    private List<Attribute> m_attributes = new ArrayList<Attribute>();

    /**
     * Constructs a new Tag.
     * @param doc the parent JspDocument
     * @param type the node type
     */
    public Tag( JspDocument doc, NodeType type )
    {
        super( doc, type );
    }

    /**
     * Adds an attribute to the Tag.
     * @param attribute the attribute to add
     */
    public void addAttribute( Attribute attribute )
    {
        m_attributes.add( attribute );
    }

    /**
     * {@inheritDoc} If the Node is of type
     * {@link NodeType#EMPTY_ELEMENT_TAG}, the tag will be split into two nodes
     * (start tag and end tag), with the child Node inserted between the two.
     * 
     * @param node the node to insert
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
     * {@inheritDoc} If the Node is of type {@link NodeType#EMPTY_ELEMENT_TAG},
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
        // If this node is an "empty element node," split it into two
        if( m_type == NodeType.EMPTY_ELEMENT_TAG )
        {
            if( m_parent == null )
            {
                throw new IllegalStateException( "Node does not have a parent!" );
            }

            // Change node type to start tag
            m_type = NodeType.START_TAG;

            // Build new end tag & set its parent
            Tag endNode = new Tag( m_doc, NodeType.END_TAG );
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
     * Returns an unmodifiable copy of the attributes of this Tag.
     * 
     * @return the list of attributes
     */
    public List<Attribute> getAttributes()
    {
        List<Attribute> attributesCopy = new ArrayList<Attribute>();
        attributesCopy.addAll( m_attributes );
        return Collections.unmodifiableList( attributesCopy );
    }

    /**
     * Returns the values of all child nodes, concatenated, if the Tag is a start tag; <code>null</code> otherwise.
     * @return the Tag value nodes
     */
    public String getValue()
    {
        if( m_type != NodeType.START_TAG )
        {
            return null;
        }
        return super.getValue();
    }

    /**
     * Returns <code>true</code> if the Tag has an attribute with a supplied name, 
     * and <code>false</code> otherwise.
     * @param name the attribute to search for
     * @return the result
     */
    public boolean hasAttribute( String name )
    {
        return getAttribute( name ) != null;
    }
    
    /**
     * Removes an attribute from the Tag.
     * @param attribute the attribute to remove
     */
    public void removeAttribute( Attribute attribute )
    {
        m_attributes.remove( attribute );
    }

    /**
     * Returns the string that represents the Tag, including the name and
     * attributes, but not any child nodes.
     */
    public String toString()
    {
        // Root node is easy!
        if( m_type == NodeType.ROOT )
        {
            return "ROOT";
        }

        StringBuilder sb = new StringBuilder();

        // Calculate start and end nodes
        String tagStart = m_type.getTagStart();
        String tagEnd = m_type.getTagEnd();
        if( tagStart == null )
            tagStart = "?";
        if( tagEnd == null )
            tagEnd = "?";

        // Print tag start
        sb.append( tagStart );
        
        // For JSP directives, add a leading space
        if ( m_type == NodeType.JSP_DIRECTIVE )
        {
            sb.append( ' ' );
        }

        // If Tag, print start/end plus attributes.
        if( isHtmlNode() || m_type == NodeType.JSP_DIRECTIVE )
        {
            sb.append( m_name );
            if( m_attributes.size() > 0 )
            {
                int dynamicAttributeLevels = 0;
                NodeType lastType = null;
                for( Attribute attr : m_attributes )
                {
                    if( dynamicAttributeLevels == 0 )
                    {
                        sb.append( ' ' );
                    }
                    sb.append( attr.toString() );
                    lastType = attr.getType();
                    if( lastType == NodeType.DYNAMIC_ATTRIBUTE  && attr.getValue().length() > 3 )
                    {
                        if( attr.getValue().charAt( 1 ) != '/' )
                        {
                            dynamicAttributeLevels++;
                        }
                        else if ( attr.getValue().charAt( attr.getValue().length() - 2 ) != '/' )
                        {
                            dynamicAttributeLevels--;
                        }
                    }
                }
                if( lastType == NodeType.DYNAMIC_ATTRIBUTE || m_type == NodeType.JSP_DIRECTIVE || m_type == NodeType.EMPTY_ELEMENT_TAG )
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
