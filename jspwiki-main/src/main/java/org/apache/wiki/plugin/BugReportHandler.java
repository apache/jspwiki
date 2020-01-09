/*
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
package org.apache.wiki.plugin;

import org.apache.log4j.Logger;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiPage;
import org.apache.wiki.api.exceptions.PluginException;
import org.apache.wiki.api.exceptions.RedirectException;
import org.apache.wiki.api.exceptions.WikiException;
import org.apache.wiki.api.plugin.WikiPlugin;
import org.apache.wiki.parser.MarkupParser;
import org.apache.wiki.preferences.Preferences;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.Principal;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.StringTokenizer;

/**
 *  Provides a handler for bug reports.  Still under construction.
 *
 *  <p>Parameters : </p>
 *  <ul>
 *  <li><b>title</b> -  title of the bug, this is required.  If it is empty (as in "")  it is a signal to the handler to return quietly.</li>
 *  <li><b>description</b> - description of the bug.</li>
 *  <li><b>version</b> - version</li>
 *  <li><b>map</b> - I have no idea </li>
 *  <li><b>page</b> - The name of the page to be created for this bug report </li>
 *  </ul>
 *
 */
public class BugReportHandler implements WikiPlugin {

    private static final Logger log = Logger.getLogger( BugReportHandler.class );
    private static final String DEFAULT_DATEFORMAT = "dd-MMM-yyyy HH:mm:ss zzz";

    /** Parameter name for setting the title.  Value is <tt>{@value}</tt>. */
    public static final String PARAM_TITLE          = "title";
    /** Parameter name for setting the description.  Value is <tt>{@value}</tt>. */
    public static final String PARAM_DESCRIPTION    = "description";
    /** Parameter name for setting the version.  Value is <tt>{@value}</tt>. */
    public static final String PARAM_VERSION        = "version";
    /** Parameter name for setting the map.  Value is <tt>{@value}</tt>. */
    public static final String PARAM_MAPPINGS       = "map";
    /** Parameter name for setting the page.  Value is <tt>{@value}</tt>. */
    public static final String PARAM_PAGE           = "page";

    /**
     *  {@inheritDoc}
     */
    public String execute( final WikiContext context, final Map< String, String > params ) throws PluginException {
        final String title = params.get( PARAM_TITLE );
        String description = params.get( PARAM_DESCRIPTION );
        String version = params.get( PARAM_VERSION );
        String submitter = null;
        final SimpleDateFormat format = new SimpleDateFormat( DEFAULT_DATEFORMAT );
        final ResourceBundle rb = Preferences.getBundle( context, WikiPlugin.CORE_PLUGINS_RESOURCEBUNDLE );
        final Principal wup = context.getCurrentUser();

        if( wup != null ) {
            submitter = wup.getName();
        }

        if( title == null ) {
            throw new PluginException(rb.getString("bugreporthandler.titlerequired"));
        }
        if( title.length() == 0 ) {
            return "";
        }

        if( description == null ) {
            description = "";
        }
        if( version == null ) {
            version = "unknown";
        }

        final Properties mappings = parseMappings( params.get( PARAM_MAPPINGS ) );

        //  Start things
        try {
            final StringWriter str = new StringWriter();
            final PrintWriter out = new PrintWriter( str );
            final Date d = new Date();

            //  Outputting of basic data
            out.println( "|" + mappings.getProperty(PARAM_TITLE,"Title" ) + "|" + title );
            out.println( "|" + mappings.getProperty("date","Date" ) + "|" + format.format( d ) );
            out.println( "|" + mappings.getProperty(PARAM_VERSION,"Version" ) + "|" + version );
            if( submitter != null ) {
                out.println("|"+mappings.getProperty("submitter","Submitter") + "|" + submitter );
            }

            //  Outputting the other parameters added to this.
            for( final Map.Entry< String, String > entry : params.entrySet() ) {
                if( !( entry.getKey().equals( PARAM_TITLE ) ||
                       entry.getKey().equals( PARAM_DESCRIPTION ) ||
                       entry.getKey().equals( PARAM_VERSION ) ||
                       entry.getKey().equals( PARAM_MAPPINGS ) ||
                       entry.getKey().equals( PARAM_PAGE ) ||
                       entry.getKey().startsWith( "_" )
                     ) ) {
                    //  If no mapping has been defined, just ignore it.
                    final String head = mappings.getProperty( entry.getKey(), entry.getKey() );
                    if( head.length() > 0 ) {
                        out.println( "|" + head + "|" + entry.getValue() );
                    }
                }
            }

            out.println();
            out.println( description );
            out.close();

            //  Now create a new page for this bug report
            final String pageName = findNextPage( context, title, params.get( PARAM_PAGE ) );
            final WikiPage newPage = new WikiPage( context.getEngine(), pageName );
            final WikiContext newContext = (WikiContext)context.clone();
            newContext.setPage( newPage );
            context.getEngine().getPageManager().saveText( newContext, str.toString() );

            final MessageFormat formatter = new MessageFormat("");
            formatter.applyPattern( rb.getString("bugreporthandler.new") );
            final String[] args = { "<a href=\""+context.getViewURL(pageName)+"\">"+pageName+"</a>" };

            return formatter.format( args );
        } catch( final RedirectException e ) {
            log.info("Saving not allowed, reason: '"+e.getMessage()+"', can't redirect to "+e.getRedirect());
            throw new PluginException("Saving not allowed, reason: "+e.getMessage());
        } catch( final WikiException e ) {
            log.error( "Unable to save page!", e );
            return rb.getString("bugreporthandler.unable" );
        }
    }

    /**
     *  Finds a free page name for adding the bug report.  Tries to construct a page, and if it's found, adds a number to it
     *  and tries again.
     */
    private synchronized String findNextPage( final WikiContext context, final String title, final String baseName ) {
        final String basicPageName = ( ( baseName != null ) ? baseName : "Bug" ) + MarkupParser.cleanLink( title );
        final WikiEngine engine = context.getEngine();

        String pageName = basicPageName;
        long   lastbug  = 2;
        while( engine.getPageManager().wikiPageExists( pageName ) ) {
            pageName = basicPageName + lastbug++;
        }

        return pageName;
    }

    /**
     *  Just parses a mappings list in the form of "a=b;b=c;c=d".
     *  <p>
     *  FIXME: Should probably be in TextUtil or somewhere.
     */
    private Properties parseMappings( final String mappings ) {
        final Properties props = new Properties();
        if( mappings == null ) {
            return props;
        }

        final StringTokenizer tok = new StringTokenizer( mappings, ";" );
        while( tok.hasMoreTokens() ) {
            final String t = tok.nextToken();
            final int colon = t.indexOf("=");
            final String key;
            final String value;

            if( colon > 0 ) {
                key = t.substring(0,colon);
                value = t.substring(colon+1);
            } else {
                key = t;
                value = "";
            }

            props.setProperty( key, value );
        }
        return props;
    }

}
