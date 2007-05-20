/*
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2007 Janne Jalkanen (Janne.Jalkanen@iki.fi)

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation; either version 2.1 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.ecyrd.jspwiki.util;

import java.io.*;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import com.ecyrd.jspwiki.FileUtil;
import com.ecyrd.jspwiki.TextUtil;

/**
 * Extends {@link java.util.Properties} by providing support for comment
 * preservation. When the properties are written to disk, previous
 * comments present in the file are preserved.
 * @author Andrew Jaquith
 * @author Janne Jalkanen
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
     * @see java.util.Properties#Properties(Properties)
     */
    public CommentedProperties( Properties defaultValues )
    {
        super( defaultValues );
    }

    /**
     * @see java.util.Properties#load(java.io.InputStream)
     */
    public synchronized void load( InputStream inStream ) throws IOException
    {
        // Load the file itself into a string
        m_propertyString = FileUtil.readContents( inStream, "ISO-8859-1" );

        // Now load it into the properties object as normal
        super.load( new ByteArrayInputStream( m_propertyString.getBytes("ISO-8859-1") ) );
    }

    public synchronized void load( Reader in ) throws IOException
    {
        m_propertyString = FileUtil.readContents( in );

        // Now load it into the properties object as normal
        super.load( new ByteArrayInputStream( m_propertyString.getBytes("ISO-8859-1") ) );
    }

    /**
     * @see java.util.Properties#setProperty(java.lang.String, java.lang.String)
     */
    public synchronized Object setProperty( String key, String value )
    {
        return put(key, value);
    }

    /**
     * @see java.util.Properties#store(java.io.OutputStream, java.lang.String)
     */
    public synchronized void store( OutputStream out, String comments ) throws IOException
    {
        byte[] bytes = m_propertyString.getBytes();
        FileUtil.copyContents( new ByteArrayInputStream( bytes ), out );
        out.flush();
    }

    /**
     * @see java.util.Hashtable#put(java.lang.Object, java.lang.Object)
     */
    public synchronized Object put( Object arg0, Object arg1 )
    {
        // Write the property to the stored string
        writeProperty( arg0, arg1 );

        // Return the result of from the superclass properties object
        return super.put(arg0, arg1);
    }

    /**
     * @see java.util.Hashtable#putAll(java.util.Map)
     */
    public synchronized void putAll( Map arg0 )
    {
        // Shove all of the entries into the property string
        for ( Iterator it = arg0.entrySet().iterator(); it.hasNext(); )
        {
            Entry entry = (Entry)it.next();
            writeProperty( entry.getKey(), entry.getValue() );
        }

        // Call the superclass method
        super.putAll(arg0);
    }

    /**
     * @see java.util.Hashtable#remove(java.lang.Object)
     */
    public synchronized Object remove( Object key )
    {
        // Remove from the property string
        deleteProperty( key );

        // Call the superclass method
        return super.remove(key);
    }

    /**
     * @see java.util.Hashtable#toString()
     */
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
