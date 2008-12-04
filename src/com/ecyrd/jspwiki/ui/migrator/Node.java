package com.ecyrd.jspwiki.ui.migrator;

import java.util.List;

public interface Node
{

    public static final int POSITION_NOT_SET = -1;

    /**
     * Returns the JspDocument the Node belongs to. 
     * @return the document
     */
    public abstract JspDocument getJspDocument();
    
    /**
     * Adds a child to the current Node.
     * 
     * @param node the node to insert
     */
    public abstract void addChild( Node node );
    
    /**
     * Adds a child to the current Node before a specified position
     * in the list of children. If the position is 0, the Node will be
     * inserted before the first child.
     * 
     * @param node the node to insert
     * @param index the position to insert the Node into
     */
    public abstract void addChild( Node node, int index );
    
    /**
     * Adds a Node as a sibling to the current Node.  The supplied sibling
     * will be added just after the current Node. 
     * @param node the node to insert
     */
    public abstract void addSibling( Node node );

    /**
     * Returns the child nodes of this node, as a defensive copy of the
     * internally-cached list.
     * 
     * @return the children
     */
    public abstract List<Node> getChildren();

    /**
     * Returns the column the node starts on, relative to the beginning of the
     * line in the source text.
     * 
     * @return the column
     */
    public abstract int getColumn();

    /**
     * Returns the exnding character position of the node, exclusive, relative
     * to the beginning of the source text.
     * 
     * @return the end position
     */
    public abstract int getEnd();

    /**
     * Returns the line of the source text the node starts on.
     * 
     * @return the line
     */
    public abstract int getLine();

    /**
     * Returns the m_name of the tag, if the node is an HTML start, end, or
     * combined tag, or the directive m_name if a JSP directive;
     * <code>null</code> otherwise.
     * 
     * @return the tag m_name
     */
    public abstract String getName();

    /**
     * Returns the parent of this node, which may be null if a top-level node.
     * 
     * @return the parent
     */
    public abstract Node getParent();

    /**
     * Returns the siblings of this node.
     * 
     * @return the siblings
     */
    public abstract List<Node> getSiblings();

    /**
     * Returns the starting character position of the node, inclusive, relative
     * to the beginning of the source text.
     * 
     * @return the start position
     */
    public abstract int getStart();

    /**
     * Returns the type of the node.
     * 
     * @return the type
     */
    public abstract NodeType getType();

    /**
     * Returns the value of the node, which will be all characters between
     * a matched pair of start and end tags (for Tags), all characters between
     * matching quotes (for Attributes), or text characters (for Texts).
     * 
     * @return the value
     */
    public abstract String getValue();

    public abstract boolean isHtmlNode();

    public abstract boolean isJspNode();

    public abstract void removeChild( Node node );

    public abstract void setColumn( int i );

    public abstract void setEnd( int pos );

    public abstract void setLine( int i );

    /**
     * For Node types that support it, sets the logical name of the Node.
     * @param name a String representing the name.
     */
    public abstract void setName( String name );

    public abstract void setParent( Node parent );

    public abstract void setStart( int pos );

    public abstract void setType( NodeType type );

    /**
     * Convenience method that replaces all child Nodes with a single text node
     * whose value is supplied by the caller.
     * @param value the value to replace
     */
    public abstract void setValue( String value );

}
