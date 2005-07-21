/*
 * (C) Janne Jalkanen 2005
 * 
 */
package com.ecyrd.jspwiki.dav;

import java.util.ArrayList;
import java.util.StringTokenizer;

public class DavPath
{
    private ArrayList m_parts = new ArrayList();
    
    private boolean   m_isAbsolute = false;
    
    private DavPath()
    {
    }
    
    public DavPath( String path )
    {
        if( path == null )
        {
            path = "/";
        }
        
        StringTokenizer st = new StringTokenizer( path, "/" );
        
        while( st.hasMoreTokens() )
        {
            String part = st.nextToken();
            
            m_parts.add( part );
        }
        
        //
        //  Add an empty path identifier
        //
        if( path.endsWith("/") )
            m_parts.add("");
        
        m_isAbsolute = path.startsWith("/") || path.length() == 0;
    }
  
    public void append( DavPath dp )
    {
        m_parts.addAll( dp.m_parts );
    }
    
    public void append( String path )
    {
        DavPath dp = new DavPath( path );
        
        append( dp );
    }
    
    public boolean isRoot()
    {
        return m_parts.size() == 0 || m_parts.get(0).equals("");
    }
    
    public boolean isDirectory()
    {
        return isRoot() || m_parts.get( m_parts.size()-1 ).equals("");
    }
    
    public String pathPart()
    {
        StringBuffer result = new StringBuffer( m_isAbsolute ? "/" : "" );
   
        for( int i = 0; i < m_parts.size()-1; i++ )
        {
            result.append( (String)m_parts.get(i) );
            result.append( "/" );
        }
        
        return result.toString();
    }
    
    public String filePart()
    {
        if( m_parts.size() > 0 )
            return (String) m_parts.get( m_parts.size()-1 );
       
        return "";
    }
    
    public String getName()
    {
        if( isRoot() ) return "/";
        if( !isDirectory() ) return filePart();
        
        return (String) m_parts.get( m_parts.size()-2 );
    }
    
    public String getPath()
    {
        return pathPart()+filePart();
    }
    
    public DavPath subPath( int idx )
    {
        DavPath dp = new DavPath();
        
        for( int i = idx; i < m_parts.size(); i++ )
        {
            dp.m_parts.add( m_parts.get(i) );
        }
           
        // Only full copies are absolute paths
        dp.m_isAbsolute = (idx == 0); 
        
        return dp;
    }
    
    /**
     * Returns the 'idx' component of the path, zero being the first component.  
     * If there is no such component,
     * it will simply return null.
     * 
     * @param idx
     * @return
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
    
    public String toString()
    {
        return "DavPath ["+getPath()+"]";
    }
}
