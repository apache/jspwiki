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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Abstract implementation of a Node that has been parsed.
 */
public abstract class AbstractNode implements Node
{
    protected NodeType m_type;

    protected int m_start = POSITION_NOT_SET;

    protected int m_end = POSITION_NOT_SET;

    protected int m_line = POSITION_NOT_SET;

    protected int m_col = POSITION_NOT_SET;

    protected Node m_parent = null;

    protected String m_name = null;

    protected JspDocument m_doc = null;

    protected List<Node> m_children = new ArrayList<Node>();

    /**
     * Constructs a new Node.
     * 
     * @param doc the parent JspDocument
     * @param type the node type
     */
    AbstractNode( JspDocument doc, NodeType type )
    {
        m_doc = doc;
        m_type = type;
    }

    /**
     * {@inheritDoc}
     */
    public void addChild( Node node )
    {
        // Set parent/child relationships
        node.setParent( this );

        // Add the node
        m_children.add( node );
    }

    /**
     * {@inheritDoc}
     */
    public void addChild( Node node, int index )
    {
        // Set parent/child relationships
        node.setParent( this );

        // Add the node
        m_children.add( index, node );
    }

    /**
     * {@inheritDoc}
     */
    public void addSibling( Node node )
    {
        if( m_parent == null )
        {
            throw new IllegalStateException( "This node does not have a parent." );
        }
        List<Node> siblings = m_parent.getChildren();
        int pos = siblings.indexOf( this );
        if( pos == siblings.size() - 1 )
        {
            m_parent.addChild( node );
        }
        else
        {
            m_parent.addChild( node, pos + 1 );
        }
    }

    /**
     * {@inheritDoc}
     */
    public List<Node> getChildren()
    {
        List<Node> nodesCopy = new ArrayList<Node>();
        nodesCopy.addAll( m_children );
        return Collections.unmodifiableList( nodesCopy );
    }

    /**
     * {@inheritDoc}
     */
    public int getColumn()
    {
        return m_col;
    }

    /**
     * {@inheritDoc}
     */
    public int getEnd()
    {
        return m_end;
    }

    /**
     * {@inheritDoc}
     */
    public JspDocument getJspDocument()
    {
        return m_doc;
    }

    /**
     * {@inheritDoc}
     */
    public int getLine()
    {
        return m_line;
    }

    /**
     * {@inheritDoc}
     */
    public String getName()
    {
        return m_name;
    }

    /**
     * {@inheritDoc}
     */
    public Node getParent()
    {
        return m_parent;
    }

    /**
     * {@inheritDoc}
     */
    public List<Node> getSiblings()
    {
        List<Node> siblings = new ArrayList<Node>();
        for( Node sibling : m_parent.getChildren() )
        {
            if( !this.equals( sibling ) )
            {
                siblings.add( sibling );
            }
        }
        return Collections.unmodifiableList( siblings );
    }

    /**
     * {@inheritDoc}
     */
    public int getStart()
    {
        return m_start;
    }

    /**
     * {@inheritDoc}
     */
    public NodeType getType()
    {
        return m_type;
    }

    /**
     * {@inheritDoc}
     */
    public String getValue()
    {
        StringBuilder builder = new StringBuilder();
        for( Node child : m_children )
        {
            builder.append( child.toString() );
        }
        return builder.toString();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isHtmlNode()
    {
        return m_type == NodeType.START_TAG || m_type == NodeType.EMPTY_ELEMENT_TAG || m_type == NodeType.UNRESOLVED_TAG
               || m_type == NodeType.END_TAG || m_type == NodeType.DECLARATION || m_type == NodeType.LINK
               || m_type == NodeType.META;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isJspNode()
    {
        return m_type == NodeType.JSP_COMMENT || m_type == NodeType.JSP_DECLARATION || m_type == NodeType.JSP_EXPRESSION
               || m_type == NodeType.SCRIPTLET || m_type == NodeType.JSP_DIRECTIVE;
    }

    /**
     * Returns <code>true</code> if the node can contain attributes,
     * <code>false</code> otherwise.
     * 
     * @return the result
     */
    public boolean isTagWithAttributes()
    {
        return m_type == NodeType.START_TAG || m_type == NodeType.EMPTY_ELEMENT_TAG || m_type == NodeType.UNRESOLVED_TAG;
    }

    /**
     * {@inheritDoc}
     */
    public void removeChild( Node node )
    {
        m_children.remove( node );
    }

    /**
     * {@inheritDoc}
     */
    public void setColumn( int i )
    {
        m_col = i;
    }

    /**
     * {@inheritDoc}
     */
    public void setEnd( int pos )
    {
        m_end = pos;
    }

    /**
     * {@inheritDoc}
     */
    public void setLine( int i )
    {
        m_line = i;
    }

    /**
     * {@inheritDoc}
     */
    public void setName( String name )
    {
        m_name = name;
    }

    /**
     * {@inheritDoc}
     */
    public void setParent( Node parent )
    {
        m_parent = parent;
    }

    /**
     * {@inheritDoc}
     */
    public void setStart( int pos )
    {
        m_start = pos;
    }

    /**
     * {@inheritDoc}
     */
    public void setType( NodeType type )
    {
        m_type = type;
    }

    /**
     * {@inheritDoc}
     */
    public void setValue( String value )
    {
        m_children.clear();
        Text node = new Text( m_doc );
        node.setValue( value );
        m_children.add( node );
    }
}
