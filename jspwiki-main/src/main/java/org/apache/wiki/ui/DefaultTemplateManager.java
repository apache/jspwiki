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
package org.apache.wiki.ui;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.wiki.InternalWikiException;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.modules.BaseModuleManager;
import org.apache.wiki.modules.WikiModuleInfo;
import org.apache.wiki.preferences.Preferences;
import org.apache.wiki.preferences.Preferences.TimeFormat;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;


/**
 *  This class takes care of managing JSPWiki templates.  This class also provides the ResourceRequest mechanism.
 *
 *  @since 2.1.62
 */
public class DefaultTemplateManager extends BaseModuleManager implements TemplateManager {

    private static final Logger log = LogManager.getLogger( DefaultTemplateManager.class );

    /**
     *  Creates a new TemplateManager.  There is typically one manager per engine.
     *
     *  @param engine The owning engine.
     *  @param properties The property list used to initialize this.
     */
    public DefaultTemplateManager( final Engine engine, final Properties properties ) {
        super( engine );
    }

    /** {@inheritDoc} */
    @Override
    // FIXME: Does not work yet
    public boolean templateExists( final String templateName ) {
        final ServletContext context = m_engine.getServletContext();
        try( final InputStream in = context.getResourceAsStream( getPath( templateName ) + "ViewTemplate.jsp" ) ) {
            if( in != null ) {
                return true;
            }
        } catch( final IOException e ) {
            log.error( e.getMessage(), e );
        }
        return false;
    }

    /**
     *  Tries to locate a given resource from the template directory. If the given resource is not found under the current name, returns the
     *  path to the corresponding one in the default template.
     *
     *  @param sContext The servlet context
     *  @param name The name of the resource
     *  @return The name of the resource which was found.
     */
    private static String findResource( final ServletContext sContext, final String name ) {
        String resourceName = name;
        try( final InputStream is = sContext.getResourceAsStream( resourceName ) ) {
            if( is == null ) {
                final String defname = makeFullJSPName( DEFAULT_TEMPLATE, removeTemplatePart( resourceName ) );
                try( final InputStream iis = sContext.getResourceAsStream( defname ) ) {
                    resourceName = iis != null ? defname : null;
                }
            }
        } catch( final IOException e ) {
            log.error( "unable to open " + name + " as resource stream", e );
        }
        return resourceName;
    }

    /**
     *  Attempts to find a resource from the given template, and if it's not found
     *  attempts to locate it from the default template.
     * @param sContext servlet context used to search for the resource
     * @param template template used to search for the resource
     * @param name resource name
     * @return the Resource for the given template and name.
     */
    private static String findResource( final ServletContext sContext, final String template, final String name ) {
        if( name.charAt(0) == '/' ) {
            // This is already a full path
            return findResource( sContext, name );
        }
        final String fullname = makeFullJSPName( template, name );
        return findResource( sContext, fullname );
    }

    /** {@inheritDoc} */
    @Override
    public String findJSP( final PageContext pageContext, final String name ) {
        final ServletContext sContext = pageContext.getServletContext();
        return findResource( sContext, name );
    }

    /**
     *  Removes the template part of a name.
     */
    private static String removeTemplatePart( String name ) {
        int idx = 0;
        if( name.startsWith( "/" ) ) {
            idx = 1;
        }

        idx = name.indexOf('/', idx);
        if( idx != -1 ) {
            idx = name.indexOf('/', idx+1); // Find second "/"
            if( idx != -1 ) {
                name = name.substring( idx+1 );
            }
        }

        if( log.isDebugEnabled() ) {
            log.debug( "Final name = "+name );
        }
        return name;
    }

    /**
     *  Returns the full name (/templates/foo/bar) for name=bar, template=foo.
     *
     * @param template The name of the template.
     * @param name The name of the resource.
     * @return The full name for a template.
     */
    private static String makeFullJSPName( final String template, final String name ) {
        return "/" + DIRECTORY + "/" + template + "/" + name;
    }

    /** {@inheritDoc} */
    @Override
    public String findJSP( final PageContext pageContext, final String template, final String name ) {
        if( name == null || template == null ) {
            log.fatal("findJSP() was asked to find a null template or name (" + template + "," + name + ")." + " JSP page '" +
                      ( ( HttpServletRequest )pageContext.getRequest() ).getRequestURI() + "'" );
            throw new InternalWikiException( "Illegal arguments to findJSP(); please check logs." );
        }

        return findResource( pageContext.getServletContext(), template, name );
    }

    /** {@inheritDoc} */
    @Override
    public String findResource( final Context ctx, final String template, final String name ) {
        if( m_engine.getServletContext() != null ) {
            return findResource( m_engine.getServletContext(), template, name );
        }

        return getPath( template ) + "/" + name;
    }

    /*
     *  Returns a property, as defined in the template.  The evaluation is lazy, i.e. the properties are not loaded until the template is
     *  actually used for the first time.
     */
    /*
    public String getTemplateProperty( WikiContext context, String key )
    {
        String template = context.getTemplate();

        try
        {
            Properties props = (Properties)m_propertyCache.getFromCache( template, -1 );

            if( props == null )
            {
                try
                {
                    props = getTemplateProperties( template );

                    m_propertyCache.putInCache( template, props );
                }
                catch( IOException e )
                {
                    log.warn("IO Exception while reading template properties",e);

                    return null;
                }
            }

            return props.getProperty( key );
        }
        catch( NeedsRefreshException ex )
        {
            // FIXME
            return null;
        }
    }
*/
    /**
     *  Returns an absolute path to a given template.
     */
    private static String getPath( final String template ) {
        return "/" + DIRECTORY + "/" + template + "/";
    }

    /** {@inheritDoc} */
    @Override
    public Set< String > listSkins( final PageContext pageContext, final String template ) {
        final String place = makeFullJSPName( template, SKIN_DIRECTORY );
        final ServletContext sContext = pageContext.getServletContext();
        final Set< String > skinSet = sContext.getResourcePaths( place );
        final Set< String > resultSet = new TreeSet<>();

        if( log.isDebugEnabled() ) {
            log.debug( "Listings skins from " + place );
        }

        if( skinSet != null ) {
            final String[] skins = skinSet.toArray( new String[]{} );
            for( final String skin : skins ) {
                final String[] s = StringUtils.split( skin, "/" );
                if( s.length > 2 && skin.endsWith( "/" ) ) {
                    final String skinName = s[ s.length - 1 ];
                    resultSet.add( skinName );
                    if( log.isDebugEnabled() ) {
                        log.debug( "...adding skin '" + skinName + "'" );
                    }
                }
            }
        }

        return resultSet;
    }


    /** {@inheritDoc} */
    @Override
    public Map< String, String > listTimeFormats( final PageContext pageContext ) {
        final Context context = Context.findContext( pageContext );
        final Properties props = m_engine.getWikiProperties();
        final ArrayList< String > tfArr = new ArrayList<>(40);
        final LinkedHashMap< String, String > resultMap = new LinkedHashMap<>();

        /* filter timeformat properties */
        for( final Enumeration< ? > e = props.propertyNames(); e.hasMoreElements(); ) {
            final String name = ( String )e.nextElement();
            if( name.startsWith( TIMEFORMATPROPERTIES ) ) {
                tfArr.add( name );
            }
        }

        /* fetch actual formats */
        if( tfArr.size() == 0 )  {/* no props found - make sure some default formats are avail */
            tfArr.add( "dd-MMM-yy" );
            tfArr.add( "d-MMM-yyyy" );
            tfArr.add( "EEE, dd-MMM-yyyy, zzzz" );
        } else {
            Collections.sort( tfArr );

            for (int i = 0; i < tfArr.size(); i++) {
                tfArr.set(i, props.getProperty(tfArr.get(i)));
            }
        }

        final String prefTimeZone = Preferences.getPreference( context, "TimeZone" );
        final TimeZone tz = TimeZone.getTimeZone( prefTimeZone );

        final Date d = new Date(); // current date
        try {
            // dummy format pattern
            final SimpleDateFormat fmt = Preferences.getDateFormat( context, TimeFormat.DATETIME );
            fmt.setTimeZone( tz );

            for( final String s : tfArr ) {
                try {
                    final String f = s;
                    fmt.applyPattern( f );
                    resultMap.put( f, fmt.format( d ) );
                } catch( final IllegalArgumentException e ) {
                } // skip parameter
            }
        } catch( final IllegalArgumentException e ) {} // skip parameter

        return resultMap;
    }

    /*
     *  Always returns a valid property map.
     */
    /*
    private Properties getTemplateProperties( String templateName )
        throws IOException
    {
        Properties p = new Properties();

        ServletContext context = m_engine.getServletContext();

        InputStream propertyStream = context.getResourceAsStream(getPath(templateName)+PROPERTYFILE);

        if( propertyStream != null )
        {
            p.load( propertyStream );

            propertyStream.close();
        }
        else
        {
            log.debug("Template '"+templateName+"' does not have a propertyfile '"+PROPERTYFILE+"'.");
        }

        return p;
    }
*/

    /** {@inheritDoc} */
    @Override
    public Collection< WikiModuleInfo > modules() {
        return new ArrayList<>();
    }

    /** {@inheritDoc} */
    @Override
    public WikiModuleInfo getModuleInfo( final String moduleName ) {
    	return null;
    }

}
