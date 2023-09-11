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
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.locks.ReentrantLock;

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
     * A lock used to ensure thread safety when accessing shared resources.
     * This lock provides more flexibility and capabilities than the intrinsic locking mechanism,
     * such as the ability to attempt to acquire a lock with a timeout, or to interrupt a thread
     * waiting to acquire a lock.
     *
     * @see java.util.concurrent.locks.ReentrantLock
     */
    private final ReentrantLock lock;

    /**
     * @see java.util.Properties#Properties()
     */
    public CommentedProperties()
    {
        super();
        lock = new ReentrantLock();
    }

    /**
     *  Creates new properties.
     *
     *  @param defaultValues A list of default values, which are used if in subsequent gets
     *                       a key is not found.
     */
    public CommentedProperties(final Properties defaultValues )
    {
        super( defaultValues );
        lock = new ReentrantLock();
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public void load(final InputStream inStream ) throws IOException
    {
        lock.lock();
        try {
            // Load the file itself into a string
            m_propertyString = FileUtil.readContents( inStream, StandardCharsets.ISO_8859_1.name() );

            // Now load it into the properties object as normal
            super.load( new ByteArrayInputStream( m_propertyString.getBytes(StandardCharsets.ISO_8859_1) ) );
        } finally {
            lock.unlock();
        }
    }

    /**
     *  Loads properties from a file opened by a supplied Reader.
     *  
     *  @param in The reader to read properties from
     *  @throws IOException in case something goes wrong.
     */
    @Override
    public void load(final Reader in ) throws IOException
    {
        lock.lock();
        try {
            m_propertyString = FileUtil.readContents( in );

            // Now load it into the properties object as normal
            super.load( new ByteArrayInputStream( m_propertyString.getBytes(StandardCharsets.ISO_8859_1) ) );
        } finally {
            lock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object setProperty(final String key, final String value )
    {
        lock.lock();
        try {
            return put(key, value);
        } finally {
            lock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void store(final OutputStream out, final String comments ) throws IOException
    {
        lock.lock();
        try {
            final byte[] bytes = m_propertyString.getBytes( StandardCharsets.ISO_8859_1 );
            FileUtil.copyContents( new ByteArrayInputStream( bytes ), out );
            out.flush();
        } finally {
            lock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object put(final Object arg0, final Object arg1 )
    {
        lock.lock();
        try {
            // Write the property to the stored string
            writeProperty( arg0, arg1 );

            // Return the result of from the superclass properties object
            return super.put(arg0, arg1);
        } finally {
            lock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putAll(final Map< ? , ? > arg0 )
    {
        lock.lock();
        try {
            // Shove all of the entries into the property string
            for (final Entry<?, ?> value : arg0.entrySet()) {
                @SuppressWarnings("unchecked") final Entry<Object, Object> entry = (Entry<Object, Object>) value;
                writeProperty(entry.getKey(), entry.getValue());
            }

            // Call the superclass method
            super.putAll(arg0);
        } finally {
            lock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object remove(final Object key )
    {
        lock.lock();
        try {
            // Remove from the property string
            deleteProperty( key );

            // Call the superclass method
            return super.remove(key);
        } finally {
            lock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        lock.lock();
        try {
            return m_propertyString;
        } finally {
            lock.unlock();
        }

    }

    private void deleteProperty(final Object arg0 )
    {
        // Get key and value
        if ( arg0 == null )
        {
            throw new IllegalArgumentException( "Key cannot be null." );
        }
        final String key = arg0.toString();

        // Iterate through each line and replace anything matching our key
        int idx = 0;
        while( ( idx < m_propertyString.length() ) && ( ( idx = m_propertyString.indexOf( key, idx ) ) != -1 ) )
        {
            final int prevret = m_propertyString.lastIndexOf( "\n", idx );
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
            final int eqsign = m_propertyString.indexOf( "=", idx );
            if ( eqsign != -1 )
            {
                final int ret = m_propertyString.indexOf( "\n", eqsign );
                m_propertyString = TextUtil.replaceString( m_propertyString, prevret, ret, "" );
                return;
            }
        }
    }

    private void writeProperty(final Object arg0, Object arg1 )
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
        final String key = arg0.toString();
        final String value = TextUtil.native2Ascii( arg1.toString() );

        // Iterate through each line and replace anything matching our key
        int idx = 0;
        while( ( idx < m_propertyString.length() ) && ( ( idx = m_propertyString.indexOf( key, idx ) ) != -1 ) )
        {
            final int prevret = m_propertyString.lastIndexOf( "\n", idx );
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
            final int eqsign = m_propertyString.indexOf( "=", idx );
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
