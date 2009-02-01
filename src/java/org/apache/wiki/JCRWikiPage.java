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
package org.apache.wiki;

import java.io.InputStream;
import java.util.*;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.apache.wiki.api.WikiException;
import org.apache.wiki.api.WikiPage;
import org.apache.wiki.auth.acl.Acl;
import org.apache.wiki.auth.acl.AclEntry;
import org.apache.wiki.auth.acl.AclImpl;
import org.apache.wiki.content.ContentManager;
import org.apache.wiki.content.WikiName;
import org.apache.wiki.providers.WikiPageProvider;


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
    private Node             m_node;
    
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

    public JCRWikiPage(WikiEngine engine, Node node)
        throws RepositoryException, WikiException
    {
        m_engine = engine;
        m_node   = node;
        m_name   = ContentManager.getWikiPath( node.getPath() );
    }
    
    
    public Node getJCRNode()
    {
        return m_node;
    }
    
    /* (non-Javadoc)
     * @see org.apache.wiki.WikiPage#getName()
     */
    public String getName()
    {
        return m_name.getPath();
    }
    
    /* (non-Javadoc)
     * @see org.apache.wiki.WikiPage#getQualifiedName()
     */
    public String getQualifiedName()
    {
        return m_name.toString();
    }

    /* (non-Javadoc)
     * @see org.apache.wiki.WikiPage#getAttribute(java.lang.String)
     */
    public Object getAttribute( String key )
    {
        return m_attributes.get( key );
    }

    /* (non-Javadoc)
     * @see org.apache.wiki.WikiPage#setAttribute(java.lang.String, java.lang.Object)
     */
    public void setAttribute( String key, Object attribute )
    {
        m_attributes.put( key, attribute );
    }

    /* (non-Javadoc)
     * @see org.apache.wiki.WikiPage#getAttributes()
     */
    public Map getAttributes() 
    {
        return m_attributes;
    }

    /* (non-Javadoc)
     * @see org.apache.wiki.WikiPage#removeAttribute(java.lang.String)
     */
    public Object removeAttribute( String key )
    {
        return m_attributes.remove( key );
    }

    /* (non-Javadoc)
     * @see org.apache.wiki.WikiPage#getLastModified()
     */
    public Date getLastModified()
    {
        return m_lastModified;
    }

    /* (non-Javadoc)
     * @see org.apache.wiki.WikiPage#setLastModified(java.util.Date)
     */
    public void setLastModified( Date date )
    {
        m_lastModified = date;
    }

    /* (non-Javadoc)
     * @see org.apache.wiki.WikiPage#setVersion(int)
     */
    public void setVersion( int version )
    {
        m_version = version;
    }

    /* (non-Javadoc)
     * @see org.apache.wiki.WikiPage#getVersion()
     */
    public int getVersion()
    {
        return m_version;
    }

    /* (non-Javadoc)
     * @see org.apache.wiki.WikiPage#getSize()
     */
    public long getSize()
    {
        return m_fileSize;
    }

    /* (non-Javadoc)
     * @see org.apache.wiki.WikiPage#setSize(long)
     */
    public void setSize( long size )
    {
        m_fileSize = size;
    }

    /* (non-Javadoc)
     * @see org.apache.wiki.WikiPage#getAcl()
     */
    public Acl getAcl()
    {
        return m_accessList;
    }

    /* (non-Javadoc)
     * @see org.apache.wiki.WikiPage#setAcl(org.apache.wiki.auth.acl.Acl)
     */
    public void setAcl( Acl acl )
    {
        m_accessList = acl;
    }

    /* (non-Javadoc)
     * @see org.apache.wiki.WikiPage#setAuthor(java.lang.String)
     */
    public void setAuthor( String author )
    {
        m_author = author;
    }

    /* (non-Javadoc)
     * @see org.apache.wiki.WikiPage#getAuthor()
     */
    public String getAuthor()
    {
        return m_author;
    }
    
    /* (non-Javadoc)
     * @see org.apache.wiki.WikiPage#getWiki()
     */
    // FIXME: Should we rename this method?
    public String getWiki()
    {
        return m_name.getSpace();
    }

    /* (non-Javadoc)
     * @see org.apache.wiki.WikiPage#invalidateMetadata()
     */
    public void invalidateMetadata()
    {        
        m_hasMetadata = false;
        setAcl( null );
        m_attributes.clear();
    }

    private boolean m_hasMetadata = false;

    /* (non-Javadoc)
     * @see org.apache.wiki.WikiPage#hasMetadata()
     */
    public boolean hasMetadata()
    {
        return m_hasMetadata;
    }

    /* (non-Javadoc)
     * @see org.apache.wiki.WikiPage#setHasMetadata()
     */
    public void setHasMetadata()
    {
        m_hasMetadata = true;
    }

    /* (non-Javadoc)
     * @see org.apache.wiki.WikiPage#toString()
     */
    public String toString()
    {
        return "WikiPage ["+m_name+",ver="+m_version+",mod="+m_lastModified+"]";
    }

    /* (non-Javadoc)
     * @see org.apache.wiki.WikiPage#clone()
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
     * @see org.apache.wiki.WikiPage#compareTo(java.lang.Object)
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
     * @see org.apache.wiki.WikiPage#hashCode()
     */
    public int hashCode()
    {
        return m_name.hashCode() * m_version;
    }

    public InputStream getContentAsStream()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public String getContentType()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public Set<String> getReferrers()
    {
        // TODO Auto-generated method stub
        return null;
    }
    
    public void setContent( InputStream in ) throws WikiException
    {
        try
        {
            m_node.setProperty( ATTR_CONTENT, in );
        }
        catch( RepositoryException e )
        {
            throw new WikiException("Unable to set content",e);
        }
    }

    public void setContentType( String contentType )
    {
        // TODO Auto-generated method stub
        
    }
    
    public void save() throws WikiException
    {
        try
        {
            if( m_node.isNew() )
                m_node.getParent().save();
            else
                m_node.save();
        }
        catch( RepositoryException e )
        {
            throw new WikiException("Save failed",e);
        }
    }
    
    private static final String ATTR_CONTENT = "wiki:content";
    
    public String getContentAsString() throws WikiException
    {
        try
        {
            Property p = m_node.getProperty( ATTR_CONTENT );
                
            return p.getString();
        }
        catch( PathNotFoundException e )
        {
        }
        catch( RepositoryException e )
        {
            throw new WikiException("Unable to get property",e);
        }
        
        return null;
    }

    public void setContent( String content ) throws WikiException
    {
        try
        {
            m_node.setProperty( ATTR_CONTENT, content );
        }
        catch( RepositoryException e )
        {
            throw new WikiException("Unable to set content",e);
        }
    }

}
