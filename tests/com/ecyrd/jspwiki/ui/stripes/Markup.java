/**
 * 
 */
package com.ecyrd.jspwiki.ui.stripes;

/**
 * Node implementation representing an HTML comment, JSP comment, declaration,
 * scriptlet or expression.
 */
public class Markup extends AbstractNode
{
    /**
     * @param doc the parent JspDocument
     * @param type
     */
    public Markup( JspDocument doc, NodeType type )
    {
        super( doc, type );
    }

    /**
     * Always returns \"(MARKUP)\".
     */
    @Override
    public String getName()
    {
        return "(MARKUP)";
    }
    
    private String m_value;

    public String getValue()
    {
        return m_value;
    }

    public void setValue( String value )
    {
        m_value = value;
    }

    /**
     * Returns the string that represents the JSP comment, declaration,
     * scriptlet or expression.
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append( m_type.getTagStart() );
        sb.append( m_value );
        sb.append( m_type.getTagEnd() );
        return sb.toString();
    }

}
