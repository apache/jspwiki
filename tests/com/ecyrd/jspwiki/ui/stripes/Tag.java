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
     * 
     * @param doc the parent JspDocument
     * @param type
     */
    public Tag ( JspDocument doc, NodeType type )
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

    private String diagnostic()
    {
        StringBuilder sb = new StringBuilder();
        sb.append( "[" );
        sb.append( m_type.toString() );
        sb.append( "(pos=" );
        sb.append( m_line );
        sb.append( ":" );
        sb.append( m_col );
        sb.append( ",chars=" );
        sb.append( m_start );
        sb.append( ":" );
        sb.append( m_end );
        sb.append( ",L" );
        sb.append( getLevel() );
        sb.append( ")," );
        sb.append( "name=\"" );
        sb.append( m_name );
        sb.append( "\"," );
        if( m_attributes.size() > 0 )
        {
            sb.append( "attributes=" );
            for( Attribute attr : m_attributes )
            {
                sb.append( "[" );
                sb.append( attr.toString() );
                sb.append( "\"]" );
            }
            sb.append( "," );
        }
        sb.append( "value=\"" );
        sb.append( getValue() );
        sb.append( "\"]" );

        return sb.toString();    }
    
    public String  getValue()
    {
        if ( m_type != NodeType.HTML_START_TAG )
        {
            return null;
        }
        return super.getValue();
    }
    
    /**
     * Returns the string that represents the Tag, including the name and attributes, but not any child nodes.
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append( m_type.getTagStart() );
        
        // HTML nodes and JSP directives are formatted in mostly the same way.
        if ( isHtmlNode() || m_type == NodeType.JSP_DIRECTIVE )
        {
            if ( m_type == NodeType.JSP_DIRECTIVE )
            {
                sb.append( ' ' );
            }
            sb.append(  m_name );
            if( m_attributes.size() > 0 )
            {
                for( Attribute attr : m_attributes )
                {
                    sb.append( ' ' );
                    sb.append( attr.toString() );
                }
            }
        }
        
        // Everything else is just the start/end tags plus the children nodes
        else {
            for ( Node child : m_children )
            {
                sb.append( child.toString() );
            }
        }
        
        sb.append( m_type.getTagEnd() );
        return sb.toString();
    }

}
