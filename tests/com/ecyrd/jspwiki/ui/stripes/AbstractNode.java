package com.ecyrd.jspwiki.ui.stripes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Element that has been parsed.
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
     * @see com.ecyrd.jspwiki.ui.stripes.Node#addChild(com.ecyrd.jspwiki.ui.stripes.Node)
     */
    public void addChild( Node node )
    {
        // Set parent/child relationships
        node.setParent( this );

        // Add the node
        m_children.add( node );
    }

    /**
     * @see com.ecyrd.jspwiki.ui.stripes.Node#addChild(Node, int)
     */
    public void addChild( Node node, int index )
    {
        // Set parent/child relationships
        node.setParent( this );

        // Add the node
        m_children.add( index, node );
    }

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
     * @see com.ecyrd.jspwiki.ui.stripes.Node#getChildren()
     */
    public List<Node> getChildren()
    {
        List<Node> nodesCopy = new ArrayList<Node>();
        nodesCopy.addAll( m_children );
        return Collections.unmodifiableList( nodesCopy );
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ecyrd.jspwiki.ui.stripes.Node#getColumn()
     */
    public int getColumn()
    {
        return m_col;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ecyrd.jspwiki.ui.stripes.Node#getEnd()
     */
    public int getEnd()
    {
        return m_end;
    }

    public JspDocument getJspDocument()
    {
        return m_doc;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ecyrd.jspwiki.ui.stripes.Node#getLevel()
     */
    public int getLevel()
    {
        if( m_parent == null )
        {
            return -1;
        }

        int level = 0;
        Node node = this;
        while ( node.getType() != NodeType.ROOT )
        {
            level++;
            node = node.getParent();
        }
        return level;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ecyrd.jspwiki.ui.stripes.Node#getLine()
     */
    public int getLine()
    {
        return m_line;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ecyrd.jspwiki.ui.stripes.Node#getName()
     */
    public String getName()
    {
        return m_name;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ecyrd.jspwiki.ui.stripes.Node#getParent()
     */
    public Node getParent()
    {
        return m_parent;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ecyrd.jspwiki.ui.stripes.Node#getSiblings()
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

    /*
     * (non-Javadoc)
     * 
     * @see com.ecyrd.jspwiki.ui.stripes.Node#getStart()
     */
    public int getStart()
    {
        return m_start;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ecyrd.jspwiki.ui.stripes.Node#getType()
     */
    public NodeType getType()
    {
        return m_type;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ecyrd.jspwiki.ui.stripes.Node#getValue()
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

    /*
     * (non-Javadoc)
     * 
     * @see com.ecyrd.jspwiki.ui.stripes.Node#isHtmlNode()
     */
    public boolean isHtmlNode()
    {
        return m_type == NodeType.HTML_START_TAG || m_type == NodeType.HTML_COMBINED_TAG || m_type == NodeType.UNRESOLVED_HTML_TAG
               || m_type == NodeType.HTML_END_TAG;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ecyrd.jspwiki.ui.stripes.Node#isJspNode()
     */
    public boolean isJspNode()
    {
        return m_type == NodeType.JSP_COMMENT || m_type == NodeType.JSP_DECLARATION || m_type == NodeType.JSP_EXPRESSION
               || m_type == NodeType.SCRIPTLET || m_type == NodeType.JSP_DIRECTIVE || m_type == NodeType.UNRESOLVED_JSP_TAG;
    }

    /**
     * Returns <code>true</code> if the node can contain attributes,
     * <code>false</code> otherwise.
     * 
     * @return the result
     */
    public boolean isTagWithAttributes()
    {
        return m_type == NodeType.HTML_START_TAG || m_type == NodeType.HTML_COMBINED_TAG || m_type == NodeType.UNRESOLVED_HTML_TAG;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ecyrd.jspwiki.ui.stripes.Node#removeChild(com.ecyrd.jspwiki.ui.stripes.Node)
     */
    public void removeChild( Node node )
    {
        m_children.remove( node );
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ecyrd.jspwiki.ui.stripes.Node#setColumn(int)
     */
    public void setColumn( int i )
    {
        m_col = i;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ecyrd.jspwiki.ui.stripes.Node#setEnd(int)
     */
    public void setEnd( int pos )
    {
        m_end = pos;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ecyrd.jspwiki.ui.stripes.Node#setLine(int)
     */
    public void setLine( int i )
    {
        m_line = i;
    }

    /**
     * Sets the name value of the Node to a supplied string. This method
     * 
     * @see com.ecyrd.jspwiki.ui.stripes.Node#setName(Node)
     */
    public void setName( String name )
    {
        m_name = name;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ecyrd.jspwiki.ui.stripes.Node#setParent(com.ecyrd.jspwiki.ui.stripes.Node)
     */
    public void setParent( Node parent )
    {
        m_parent = parent;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ecyrd.jspwiki.ui.stripes.Node#setStart(int)
     */
    public void setStart( int pos )
    {
        m_start = pos;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ecyrd.jspwiki.ui.stripes.Node#setType(com.ecyrd.jspwiki.ui.stripes.NodeType)
     */
    public void setType( NodeType type )
    {
        m_type = type;
    }

    /**
     * Replaces all children of the Tag with a single Text node.
     * 
     * @param value the string to set
     */
    public void setValue( String value )
    {
        m_children.clear();
        Text node = new Text( m_doc );
        node.setValue( value );
        m_children.add( node );
    }
}
