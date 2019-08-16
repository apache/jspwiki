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

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.jdom2.Element;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

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
    
    private static Map<String, String> c_classMappings = new ConcurrentHashMap<>();

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
                
                c_classMappings.put( key, className );
                
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
     * Setup the plugin classloader, checking if there are external JARS to add.
     * 
     * @param externaljars external jars to load into the classloader.
     * @return the classloader that can load classes from the configured external jars or, if not specified, the classloader that loaded this class.
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
        for( String externaljar : externaljars ) {
            try {
                File jarFile = new File( externaljar );
                URL ucl = jarFile.toURI().toURL();
                urls[i++] = ucl;
                log.info("added " + ucl + " to list of external jars");
            } catch (MalformedURLException e) {
                log.error("exception (" + e.getMessage() +") while setting up classloaders for external jar:" + externaljar + ", continuing without external jars.");
            }
        }
        
        if( i == 0 ) {
            log.error( "all external jars threw an exception while setting up classloaders for them, continuing with standard classloading. " + 
                       "See https://jspwiki-wiki.apache.org/Wiki.jsp?page=InstallingPlugins for help on how to install custom plugins." );
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
        List< String > results = new ArrayList< >();
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
    static void jarEntriesUnder( List< String > results, JarURLConnection jurlcon, String rootPackage ) {
        try( JarFile jar = jurlcon.getJarFile() ) {
            log.debug( "scanning [" + jar.getName() +"]" );
            Enumeration< JarEntry > entries = jar.entries();
            while( entries.hasMoreElements() ) {
                JarEntry entry = entries.nextElement();
                if( entry.getName().startsWith( rootPackage ) && !entry.isDirectory() ) {
                    results.add( entry.getName() );
                }
            }
        } catch( IOException ioe ) {
            log.error( ioe.getMessage(), ioe );
        }
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
     *  @throws IllegalArgumentException If the class cannot be found or instantiated. 
     *  @throws ReflectiveOperationException If the class cannot be found or instantiated.
     *  @since 2.5.40
     */
    @SuppressWarnings("unchecked")
    public static < T > T getMappedObject( String requestedClass )
        throws ReflectiveOperationException, IllegalArgumentException
    {
        Object[] initargs = {};
        return ( T )getMappedObject(requestedClass, initargs );
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
     *  @throws IllegalArgumentException If the class cannot be found or instantiated. 
     *  @throws ReflectiveOperationException If the class cannot be found or instantiated.
     *  @since 2.5.40
     */
    @SuppressWarnings( "unchecked" )
    public static < T > T getMappedObject( String requestedClass, Object... initargs )
        throws ReflectiveOperationException, IllegalArgumentException
    {
        Class<?> cl = getMappedClass( requestedClass );
        Constructor<?>[] ctors = cl.getConstructors();
        
        //
        //  Try to find the proper constructor by comparing the
        //  initargs array classes and the constructor types.
        //
        for( int c = 0; c < ctors.length; c++ )
        {
            Class<?>[] params = ctors[c].getParameterTypes();
            
            if( params.length == initargs.length )
            {
                for( int arg = 0; arg < initargs.length; arg++ )
                {
                    if( params[arg].isAssignableFrom(initargs[arg].getClass()))
                    {
                        //
                        //  Ha, found it!  Instantiating and returning...
                        //
                        return ( T )ctors[c].newInstance(initargs);
                    }
                }
            }
        }
        
        //
        //  No arguments, so we can just call a default constructor and ignore the arguments.
        //
        return ( T )cl.getDeclaredConstructor().newInstance();
    }

    /**
     *  Finds a mapped class from the c_classMappings list.  If there is no
     *  mappped class, will use the requestedClass.
     *  
     *  @param requestedClass
     *  @return A Class object which you can then instantiate.
     *  @throws ClassNotFoundException
     */
    public static Class< ? > getMappedClass( String requestedClass ) throws ClassNotFoundException {
        String mappedClass = c_classMappings.get( requestedClass );
        
        if( mappedClass == null )
        {
            mappedClass = requestedClass;
        }
        
        Class< ? > cl = Class.forName(mappedClass);
        
        return cl;
    }
    
    /**
     * checks if {@code srcClassName} is a subclass of {@code parentClassname}.
     * 
     * @param srcClassName expected subclass.
     * @param parentClassName expected parent class.
     * @return {@code true} if {@code srcClassName} is a subclass of {@code parentClassname}, {@code false} otherwise.
     */
    public static boolean assignable( String srcClassName, String parentClassName ) {
        try {
            Class< ? > src = Class.forName( srcClassName );
            Class< ? > parent = Class.forName( parentClassName );
            return parent.isAssignableFrom( src );
        } catch( Exception e ) {
            log.error( e.getMessage(), e );
        }
        return false;
    }
    
}
