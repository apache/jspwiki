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

    /** The location of the classmappings.xml document. It will be searched for in the classpath.  It's value is "{@value}". */
    public  static final String MAPPINGS = "ini/classmappings.xml";

    /** The location of the classmappings-extra.xml document. It will be searched for in the classpath.  It's value is "{@value}". */
    public  static final String MAPPINGS_EXTRA = "ini/classmappings-extra.xml";

    /** Initialize the class mappings document. */
    private static Map< String, String > c_classMappings = populateClassMappingsFrom( MAPPINGS );

    /** Initialize the class mappings extra document. */
    private static Map< String, String > c_classMappingsExtra = populateClassMappingsFrom( MAPPINGS_EXTRA ) ;

    private static boolean classLoaderSetup = false;
    private static ClassLoader loader = null;

    private static Map< String, String > populateClassMappingsFrom( final String fileLoc ) {
        final Map< String, String > map = new ConcurrentHashMap<>();
        final List< Element > nodes = XmlUtil.parse( fileLoc, "/classmappings/mapping" );

        if( nodes.size() > 0 ) {
            for( final Element f : nodes ) {
                final String key = f.getChildText( "requestedClass" );
                final String className = f.getChildText( "mappedClass" );

                map.put( key, className );
                log.debug( "Mapped class '" + key + "' to class '" + className + "'" );
            }
        } else {
            log.info( "Didn't find class mapping document in " + MAPPINGS );
        }
        return map;
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
     *  @throws ClassNotFoundException if this particular class cannot be found from the list.
     */
    public static Class<?> findClass( final List< String > packages,  final List< String > externaljars, final String className ) throws ClassNotFoundException {
        if (!classLoaderSetup) {
            loader = setupClassLoader(externaljars);
        }

        try {
            return loader.loadClass( className );
        } catch( final ClassNotFoundException e ) {
            for( final String packageName : packages ) {
                try {
                    return loader.loadClass( packageName + "." + className );
                } catch( final ClassNotFoundException ex ) {
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
    private static ClassLoader setupClassLoader( final List< String > externaljars) {
        classLoaderSetup = true;
        log.info( "setting up classloaders for external (plugin) jars" );
        if( externaljars.size() == 0 ) {
            log.info( "no external jars configured, using standard classloading" );
            return ClassUtil.class.getClassLoader();
        }
        final URL[] urls = new URL[externaljars.size()];
        int i = 0;
        for( final String externaljar : externaljars ) {
            try {
                final File jarFile = new File( externaljar );
                final URL ucl = jarFile.toURI().toURL();
                urls[ i++ ] = ucl;
                log.info( "added " + ucl + " to list of external jars" );
            } catch( final MalformedURLException e ) {
                log.error( "exception (" + e.getMessage() + ") while setting up classloaders for external jar:" + externaljar + ", continuing without external jars." );
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
     *  It will first attempt to instantiate the class directly from the className, and will then try to prefix it with the packageName.
     *
     *  @param packageName A package name (such as "org.apache.wiki.plugins").
     *  @param className The class name to find.
     *  @return The class, if it was found.
     *  @throws ClassNotFoundException if this particular class cannot be found.
     */
    public static Class< ? > findClass( final String packageName, final String className ) throws ClassNotFoundException {
        try {
            return ClassUtil.class.getClassLoader().loadClass( className );
        } catch( final ClassNotFoundException e ) {
            return ClassUtil.class.getClassLoader().loadClass( packageName + "." + className );
        }
    }
    
    /**
     * Lists all the files in classpath under a given package.
     * 
     * @param rootPackage the base package. Can be {code null}.
     * @return all files entries in classpath under the given package
     */
    public static List< String > classpathEntriesUnder( final String rootPackage ) {
        final List< String > results = new ArrayList<>();
        Enumeration< URL > en = null;
        if( StringUtils.isNotEmpty( rootPackage ) ) {
            try {
                en = ClassUtil.class.getClassLoader().getResources( rootPackage );
            } catch( final IOException e ) {
                log.error( e.getMessage(), e );
            }
        }
        
        while( en != null && en.hasMoreElements() ) {
            final URL url = en.nextElement();
            try {
                if( "jar".equals( url.getProtocol() ) ) {
                    jarEntriesUnder( results, ( JarURLConnection )url.openConnection(), rootPackage );
                } else if( "file".equals( url.getProtocol() ) ) {
                    fileEntriesUnder( results, new File( url.getFile() ), rootPackage );
                }
                
            } catch( final IOException ioe ) {
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
    static void fileEntriesUnder( final List< String > results, final File file, final String rootPackage ) {
        log.debug( "scanning [" + file.getName() + "]" );
        if( file.isDirectory() ) {
            final Iterator< File > files = FileUtils.iterateFiles( file, null, true );
            while( files.hasNext() ) {
                final File subfile = files.next();
                // store an entry similar to the jarSearch(..) below ones
                final String entry = StringUtils.replace( subfile.getAbsolutePath(), file.getAbsolutePath() + File.separatorChar, StringUtils.EMPTY );
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
    static void jarEntriesUnder( final List< String > results, final JarURLConnection jurlcon, final String rootPackage ) {
        try( final JarFile jar = jurlcon.getJarFile() ) {
            log.debug( "scanning [" + jar.getName() +"]" );
            final Enumeration< JarEntry > entries = jar.entries();
            while( entries.hasMoreElements() ) {
                final JarEntry entry = entries.nextElement();
                if( entry.getName().startsWith( rootPackage ) && !entry.isDirectory() ) {
                    results.add( entry.getName() );
                }
            }
        } catch( final IOException ioe ) {
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
    public static < T > T getMappedObject( final String requestedClass ) throws ReflectiveOperationException, IllegalArgumentException {
        final Object[] initargs = {};
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
    public static < T > T getMappedObject( final String requestedClass, final Object... initargs ) throws ReflectiveOperationException, IllegalArgumentException {
        final Class< ? > cl = getMappedClass( requestedClass );
        final Constructor< ? >[] ctors = cl.getConstructors();
        
        //  Try to find the proper constructor by comparing the initargs array classes and the constructor types.
        for( final Constructor< ? > ctor : ctors ) {
            final Class< ? >[] params = ctor.getParameterTypes();
            if( params.length == initargs.length ) {
                for( int arg = 0; arg < initargs.length; arg++ ) {
                    if( params[ arg ].isAssignableFrom( initargs[ arg ].getClass() ) ) {
                        //  Ha, found it!  Instantiating and returning...
                        return ( T )ctor.newInstance( initargs );
                    }
                }
            }
        }
        
        //  No arguments, so we can just call a default constructor and ignore the arguments.
        return ( T )cl.getDeclaredConstructor().newInstance();
    }

    /**
     *  Finds a mapped class from the c_classMappings list.  If there is no mappped class, will use the requestedClass.
     *  
     *  @param requestedClass requested class.
     *  @return A Class object which you can then instantiate.
     *  @throws ClassNotFoundException if the class is not found.
     */
    public static Class< ? > getMappedClass( final String requestedClass ) throws ClassNotFoundException {
        String mappedClass = c_classMappings.get( requestedClass );
        if( mappedClass == null ) {
            mappedClass = requestedClass;
        }
        
        return Class.forName( mappedClass );
    }
    
    /**
     * checks if {@code srcClassName} is a subclass of {@code parentClassname}.
     * 
     * @param srcClassName expected subclass.
     * @param parentClassName expected parent class.
     * @return {@code true} if {@code srcClassName} is a subclass of {@code parentClassname}, {@code false} otherwise.
     */
    public static boolean assignable( final String srcClassName, final String parentClassName ) {
        try {
            final Class< ? > src = Class.forName( srcClassName );
            final Class< ? > parent = Class.forName( parentClassName );
            return parent.isAssignableFrom( src );
        } catch( final Exception e ) {
            log.error( e.getMessage(), e );
        }
        return false;
    }

    /**
     * Checks if a given class exists in classpath.
     *
     * @param className the class to check for existence.
     * @return {@code true} if it exists, {@code false} otherwise.
     */
    public static boolean exists( final String className ) {
        try {
            Class.forName( className, false, ClassUtil.class.getClassLoader() );
            return true;
        } catch( final ClassNotFoundException e ) {
            return false;
        }
    }

    public static Map< String, String > getExtraClassMappings() {
        return c_classMappingsExtra;
    }
    
}
