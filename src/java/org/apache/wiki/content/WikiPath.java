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
package org.apache.wiki.content;

import java.io.Serializable;

/**
 * <p>
 * A WikiPath represents a combination of a WikiSpace as well as a path within
 * that space. For example, in "MyWiki:MainPage/foo.jpg", "MyWiki" is the space,
 * and "MainPage/foo.jpg" is the path within that space.
 * </p>
 * <p>
 * The path itself is a sequence of names indicating a directory-like hierarchy,
 * with each name separated by slashes (/). The WikiPath's <em>name</em> is
 * the last name in the WikiPath's name sequence.
 * <p>
 * A WikiPath is an immutable object which cannot be changed after it has been
 * created.
 * </p>
 * 
 * @since 3.0
 */
public final class WikiPath implements Serializable, Comparable<WikiPath>
{
    private static final long serialVersionUID = 1L;

    private final String m_space;

    private final String m_path;
    
    private final String m_name;

    private final String m_stringRepresentation;

    private final String m_insensitiveString;

    /**
     * Create a WikiPath from a space and a path.
     * 
     * @param space the space. If space == null, then uses
     *            {@link ContentManager#DEFAULT_SPACE}
     * @param path the path
     * @throws IllegalArgumentException if <code>path</code> is null
     */
    public WikiPath( String space, String path )
    {
        if ( path == null )
        {
            throw new IllegalArgumentException( "Path must not be null!" );
        }
        m_path = path;
        int lastSlash = path.lastIndexOf( "/" );
        if ( lastSlash == -1 )
        {
            m_name = path;
        }
        else if ( lastSlash == path.length() -  1 )
        {
            m_name = "";
        }
        else
        {
            m_name = path.substring( lastSlash + 1, path.length() );
        }
        m_space = space == null ? ContentManager.DEFAULT_SPACE : space;
        m_stringRepresentation = m_space + ":" + m_path;
        m_insensitiveString = m_stringRepresentation.toLowerCase();
    }

    /**
     * Parses a fully-qualified name (FQN) and turns it into a WikiPath. If the
     * space name is missing, uses {@link ContentManager#DEFAULT_SPACE} for the
     * space name. If the path is null, throws an IllegalArgumentException.
     * 
     * @param path Path to parse
     * @return A WikiPath
     * @throws IllegalArgumentException If the path is null.
     */
    // TODO: Measure performance in realtime situations, then figure out whether
    // we should have an internal HashMap for path objects.
    public static WikiPath valueOf( String path ) throws IllegalArgumentException
    {
        if( path == null )
            throw new IllegalArgumentException( "null path given to WikiPath.valueOf()." );

        int colon = path.indexOf( ':' );

        if( colon != -1 )
        {
            // This is a FQN
            return new WikiPath( path.substring( 0, colon ), path.substring( colon + 1 ) );
        }

        return new WikiPath( ContentManager.DEFAULT_SPACE, path );
    }

    /**
     * Returns the name portion of the WikiPath; that is, the portion
     * of the name after the last slash.
     * 
     * @return the name
     */
    public String getName()
    {
        return m_name;
    }

    /**
     * Return the space part of the WikiPath
     * 
     * @return Just the space name.
     */
    public String getSpace()
    {
        return m_space;
    }

    /**
     * Return the path part of the WikiPath.
     * 
     * @return Just the path.
     */
    public String getPath()
    {
        return m_path;
    }

    /**
     * Returns the WikiPath of the parent.
     * 
     * @return A Valid WikiPath or null, if there is no parent.
     */
    // FIXME: Would it make more sense to throw an exception?
    public WikiPath getParent()
    {
        int slash = m_path.lastIndexOf( '/' );

        if( slash == -1 )
            return null;

        return new WikiPath( m_space, m_path.substring( 0, slash ) );
    }

    /**
     * Resolves a path with respect to this WikiPath. This is typically used
     * when figuring out where a subpage should be pointing at.
     * 
     * @param path Path to resolve
     * @return A new WikiPath
     */
    public WikiPath resolve( String path )
    {
        int colon = path.indexOf( ':' );

        if( colon != -1 )
        {
            // It is a FQN, essentially an absolute path, so no resolution
            // necessary
            return WikiPath.valueOf( path );
        }

        return new WikiPath( getSpace(), path );
    }

    /**
     * Returns the FQN format (space:path) of the name.
     * 
     * @return The name in FQN format.
     */
    public String toString()
    {
        return m_stringRepresentation;
    }

    /**
     * The hashcode of the WikiPath is exactly the same as the hashcode of its
     * String representation. This is to fulfill the general contract of
     * equals().
     * 
     * @return {@inheritDoc}
     */
    public int hashCode()
    {
        return m_insensitiveString.hashCode();
    }

    /**
     * A WikiPath is compared using it's toString() method.
     * 
     * @param o The Object to compare against.
     * @return {@inheritDoc}
     */
    // FIXME: Slow, since it creates a new String every time.
    public int compareTo( WikiPath o )
    {
        return m_insensitiveString.compareTo( o.m_insensitiveString );
    }

    /**
     * A WikiPath is equal to another WikiPath if the space and the path match.
     * A WikiPath can also be compared to a String, in which case a WikiPath is
     * equal to the String if its String representation is the same. This is to
     * make it easier to compare.
     * 
     * @param o The Object to compare against.
     * @return True, if this WikiPath is equal to another WikiPath.
     */
    public boolean equals( Object o )
    {
        if( o instanceof WikiPath )
        {
            WikiPath n = (WikiPath) o;
            return m_insensitiveString.equals( n.m_insensitiveString );
        }
        else if( o instanceof String )
        {
            return m_insensitiveString.equals( ((String)o).toLowerCase() );
        }
        return false;
    }
}
