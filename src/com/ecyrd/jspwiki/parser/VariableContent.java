/*
 * (C) Janne Jalkanen 2005
 * 
 */
package com.ecyrd.jspwiki.parser;

import org.apache.commons.lang.StringEscapeUtils;
import org.jdom.Text;

import com.ecyrd.jspwiki.NoSuchVariableException;
import com.ecyrd.jspwiki.WikiContext;

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
