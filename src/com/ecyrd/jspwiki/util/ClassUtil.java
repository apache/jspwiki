package com.ecyrd.jspwiki.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ClassUtil
{
    /**
     *  Attempts to find a class from a collection of packages.
     */
    public static Class findClass( List m_packages, String className )
        throws ClassNotFoundException
    {
        ClassLoader loader = ClassUtil.class.getClassLoader();

        try
        {
            return loader.loadClass( className );
        }
        catch( ClassNotFoundException e )
        {
            for( Iterator i = m_packages.iterator(); i.hasNext(); )
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

        throw new ClassNotFoundException("Class not found in search path!");
    }
    
    /**
     *  A shortcut for findClass when you only have a singular package.
     */

    public static Class findClass( String packageName, String className )
        throws ClassNotFoundException
    {
        ArrayList list = new ArrayList();
        list.add( packageName );

        return findClass( list, className );
    }
}
