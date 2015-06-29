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

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiProvider;
import org.apache.wiki.api.engine.PluginManager;
import org.apache.wiki.api.exceptions.NoRequiredPropertyException;
import org.apache.wiki.api.exceptions.WikiException;
import org.apache.wiki.modules.InternalModule;
import org.jdom2.Element;
import org.picocontainer.DefaultPicoContainer;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.PicoContainer;

/**
 *  Contains useful utilities for class file manipulation.  This is a static class,
 *  so there is no need to instantiate it.
 *
 *  @since 2.1.29.
 */
public final class ClassUtil {

    private static final Logger log = Logger.getLogger(ClassUtil.class);
    /**
     *  The location of the classmappings.xml document. It will be searched for
     *  in the classpath.  It's value is "{@value}".
     */
    public  static final String MAPPINGS = "ini/classmappings.xml";
    
    private static MutablePicoContainer picoContainer = new DefaultPicoContainer();

	private static boolean classLoaderSetup = false;
    private static ClassLoader loader = null;


    /**
     *  Initialize the class mappings document.
     */
    static {
    	List< Element > nodes = XmlUtil.parse( MAPPINGS, "/classmappings/mapping" );

        if( nodes.size() > 0 ) {
            for( Iterator< Element > i = nodes.iterator(); i.hasNext(); ) {
                Element f = i.next();
            
                String key = f.getChildText("requestedClass");
                String className = f.getChildText("mappedClass");

                try {
                	picoContainer.addComponent(Class.forName(className));
                } catch (ClassNotFoundException e) {
                	log.fatal(e,e);
                }
                
                log.debug("Mapped class '"+key+"' to class '"+className+"'");
            }
        } else {
            log.info("Didn't find class mapping document in "+MAPPINGS);
        }
    }

    /**
     * Private constructor to prevent direct instantiation.
     */
    private ClassUtil() {}
    
    /**
     *  Attempts to find a class from a collection of packages.  This will first
     *  attempt to find the class based on just the className parameter, but
     *  should that fail, will iterate through the "packages" -list, prefixes
     *  the package name to the className, and then tries to find the class
     *  again.
     *
     * @param packages A List of Strings, containing different package names.
     *  @param className The name of the class to find.
     * @return The class, if it was found.
     *  @throws ClassNotFoundException if this particular class cannot be found
     *          from the list.
     */
    public static Class<?> findClass( List< String > packages,  List< String > externaljars, String className ) throws ClassNotFoundException {
        if (!classLoaderSetup) {
            loader = setupClassLoader(externaljars);
        }

        try {
            return loader.loadClass( className );
        } catch( ClassNotFoundException e ) {
            for( Iterator< String > i = packages.iterator(); i.hasNext(); ) {
                String packageName = i.next();
                try {
                    return loader.loadClass( packageName + "." + className );
                } catch( ClassNotFoundException ex ) {
                    // This is okay, we go to the next package.
                }
            }

        }

        throw new ClassNotFoundException( "Class '" + className + "' not found in search path!" );
    }

    /**
     * Setup the plugin classloader.
     * Check if there are external JARS to add via property {@link org.apache.wiki.api.engine.PluginManager#PROP_EXTERNALJARS}
     *
     * @return the classloader that can load classes from the configured external jars or
     *         ,if not specified, the classloader that loaded this class.
     * @param externaljars
     */
    private static ClassLoader setupClassLoader(List<String> externaljars) {
        classLoaderSetup = true;
        log.info("setting up classloaders for external (plugin) jars");
        if (externaljars.size() == 0) {
            log.info("no external jars configured, using standard classloading");
            return ClassUtil.class.getClassLoader();
        }
        URL[] urls = new URL[externaljars.size()];
        int i = 0;
        try {
            for (String externaljar : externaljars) {
                File jarFile = new File(externaljar);
                URL ucl = jarFile.toURI().toURL();
                urls[i++] = ucl;
                log.info("added " + ucl + " to list of external jars");
            }
        } catch (MalformedURLException e) {
            log.error("exception while setting up classloaders for external jars via property" + PluginManager.PROP_EXTERNALJARS + ", continuing without external jars.");
            return ClassUtil.class.getClassLoader();
        }
        return new URLClassLoader(urls, ClassUtil.class.getClassLoader());
    }

    /**
     *
     *  It will first attempt to instantiate the class directly from the className,
     *  and will then try to prefix it with the packageName.
     *
     *  @param packageName A package name (such as "org.apache.wiki.plugins").
     *  @param className The class name to find.
     *  @return The class, if it was found.
     *  @throws ClassNotFoundException if this particular class cannot be found.
     */

    public static Class<?> findClass(String packageName, String className) throws ClassNotFoundException {
        try {
            return ClassUtil.class.getClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
            return ClassUtil.class.getClassLoader().loadClass(packageName + "." + className);
        }
    }
    
    /**
     * Lists all the files in classpath under a given package.
     * 
     * @param rootPackage the base package. Can be {code null}.
     * @return all files entries in classpath under the given package
     */
    public static List< String > classpathEntriesUnder( final String rootPackage ) 
    {
        List< String > results = new ArrayList< String >();
        Enumeration< URL > en = null;
        if( StringUtils.isNotEmpty( rootPackage ) ) {
            try
            {
                en = ClassUtil.class.getClassLoader().getResources( rootPackage );
            }
            catch( IOException e )
            {
                log.error( e.getMessage(), e );
            }
        }
        
        while( en != null && en.hasMoreElements() )
        {
            URL url = en.nextElement();
            try
            {
                if( "jar".equals( url.getProtocol() ) ) 
                {
                    jarEntriesUnder( results, ( JarURLConnection )url.openConnection(), rootPackage );
                } 
                else if( "file".equals( url.getProtocol() ) ) 
                {
                    fileEntriesUnder( results, new File( url.getFile() ), rootPackage );
                }
                
            }
            catch (IOException ioe)
            {
                log.error( ioe.getMessage(), ioe );
            }
        }
        return results;
    }
    
    /**
     * Searchs for all the files in classpath under a given package, for a given {@link File}. If the 
     * {@link File} is a directory all files inside it are stored, otherwise the {@link File} itself is
     * stored
     * 
     * @param results collection in which the found entries are stored
     * @param file given {@link File} to search in.
     * @param rootPackage base package.
     */
    static void fileEntriesUnder( List< String > results, File file, String rootPackage ) 
    {
        log.debug( "scanning [" + file.getName() +"]" );
        if( file.isDirectory() ) {
            Iterator< File > files = FileUtils.iterateFiles( file, null, true );
            while( files.hasNext() ) 
            {
                File subfile = files.next();
                // store an entry similar to the jarSearch(..) below ones
                String entry = StringUtils.replace( subfile.getAbsolutePath(), file.getAbsolutePath() + File.separatorChar, StringUtils.EMPTY );
                results.add( rootPackage + "/" + entry );
            }
        } else {
            results.add( file.getName() );
        }
    }
    
    /**
     * Searchs for all the files in classpath under a given package, for a given {@link JarURLConnection}.
     * 
     * @param results collection in which the found entries are stored
     * @param jurlcon given {@link JarURLConnection} to search in.
     * @param rootPackage base package.
     */
    static void jarEntriesUnder( List< String > results, JarURLConnection jurlcon, String rootPackage )
    {
        JarFile jar = null;
        try
        {
            jar = jurlcon.getJarFile();
            log.debug( "scanning [" + jar.getName() +"]" );
            Enumeration< JarEntry > entries = jar.entries();
            while( entries.hasMoreElements() )
            {
                JarEntry entry = entries.nextElement();
                if( entry.getName().startsWith( rootPackage ) && !entry.isDirectory() ) 
                {
                    results.add( entry.getName() );
                }
            }
        }
        catch( IOException ioe )
        {
            log.error( ioe.getMessage(), ioe );
        }
        finally 
        {
            if (jar != null)
            {
                try
                {
                    jar.close();
                }
                catch( IOException ioe )
                {
                    log.error( ioe.getMessage(), ioe );
                }
            }
        }
    }
    
    public static <T extends InternalModule> T getInternalModule( Class<T> classType, WikiEngine engine, Properties props ) throws WikiException {
    	InternalModule module = (InternalModule) getPicoContainer().getComponent(classType);
    	module.initialize(engine, props);
    	return classType.cast(module);
    }
    
    public static <T extends WikiProvider> T getWikiProvider( Class<T> classType, WikiEngine engine, Properties props, String packageName, String providerClassName, WikiProvider defaultProvider, boolean throwException) throws WikiException {
    	WikiProvider provider = defaultProvider;
    	
    	String defaultProviderClassName = defaultProvider != null ? defaultProvider.getClass().getName() : null;
        String errorMessageLoad = "Failed loading WikiProvider "+providerClassName+", will use "+defaultProviderClassName+". ";
        
        try {
            Class<?> providerClass = ClassUtil.findClass(packageName, providerClassName);
            provider = (WikiProvider) providerClass.newInstance();
        } catch (ClassNotFoundException e) {
            log.warn("no class for WikiProvider "+providerClassName+" "+e.getMessage(), e);
            if (throwException) { throw new WikiException(errorMessageLoad+e.getMessage()); }
        } catch (InstantiationException e) {
        	log.warn("Faulty class WikiProvider "+providerClassName+" "+e.getMessage(), e);
            if (throwException) { throw new WikiException(errorMessageLoad+e.getMessage()); }
        } catch (IllegalAccessException e) {
        	log.warn("Illegal class WikiProvider "+providerClassName+" "+e.getMessage(), e);
            if (throwException) { throw new WikiException(errorMessageLoad+e.getMessage()); }
        }

        if (null == provider) {
        	provider = defaultProvider;
        } else {
	        String errorMessageInit = "Failed initializing WikiProvider "+providerClassName+", will use "+defaultProviderClassName+". ";
	        try {
	        	provider.initialize(engine, props);
	        } catch (NoRequiredPropertyException e) {
	            log.warn(errorMessageInit+e.getMessage(), e);
	            if (throwException) { throw new WikiException(errorMessageInit+e.getMessage()); }
	            provider = defaultProvider;
	        } catch (WikiException e) {
	            log.warn(errorMessageInit+e.getMessage(), e);
	            if (throwException) { throw new WikiException(errorMessageInit+e.getMessage()); }
	            provider = defaultProvider;
	        }
        }
        
        return classType.cast(provider);
    }

    /**
     *  This method is used to locate and instantiate a mapped class.
     *  You may redefine anything in the resource file which is located in your classpath
     *  under the name <code>ClassUtil.MAPPINGS ({@value #MAPPINGS})</code>.
     *  <p>
     *  This is an extremely powerful system, which allows you to remap many of
     *  the JSPWiki core classes to your own class.  Please read the documentation
     *  included in the default <code>{@value #MAPPINGS}</code> file to see
     *  how this method works. 
     *  
     *  @param requestedClass The name of the class you wish to instantiate.
     *  @return An instantiated Object.
     *  @throws WikiException If the class cannot be found or instantiated.
     *  @since 2.5.40
     */
    public static Object getMappedObject( String requestedClass )
        throws WikiException
    {
//    	WikiEngine engine = WikiEngine.getInstance(context,null);
//    	Properties props = engine.getWikiProperties();
        return getMappedObject(requestedClass, null, null );
    }
    
    /**
     *  This method is used to locate and instantiate a mapped class.
     *  You may redefine anything in the resource file which is located in your classpath
     *  under the name <code>{@value #MAPPINGS}</code>.
     *  <p>
     *  This is an extremely powerful system, which allows you to remap many of
     *  the JSPWiki core classes to your own class.  Please read the documentation
     *  included in the default <code>{@value #MAPPINGS}</code> file to see
     *  how this method works. 
     *  <p>
     *  This method takes in an object array for the constructor arguments for classes
     *  which have more than two constructors.
     *  
     *  @param requestedClass The name of the class you wish to instantiate.
     *  @param initargs The parameters to be passed to the constructor. May be <code>null</code>.
     *  @return An instantiated Object.
     *  @throws WikiException If the class cannot be found or instantiated.  The error is logged.
     *  @since 2.5.40
     */
    public static Object getMappedObject( String requestedClass, WikiEngine engine, Properties props )
        throws WikiException
    {
    	try {
    		Object o = ClassUtil.getPicoContainer().getComponent(Class.forName(requestedClass));
			if (engine != null && props != null && o instanceof InternalModule) {
				((InternalModule)o).initialize(engine, props);
            }
    		return o;
    	} catch (ClassNotFoundException e) {
    		throw new WikiException(e.getMessage());
    	}
    }
    
    public static PicoContainer getPicoContainer() {
		return picoContainer;
	}
}
