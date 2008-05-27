/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2002 Janne Jalkanen (Janne.Jalkanen@iki.fi)

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation; either version 2.1 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package com.ecyrd.jspwiki.tags;

import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.tagext.BodyTagSupport;
import javax.servlet.jsp.tagext.Tag;

/**
 * ParamTag submits name-value pairs to the first enclosing 
 * ParamHandler instance. Name and value are strings, and can
 * be given as tag attributes, or alternatively the value can be 
 * given as the body contents of this tag. 
 * <p>
 * The name-value pair is passed to the closest containing 
 * ancestor tag that implements ParamHandler. 
 */
public class ParamTag 
    extends BodyTagSupport
{

    private static final long serialVersionUID = -4671059568218551633L;
    private String m_name;
    private String m_value;
    
    public void release() 
    {
        m_name = m_value = null;
    }
    
    public void setName( String s ) 
    {
        m_name = s;
    }
    
    public void setValue( String s ) 
    {
        m_value = s;
    }
    
    public int doEndTag()
    {
        Tag t = null;
        while( (t = getParent()) != null && !(t instanceof ParamHandler) )
            ;
        if( t != null )
        {
            String val = m_value;
            if( val == null )
            {
                BodyContent bc = getBodyContent();
                if( bc != null ) 
                {
                    val = bc.getString();
                }
            }
            if( val != null ) 
            {
                ((ParamHandler)t).setContainedParameter( m_name, val );
            }
        }
        
        
        return EVAL_PAGE;
    }
}
