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

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

/**
 * A collection of (static) utilities used by the WikiForms code.
 * FormUtil is mainly concerned with mapping HTTP parameters to
 * WikiPlugin parameters.
 *
 */
public final class FormUtil
{
    /**
     * Private constructor to prevent direct instantiation.
     */
    private FormUtil()
    {
    }
    
    /**
     * <p>Looks for a named value in the Map. Returns either the
     * value named by key, or values named by key.0, key.1, ...
     * if the direct value is not found. The values are packed
     * in an ArrayList.</p>
     * <p>This is a utility method, mainly used when we don't know
     * whether there was just one value, or several, in a mapping list
     * (e.g. an HttpRequest / FORM checkbox).</p>
     * @param params the Map container form parameters
     * @param key the key to look up
     * @return the List of keys
     */
    public static List< ? > getValues( Map< ? , ? > params, String key )
    {
        if( params == null || key == null )
            return new ArrayList<>(0);

        Object entry = params.get( key );
        if( entry != null )
        {
            ArrayList<Object> rval = new ArrayList<>(1);
            rval.add( entry );
            return rval;
        }

        return getNumberedValues( params, key );
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
    public static List< ? > getNumberedValues( Map< ?, ? > params, String keyPrefix )
    {
        ArrayList<Object> rval = new ArrayList<>();
        if( params == null || 
            params.size() == 0 || 
            keyPrefix == null || 
            keyPrefix.length() == 0 )
            return rval;

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
            return rval;
        while( true )
        {
            rval.add( value );
            value = params.get( fullPrefix + (ix++) );
            if( value == null )
                break;
        }

        return rval;
    }


    /**
     * <p>Converts the parameter contents of an HTTP request into a map,
     * modifying the keys to preserve multiple values per key. This
     * is done by adding an ordered suffix to the key:</p>
     * <p><pre>foo=bar,baz,xyzzy</pre></p>
     * <p>becomes</p>
     * <p><pre>foo.0=bar foo.1=baz foo.2=xyzzy</pre></p>
     * <p>If filterPrefix is specified, only keys starting with the prefix
     * are included in the result map. If the prefix is null, all keys are
     * checked.</p>
     * <p>FIX: this is not necessarily encoding-safe: see
     * WikiContext.getHttpParameter().</p>
     * @param req the HTTP request
     * @param filterPrefix the prefix
     * @return the Map containing parsed key/value pairs
     */
    public static Map< String, String > requestToMap( HttpServletRequest req, String filterPrefix )
    {
        HashMap<String,String> params = new HashMap<>();
        
        if( filterPrefix == null ) filterPrefix = "";
        
        Enumeration< String > en = req.getParameterNames();
        while( en.hasMoreElements() )
        {
            String param = en.nextElement();
            
            if( param.startsWith( filterPrefix ) )
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

        return params;
    }

}
