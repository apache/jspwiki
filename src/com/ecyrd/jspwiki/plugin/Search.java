/*
 * (C) Janne Jalkanen 2005
 * 
 */
package com.ecyrd.jspwiki.plugin;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.apache.ecs.xhtml.*;
import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.SearchResult;
import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;

/**
 *  @author jalkanen
 *
 *  @since 
 */
public class Search implements WikiPlugin
{
    static Logger log = Logger.getLogger(Search.class);
    
    public static final String PARAM_QUERY = "query";
    public static final String PARAM_SET   = "set";
    public static final String DEFAULT_SETNAME = "_defaultSet";
    public static final String PARAM_MAX   = "max";
    
    /* (non-Javadoc)
     * @see com.ecyrd.jspwiki.plugin.WikiPlugin#execute(com.ecyrd.jspwiki.WikiContext, java.util.Map)
     */
    public String execute( WikiContext context, Map params ) throws PluginException
    {
        int maxItems = Integer.MAX_VALUE;
        Collection results = null;
        
        String queryString = (String)params.get( PARAM_QUERY );
        String set         = (String)params.get( PARAM_SET );
        String max         = (String)params.get( PARAM_MAX );
        
        if( set == null ) set = DEFAULT_SETNAME;
        if( max != null ) maxItems = Integer.parseInt( max );
        
        if( queryString == null )
        {
            results = (Collection)context.getVariable( set );
        }
        else
        {
            results = doBasicQuery( context, queryString );
            context.setVariable( set, results );
        }
        
        String res = "";
        
        if( results != null )
        {
            res = renderResults( results, context, maxItems );
        }
        
        return res;
    }
    
    private Collection doBasicQuery( WikiContext context, String query )
    {
        log.info("Searching for string "+query);

        Collection list = context.getEngine().findPages( query );

        return list;
    }
    
    private String renderResults( Collection results, WikiContext context, int maxItems )
    {
        WikiEngine engine = context.getEngine();
        table t = new table();
        t.setBorder(0);
        t.setCellPadding(4);

        tr row = new tr();
        t.addElement( row );
        
        row.addElement( new th().setWidth("30%").setAlign("left").addElement("Page") );
        row.addElement( new th().setAlign("left").addElement("Score"));

        int idx = 0;
        for( Iterator i = results.iterator(); i.hasNext() && idx++ <= maxItems; )
        {
            SearchResult sr = (SearchResult) i.next();
            row = new tr();
            
            td name = new td().setWidth("30%");
            name.addElement( "<a href=\""+
                             context.getURL( WikiContext.VIEW, sr.getPage().getName() )+
                             "\">"+engine.beautifyTitle(sr.getPage().getName())+"</a>");
            row.addElement( name );
            
            row.addElement( new td().addElement(""+sr.getScore()));
            
            t.addElement( row );
        }
        
        if( results.isEmpty() )
        {
            row = new tr();
            
            row.addElement( new td().setColSpan(2).addElement( new b().addElement("No results")));

            t.addElement(row);
        }
        
        return t.toString();
    }
}
