/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2004 Janne Jalkanen (Janne.Jalkanen@iki.fi)

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation; either version 2.1 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.ecyrd.jspwiki.plugin;

import org.apache.log4j.Logger;
import com.ecyrd.jspwiki.*;
import java.util.*;
import java.io.*;
import java.text.SimpleDateFormat;

/**
 *  @author Janne Jalkanen
 */
public class BugReportHandler
    implements WikiPlugin
{
    private static Logger log = Logger.getLogger( BugReportHandler.class );
    
    public static String TITLE          = "title";
    public static String DESCRIPTION    = "description";
    public static String VERSION        = "version";
    public static String MAPPINGS       = "map";
    public static String PAGE           = "page";

    public String execute( WikiContext context, Map params )
        throws PluginException
    {
        ArrayList otherparams = new ArrayList();
        String    title;
        String    description;
        String    version;

        title       = (String) params.get( TITLE );
        description = (String) params.get( DESCRIPTION );
        version     = (String) params.get( VERSION );

        if( title == null ) throw new PluginException("Title is required");
        if( title.length() == 0 ) return "";

        if( description == null ) description = "";
        if( version == null ) version = "unknown";

        Properties mappings = parseMappings( (String) params.get( MAPPINGS ) );

        //
        //  Start things
        //

        try
        {
            StringWriter str = new StringWriter();
            PrintWriter out = new PrintWriter( str );

            out.println("|"+mappings.getProperty(TITLE,"Title")+"|"+title);
            out.println("|Date|"+new Date());
            out.println("|"+mappings.getProperty(VERSION,"Version")+"|"+version);

            for( Iterator i = params.entrySet().iterator(); i.hasNext(); )
            {
                Map.Entry entry = (Map.Entry) i.next();

                if( entry.getKey().equals( TITLE ) ||
                    entry.getKey().equals( DESCRIPTION ) ||
                    entry.getKey().equals( VERSION ) ||
                    entry.getKey().equals( MAPPINGS ) ||
                    entry.getKey().equals( PAGE ) )
                {
                    // Ignore this
                }
                else
                {
                    //
                    //  If no mapping has been defined, just ignore
                    //  it.
                    //
                    String head = mappings.getProperty( (String)entry.getKey(),
                                                        (String)entry.getKey() );
                    if( head.length() > 0 )
                    {
                        out.println("|"+head+
                                    "|"+entry.getValue());
                    }
                }
            }

            out.println();
            out.println( description );

            out.close();

            String pageName = findNextPage( context, title, 
                                            (String)params.get( PAGE ) );

            WikiPage newPage = new WikiPage( pageName );
            WikiContext newContext = (WikiContext)context.clone();
            newContext.setPage( newPage );
            
            context.getEngine().saveText( newContext,
                                          str.toString() );

            return "A new bug report has been created: <a href=\""+context.getEngine().getViewURL(pageName)+"\">"+pageName+"</a>";
        }
        catch( WikiException e )
        {
            log.error("Unable to save page!",e);

            return "Unable to create bug report";
        }
    }
    
    private synchronized String findNextPage( WikiContext context, 
                                              String title,
                                              String baseName )
    {
        String basicPageName = "Bug"+((baseName != null)?baseName:"")+TranslatorReader.cleanLink(title);

        WikiEngine engine = context.getEngine();
        
        String pageName = basicPageName;
        long   lastbug  = 2;

        while( engine.pageExists( pageName ) )
        {
            pageName = basicPageName + lastbug++;
        }
        
        return pageName;
    }

    /**
     *  Just parses a mappings list in the form of "a=b;b=c;c=d".
     *  <p>
     *  FIXME: Should probably be in TextUtil or somewhere.
     */
    private Properties parseMappings( String mappings )
    {
        Properties props = new Properties();

        if( mappings == null ) return props;

        StringTokenizer tok = new StringTokenizer( mappings, ";" );

        while( tok.hasMoreTokens() )
        {
            String t = tok.nextToken();

            int colon = t.indexOf("=");

            String key, value;

            if( colon > 0 )
            {
                key = t.substring(0,colon-1);
                value = t.substring(colon+1);
            }
            else
            {
                key = t;
                value = "";
            }
            
            props.setProperty( key, value );
        }

        return props;
    }
}
