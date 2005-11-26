package com.ecyrd.jspwiki.tags;

import com.ecyrd.jspwiki.ui.TemplateManager;

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
        
    private String m_type;
    
    public void setType( String type )
    {
        m_type = type;
    }
    
    public int doWikiStartTag() throws Exception
    {
        String marker = TemplateManager.getMarker(m_type);

        pageContext.getOut().println( marker );
        
        return SKIP_BODY;
    }

}
