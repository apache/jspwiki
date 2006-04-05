package com.ecyrd.jspwiki.tags;

public class RequestResourceTag extends WikiTagBase
{
    private static final long serialVersionUID = 0L;
    
    private String m_type;
    private String m_path;

    public void initTag()
    {
        super.initTag();
        m_type = m_path = null;
    }
    
    public int doWikiStartTag() throws Exception
    {
        m_wikiContext.getEngine().getTemplateManager();
        return SKIP_BODY;
    }

    public String getPath()
    {
        return m_path;
    }

    public void setPath(String m_path)
    {
        this.m_path = m_path;
    }

    public String getType()
    {
        return m_type;
    }

    public void setType(String m_type)
    {
        this.m_type = m_type;
    }

}
