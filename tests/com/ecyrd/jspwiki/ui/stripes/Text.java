package com.ecyrd.jspwiki.ui.stripes;

public class Text extends AbstractNode
{

    private String m_value = null;
    
    public Text( JspDocument doc )
    {
        super( doc, NodeType.TEXT );
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
        throw new UnsupportedOperationException( "Text nodes cannot have names.");
    }

    public void setValue( String value )
    {
        m_value = value;
    }
    
    /**
     * Returns the string that represents the Tag, including the name and attributes, but not any child nodes.
     */
    public String toString()
    {
        return m_value;
    }
}
