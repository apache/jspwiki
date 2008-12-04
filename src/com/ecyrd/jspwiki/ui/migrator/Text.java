package com.ecyrd.jspwiki.ui.migrator;

/**
 * Represents markup in a JSP that is not a {@link Tag} or {@link Attribute},
 * such as text, an HTML comment, a JSP comment, a JSP declaration, scriptlet or
 * a JSP expression.
 */
public class Text extends AbstractNode
{

    private String m_value = null;

    public Text( JspDocument doc )
    {
        super( doc, NodeType.TEXT );
    }

    public Text( JspDocument doc, NodeType type )
    {
        super( doc, type );
    }

    /**
     * Always returns \"(TEXT)\".
     */
    @Override
    public String getName()
    {
        return "(TEXT)";
    }

    public String getValue()
    {
        return m_value;
    }

    public void setName( String name )
    {
        throw new UnsupportedOperationException( "Text nodes cannot have names." );
    }

    public void setValue( String value )
    {
        m_value = value;
    }

    /**
     * Returns the string that represents the Tag, including the name and
     * attributes, but not any child nodes.
     */
    public String toString()
    {
        return m_type.getTagStart() + m_value + m_type.getTagEnd();
    }
}
