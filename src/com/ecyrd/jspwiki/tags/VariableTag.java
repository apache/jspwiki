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

import java.io.IOException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.JspException;

import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.NoSuchVariableException;

/**
 *  Returns the value of an Wiki variable.
 *
 *  <P><B>Attributes</B></P>
 *  <UL>
 *    <LI>var - Name of the variable.  Required.
 *    <LI>default - Revert to this value, if the value of "var" is null. 
 *                  If left out, this tag will produce a concise error message
 *                  if the named variable is not found. Set to empty (default="")
 *                  to hide the message.
 *  </UL>
 *
 *  <P>A default value implies <I>failmode='quiet'</I>.
 *
 *  @author Janne Jalkanen
 *  @since 2.0
 */
public class VariableTag
    extends WikiTagBase
{
    private String m_var      = null;
    private String m_default  = null;
    
    public String getVar()
    {
        return m_var;
    }

    public void setVar( String arg )
    {
        m_var = arg;
    }

    public void setDefault( String arg )
    {
        m_default = arg;
    }
    
    public final int doWikiStartTag()
        throws JspException,
               IOException
    {
        WikiEngine engine   = m_wikiContext.getEngine();
        JspWriter out = pageContext.getOut();
        String msg = null;
        String value = null;
        
        try
        {
            value = engine.getVariableManager().getValue( m_wikiContext,
                                                          getVar() );
        }
        catch( NoSuchVariableException e )
        {
            msg = "No such variable: "+e.getMessage();
        }
        catch( IllegalArgumentException e )
        {
            msg = "Incorrect variable name: "+e.getMessage();
        }

        if( value == null )
        {
            value = m_default;
        }

        if( value == null )
        {
            value = msg;
        }
        out.write( value );
        return( SKIP_BODY );
    }
}
