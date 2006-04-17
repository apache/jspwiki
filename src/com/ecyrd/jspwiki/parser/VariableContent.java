/* 
  JSPWiki - a JSP-based WikiWiki clone.

  Copyright (C) 2001-2006 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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
package com.ecyrd.jspwiki.parser;

import org.apache.commons.lang.StringEscapeUtils;
import org.jdom.Text;

import com.ecyrd.jspwiki.NoSuchVariableException;
import com.ecyrd.jspwiki.WikiContext;

/**
 *  Stores the contents of a WikiVariable in a WikiDocument DOM tree.
 *  @author Janne Jalkanen
 *  @since  2.4
 */
public class VariableContent extends Text
{
    private static final long serialVersionUID = 1L;

    private String m_varName;
    
    public VariableContent( String varName )
    {
        m_varName = varName;
    }
    
    /**
     *   Evaluates the variable and returns the contents.
     */
    public String getValue()
    {
        WikiDocument root = (WikiDocument) getDocument();
        WikiContext context = root.getContext();

        if( context == null )
            return "No WikiContext available: INTERNAL ERROR";
        
        String result = "";
        try
        {
            result = context.getEngine().getVariableManager().parseAndGetValue( context, m_varName );
        }
        catch( NoSuchVariableException e )
        {
            result = JSPWikiMarkupParser.makeError("No such variable: "+e.getMessage()).getText(); 
        }

        return StringEscapeUtils.escapeXml( result );
    }
    
    public String getText()
    {
        return getValue();
    }

    public String toString()
    {
        return "VariableElement[\""+m_varName+"\"]";
    }
}
