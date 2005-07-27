/*
 * (C) Janne Jalkanen 2005
 * 
 */
package com.ecyrd.jspwiki.dav;

import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 *  The DavPath represents an abstract path to any resource within the WebDav
 *  system.  Since the file tree displayed by the DAV storage may be different
 *  from the actual representation, this component is needed.
 *  <p>
 *  You instantiate a new DavPath simply by saying 
 *  <code>DavPath dp = new DavPath("/path/to/my/object");</code>
 *  <p>
 *  If the path ends in a slash, it is understood to be a directory.  If not,
 *  it represents a file.
 *  
 * @author jalkanen
 *
 */
public class DavPath
{
    private ArrayList m_parts = new ArrayList();
    
    private boolean   m_isAbsolute  = false;
    private boolean   m_isDirectory = false;
    
    /**
     * Creates a new, empty path.
     *
     */
    private DavPath()
    {
    }
    
    /**
     *  Creates a new DavPath from an old one.
     * @param dp
     */
    public DavPath( DavPath dp )
    {
        m_parts.addAll( dp.m_parts );
        m_isAbsolute = dp.m_isAbsolute;
        m_isDirectory = dp.m_isDirectory;
    }
    
    /**
     *  Creates a new DavPath object.  The path parameter should be
     *  an arbitrary string with components separated with slashes.
     * @param path
     */
    public DavPath( String path )
    {
        if( path == null )
        {
            path = "/";
            m_isDirectory = true;
            m_isAbsolute = true;
            return;
        }
        
        StringTokenizer st = new StringTokenizer( path, "/" );
        
        while( st.hasMoreTokens() )
        {
            String part = st.nextToken();
            
            //
            // Skip empty components so that // gets transformed to a single /
            //
            if( part.length() > 0 )
                m_parts.add( part );
        }
        
        //
        //  Figure out path attributes
        //
        if( path.endsWith("/") )
            m_isDirectory = true;
        
        m_isAbsolute = path.startsWith("/");
    }
  
    /**
     * Adds another path to the end of this path.
     * 
     * @param dp
     */
    public void append( DavPath dp )
    {
        m_parts.addAll( dp.m_parts );
        m_isDirectory = dp.m_isDirectory;
    }
    
    /**
     * Adds another path to the end of this path.  The "path" parameter
     * may contain a slash-separated path (e.g. "foo/bar/blog.txt").
     * 
     * @param path
     */
    public void append( String path )
    {
        DavPath dp = new DavPath( path );
        
        append( dp );
    }
    
    /**
     * Returns true, if the path represents the top-level entity.  This is true,
     * if the path is "/" or it is empty.
     * 
     * @return True or false.
     */
    public boolean isRoot()
    {
        return m_parts.size() == 0 || m_parts.get(0).equals("");
    }
    
    /**
     * Returns true, if the path represents a directory.
     * 
     * @return
     */
    public boolean isDirectory()
    {
        return isRoot() || m_isDirectory;
    }
    
    /**
     * Returns the directory part of the DavPath.
     * 
     * @return
     */
    public String pathPart()
    {
        StringBuffer result = new StringBuffer( m_isAbsolute ? "/" : "" );
   
        for( int i = 0; i < m_parts.size()-(m_isDirectory ? 0 : 1); i++ )
        {
            result.append( (String)m_parts.get(i) );
            result.append( "/" );
        }
        
        return result.toString();
    }
    
    /**
     * Returns the file part of the DavPath.  The method returns the last component
     * of the path, unless the path is a directory, in which case it returns an
     * empty string.
     * 
     * @return  File name or empty string.
     */
    public String filePart()
    {
        if( m_parts.size() > 0 && !m_isDirectory )
            return (String) m_parts.get( m_parts.size()-1 );
       
        return "";
    }
    
    /**
     * Returns the name of the last component of the DavPath.  This is either
     * the name of a directory, or the name of a file.
     * 
     * @return
     */
    public String getName()
    {
        if( isRoot() ) return "/";
        if( !isDirectory() ) return filePart();
        
        return (String) m_parts.get( m_parts.size()-1 );
    }
    
    /**
     * Returns the entire path as a String.
     * @return
     */
    public String getPath()
    {
        return pathPart()+filePart();
    }
    
    /**
     * Returns a new DavPath object that is a sub-path of this path.  E.g. if
     * the path is "/foo/bar/blog.txt", subPath(1) would return "bar/blog.txt".
     * Notice that the resulting path is not absolute in this case.
     * 
     * @param idx Start from this part.
     * @return A sub-path of this path.
     */
    public DavPath subPath( int idx )
    {
        DavPath dp = new DavPath();
        
        for( int i = idx; i < m_parts.size(); i++ )
        {
            dp.m_parts.add( m_parts.get(i) );
        }
           
        // Only full copies are absolute paths
        dp.m_isAbsolute = (idx == 0); 
        dp.m_isDirectory = m_isDirectory;
        
        return dp;
    }
    
    /**
     * Returns the 'idx' component of the path, zero being the first component.  
     * If there is no such component,
     * it will simply return null.
     * 
     * @param idx The component to return. Zero is the first element.
     * @return A part of the path.
     */
    public String get( int idx )
    {
        if( idx > size() )
            return null;
        
        return (String)m_parts.get(idx);
    }
    
    /**
     * Exactly equivalent to length().
     * 
     * @return  The length of the path.
     */
    public int size()
    {
        return m_parts.size();
    }
    
    /**
     * Exactly equivalent to size().  I'm too lazy to remember whether it's length() or size(),
     * so I'll provide both...
     * 
     * @return
     */
    public int length()
    {
        return m_parts.size();
    }
    
    /**
     * Returns a human-readable version of the path.  Please use getPath() instead
     * of toString(), as this method is only good for debugging purposes.
     */
    public String toString()
    {
        return "DavPath ["+getPath()+"]";
    }
}
