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
package com.ecyrd.jspwiki;

import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import com.ecyrd.jspwiki.auth.acl.Acl;
import com.ecyrd.jspwiki.auth.acl.AclEntry;
import com.ecyrd.jspwiki.auth.acl.AclImpl;
import com.ecyrd.jspwiki.content.WikiName;
import com.ecyrd.jspwiki.providers.WikiPageProvider;

/**
 *  Simple wrapper class for the Wiki page attributes.  The Wiki page
 *  content is moved around in Strings, though.
 *  
 *  @since 3.0
 */

// FIXME: We need to rethink how metadata is being used - probably the 
//        author, date, etc. should also be part of the metadata.  We also
//        need to figure out the metadata lifecycle.

public class JCRWikiPage
    implements Cloneable,
               Comparable, WikiPage
{
    private static final long serialVersionUID = 1L;

    private       WikiName   m_name;
    private       WikiEngine m_engine;
    private Date             m_lastModified;
    private long             m_fileSize = -1;
    private int              m_version = WikiPageProvider.LATEST_VERSION;
    private String           m_author = null;
    private final HashMap<String,Object> m_attributes = new HashMap<String,Object>();

    private Acl m_accessList = null;
    
    /**
     *  Use {@link WikiEngine#createPage(String)} instead.
     *  @deprecated
     */
    public JCRWikiPage( WikiEngine engine, String path )
    {
        m_engine = engine;
        m_name   = WikiName.valueOf( path );
    }

    /** 
     * Use {@link WikiEngine#createPage(WikiName)} instead. 
     * @deprecated 
     */
    public JCRWikiPage( WikiEngine engine, WikiName name )
    {
        m_engine = engine;
        m_name   = name;
    }

    /* (non-Javadoc)
     * @see com.ecyrd.jspwiki.WikiPage#getName()
     */
    public String getName()
    {
        return m_name.getPath();
    }
    
    /* (non-Javadoc)
     * @see com.ecyrd.jspwiki.WikiPage#getQualifiedName()
     */
    public String getQualifiedName()
    {
        return m_name.toString();
    }

    /* (non-Javadoc)
     * @see com.ecyrd.jspwiki.WikiPage#getAttribute(java.lang.String)
     */
    public Object getAttribute( String key )
    {
        return m_attributes.get( key );
    }

    /* (non-Javadoc)
     * @see com.ecyrd.jspwiki.WikiPage#setAttribute(java.lang.String, java.lang.Object)
     */
    public void setAttribute( String key, Object attribute )
    {
        m_attributes.put( key, attribute );
    }

    /* (non-Javadoc)
     * @see com.ecyrd.jspwiki.WikiPage#getAttributes()
     */
    public Map getAttributes() 
    {
        return m_attributes;
    }

    /* (non-Javadoc)
     * @see com.ecyrd.jspwiki.WikiPage#removeAttribute(java.lang.String)
     */
    public Object removeAttribute( String key )
    {
        return m_attributes.remove( key );
    }

    /* (non-Javadoc)
     * @see com.ecyrd.jspwiki.WikiPage#getLastModified()
     */
    public Date getLastModified()
    {
        return m_lastModified;
    }

    /* (non-Javadoc)
     * @see com.ecyrd.jspwiki.WikiPage#setLastModified(java.util.Date)
     */
    public void setLastModified( Date date )
    {
        m_lastModified = date;
    }

    /* (non-Javadoc)
     * @see com.ecyrd.jspwiki.WikiPage#setVersion(int)
     */
    public void setVersion( int version )
    {
        m_version = version;
    }

    /* (non-Javadoc)
     * @see com.ecyrd.jspwiki.WikiPage#getVersion()
     */
    public int getVersion()
    {
        return m_version;
    }

    /* (non-Javadoc)
     * @see com.ecyrd.jspwiki.WikiPage#getSize()
     */
    public long getSize()
    {
        return m_fileSize;
    }

    /* (non-Javadoc)
     * @see com.ecyrd.jspwiki.WikiPage#setSize(long)
     */
    public void setSize( long size )
    {
        m_fileSize = size;
    }

    /* (non-Javadoc)
     * @see com.ecyrd.jspwiki.WikiPage#getAcl()
     */
    public Acl getAcl()
    {
        return m_accessList;
    }

    /* (non-Javadoc)
     * @see com.ecyrd.jspwiki.WikiPage#setAcl(com.ecyrd.jspwiki.auth.acl.Acl)
     */
    public void setAcl( Acl acl )
    {
        m_accessList = acl;
    }

    /* (non-Javadoc)
     * @see com.ecyrd.jspwiki.WikiPage#setAuthor(java.lang.String)
     */
    public void setAuthor( String author )
    {
        m_author = author;
    }

    /* (non-Javadoc)
     * @see com.ecyrd.jspwiki.WikiPage#getAuthor()
     */
    public String getAuthor()
    {
        return m_author;
    }
    
    /* (non-Javadoc)
     * @see com.ecyrd.jspwiki.WikiPage#getWiki()
     */
    // FIXME: Should we rename this method?
    public String getWiki()
    {
        return m_name.getSpace();
    }

    /* (non-Javadoc)
     * @see com.ecyrd.jspwiki.WikiPage#invalidateMetadata()
     */
    public void invalidateMetadata()
    {        
        m_hasMetadata = false;
        setAcl( null );
        m_attributes.clear();
    }

    private boolean m_hasMetadata = false;

    /* (non-Javadoc)
     * @see com.ecyrd.jspwiki.WikiPage#hasMetadata()
     */
    public boolean hasMetadata()
    {
        return m_hasMetadata;
    }

    /* (non-Javadoc)
     * @see com.ecyrd.jspwiki.WikiPage#setHasMetadata()
     */
    public void setHasMetadata()
    {
        m_hasMetadata = true;
    }

    /* (non-Javadoc)
     * @see com.ecyrd.jspwiki.WikiPage#toString()
     */
    public String toString()
    {
        return "WikiPage ["+m_name+",ver="+m_version+",mod="+m_lastModified+"]";
    }

    /* (non-Javadoc)
     * @see com.ecyrd.jspwiki.WikiPage#clone()
     */
    public Object clone()
    {
        JCRWikiPage p = new JCRWikiPage( m_engine, m_name );
            
        p.m_author       = m_author;
        p.m_version      = m_version;
        p.m_lastModified = m_lastModified != null ? (Date)m_lastModified.clone() : null;

        p.m_fileSize     = m_fileSize;

        for( Map.Entry<String,Object> entry : m_attributes.entrySet() )
        {
            p.m_attributes.put( entry.getKey(), 
                                entry.getValue() );
        }

        if( m_accessList != null )
        {
            p.m_accessList = new AclImpl();
            
            for( Enumeration entries = m_accessList.entries(); entries.hasMoreElements(); )
            {
                AclEntry e = (AclEntry)entries.nextElement();
            
                p.m_accessList.addEntry( e );
            }
        }
            
        return p;
    }
    
    /* (non-Javadoc)
     * @see com.ecyrd.jspwiki.WikiPage#compareTo(java.lang.Object)
     */
    public int compareTo( Object o )
    {
        int res = 0;
        if( o instanceof WikiPage )
        {
            WikiPage c = (WikiPage)o;
        
            res = this.getName().compareTo(c.getName());
            
            if( res == 0 ) res = this.getVersion()-c.getVersion();
        }
            
        return res;
    }
    
    /**
     *  A page is equal to another page if its name and version are equal.
     *  
     *  {@inheritDoc}
     */
    // TODO: I have a suspicion that defining this method causes some problems
    //       with page attributes and caching.  So as of 2.7.32, it's disabled.
    /*
    public boolean equals( Object o )
    {
        if( o != null && o instanceof WikiPage )
        {
            WikiPage oo = (WikiPage) o;
        
            if( oo.getName().equals( getName() ) )
            {
                if( oo.getVersion() == getVersion() )
                {
                    return true;
                }
            }
        }
        
        return false;
    }
    */
    /* (non-Javadoc)
     * @see com.ecyrd.jspwiki.WikiPage#hashCode()
     */
    public int hashCode()
    {
        return m_name.hashCode() * m_version;
    }
}
