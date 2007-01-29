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
    
    public String translate(String creole)
    {
        StringBuffer sb = new StringBuffer();
        
        String[] patterns = {
           "//", "**", "\n", "<<", "[[", "{{{"
        };
        
        int idx;
        int begin = 0;
        
        while( (idx = StringUtils.indexOfAny(creole, patterns) ) != -1 )
        {
            if( creole.substring(idx).equals("//") )
            {
                
            }
            
            // Add whatever came before
            // sb.append( creole.substring(begin, end) );
        }
        
        return sb.toString();
    }
}
