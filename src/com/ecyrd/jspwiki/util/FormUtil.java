/*
    WikiForms - a WikiPage FORM handler for JSPWiki.
 
    Copyright (C) 2003 BaseN. 

    JSPWiki Copyright (C) 2002 Janne Jalkanen (Janne.Jalkanen@iki.fi)

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published
    by the Free Software Foundation; either version 2.1 of the License, or
    (at your option) any later version.
 
    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.
 
    You should have received a copy of the GNU Lesser General Public License
    along with this program; if not, write to the Free Software
*/
package com.ecyrd.jspwiki.util;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

/**
 * A collection of (static) utilities used by the WikiForms code.
 * FormUtil is mainly concerned with mapping HTTP parameters to
 * WikiPlugin parameters.
 *
 * @author ebu
 */
public class FormUtil
{
    /**
     * Looks for a named value in the Map. Returns either the
     * value named by key, or values named by key.0, key.1, ...
     * if the direct value is not found. The values are packed
     * in an ArrayList.
     *
     * <p>This is a utility method, mainly used when we don't know
     * whether there was just one value, or several, in a mapping list
     * (e.g. an HttpRequest / FORM checkbox).
     */
    public static ArrayList getValues( Map params, String key )
    {
        if( params == null || key == null )
            return( new ArrayList(0) );

        Object entry = params.get( key );
        if( entry != null )
        {
            ArrayList rval = new ArrayList(1);
            rval.add( entry );
            return( rval );
        }

        return( getNumberedValues( params, key ) );
    }


    /**
     * Looks up all keys starting with a given prefix and returns
     * the values in an ArrayList. The keys must be Strings.
     *
     * <p>For example, calling this method for a Map containing
     * key-value pairs foo.1 = a, foo.2 = b, and foo.3 = c returns
     * an ArrayList containing [a, b, c].
     *
     * <p>Handles both 0- and 1-indexed names. Parsing stops at the
     * first gap in the numeric postfix.
     *
     * @param params a Map of string-object pairs, presumably containing
     *               key.1, key.2,...
     * @param keyPrefix a String prefix; values will be looked up by adding
     *                  ".0", ".1", and so on, until the first gap.
     * @return ArrayList, containing the values corresponding to the
     *          keyPrefix, in order.
     */
    public static ArrayList getNumberedValues( Map params, String keyPrefix )
    {
        ArrayList rval = new ArrayList();
        if( params == null || 
            params.size() == 0 || 
            keyPrefix == null || 
            keyPrefix.length() == 0 )
            return( rval );

        String fullPrefix = null;
        if( keyPrefix.charAt( keyPrefix.length() - 1 ) == '.' )
            fullPrefix = keyPrefix;
        else
            fullPrefix = keyPrefix + ".";

        int ix = 0;
        Object value = params.get( fullPrefix + (ix++) );
        if( value == null )
            value = params.get( fullPrefix + (ix++) );
        if( value == null )
            return( rval );
        while( true )
        {
            rval.add( value );
            value = params.get( fullPrefix + (ix++) );
            if( value == null )
                break;
        }

        return( rval );
    }


    /**
     * Converts the parameter contents of an HTTP request into a map,
     * modifying the keys to preserve multiple values per key. This
     * is done by adding an ordered suffix to the key:
     * <p><pre>foo=bar,baz,xyzzy</pre>
     * <p>becomes
     * <p><pre>foo.0=bar foo.1=baz foo.2=xyzzy</pre>
     *
     * <p>If filterPrefix is specified, only keys starting with the prefix
     * are included in the result map. If the prefix is null, all keys are
     * checked.
     *
     * <p>FIX: this is not necessarily encoding-safe: see
     * WikiContext.getHttpParameter().
     */
    public static Map requestToMap( HttpServletRequest req, 
				    String filterPrefix )
    {
        HashMap params = new HashMap();

        Enumeration enum = req.getParameterNames();
        while( enum.hasMoreElements() )
        {
            String param = (String)enum.nextElement();
            if( filterPrefix == null || param.startsWith( filterPrefix ) )
            {
                String realName = param.substring( filterPrefix.length() );
                String[] values = req.getParameterValues( param );
                if( values != null )
                {
                    if( values.length == 1 )
                    {
                        params.put( realName, values[0] );
                    }
                    else
                    {
                        for( int i = 0; i < values.length; i++ )
                        {
                            if( values[i] != null && values[i].length() > 0 )
                            {
                                params.put( realName + "." + i, values[i] );
                            }
                        }
                    }
                }
            }
        }

        return( params );
    }

}
