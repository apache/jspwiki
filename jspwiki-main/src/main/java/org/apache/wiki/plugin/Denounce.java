/*
    Copyright (C) 2003 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.apache.oro.text.GlobCompiler;
import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.PatternCompiler;
import org.apache.oro.text.regex.PatternMatcher;
import org.apache.oro.text.regex.Perl5Matcher;
import org.apache.wiki.WikiContext;
import org.apache.wiki.api.exceptions.PluginException;
import org.apache.wiki.api.plugin.WikiPlugin;
import org.apache.wiki.util.TextUtil;

/**
 *  Denounces a link by removing it from any search engine.
 *  <br> The bots are listed in org/apache/wiki/plugin/denounce.properties.
 *
 *  <p>Parameters : </p>
 *  <ul>
 *  <li><b>link</b> - The link to be denounced, this parameter is required</li>
 *  <li><b>text</b> - The text to use, defaults to the link</li>
 *  </ul>
 *
 *  @since 2.1.40.
 */
public class Denounce implements WikiPlugin
{
    private static Logger     log = Logger.getLogger(Denounce.class);

    /** Parameter name for setting the link.  Value is <tt>{@value}</tt>. */
    public static final String PARAM_LINK = "link";
    /** Parameter name for setting the text.  Value is <tt>{@value}</tt>. */
    public static final String PARAM_TEXT = "text";

    private static final String PROPERTYFILE = "org/apache/wiki/plugin/denounce.properties";
    private static final String PROP_AGENTPATTERN   = "denounce.agentpattern.";
    private static final String PROP_HOSTPATTERN    = "denounce.hostpattern.";
    private static final String PROP_REFERERPATTERN = "denounce.refererpattern.";

    private static final String PROP_DENOUNCETEXT   = "denounce.denouncetext";

    private static ArrayList<Pattern> c_refererPatterns = new ArrayList<>();
    private static ArrayList<Pattern> c_agentPatterns   = new ArrayList<>();
    private static ArrayList<Pattern> c_hostPatterns    = new ArrayList<>();

    private static String    c_denounceText    = "";

    /**
     *  Prepares the different patterns for later use.  Compiling is
     *  (probably) expensive, so we do it statically at class load time.
     */
    static
    {
        try
        {
            PatternCompiler compiler = new GlobCompiler();
            ClassLoader loader = Denounce.class.getClassLoader();

            InputStream in = loader.getResourceAsStream( PROPERTYFILE );

            if( in == null )
            {
                throw new IOException("No property file found! (Check the installation, it should be there.)");
            }

            Properties props = new Properties();
            props.load( in );

            c_denounceText = props.getProperty( PROP_DENOUNCETEXT, c_denounceText );

            for( Enumeration< ? > e = props.propertyNames(); e.hasMoreElements(); )
            {
                String name = (String) e.nextElement();

                try
                {
                    if( name.startsWith( PROP_REFERERPATTERN ) )
                    {
                        c_refererPatterns.add( compiler.compile( props.getProperty(name) ) );
                    }
                    else if( name.startsWith( PROP_AGENTPATTERN ) )
                    {
                        c_agentPatterns.add( compiler.compile( props.getProperty(name) ) );
                    }
                    else if( name.startsWith( PROP_HOSTPATTERN ) )
                    {
                        c_hostPatterns.add( compiler.compile( props.getProperty(name) ) );
                    }
                }
                catch( MalformedPatternException ex )
                {
                    log.error( "Malformed URL pattern in "+PROPERTYFILE+": "+props.getProperty(name), ex );
                }
            }

            log.debug("Added "+c_refererPatterns.size()+c_agentPatterns.size()+c_hostPatterns.size()+" crawlers to denounce list.");
        }
        catch( IOException e )
        {
            log.error( "Unable to load URL patterns from "+PROPERTYFILE, e );
        }
        catch( Exception e )
        {
            log.error( "Unable to initialize Denounce plugin", e );
        }
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public String execute( WikiContext context, Map<String, String> params )
        throws PluginException
    {
        String link = params.get( PARAM_LINK );
        String text = params.get( PARAM_TEXT );
        boolean linkAllowed = true;

        if( link == null )
        {
            throw new PluginException("Denounce: No parameter "+PARAM_LINK+" defined!");
        }

        HttpServletRequest request = context.getHttpRequest();

        if( request != null )
        {
            linkAllowed = !matchHeaders( request );
        }

        if( text == null ) text = link;

        if( linkAllowed )
        {
            // FIXME: Should really call TranslatorReader
            return "<a href=\""+link+"\">"+ TextUtil.replaceEntities(text) +"</a>";
        }

        return c_denounceText;
    }

    /**
     *  Returns true, if the path is found among the referers.
     */
    private boolean matchPattern( List< Pattern > list, String path )
    {
        PatternMatcher matcher = new Perl5Matcher();

        for( Iterator< Pattern > i = list.iterator(); i.hasNext(); )
        {
            if( matcher.matches( path, i.next() ) )
            {
                return true;
            }
        }

        return false;
    }

    // FIXME: Should really return immediately when a match is found.

    private boolean matchHeaders( HttpServletRequest request )
    {
        //
        //  User Agent
        //

        String userAgent = request.getHeader("User-Agent");

        if( userAgent != null && matchPattern( c_agentPatterns, userAgent ) )
        {
            log.debug("Matched user agent "+userAgent+" for denounce.");
            return true;
        }

        //
        //  Referrer header
        //

        String refererPath = request.getHeader("Referer");

        if( refererPath != null && matchPattern( c_refererPatterns, refererPath ) )
        {
            log.debug("Matched referer "+refererPath+" for denounce.");
            return true;
        }

        //
        //  Host
        //

        String host = request.getRemoteHost();

        if( host != null && matchPattern( c_hostPatterns, host ) )
        {
            log.debug("Matched host "+host+" for denounce.");
            return true;
        }

        return false;
    }
}
