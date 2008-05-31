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
package com.ecyrd.jspwiki.util;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

import com.ecyrd.jspwiki.WikiException;

/**
 *  Contains useful utilities for class file manipulation.  This is a static class,
 *  so there is no need to instantiate it.
 *
 *  @since 2.1.29.
 */
public final class ClassUtil
{
    private static final Logger log = Logger.getLogger(ClassUtil.class);
    /**
     *  The location of the classmappings.xml document. It will be searched for
     *  in the classpath.  It's value is "{@value}".
     */
    public  static final String MAPPINGS = "/ini/classmappings.xml";
    
    private static Map<String, String> c_classMappings = new Hashtable<String, String>();

    /**
     *  Initialize the class mappings document.
     */
    static
    {
        try
        {
            InputStream is = ClassUtil.class.getResourceAsStream( MAPPINGS );
    
            if( is != null )
            {
                Document doc = new SAXBuilder().build( is );
        
                XPath xpath = XPath.newInstance("/classmappings/mapping");
    
                List nodes = xpath.selectNodes( doc );
            
                for( Iterator i = nodes.iterator(); i.hasNext(); )
                {
                    Element f = (Element) i.next();
                
                    String key = f.getChildText("requestedClass");
                    String className = f.getChildText("mappedClass");
                    
                    c_classMappings.put( key, className );
                    
                    log.debug("Mapped class '"+key+"' to class '"+className+"'");
                }
            }
            else
            {
                log.info("Didn't find class mapping document in "+MAPPINGS);
            }
        }
        catch( Exception ex )
        {
            log.error("Unable to parse mappings document!",ex);
        }
    }

    /**
     * Private constructor to prevent direct instantiation.
     */
    private ClassUtil()
    {
    }
    
    /**
     *  Attempts to find a class from a collection of packages.  This will first
     *  attempt to find the class based on just the className parameter, but
     *  should that fail, will iterate through the "packages" -list, prefixes
     *  the package name to the className, and then tries to find the class
     *  again.
     *
     *  @param packages A List of Strings, containing different package names.
     *  @param className The name of the class to find.
     *  @return The class, if it was found.
     *  @throws ClassNotFoundException if this particular class cannot be found
     *          from the list.
     */
    public static Class findClass( List packages, String className )
        throws ClassNotFoundException
    {
        ClassLoader loader = ClassUtil.class.getClassLoader();

        try
        {
            return loader.loadClass( className );
        }
        catch( ClassNotFoundException e )
        {
            for( Iterator i = packages.iterator(); i.hasNext(); )
            {
                String packageName = (String)i.next();

                try
                {
                    return loader.loadClass( packageName + "." + className );
                }
                catch( ClassNotFoundException ex )
                {
                    // This is okay, we go to the next package.
                }
            }
        }

        throw new ClassNotFoundException("Class '"+className+"' not found in search path!");
    }
    
    /**
     *  A shortcut for findClass when you only have a singular package to search.
     *  It will first attempt to instantiate the class directly from the className,
     *  and will then try to prefix it with the packageName.
     *
     *  @param packageName A package name (such as "com.ecyrd.jspwiki.plugins").
     *  @param className The class name to find.
     *  @return The class, if it was found.
     *  @throws ClassNotFoundException if this particular class cannot be found.
     */

    public static Class findClass( String packageName, String className )
        throws ClassNotFoundException
    {
        ArrayList<String> list = new ArrayList<String>();
        list.add( packageName );

        return findClass( list, className );
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
        Object[] initargs = {};
        return getMappedObject(requestedClass, initargs );
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
     *  @param arg1 Argument for the constructor.
     *  @return An instantiated Object.
     *  @throws WikiException If the class cannot be found or instantiated.
     *  @since 2.5.40
     */
    public static Object getMappedObject( String requestedClass, Object arg1 )
        throws WikiException
    {
        Object[] initargs = { arg1 };
        return getMappedObject(requestedClass, initargs );
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
     *  @param arg1 Argument for the constructor
     *  @param arg2 A second argument for the constructor
     *  @return An instantiated Object.
     *  @throws WikiException If the class cannot be found or instantiated.
     *  @since 2.5.40
     */
    public static Object getMappedObject( String requestedClass, Object arg1, Object arg2 )
        throws WikiException
    {
        Object[] initargs = { arg1, arg2 };
        return getMappedObject(requestedClass, initargs );
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
    public static Object getMappedObject( String requestedClass, Object[] initargs )
        throws WikiException
    {
        try
        {
            Class cl = getMappedClass( requestedClass );
         
            Constructor[] ctors = cl.getConstructors();
            
            //
            //  Try to find the proper constructor by comparing the
            //  initargs array classes and the constructor types.
            //
            for( int c = 0; c < ctors.length; c++ )
            {
                Class[] params = ctors[c].getParameterTypes();
                
                if( params.length == initargs.length )
                {
                    for( int arg = 0; arg < initargs.length; arg++ )
                    {
                        if( params[arg].isAssignableFrom(initargs[arg].getClass()))
                        {
                            //
                            //  Ha, found it!  Instantiating and returning...
                            //
                            return ctors[c].newInstance(initargs);
                        }
                    }
                }
            }
            
            //
            //  No arguments, so we can just call a default constructor and
            //  ignore the arguments.
            //
            Object o = cl.newInstance();
            
            return o;
        }
        catch( InstantiationException e )
        {
            log.info( "Cannot instantiate requested class "+requestedClass, e );
            
            throw new WikiException("Failed to instantiate class "+requestedClass);
        }
        catch (IllegalAccessException e)
        {
            log.info( "Cannot access requested class "+requestedClass, e );
            
            throw new WikiException("Failed to instantiate class "+requestedClass);
        }
        catch (IllegalArgumentException e)
        {
            log.info( "Illegal arguments when constructing new object", e );
            
            throw new WikiException("Failed to instantiate class "+requestedClass);
        }
        catch (InvocationTargetException e)
        {
            log.info( "You tried to instantiate an abstract class "+requestedClass, e );
            
            throw new WikiException("Failed to instantiate class "+requestedClass);
        }
    }

    /**
     *  Finds a mapped class from the c_classMappings list.  If there is no
     *  mappped class, will use the requestedClass.
     *  
     *  @param requestedClass
     *  @return A Class object which you can then instantiate.
     *  @throws WikiException
     */
    private static Class getMappedClass( String requestedClass )
        throws WikiException
    {
        String mappedClass = c_classMappings.get( requestedClass );
        
        if( mappedClass == null )
        {
            mappedClass = requestedClass;
        }
        
        try
        {
            Class cl = Class.forName(mappedClass);
            
            return cl;
        }
        catch (ClassNotFoundException e)
        {
            log.info( "Cannot find requested class", e );
            
            throw new WikiException("Failed to instantiate class "+requestedClass);
        }
    }
}
