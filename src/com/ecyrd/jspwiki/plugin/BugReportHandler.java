/*
    JSPWiki - a JSP-based WikiWiki clone.

    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.  
 */
package com.ecyrd.jspwiki.plugin;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.Principal;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiException;
import com.ecyrd.jspwiki.WikiPage;
import com.ecyrd.jspwiki.filters.RedirectException;
import com.ecyrd.jspwiki.parser.MarkupParser;

/**
 *  Provides a handler for bug reports.  Still under construction.
 *
 *  <ul>
 *   <li>"title" = title of the bug.  This is required.  If it is empty (as in "")
 *       it is a signal to the handler to return quietly.</li>
 *  </ul>
 *
 */
public class BugReportHandler
    implements WikiPlugin
{
    private static Logger log = Logger.getLogger( BugReportHandler.class );

    public static final String TITLE          = "title";
    public static final String DESCRIPTION    = "description";
    public static final String VERSION        = "version";
    public static final String MAPPINGS       = "map";
    public static final String PAGE           = "page";

    private static final String DEFAULT_DATEFORMAT = "dd-MMM-yyyy HH:mm:ss zzz";

    /**
     *  {@inheritDoc}
     */
    public String execute( WikiContext context, Map params )
        throws PluginException
    {
        String    title;
        String    description;
        String    version;
        String    submitter = null;
        SimpleDateFormat format = new SimpleDateFormat( DEFAULT_DATEFORMAT );
        ResourceBundle rb = context.getBundle(WikiPlugin.CORE_PLUGINS_RESOURCEBUNDLE);

        title       = (String) params.get( TITLE );
        description = (String) params.get( DESCRIPTION );
        version     = (String) params.get( VERSION );

        Principal wup = context.getCurrentUser();

        if( wup != null )
        {
            submitter = wup.getName();
        }

        if( title == null ) throw new PluginException(rb.getString("bugreporthandler.titlerequired"));
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

            Date d = new Date();

            //
            //  Outputting of basic data
            //
            out.println("|"+mappings.getProperty(TITLE,"Title")+"|"+title);
            out.println("|"+mappings.getProperty("date","Date")+"|"+format.format(d));
            out.println("|"+mappings.getProperty(VERSION,"Version")+"|"+version);
            if( submitter != null )
            {
                out.println("|"+mappings.getProperty("submitter","Submitter")+
                            "|"+submitter);
            }

            //
            //  Outputting the other parameters added to this.
            //
            for( Iterator i = params.entrySet().iterator(); i.hasNext(); )
            {
                Map.Entry entry = (Map.Entry) i.next();

                if( entry.getKey().equals( TITLE ) ||
                    entry.getKey().equals( DESCRIPTION ) ||
                    entry.getKey().equals( VERSION ) ||
                    entry.getKey().equals( MAPPINGS ) ||
                    entry.getKey().equals( PAGE ) ||
                    entry.getKey().toString().startsWith("_") )
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

            //
            //  Now create a new page for this bug report
            //
            String pageName = findNextPage( context, title,
                                            (String)params.get( PAGE ) );

            WikiPage newPage = new WikiPage( context.getEngine(), pageName );
            WikiContext newContext = (WikiContext)context.clone();
            newContext.setPage( newPage );

            context.getEngine().saveText( newContext,
                                          str.toString() );

            MessageFormat formatter = new MessageFormat("");
            formatter.applyPattern( rb.getString("bugreporthandler.new") );
            String[] args = { "<a href=\""+context.getViewURL(pageName)+"\">"+pageName+"</a>" };

            return formatter.format( args );
        }
        catch( RedirectException e )
        {
            log.info("Saving not allowed, reason: '"+e.getMessage()+"', can't redirect to "+e.getRedirect());

            throw new PluginException("Saving not allowed, reason: "+e.getMessage());
        }
        catch( WikiException e )
        {
            log.error("Unable to save page!",e);

            return rb.getString("bugreporthandler.unable");
        }
    }

    /**
     *  Finds a free page name for adding the bug report.  Tries to construct a page,
     *  and if it's found, adds a number to it and tries again.
     */
    private synchronized String findNextPage( WikiContext context,
                                              String title,
                                              String baseName )
    {
        String basicPageName = ((baseName != null)?baseName:"Bug")+MarkupParser.cleanLink(title);

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

            String key;
            String value;

            if( colon > 0 )
            {
                key = t.substring(0,colon);
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
