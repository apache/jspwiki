package com.ecyrd.jspwiki.ui.stripes;

/**
 * Represents an HTML attribute
 */
public class Attribute extends AbstractNode
{
    private char m_quote = '\"';
    
    public Attribute( JspDocument doc )
    {
        super(doc, NodeType.ATTRIBUTE);
    }

    public char getAttributeDelimiter()
    {
        return m_quote;
    }
    
    public void setAttributeDelimiter( char quote )
    {
        m_quote = quote;
    }
    
    /**
     * Always throws an {@link java.lang.UnsupportedOperationException}
     */
    @Override
    public void setType( NodeType type )
    {
        throw new UnsupportedOperationException( "Attributes are always of type NodeType.ATTRIBUTE; illegal to call this method." );
    }

    /**
     * Returns the string that represents the Attribute, including the name and value, which may include embedded tags.
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(  m_name );
        sb.append( '=' );
        sb.append( m_quote );
        for ( Node valueNode : m_children )
        {
            sb.append( valueNode.toString() );
        }
        sb.append( m_quote );
        return sb.toString();
    }
}
