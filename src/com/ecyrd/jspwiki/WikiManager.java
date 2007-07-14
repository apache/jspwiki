package com.ecyrd.jspwiki;

import java.util.*;

public class WikiManager
{
    private Wiki m_defaultWiki = new Wiki("Main");
    private Map  m_wikiList    = new HashMap();
    
    public void initialize( WikiEngine engine, Properties props )
    {
        Collection wikis = engine.getPageManager().getProvider().listAllWikis();
        
        for( Iterator i = wikis.iterator(); i.hasNext(); )
        {
            String wikiName = (String) i.next();
            
            Wiki wiki = new Wiki(wikiName);
            
            m_wikiList.put( wikiName, wiki );
        }
        
        if( !hasWiki("Main") )
        {
            m_wikiList.put( "Main", m_defaultWiki );
        }
    }
    
    public Wiki getWiki( String name )
    {
        Wiki w = (Wiki)m_wikiList.get(name);
        
        if( w == null )
        {
            w = new Wiki(name);
            
            m_wikiList.put( name, w );
        }
        
        return w;
    }
    
    public boolean hasWiki( String name )
    {
        return m_wikiList.containsKey(name);
    }
    
    public Wiki getDefaultWiki()
    {
        return m_defaultWiki;
    }
}
