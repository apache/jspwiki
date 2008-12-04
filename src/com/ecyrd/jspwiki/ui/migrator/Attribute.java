package com.ecyrd.jspwiki.ui.migrator;

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

    /**
     * Convenience method that creates an attribute with a name and string value.
     * @param doc the JspDocument the Attribute is a child of
     * @param name the Attribute's name
     * @param value the Attribute's value
     */
    public Attribute( JspDocument doc, String name, String value )
    {
        super(doc, NodeType.ATTRIBUTE);
        setName( name );
        setValue( value );
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
        if ( type != NodeType.ATTRIBUTE && type != NodeType.DYNAMIC_ATTRIBUTE )
        {
            throw new UnsupportedOperationException( "Attributes are always of type NodeType.ATTRIBUTE or NodeType.DYNAMIC_ATTRIBUTE." );
        }
        super.setType( type );
    }

    /**
     * Returns the string that represents the Attribute, including the name and value, which may include embedded tags.
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        if ( m_type == NodeType.ATTRIBUTE )
        {
            sb.append(  m_name );
            sb.append( '=' );
            sb.append( m_quote );
            for ( Node valueNode : m_children )
            {
                sb.append( valueNode.toString() );
            }
            sb.append( m_quote );
        }
        else
        {
            sb.append( getValue() );
        }
        return sb.toString();
    }
}
