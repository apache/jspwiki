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
  
    public boolean isRoot()
    {
        return m_parts.size() == 0 || m_parts.get(0).equals("");
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
        else
            return "";
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
    
    public String get( int idx )
    {
        return (String)m_parts.get(idx);
    }
    
    public String toString()
    {
        return "DavPath:"+getPath();
    }
}
