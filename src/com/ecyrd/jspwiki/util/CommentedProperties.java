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

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extends {@link java.util.Properties} by providing support for comment
 * preservation. When the properties are written to disk, previous comments
 * present in the file are preserved.
 * 
 * @author Andrew Jaquith
 * @since 2.4.22
 */
public class CommentedProperties extends Properties
{
    private static final long serialVersionUID = 8057284636436329669L;

    /** Map with property names as keys, and comments as values. */
    private Map<String, String> m_propertyComments = new HashMap<String, String>();

    /**
     * Ordered map with property names inserted in the order encountered in the
     * text file.
     */
    private Set<Object> m_keys = new LinkedHashSet<Object>();

    private String m_trailingComment = null;

    private final String m_br;

    /**
     * @see java.util.Properties#Properties()
     */
    public CommentedProperties()
    {
        super();
        m_br = System.getProperty( "line.separator" );
    }

    /**
     * Creates new properties.
     * 
     * @param defaultValues A list of default values, which are used if in
     *            subsequent gets a key is not found.
     */
    public CommentedProperties( Properties defaultValues )
    {
        super( defaultValues );
        m_br = System.getProperty( "line.separator" );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void load( InputStream inStream ) throws IOException
    {
        // Load the file itself into a string
        String propertyString = FileUtil.readContents( inStream, "ISO-8859-1" );

        // Now load it into the properties object as normal
        super.load( new ByteArrayInputStream( propertyString.getBytes( "ISO-8859-1" ) ) );

        // Load all of the comments
        loadComments( propertyString );
    }

    /**
     * Loads properties from a file opened by a supplied Reader.
     * 
     * @param in The reader to read properties from
     * @throws IOException in case something goes wrong.
     */
    public synchronized void load( Reader in ) throws IOException
    {
        String propertyString = FileUtil.readContents( in );

        // Now load it into the properties object as normal
        super.load( new ByteArrayInputStream( propertyString.getBytes( "ISO-8859-1" ) ) );

        // Load all of the comments
        loadComments( propertyString );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void store( OutputStream out, String comments ) throws IOException
    {
        byte[] bytes = toString().getBytes( "ISO-8859-1" );
        FileUtil.copyContents( new ByteArrayInputStream( bytes ), out );
        out.flush();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized String toString()
    {
        StringBuilder b = new StringBuilder();
        for( Object key : m_keys )
        {
            Object value = get( key );
            String comment = m_propertyComments.get( key );
            if( comment != null )
            {
                b.append( comment );
                b.append( m_br );
            }
            printProperty( b, key, value );
        }

        // Now print the keys we did not encounter (i.e., were added after the
        // load method)
        for( Map.Entry<Object, Object> entry : this.entrySet() )
        {
            String key = entry.getKey().toString();
            if( !m_keys.contains( key ) )
            {
                Object value = entry.getValue();
                printProperty( b, key, value );
            }
        }

        // Add any trailing comments
        if( m_trailingComment != null )
        {
            b.append( m_trailingComment );
            b.append( m_br );
        }
        return b.toString();
    }

    private void printProperty( StringBuilder b, Object key, Object value )
    {
        b.append( key.toString() );
        b.append( ' ' );
        b.append( '=' );
        b.append( ' ' );
        b.append( value.toString() );
        b.append( m_br );
    }

    /**
     * Trims whitespace from property line strings: \n \r \u0020 \t \u0009 \f
     * \u000c space
     */
    private static final Pattern LINE_TRIMMER = Pattern.compile( "^[ \\r\\n\\t\\f]*(.*?)[ \\r\\n\\t\\f]*$" );

    /**
     * Determines if a line consists entirely of whitespace.
     */
    private static final Pattern BLANK_LINE_DETECTOR = Pattern.compile( "^[ \\r\\n\\t\\f]*$" );

    /**
     * Parses the comments from the properties file (stored as a string)
     */
    private void loadComments( String propertyString ) throws IOException
    {
        LineNumberReader reader = new LineNumberReader( new StringReader( propertyString ) );
        String line = null;
        boolean inProperty = false;
        String comment = null;
        while ( (line = reader.readLine()) != null )
        {
            // Trim line of leading/trailing whitespace
            Matcher m = LINE_TRIMMER.matcher( line );
            if( m.matches() )
            {
                String text = m.group( 1 );

                // Is first character ! or #? We are in a comment line...
                boolean isComment = text.startsWith( "#" ) || text.startsWith( "!" );
                boolean isWhitespace = BLANK_LINE_DETECTOR.matcher( text ).matches();
                if( isComment )
                {
                    comment = comment == null ? text : comment + m_br + text;
                    inProperty = false;
                }

                // If all whitespace and part of comment, append it
                else if( isWhitespace && !inProperty )
                {
                    comment = comment == null ? text : comment + m_br + text;
                }

                // Otherwise, see if we're starting a new property key
                else if( !inProperty )
                {
                    // If we are, lookup the key and add the comment
                    String key = extractKey( text );
                    if( key != null )
                    {
                        String value = getProperty( key );
                        if( value != null && comment != null )
                        {
                            m_propertyComments.put( key, comment );
                            m_keys.add( key );
                        }
                        inProperty = true;
                        comment = null;
                    }
                }
            }
        }

        // Any leftover comments are "trailing" comments that go at the end of
        // the file
        m_trailingComment = comment;

        reader.close();
    }

    /**
     * Extracts a key name from a trimmed line of text, which may include
     * escaped characters.
     * 
     * @param text
     * @return the key
     */
    protected String extractKey( String text )
    {
        char[] chars = text.toCharArray();
        char lastChar = ' ';
        for( int i = 0; i < chars.length; i++ )
        {
            char ch = chars[i];
            switch( ch )
            {
                case ' ':
                case '\t':
                case ':':
                case '=':
                case '\f': {
                    if( lastChar != '\'' )
                    {
                        return text.substring( 0, i );
                    }
                }
            }
        }
        return null;
    }

    /**
     * Returns the comment for a supplied key, as parsed by the
     * {@link #load(Reader)} or {@link #load(InputStream)} methods, <em>or</em>
     * as supplied to the{@link #setProperty(String, String, String)} method.
     * 
     * @param key the key to look up
     * @return the comment (including the trailing <code>!</code> or
     *         <code>#</code> character, or <code>null</code> if not found
     */
    public String getComment( String key )
    {
        return(m_propertyComments.get( key ));
    }

    /**
     * Sets a property value for a supplied key, and adds a comment that will be
     * written to disk when the {@link #store(OutputStream, String)} method is
     * called. This method behaves otherwise identically to
     * {@link #setProperty(String, String)}.
     * 
     * @param key the string key to store
     * @param value the property value associated with the key
     * @param comment the comment to add
     * @return the the previous value of the specified key in this property
     *         list, or <code>null</code> if it did not have one.
     */
    public synchronized Object setProperty( String key, String value, String comment )
    {
        if( key != null )
        {
            if( comment != null )
            {
                comment = comment.trim();
                if( !comment.startsWith( "#" ) )
                {
                    comment = "# " + comment;
                }
                m_propertyComments.put( key, comment );
            }
            m_keys.add( key );
        }
        return super.setProperty( key, value );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized Object setProperty( String key, String value )
    {
        return setProperty( key, value, null );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized Object put( Object key, Object value )
    {
        if( key != null )
        {
            m_keys.add( key );
        }
        return super.put( key, value );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void putAll( Map<? extends Object, ? extends Object> t )
    {
        for( Object key : t.keySet() )
        {
            if( key != null )
            {
                m_keys.add( key );
            }
        }
        super.putAll( t );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized Object remove( Object key )
    {
        if( key != null )
        {
            m_propertyComments.remove( key );
            m_keys.remove( key );
        }
        return super.remove( key );
    }

}
