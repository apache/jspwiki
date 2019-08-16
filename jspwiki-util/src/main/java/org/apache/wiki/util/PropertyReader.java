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
package org.apache.wiki.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.log4j.Logger;

import javax.servlet.ServletContext;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;


/**
 * Property Reader for the WikiEngine. Reads the properties for the WikiEngine
 * and implements the feature of cascading properties and variable substitution,
 * which come in handy in a multi wiki installation environment: It reduces the
 * need for (shell) scripting in order to generate different jspwiki.properties
 * to a minimum.
 *
 * @since 2.5.x
 */
public final class PropertyReader {
	
	private static final Logger LOG = Logger.getLogger( PropertyReader.class );
	
    /**
     * Path to the base property file, usually overridden by values provided in
     * a jspwiki-custom.properties file
     * {@value #DEFAULT_JSPWIKI_CONFIG}
     */
    public static final String DEFAULT_JSPWIKI_CONFIG = "/ini/jspwiki.properties";

    /** The servlet context parameter (from web.xml)  that defines where the
     *  config file is to be found.
     *  If it is not defined, checks the Java System Property, if that is not defined either, 
     *  uses the default as defined by DEFAULT_PROPERTYFILE.
     *  {@value #DEFAULT_JSPWIKI_CONFIG}
     */
    public static final String PARAM_CUSTOMCONFIG = "jspwiki.custom.config";

    /**
     *  The prefix when you are cascading properties.  
     *  
     *  @see #loadWebAppProps(ServletContext)
     */
    public static final String PARAM_CUSTOMCONFIG_CASCADEPREFIX = "jspwiki.custom.cascade.";

    public static final String  CUSTOM_JSPWIKI_CONFIG = "/jspwiki-custom.properties";

    private static final String PARAM_VAR_DECLARATION = "var.";
    private static final String PARAM_VAR_IDENTIFIER  = "$";

    /**
     *  Private constructor to prevent instantiation.
     */
    private PropertyReader()
    {}

    /**
     *  Loads the webapp properties based on servlet context information, 
     *  or (if absent) based on the Java System Property PARAM_CUSTOMCONFIG .
     *  Returns a Properties object containing the settings, or null if unable
     *  to load it. (The default file is ini/jspwiki.properties, and can
     *  be customized by setting PARAM_CUSTOMCONFIG in the server or webapp
     *  configuration.)
     *
     *  <h3>Cascading Properties</h3>
     *  <p>
     *  You can define additional property files and merge them into the default
     *  properties file in a similar process to how you define cascading style
     *  sheets; hence we call this <i>cascading property files</i>. This way you
     *  can overwrite the default values and only specify the properties you
     *  need to change in a multiple wiki environment.
     *  <p>
     *  You define a cascade in the context mapping of your servlet container.
     *  <pre>
     *  jspwiki.custom.cascade.1
     *  jspwiki.custom.cascade.2
     *  jspwiki.custom.cascade.3
     *  </pre>
     *  and so on. You have to number your cascade in a descending way starting
     *  with "1". This means you cannot leave out numbers in your cascade. This
     *  method is based on an idea by Olaf Kaus, see [JSPWiki:MultipleWikis].
     *  
     *  @param context A Servlet Context which is used to find the properties
     *  @return A filled Properties object with all the cascaded properties in place
     */
    public static Properties loadWebAppProps( ServletContext context ) {
        String propertyFile = getInitParameter( context, PARAM_CUSTOMCONFIG );

        try( InputStream propertyStream = loadCustomPropertiesFile(context, propertyFile) ) {
            Properties props = getDefaultProperties();
            if( propertyStream == null ) {
                LOG.info("No custom property file found, relying on JSPWiki defaults.");
            } else {
                props.load( propertyStream );
            }

            //this will add additional properties to the default ones:
            LOG.debug( "Loading cascading properties..." );

            //now load the cascade (new in 2.5)
            loadWebAppPropsCascade( context, props );

            //finally expand the variables (new in 2.5)
            expandVars( props );

            return props;
        } catch( Exception e ) {
            LOG.error( "JSPWiki: Unable to load and setup properties from jspwiki.properties. " + e.getMessage() );
        }

        return null;
    }

    /**
     * Figure out where our properties lie.
     * 
     * @param context
     * @param propertyFile
     * @return
     * @throws FileNotFoundException
     */
	static InputStream loadCustomPropertiesFile(ServletContext context, String propertyFile) throws FileNotFoundException {
		InputStream propertyStream;
		if( propertyFile == null ) {
		    LOG.info( "No " + PARAM_CUSTOMCONFIG + " defined for this context, looking for custom properties file with default name of: " + CUSTOM_JSPWIKI_CONFIG );
		    //  Use the custom property file at the default location
		    propertyStream =  locateClassPathResource(context, CUSTOM_JSPWIKI_CONFIG);
		} else {
		    LOG.info( PARAM_CUSTOMCONFIG + " defined, using " + propertyFile + " as the custom properties file." );
		    propertyStream = new FileInputStream( new File(propertyFile) );
		}
		return propertyStream;
	}


    /**
     *  Returns the property set as a Properties object.
     *
     *  @return A property set.
     */
    public static Properties getDefaultProperties() {
        Properties props = new Properties();
        try( InputStream in = PropertyReader.class.getResourceAsStream( DEFAULT_JSPWIKI_CONFIG ) ) {
            if( in != null ) {
                props.load( in );
            }
        } catch( IOException e ) {
            LOG.error( "Unable to load default propertyfile '" + DEFAULT_JSPWIKI_CONFIG + "'" + e.getMessage(), e );
        }
        
        return props;
    }

    /**
     *  Returns a property set consisting of the default Property Set overlaid with a custom property set
     *
     *  @param fileName Reference to the custom override file
     *  @return A property set consisting of the default property set and custom property set, with
     *          the latter's properties replacing the former for any common values
     */
    public static Properties getCombinedProperties( String fileName ) {
        Properties newPropertySet = getDefaultProperties();
        try( InputStream in = PropertyReader.class.getResourceAsStream( fileName ) ) {
            if( in != null ) {
                newPropertySet.load( in );
            } else {
                LOG.error( "*** Custom property file \"" + fileName + "\" not found, relying on default file alone." );
            }
        } catch( IOException e ) {
            LOG.error( "Unable to load propertyfile '" + fileName + "'" + e.getMessage(), e );
        }

        return newPropertySet;
    }

    /**
     *  Returns the ServletContext Init parameter if has been set, otherwise
     *  checks for a System property of the same name. If neither are defined,
     *  returns null. This permits both Servlet- and System-defined cascading
     *  properties.
     */
    private static String getInitParameter( ServletContext context, String name ) {
        String value = context.getInitParameter( name );
        return ( value != null )
                ? value
                : System.getProperty( name ) ;
    }


    /**
     *  Implement the cascade functionality.
     *
     * @param context             where to read the cascade from
     * @param defaultProperties   properties to merge the cascading properties to
     * @since 2.5.x
     */
    private static void loadWebAppPropsCascade( ServletContext context, Properties defaultProperties ) {
        if( getInitParameter( context, PARAM_CUSTOMCONFIG_CASCADEPREFIX + "1" ) == null ) {
            LOG.debug( " No cascading properties defined for this context" );
            return;
        }

        // get into cascade...
        int depth = 0;
        boolean more = true;
        while( more ) {
            depth++;
            String propertyFile = getInitParameter( context, PARAM_CUSTOMCONFIG_CASCADEPREFIX + depth );

            if( propertyFile == null ) {
                more = false;
                break;
            }

            try( InputStream propertyStream = new FileInputStream( new File( propertyFile ) ) ) {
                LOG.info( " Reading additional properties from " + propertyFile + " and merge to cascade." );
                Properties additionalProps = new Properties();
                additionalProps.load( propertyStream );
                defaultProperties.putAll( additionalProps );
            } catch( Exception e ) {
                LOG.error( "JSPWiki: Unable to load and setup properties from " + propertyFile + "." + e.getMessage() );
            }
        }

        return;
    }

    /**
     *  You define a property variable by using the prefix "var.x" as a
     *  property. In property values you can then use the "$x" identifier
     *  to use this variable.
     *
     *  For example you could declare a base directory for all your files
     *  like this and use it in all your other property definitions with
     *  a "$basedir". Note that it does not matter if you define the
     *  variable before its usage.
     *  <pre>
     *  var.basedir = /p/mywiki;
     *  jspwiki.fileSystemProvider.pageDir =         $basedir/www/
     *  jspwiki.basicAttachmentProvider.storageDir = $basedir/www/
     *  jspwiki.workDir =                            $basedir/wrk/
     *  </pre>
     *
     * @param properties - properties to expand;
     */
    public static void expandVars(Properties properties) {
        //get variable name/values from properties...
        Map< String, String > vars = new HashMap< String, String >();
        Enumeration< ? > propertyList = properties.propertyNames();
        while( propertyList.hasMoreElements() ) {
            String propertyName = ( String )propertyList.nextElement();
            String propertyValue = properties.getProperty( propertyName );

            if ( propertyName.startsWith( PARAM_VAR_DECLARATION ) ) {
                String varName = propertyName.substring( 4, propertyName.length() ).trim();
                String varValue = propertyValue.trim();
                vars.put( varName, varValue );
            }
        }

        //now, substitute $ values in property values with vars...
        propertyList = properties.propertyNames();
        while( propertyList.hasMoreElements() ) {
            String propertyName = ( String )propertyList.nextElement();
            String propertyValue = properties.getProperty( propertyName );

            //skip var properties itself...
            if( propertyName.startsWith( PARAM_VAR_DECLARATION ) ) {
                continue;
            }

            Iterator< Map.Entry< String, String > > iter = vars.entrySet().iterator();
            while ( iter.hasNext() ) {
                Map.Entry< String, String > entry = iter.next();
                String varName = entry.getKey();
                String varValue = entry.getValue();

                //replace old property value, using the same variabe. If we don't overwrite
                //the same one the next loop works with the original one again and
                //multiple var expansion won't work...
                propertyValue = TextUtil.replaceString( propertyValue, PARAM_VAR_IDENTIFIER + varName, varValue );

                //add the new PropertyValue to the properties
                properties.put(propertyName, propertyValue);
            }
        }
    }

    /**
     * Locate a resource stored in the class path. Try first with "WEB-INF/classes"
     * from the web app and fallback to "resourceName".
     *
     * @param context the servlet context
     * @param resourceName the name of the resource
     * @return the input stream of the resource or <b>null</b> if the resource was not found
     */
    public static InputStream locateClassPathResource( ServletContext context, String resourceName ) {
        InputStream result;
        String currResourceLocation;

        // garbage in - garbage out
        if( StringUtils.isEmpty( resourceName ) ) {
            return null;
        }

        // try with web app class loader searching in "WEB-INF/classes"
        currResourceLocation = createResourceLocation( "/WEB-INF/classes", resourceName );
        result = context.getResourceAsStream( currResourceLocation );
        if( result != null ) {
            LOG.debug( " Successfully located the following classpath resource : " + currResourceLocation );
            return result;
        }

        // if not found - try with the current class loader and the given name
        currResourceLocation = createResourceLocation( "", resourceName );
        result = PropertyReader.class.getResourceAsStream( currResourceLocation );
        if( result != null ) {
            LOG.debug( " Successfully located the following classpath resource : " + currResourceLocation );
            return result;
        }

        LOG.debug( " Unable to resolve the following classpath resource : " + resourceName );

        return result;
    }

    /**
     * Create a resource location with proper usage of "/".
     *
     * @param path a path
     * @param name a resource name
     * @return a resource location
     */
    static String createResourceLocation( String path, String name ) {
        Validate.notEmpty( name, "name is empty" );
        StringBuilder result = new StringBuilder();

        // strip an ending "/"
        String sanitizedPath = ( path != null && !path.isEmpty() && path.endsWith( "/" ) ? path.substring( 0, path.length() - 1 ) : path );

        // strip leading "/"
        String sanitizedName = ( name.startsWith( "/" ) ? name.substring( 1, name.length() ) : name );

        // append the optional path
        if( sanitizedPath == null || sanitizedPath.isEmpty() ) {
            result.append( "/" );
        } else {
            if( !sanitizedPath.startsWith( "/" ) ) {
                result.append( "/" );
            }
            result.append( sanitizedPath );
            result.append( "/" );
        }

        // append the name
        result.append( sanitizedName );
        return result.toString();
    }

}
