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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

/**
 * Extends {@link java.util.Properties} by providing support for comment
 * preservation. When the properties are written to disk, previous
 * comments present in the file are preserved.
 * @since 2.4.22
 */
public class CommentedProperties extends Properties
{
    private static final long serialVersionUID = 8057284636436329669L;

    private String m_propertyString;

    /**
     * @see java.util.Properties#Properties()
     */
    public CommentedProperties()
    {
        super();
    }

    /**
     *  Creates new properties.
     *
     *  @param defaultValues A list of default values, which are used if in subsequent gets
     *                       a key is not found.
     */
    public CommentedProperties( Properties defaultValues )
    {
        super( defaultValues );
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public synchronized void load( InputStream inStream ) throws IOException
    {
        // Load the file itself into a string
        m_propertyString = FileUtil.readContents( inStream, "ISO-8859-1" );

        // Now load it into the properties object as normal
        super.load( new ByteArrayInputStream( m_propertyString.getBytes("ISO-8859-1") ) );
    }

    /**
     *  Loads properties from a file opened by a supplied Reader.
     *  
     *  @param in The reader to read properties from
     *  @throws IOException in case something goes wrong.
     */
    @Override
    public synchronized void load( Reader in ) throws IOException
    {
        m_propertyString = FileUtil.readContents( in );

        // Now load it into the properties object as normal
        super.load( new ByteArrayInputStream( m_propertyString.getBytes("ISO-8859-1") ) );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized Object setProperty( String key, String value )
    {
        return put(key, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void store( OutputStream out, String comments ) throws IOException
    {
        byte[] bytes = m_propertyString.getBytes("ISO-8859-1");
        FileUtil.copyContents( new ByteArrayInputStream( bytes ), out );
        out.flush();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized Object put( Object arg0, Object arg1 )
    {
        // Write the property to the stored string
        writeProperty( arg0, arg1 );

        // Return the result of from the superclass properties object
        return super.put(arg0, arg1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void putAll( Map< ? , ? > arg0 )
    {
        // Shove all of the entries into the property string
        for( Iterator< ? > it = arg0.entrySet().iterator(); it.hasNext(); )
        {
            @SuppressWarnings("unchecked") 
            Entry< Object, Object > entry = ( Entry< Object, Object > )it.next();
            writeProperty( entry.getKey(), entry.getValue() );
        }

        // Call the superclass method
        super.putAll(arg0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized Object remove( Object key )
    {
        // Remove from the property string
        deleteProperty( key );

        // Call the superclass method
        return super.remove(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized String toString()
    {
        return m_propertyString;
    }

    private void deleteProperty( Object arg0 )
    {
        // Get key and value
        if ( arg0 == null )
        {
            throw new IllegalArgumentException( "Key cannot be null." );
        }
        String key = arg0.toString();

        // Iterate through each line and replace anything matching our key
        int idx = 0;
        while( ( idx < m_propertyString.length() ) && ( ( idx = m_propertyString.indexOf( key, idx ) ) != -1 ) )
        {
            int prevret = m_propertyString.lastIndexOf( "\n", idx );
            if ( prevret != -1 )
            {
                // Commented lines are skipped
                if ( m_propertyString.charAt( prevret + 1 ) == '#' )
                {
                    idx += key.length();
                    continue;
                }
            }

            // If "=" present, delete the entire line
            int eqsign = m_propertyString.indexOf( "=", idx );
            if ( eqsign != -1 )
            {
                int ret = m_propertyString.indexOf( "\n", eqsign );
                m_propertyString = TextUtil.replaceString( m_propertyString, prevret, ret, "" );
                return;
            }
        }
    }

    private void writeProperty( Object arg0, Object arg1 )
    {
        // Get key and value
        if ( arg0 == null )
        {
            throw new IllegalArgumentException( "Key cannot be null." );
        }
        if ( arg1 == null )
        {
            arg1 = "";
        }
        String key = arg0.toString();
        String value = TextUtil.native2Ascii( arg1.toString() );

        // Iterate through each line and replace anything matching our key
        int idx = 0;
        while( ( idx < m_propertyString.length() ) && ( ( idx = m_propertyString.indexOf( key, idx ) ) != -1 ) )
        {
            int prevret = m_propertyString.lastIndexOf( "\n", idx );
            if ( prevret != -1 )
            {
                // Commented lines are skipped
                if ( m_propertyString.charAt( prevret + 1 ) == '#' )
                {
                    idx += key.length();
                    continue;
                }
            }

            // If "=" present, replace everything in line after it
            int eqsign = m_propertyString.indexOf( "=", idx );
            if ( eqsign != -1 )
            {
                int ret = m_propertyString.indexOf( "\n", eqsign );
                if ( ret == -1 )
                {
                    ret = m_propertyString.length();
                }
                m_propertyString = TextUtil.replaceString( m_propertyString, eqsign + 1, ret, value );
                return;
            }
        }

        // If it was not found, we'll add it to the end.
        m_propertyString += "\n" + key + " = " + value + "\n";
    }

}
