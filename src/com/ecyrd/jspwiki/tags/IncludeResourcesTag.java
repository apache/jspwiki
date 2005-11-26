package com.ecyrd.jspwiki.tags;

/**
 *  This tag is used to include any programmatic includes into the
 *  output stream.  Actually, what it does is that it simply emits a
 *  tiny marker into the stream, and then a ServletFilter will take
 *  care of the actual inclusion.
 *  
 *  @author jalkanen
 *
 */
public class IncludeResourcesTag extends WikiTagBase
{
    private static final long serialVersionUID = 0L;
    
    public static final String MARKER = "<!-- INCLUDERESOURCESTAG 432897493 -->";
    
    public int doWikiStartTag() throws Exception
    {
        pageContext.getOut().println( MARKER );
        
        return SKIP_BODY;
    }

}
