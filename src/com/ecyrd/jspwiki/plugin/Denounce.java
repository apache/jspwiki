/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2003 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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

import com.ecyrd.jspwiki.*;
import com.ecyrd.jspwiki.providers.ProviderException;
import org.apache.log4j.Category;
import org.apache.oro.text.*;
import org.apache.oro.text.regex.*;

import java.util.*;
import java.io.InputStream;
import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

/**
 *  Denounces a link by removing it from any search engine.  The bots are listed
 *  in com/ecyrd/jspwiki/plugin/denounce.properties.
 *
 *  @author Janne Jalkanen
 *  @since 2.1.40.
 */
public class Denounce implements WikiPlugin
{
    private static Category     log = Category.getInstance(Denounce.class);

    public static final String PARAM_LINK = "link";
    public static final String PARAM_TEXT = "text";

    public static final String PROPERTYFILE = "com/ecyrd/jspwiki/plugin/denounce.properties";
    public static final String PROP_AGENTPATTERN   = "denounce.agentpattern.";
    public static final String PROP_HOSTPATTERN    = "denounce.hostpattern.";
    public static final String PROP_REFERERPATTERN = "denounce.refererpattern.";

    public static final String PROP_DENOUNCETEXT   = "denounce.denouncetext";

    private static ArrayList c_refererPatterns = new ArrayList();
    private static ArrayList c_agentPatterns   = new ArrayList();
    private static ArrayList c_hostPatterns    = new ArrayList();

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

            for( Enumeration e = props.propertyNames(); e.hasMoreElements(); )
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

    public String execute( WikiContext context, Map params )
        throws PluginException
    {
        String link = (String) params.get( PARAM_LINK );
        String text = (String) params.get( PARAM_TEXT );
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
            return "<a href=\""+link+"\">"+text+"</a>";
        }

        return c_denounceText;
    }

    /**
     *  Returns true, if the path is found among the referers.
     */
    private boolean matchPattern( List list, String path )
    {
        PatternMatcher matcher = new Perl5Matcher();

        for( Iterator i = list.iterator(); i.hasNext(); )
        {
            if( matcher.matches( path, (Pattern)i.next() ) )
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

        if( matchPattern( c_agentPatterns, userAgent ) )
        {
            log.debug("Matched user agent "+userAgent+" for denounce.");
            return true;
        }

        //
        //  Referrer header
        //

        String refererPath = request.getHeader("Referer");

        if( matchPattern( c_refererPatterns, refererPath ) )
        {
            log.debug("Matched referer "+refererPath+" for denounce.");
            return true;
        }

        //
        //  Host
        // 

        String host = request.getRemoteHost();

        if( matchPattern( c_hostPatterns, host ) )
        {
            log.debug("Matched host "+host+" for denounce.");
            return true;
        }

        return false;
    }
}
