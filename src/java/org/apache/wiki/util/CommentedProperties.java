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
package org.apache.wiki.util;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extends {@link java.util.Properties} by providing support for comment
 * preservation. When the properties are written to disk, previous comments
 * present in the file are preserved.
 * 
 * @since 2.4.22
 */
public class CommentedProperties extends Properties
{
    private static final long serialVersionUID = 8057284636436329669L;

    /** Map with property names as keys, and comments as values. */
    private Map<String, List<String>> m_propertyComments = new HashMap<String, List<String>>();

    /**
     * Ordered map with property names inserted in the order encountered in the
     * text file.
     */
    private Set<Object> m_keys = new LinkedHashSet<Object>();

    private List<String> m_trailingComments = null;

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
     * <p>
     * <em>Notes specific to this implementation:</em>
     * </p>
     * <p>
     * </p>
     */
    @Override
    public synchronized void store( OutputStream out, String comments ) throws IOException
    {
        BufferedWriter writer;
        writer = new BufferedWriter( new OutputStreamWriter( out, "8859_1" ) );
        writeProperties( writer, comments );
        writer.flush();
    }

    /**
     * @param writer
     * @param comments
     */
    private void writeProperties( BufferedWriter writer, String comments ) throws IOException
    {
        if( comments != null )
        {
            writer.write( "# " );
            writeText( writer, "# " + comments, EscapeMode.COMMENT );
            writer.newLine();
            writer.write( "# " + new Date().toString() );
            writer.newLine();
        }

        for( Object key : m_keys )
        {
            Object value = get( key );
            List<String> commentList = m_propertyComments.get( key );
            writeComments( writer, commentList );
            writeText( writer, key, EscapeMode.KEY );
            writer.write( '=' );
            writeText( writer, value, EscapeMode.ENTRY );
            writer.newLine();
        }

        // Now print the keys we did not encounter (i.e., were added after the
        // load method)
        for( Map.Entry<Object, Object> entry : this.entrySet() )
        {
            String key = entry.getKey().toString();
            if( !m_keys.contains( key ) )
            {
                Object value = entry.getValue();
                writeText( writer, key, EscapeMode.KEY );
                writer.write( '=' );
                writeText( writer, value, EscapeMode.ENTRY );
                writer.newLine();
            }
        }

        // Add any trailing comments
        writeComments( writer, m_trailingComments );
    }

    enum EscapeMode
    {
        COMMENT, KEY, ENTRY;
    }

    private void writeComments( BufferedWriter writer, List<String> commentList ) throws IOException
    {
        if( commentList != null )
        {
            for( String comment : commentList )
            {
                if( BLANK_LINE_DETECTOR.matcher( comment ).matches() )
                {
                    writer.write( comment );
                }
                else
                {
                    writer.write( "#" );
                    writeText( writer, comment, EscapeMode.COMMENT );
                }
                writer.newLine();
            }
        }
    }

    private void writeText( BufferedWriter writer, Object entry, EscapeMode mode ) throws IOException
    {
        String e = entry == null ? "" : entry.toString();
        boolean leadingSpaces = true;
        for( int i = 0; i < e.length(); i++ )
        {
            char ch = e.charAt( i );

            // Escape linefeeds, newline, carriage returns and tabs
            switch( ch )
            {
                case '\t': {
                    leadingSpaces = false;
                    writer.write( "\\t" );
                    break;
                }
                case '\f': {
                    leadingSpaces = false;
                    writer.write( "\\f" );
                    break;
                }
                case '\n': {
                    leadingSpaces = false;
                    if( mode != EscapeMode.COMMENT )
                    {
                        writer.write( "\\n" );
                    }
                    break;
                }
                case '\r': {
                    leadingSpaces = false;
                    if( mode != EscapeMode.COMMENT )
                    {
                        writer.write( "\\r" );
                    }
                    break;
                }
                case ' ': {
                    if( (mode == EscapeMode.ENTRY && leadingSpaces) || mode == EscapeMode.KEY )
                    {
                        writer.write( '\\' );
                    }
                    writer.write( ' ' );
                    break;
                }
                case '\\':
                case '#':
                case '!':
                case '=':
                case ':': {
                    leadingSpaces = false;
                    if( mode == EscapeMode.KEY )
                    {
                        writer.write( '\\' );
                    }
                    writer.write( ch );
                    break;
                }
                default: {
                    leadingSpaces = false;
                    if( ch < 32 || ( ch > 126 && ch < 160 ) || ch > 255 )
                    {
                        writer.write( '\\' );
                        writer.write( 'u' );
                        writer.write( HEX[(ch >>> 12) & 15] );
                        writer.write( HEX[(ch >>> 8) & 15] );
                        writer.write( HEX[(ch >>> 4) & 15] );
                        writer.write( HEX[ch & 15] );
                    }
                    else
                    {
                        writer.write( ch );
                    }
                }
            }
        }
    }

    private char[] HEX = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized String toString()
    {
        StringWriter s = new StringWriter();
        BufferedWriter writer;
        writer = new BufferedWriter( s );
        try
        {
            writeProperties( writer, null );
            writer.flush();
            return s.toString();
        }
        catch( IOException e )
        {
        }
        return null;
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
        List<String> commentList = new ArrayList<String>();
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
                    commentList.add( text.substring( 1 ) );
                    inProperty = false;
                }

                // If all whitespace and part of comment, append it
                else if( isWhitespace && !inProperty )
                {
                    commentList.add( text );
                }

                // Otherwise, see if we're starting a new property key
                else if( !inProperty )
                {
                    // If we are, lookup the key and add the comment
                    String key = extractKey( text );
                    if( key != null )
                    {
                        String value = getProperty( key );
                        if( value != null && commentList.size() > 0 )
                        {
                            m_propertyComments.put( key, commentList );
                            m_keys.add( key );
                        }
                        inProperty = true;
                        commentList = new ArrayList<String>();
                    }
                }
            }
        }

        // Any leftover comments are "trailing" comments that go at the end of
        // the file
        m_trailingComments = commentList;

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
     * The leading <code>#</code> or <code>!</code> characters are not
     * included, but everything after these characters, including whitespace, is
     * returned. If the comment spans more than one line, the String returned
     * will contain line separator characters separating each line. The line
     * separator characters will be specific to the JRE platform in use, as
     * returned by the System property <code>line.separator</code>.
     * 
     * @param key the key to look up
     * @return the list of strings representing the comment, each item of which
     *         represents a line, or <code>null</code> if not found
     */
    public String getComment( String key )
    {
        StringBuffer b = new StringBuffer();
        List<String> commentList = m_propertyComments.get( key );
        if( commentList == null )
        {
            return null;
        }
        for( String comment : commentList )
        {
            b.append( comment );
            if( commentList.indexOf( comment ) < commentList.size() - 1 )
            {
                b.append( m_br );
            }
        }
        return b.toString();
    }

    /**
     * Sets a property value for a supplied key, and adds a comment that will be
     * written to disk when the {@link #store(OutputStream, String)} method is
     * called. This method behaves otherwise identically to
     * {@link #setProperty(String, String)}. The leading <code>#</code> or
     * <code>!</code> characters should not be included unless it is meant to
     * be part of the comment itself.
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
                if( comment.length() > 0 )
                {
                    List<String> commentList = new ArrayList<String>();
                    commentList.add( comment );
                    m_propertyComments.put( key, commentList );
                }
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
