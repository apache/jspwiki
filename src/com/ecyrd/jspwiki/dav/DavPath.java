/*
 * (C) Janne Jalkanen 2005
 * 
 */
package com.ecyrd.jspwiki.dav;

public class DavPath
{
    private String m_filePart = "";
    private String m_pathPart = "";
    
    public DavPath( String path )
    {
        int slashIdx = path.lastIndexOf( '/' );
        
        if( slashIdx != -1 )
        {
            if( slashIdx < path.length() )
            {
                m_filePart = path.substring( slashIdx+1 );
            }
            m_pathPart = path.substring( 0, slashIdx+1 );
        }
        else
        {
            if( path.length() > 0 )
            {
                m_pathPart = path;
            }
            else
            {
                m_pathPart = "/";
            }
        }
    }
  
    public String pathPart()
    {
        return m_pathPart;
    }
    
    public String filePart()
    {
        return m_filePart;
    }
    
    public String getPath()
    {
        return m_pathPart+"/"+m_filePart;
    }
}
