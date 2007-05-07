package com.ecyrd.jspwiki.parser;

import org.apache.commons.lang.StringUtils;

/**
 *  Simple class which does Creole => JSPWiki markup translation
 *  
 *  @author jalkanen
 *
 */
public class CreoleParser
{
    
    private static final String SLASH_SLASH = "//";
    private static final String[] PATTERNS = { SLASH_SLASH, "**", "\n", "<<", "[[", "{{{" };
                      
    public String translate(String creole)
    {
        StringBuffer sb = new StringBuffer();
        
        int idx;
        
        while( (idx = StringUtils.indexOfAny(creole, PATTERNS) ) != -1 )
        {
            if( creole.substring(idx).equals( SLASH_SLASH ) )
            {
                
            }
            
            // Add whatever came before
            // sb.append( creole.substring(begin, end) );
        }
        
        return sb.toString();
    }
}
