package com.ecyrd.jspwiki.tags;

import com.ecyrd.jspwiki.ui.TemplateManager;

/**
 *  Provides easy access to TemplateManager.addResourceRequest().  You may use
 *  any of the request types defined there.
 * 
 *  @author jalkanen
 *
 */
public class RequestResourceTag extends WikiTagBase
{
    private static final long serialVersionUID = 0L;
    
    private String m_type;
    private String m_resource;

    public void initTag()
    {
        super.initTag();
        m_type = m_resource = null;
    }
    
    public int doWikiStartTag() throws Exception
    {   
        if( m_type != null && m_resource != null )
        {
            TemplateManager.addResourceRequest( m_wikiContext, m_type, m_resource );
        }

        return SKIP_BODY;
    }

    public String getResource()
    {
        return m_resource;
    }

    public void setResource(String r)
    {
        m_resource = r;
    }

    public String getType()
    {
        return m_type;
    }

    public void setType(String type)
    {
        m_type = type;
    }

}
