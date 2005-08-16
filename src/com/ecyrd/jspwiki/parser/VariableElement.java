/*
 * (C) Janne Jalkanen 2005
 * 
 */
package com.ecyrd.jspwiki.parser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.transform.Result;

import org.jdom.*;

import com.ecyrd.jspwiki.NoSuchVariableException;
import com.ecyrd.jspwiki.WikiContext;

public class VariableElement extends Text
{
    private static final long serialVersionUID = 1L;

    private String m_varName;
    
    public VariableElement( String varName )
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

        return result;
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
