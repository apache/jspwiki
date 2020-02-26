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
package org.apache.wiki.api.core;

import org.apache.log4j.Logger;
import org.apache.wiki.api.exceptions.ProviderException;
import org.apache.wiki.event.WikiEventListener;

import javax.servlet.ServletContext;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Date;
import java.util.Properties;


/**
 *  Provides Wiki services to the JSP page.
 *
 *  <P>
 *  This is the main interface through which everything should go.
 *
 *  <p>
 *  There's basically only a single Engine for each web application, and you should always get it using the {@code Engine.getInstance(..)}
 *  method.
 */
public interface Engine {

    /** The default inlining pattern.  Currently "*.png" */
    String DEFAULT_INLINEPATTERN = "*.png";

    /** The name used for the default template. The value is {@value}. */
    String DEFAULT_TEMPLATE_NAME = "default";

    /** Property for application name */
    String PROP_APPNAME = "jspwiki.applicationName";

    /** This property defines the inline image pattern.  It's current value is {@value} */
    String PROP_INLINEIMAGEPTRN = "jspwiki.translatorReader.inlinePattern";

    /** Property start for any interwiki reference. */
    String PROP_INTERWIKIREF = "jspwiki.interWikiRef.";

    /** If true, then the user name will be stored with the page data.*/
    String PROP_STOREUSERNAME= "jspwiki.storeUserName";

    /** Define the used encoding.  Currently supported are ISO-8859-1 and UTF-8 */
    String PROP_ENCODING = "jspwiki.encoding";

    /** Do not use encoding in WikiJSPFilter, default is false for most servers.
     Double negative, cause for most servers you don't need the property */
    String PROP_NO_FILTER_ENCODING = "jspwiki.nofilterencoding";

    /** Property name for where the jspwiki work directory should be.
     If not specified, reverts to ${java.tmpdir}. */
    String PROP_WORKDIR = "jspwiki.workDir";

    /** The name of the cookie that gets stored to the user browser. */
    String PREFS_COOKIE_NAME = "JSPWikiUserProfile";

    /** Property name for the "match english plurals" -hack. */
    String PROP_MATCHPLURALS = "jspwiki.translatorReader.matchEnglishPlurals";

    /** Property name for the template that is used. */
    String PROP_TEMPLATEDIR = "jspwiki.templateDir";

    /** Property name for the default front page. */
    String PROP_FRONTPAGE = "jspwiki.frontPage";

    /** Property name for setting the url generator instance */
    String PROP_URLCONSTRUCTOR = "jspwiki.urlConstructor";

    /** The name of the property containing the ACLManager implementing class. The value is {@value}. */
    String PROP_ACL_MANAGER_IMPL = "jspwiki.aclManager";

    /** If this property is set to false, we don't allow the creation of empty pages */
    String PROP_ALLOW_CREATION_OF_EMPTY_PAGES = "jspwiki.allowCreationOfEmptyPages";

    /**
     * Adapt Engine to a concrete type.
     *
     * @param cls class denoting the type to adapt to.
     * @param <E> type to adapt to.
     * @return engine instance adapted to the requested type. Might throw an unchecked exception if the instance cannot be adapted to requested type!
     */
    @SuppressWarnings( "unchecked" )
    default < E extends Engine > E adapt( final Class< E > cls ) {
        return ( E )this;
    }

    /**
     * Retrieves the requested object instantiated by the Engine.
     *
     * @param manager requested object instantiated by the Engine.
     * @param <T> type of the requested object.
     * @return requested object instantiated by the Engine, {@code null} if not available.
     */
    < T > T getManager( Class< T > manager );

    /**
     * check if the Engine has been configured.
     *
     * @return {@code true} if it has, {@code false} otherwise.
     */
    boolean isConfigured();

    /**
     *  Returns the set of properties that the Engine was initialized with.  Note that this method returns a direct reference, so it's
     *  possible to manipulate the properties.  However, this is not advised unless you really know what you're doing.
     *
     *  @return The wiki properties
     */
    Properties getWikiProperties();

    /**
     *  Returns the JSPWiki working directory set with "jspwiki.workDir".
     *
     *  @since 2.1.100
     *  @return The working directory.
     */
    String getWorkDir();

    /**
     *  Returns the current template directory.
     *
     *  @since 1.9.20
     *  @return The template directory as initialized by the engine.
     */
    String getTemplateDir();

    /**
     *  Returns the moment when this engine was started.
     *
     *  @since 2.0.15.
     *  @return The start time of this wiki.
     */
    Date getStartTime();

    /**
     *  Returns the base URL, telling where this Wiki actually lives.
     *
     *  @since 1.6.1
     *  @return The Base URL.
     */
    String getBaseURL();

    /**
     *  Returns the URL of the global RSS file.  May be null, if the RSS file generation is not operational.
     *
     *  @since 1.7.10
     *  @return The global RSS url
     */
    String getGlobalRSSURL();

    /**
     *  Returns an URL to some other Wiki that we know.
     *
     *  @param  wikiName The name of the other wiki.
     *  @return null, if no such reference was found.
     */
    String getInterWikiURL( String wikiName );

    /**
     *  Returns an URL if a WikiContext is not available.
     *
     *  @param context The WikiContext (VIEW, EDIT, etc...)
     *  @param pageName Name of the page, as usual
     *  @param params List of parameters. May be null, if no parameters.
     *  @return An URL (absolute or relative).
     */
    String getURL( String context, String pageName, String params );

    /**
     *  Returns the default front page, if no page is used.
     *
     *  @return The front page name.
     */
    String getFrontPage();

    /**
     *  Returns the ServletContext that this particular Engine was initialized with. <strong>It may return {@code null}</strong>,
     *  if the Engine is not running inside a servlet container!
     *
     *  @since 1.7.10
     *  @return ServletContext of the Engine, or {@code null}.
     */
    ServletContext getServletContext();

    /**
     * Looks up and obtains a configuration file inside the WEB-INF folder of a wiki webapp.
     *
     * @param name the file to obtain, <em>e.g.</em>, <code>jspwiki.policy</code>
     * @return the URL to the file
     */
    default URL findConfigFile( final String name ) {
        Logger.getLogger( Engine.class ).info( "looking for " + name + " inside WEB-INF " );
        // Try creating an absolute path first
        File defaultFile = null;
        if( getRootPath() != null ) {
            defaultFile = new File( getRootPath() + "/WEB-INF/" + name );
        }
        if ( defaultFile != null && defaultFile.exists() ) {
            try {
                return defaultFile.toURI().toURL();
            } catch ( final MalformedURLException e ) {
                // Shouldn't happen, but log it if it does
                Logger.getLogger( Engine.class ).warn( "Malformed URL: " + e.getMessage() );
            }
        }

        // Ok, the absolute path didn't work; try other methods
        URL path = null;

        if( getServletContext() != null ) {
            final File tmpFile;
            try {
                tmpFile = File.createTempFile( "temp." + name, "" );
            } catch( final IOException e ) {
                Logger.getLogger( Engine.class ).error( "unable to create a temp file to load onto the policy", e );
                return null;
            }
            tmpFile.deleteOnExit();
            Logger.getLogger( Engine.class ).info( "looking for /" + name + " on classpath" );
            //  create a tmp file of the policy loaded as an InputStream and return the URL to it
            try( final InputStream is = Engine.class.getResourceAsStream( "/" + name );
                    final OutputStream os = new FileOutputStream( tmpFile ) ) {
                if( is == null ) {
                    throw new FileNotFoundException( name + " not found" );
                }
                final URL url = getServletContext().getResource( "/WEB-INF/" + name );
                if( url != null ) {
                    return url;
                }

                final byte[] buff = new byte[1024];
                int bytes;
                while( ( bytes = is.read( buff ) ) != -1 ) {
                    os.write( buff, 0, bytes );
                }

                path = tmpFile.toURI().toURL();
            } catch( final MalformedURLException e ) {
                // This should never happen unless I screw up
                Logger.getLogger( Engine.class ).fatal( "Your code is b0rked.  You are a bad person.", e );
            } catch( final IOException e ) {
                Logger.getLogger( Engine.class ).error( "failed to load security policy from file " + name + ",stacktrace follows", e );
            }
        }
        return path;
    }

    /**
     *  Returns a collection of all supported InterWiki links.
     *
     *  @return A Collection of Strings.
     */
    Collection< String > getAllInterWikiLinks();

    /**
     *  Returns a collection of all image types that get inlined.
     *
     *  @return A Collection of Strings with a regexp pattern.
     */
    Collection< String > getAllInlinedImagePatterns();

    /**
     *  <p>If the page is a special page, then returns a direct URL to that page. Otherwise returns <code>null</code>.
     *  This method delegates requests to {@link org.apache.wiki.ui.CommandResolver#getSpecialPageReference(String)}.</p>
     *  <p>Special pages are defined in jspwiki.properties using the jspwiki.specialPage setting. They're typically used to give Wiki page
     *  names to e.g. custom JSP pages.</p>
     *
     *  @param original The page to check
     *  @return A reference to the page, or null, if there's no special page.
     */
    String getSpecialPageReference( String original );

    /**
     *  Returns the name of the application.
     *
     *  @return A string describing the name of this application.
     */
    String getApplicationName();

    /**
     *  Returns the root path.  The root path is where the Engine is located in the file system.
     *
     *  @since 2.2
     *  @return A path to where the Wiki is installed in the local filesystem.
     */
    String getRootPath();

    /**
     *  Returns the correct page name, or null, if no such page can be found.  Aliases are considered. This method simply delegates to
     *  {@link org.apache.wiki.ui.CommandResolver#getFinalPageName(String)}.
     *
     *  @since 2.0
     *  @param page Page name.
     *  @return The rewritten page name, or null, if the page does not exist.
     *  @throws ProviderException If something goes wrong in the backend.
     */
    String getFinalPageName( String page ) throws ProviderException;

    /**
     *  Turns a WikiName into something that can be called through using an URL.
     *
     *  @since 1.4.1
     *  @param pagename A name. Can be actually any string.
     *  @return A properly encoded name.
     *  @see #decodeName(String)
     */
    String encodeName( String pagename );

    /**
     *  Decodes a URL-encoded request back to regular life.  This properly heeds the encoding as defined in the settings file.
     *
     *  @param pagerequest The URL-encoded string to decode
     *  @return A decoded string.
     *  @see #encodeName(String)
     */
    String decodeName( String pagerequest );

    /**
     *  Returns the IANA name of the character set encoding we're supposed to be using right now.
     *
     *  @since 1.5.3
     *  @return The content encoding (either UTF-8 or ISO-8859-1).
     */
    Charset getContentEncoding();

    /**
     * Registers a WikiEventListener with this instance.
     *
     * @param listener the event listener
     */
    void addWikiEventListener( WikiEventListener listener );

    /**
     * Un-registers a WikiEventListener with this instance.
     *
     * @param listener the event listener
     */
    void removeWikiEventListener( WikiEventListener listener );

    /**
     * Adds an attribute to the engine for the duration of this engine.  The value is not persisted.
     *
     * @since 2.4.91
     * @param key the attribute name
     * @param value the value
     */
    void setAttribute( String key, Object value );

    /**
     *  Gets an attribute from the engine.
     *
     *  @param key the attribute name
     *  @return the value
     */
    < T > T getAttribute( String key );

    /**
     *  Removes an attribute.
     *
     *  @param key The key of the attribute to remove.
     *  @return The previous attribute, if it existed.
     */
    < T > T removeAttribute( String key );

    /**
     * Signals that the Engine will be shut down by the servlet container.
     */
    void shutdown();

}
