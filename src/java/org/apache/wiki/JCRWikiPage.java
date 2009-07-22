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

import java.io.*;
import java.util.*;

import javax.jcr.*;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

import org.apache.wiki.api.WikiPage;
import org.apache.wiki.attachment.Attachment;
import org.apache.wiki.auth.acl.Acl;
import org.apache.wiki.content.ContentManager;
import org.apache.wiki.content.PageNotFoundException;
import org.apache.wiki.content.WikiPath;
import org.apache.wiki.providers.ProviderException;


/**
 *  JCR-backed implementation of the WikiPage.
 *  
 *  @since 3.0
 */

// FIXME: We need to rethink how metadata is being used - probably the 
//        author, date, etc. should also be part of the metadata.  We also
//        need to figure out the metadata lifecycle.

public class JCRWikiPage
    implements Cloneable, WikiPage, Attachment, Comparable<WikiPage>
{
    private static final long serialVersionUID = 1L;

    private static final String LASTMODIFIED = "wiki:lastModified";

    private static final String AUTHOR       = "wiki:author";

    private static final String ACL          = "wiki:acl";

    private static final String ATTR_CONTENT = "wiki:content";

    public static final String ATTR_VERSION  = "wiki:version";
    
    public  static final String CONTENTTYPE  = "wiki:contentType";
    

    private       WikiPath   m_path;
    private       WikiEngine m_engine;
    private String           m_jcrPath = null;
    
    /** 
     * Use {@link WikiEngine#createPage(WikiPath)} instead. 
     * @deprecated 
     */
    public JCRWikiPage( WikiEngine engine, WikiPath path )
    {
        m_engine  = engine;
        m_path    = path;
        m_jcrPath = ContentManager.getJCRPath( path );
    }

    /**
     *  Creates a JCRWikiPage using the default path.
     *  
     *  @param engine
     *  @param node
     *  @throws RepositoryException
     *  @throws ProviderException
     */
    public JCRWikiPage(WikiEngine engine, Node node)
        throws RepositoryException, ProviderException
    {
        this( engine, ContentManager.getWikiPath( node.getPath() ), node );
    }
        
    /**
     *  Creates a WikiPage with a given Node.  This constructor
     *  can be used when you wish to put create a WikiPage outside the
     *  default page hierarchy, for example when you need to create
     *  a temporary storage for workflows.
     *  
     *  @param engine
     *  @param name
     *  @param node
     *  @throws RepositoryException
     */
    public JCRWikiPage(WikiEngine engine, WikiPath name, Node node) 
        throws RepositoryException
    {
        m_engine  = engine;
        m_jcrPath = node.getPath();
        m_path    = name;
    }
    
    /**
     *  Returns the JCR Node which backs this WikiPage implementation.
     *  
     *  @return The JCR Node
     *  @throws RepositoryException If the page cannot be located.
     */
    public Node getJCRNode() throws RepositoryException
    {
        return m_engine.getContentManager().getJCRNode(m_jcrPath);
    }
    
    /* (non-Javadoc)
     * @see org.apache.wiki.WikiPage#getName()
     */
    public String getName()
    {
        return m_path.getPath();
    }
    
    /* (non-Javadoc)
     * @see org.apache.wiki.WikiPage#getQualifiedName()
     */
    public WikiPath getPath()
    {
        return m_path;
    }

    /* (non-Javadoc)
     * @see org.apache.wiki.WikiPage#getAttribute(java.lang.String)
     */
    public Serializable getAttribute( String key )
    {
        try
        {
            Property property = getJCRNode().getProperty( key );
            Object value = getValue( property );
            if ( value instanceof Serializable )
            {
                return (Serializable)value;
            }
            else
            {
                throw new IllegalStateException( "The value returned by " + key + " was not a Serializable, as expected.");
            }
        }
        catch( ItemNotFoundException e ) {}
        catch( RepositoryException e ) {} // FIXME: Should log this at least.
        
        return null;
    }

    /**
     *  Direct access to JCR Properties.
     *  
     *  @param key the key for which we want the property
     *  @return Property
     *  @throws PathNotFoundException
     *  @throws RepositoryException
     */
    public Property getProperty( String key ) throws PathNotFoundException, RepositoryException
    {
        return getJCRNode().getProperty(key);
    }
    
    private Object getValue( Property property ) throws RepositoryException, ValueFormatException
    {
        switch( property.getType() )
        {
            case PropertyType.STRING:
                return property.getString();
        }
        
        return property.getString();
    }

    /**
     * {@inheritDoc}
     */
    public void setAttribute( String key, Serializable attribute )
    {
        try
        {
            // FIXME: Using the string value can't possibly be
            // the right thing to do here.
            getJCRNode().setProperty( key, attribute.toString() );
        }
        catch(RepositoryException e) {} // FIXME: Should log
    }

    public void setAttribute( String key, Date attribute )
    {
        try
        {
            Calendar c = Calendar.getInstance();
            c.setTime( attribute );
            getJCRNode().setProperty( key, c );
        }
        catch(RepositoryException e) {} // FIXME: Should log        
    }
    
    /* (non-Javadoc)
     * @see org.apache.wiki.WikiPage#getAttributes()
     */
    //
    // This method will be removed, since it makes no sense to get
    // all of the attributes, as the end result may be very, very large.
    //
    public Map<String,Serializable> getAttributes() 
    {
        return null;
    }

    /* (non-Javadoc)
     * @see org.apache.wiki.WikiPage#removeAttribute(java.lang.String)
     */
    public Serializable removeAttribute( String key )
    {
        try
        {
            Property p = getJCRNode().getProperty( key );
            
            Object value = getValue(p);
            if ( value instanceof Serializable )
            {
                p.remove();
                return (Serializable)value;
            }
            else
            {
                throw new IllegalStateException( "The value returned by " + key + " was not a Serializalble, as expected.");
            }
        }
        catch(RepositoryException e) {}
        
        return null;
    }

    /**
     * {@inheritDoc}
     * @see org.apache.wiki.api.WikiPage#getLastModified()
     */
    public Date getLastModified()
    {
        try
        {
            if ( getJCRNode().hasProperty( LASTMODIFIED ) )
            {
                return getJCRNode().getProperty( LASTMODIFIED ).getDate().getTime();
            }
        }
        catch( RepositoryException e )
        {
            // FIXME: Should rethrow
        }
        return null;
    }

    /* (non-Javadoc)
     * @see org.apache.wiki.WikiPage#setLastModified(java.util.Date)
     */
    public void setLastModified( Date date )
    {
        setAttribute( LASTMODIFIED, date );
    }

    /* (non-Javadoc)
     * @see org.apache.wiki.WikiPage#getVersion()
     */
    public int getVersion()
    {
        try
        {
            return (int) getJCRNode().getProperty( ATTR_VERSION ).getLong();
        }
        catch( PathNotFoundException e )
        {}
        catch( RepositoryException e )
        {
            // FIXME: SHould really throw something else.
        }
        return 0;
    }

    /* (non-Javadoc)
     * @see org.apache.wiki.WikiPage#getSize()
     */
    public long getSize()
    {
        try
        {
            return getJCRNode().getProperty( ATTR_CONTENT ).getLength();
        }
        catch(RepositoryException e){}
        
        return -1;
    }

    /* (non-Javadoc)
     * @see org.apache.wiki.WikiPage#getAcl()
     */
    public Acl getAcl()
    {
        ObjectInputStream in = null;
        
        try
        {
            Property acl = getJCRNode().getProperty( ACL );
            
            in = new ObjectInputStream( acl.getStream() );
            
            Acl a = (Acl) in.readObject();
            
            return a;
        }
        catch( PathNotFoundException e )
        {
            // No ACL, so this is ok.
        }
        catch( RepositoryException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch( IOException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch( ClassNotFoundException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        finally
        {
            if( in != null )
                try
                {
                    in.close();
                }
                catch( IOException e )
                {
                }
        }
        
        return null;
    }

    /* (non-Javadoc)
     * @see org.apache.wiki.WikiPage#setAcl(org.apache.wiki.auth.acl.Acl)
     */
    public void setAcl( Acl acl )
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        ObjectOutputStream oout;
        try
        {
            oout = new ObjectOutputStream(out);
            oout.writeObject( acl );
        
            oout.close();
        
            getJCRNode().setProperty( ACL, new ByteArrayInputStream(out.toByteArray()) );
        }
        catch( IOException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch( ValueFormatException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch( VersionException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch( LockException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch( ConstraintViolationException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch( RepositoryException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
    }

    /* (non-Javadoc)
     * @see org.apache.wiki.WikiPage#setAuthor(java.lang.String)
     */
    public void setAuthor( String author )
    {
        setAttribute( AUTHOR, author );
    }

    /* (non-Javadoc)
     * @see org.apache.wiki.WikiPage#getAuthor()
     */
    public String getAuthor()
    {
        return (String)getAttribute( AUTHOR );
    }
    
    /* (non-Javadoc)
     * @see org.apache.wiki.WikiPage#getWiki()
     */
    // FIXME: Should we rename this method?
    public String getWiki()
    {
        return m_path.getSpace();
    }

    /* (non-Javadoc)
     * @see org.apache.wiki.WikiPage#toString()
     */
    public String toString()
    {
        return "WikiPage ["+m_path+",ver="+getVersion()+",mod="+getLastModified()+"]";
    }

    /* (non-Javadoc)
     * @see org.apache.wiki.WikiPage#clone()
     */
    public Object clone()
    {
        JCRWikiPage p = new JCRWikiPage( m_engine, m_path );
            
        return p;
    }
    
    /**
     * {@inheritDoc}
     * @see org.apache.wiki.api.WikiPage#compareTo(java.lang.Object)
     */
    public int compareTo( WikiPage o )
    {
        WikiPage c = (WikiPage)o;
        
        int res = this.getName().compareTo(c.getName());
        
        if( res == 0 ) res = this.getVersion()-c.getVersion();
            
        return res;
    }
    
    /**
     *  A page is equal to another page if its name and version are equal.
     *  
     *  {@inheritDoc}
     */
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

    /* (non-Javadoc)
     * @see org.apache.wiki.WikiPage#hashCode()
     */
    public int hashCode()
    {
        return m_path.hashCode() * getVersion();
    }

    public InputStream getContentAsStream() throws ProviderException
    {
        try
        {
            Property p = getJCRNode().getProperty( ATTR_CONTENT );

            return p.getStream();
        }
        catch( PathNotFoundException e )
        {
        }
        catch( RepositoryException e )
        {
            throw new ProviderException("Unable to get property",e);
        }
        return null;
    }

    public String getContentType()
    {
        return (String)getAttribute( CONTENTTYPE );
    }

    public List<WikiPath> getReferrers() throws ProviderException
    {
        return m_engine.getReferenceManager().getReferredBy( m_path );
    }
    
    public List<WikiPath> getRefersTo() throws ProviderException
    {
        return m_engine.getReferenceManager().getRefersTo( m_path );
    }
    

    public void setContent( InputStream in ) throws ProviderException
    {
        try
        {
            getJCRNode().setProperty( ATTR_CONTENT, in );
        }
        catch( RepositoryException e )
        {
            throw new ProviderException("Unable to set content",e);
        }
    }

    public void setContentType( String contentType )
    {
        setAttribute( CONTENTTYPE, contentType );
    }
    
    /**
     * {@inheritDoc}. This implementation delegates to {@link ContentManager#save(WikiPage)}.
     */
    public void save() throws ProviderException
    {
        try
        {
            m_engine.getContentManager().save( this );
        }
        catch( RepositoryException e )
        {
            throw new ProviderException("Save failed",e);
        }
    }

    public String getContentAsString() throws ProviderException
    {
        try
        {
            Property p = getJCRNode().getProperty( ATTR_CONTENT );
                
            return p.getString();
        }
        catch( PathNotFoundException e )
        {
        }
        catch( RepositoryException e )
        {
            throw new ProviderException("Unable to get property",e);
        }
        
        return null;
    }

    public void setContent( String content ) throws ProviderException
    {
        try
        {
            getJCRNode().setProperty( ATTR_CONTENT, content );
        }
        catch( RepositoryException e )
        {
            throw new ProviderException("Unable to set content",e);
        }
    }

    /**
     *  Returns the parent page of this subpage. If this is not a subpage,
     *  it will simply throw a PageNotFoundException.
     */
    public WikiPage getParent() throws PageNotFoundException, ProviderException
    {
        return m_engine.getContentManager().getPage( m_path.getParent() );
    }

    /**
     * Returns the file name of the WikiPage. For both attachments and wiki pages,
     * this is the name portion of the WikiPath that comes after the last trailing slash.
     * It is equal to the value of {@link WikiPath#getName()}.
     */
    public String getFileName()
    {
        return m_path.getName();
    }

    public boolean isLatest() throws RepositoryException
    {
        // TODO: This is a bit kludgish, but works.
        return getJCRNode().getPath().indexOf( "/wiki:versions/" ) == -1;
    }
    
    /** @deprecated */
    public boolean isCacheable()
    {
        // TODO Auto-generated method stub
        return false;
    }

    /** @deprecated */
    public void setCacheable( boolean value )
    {
        // TODO Auto-generated method stub
        
    }

    public void setFileName( String name )
    {
        // TODO Auto-generated method stub
        
    }

    /**
     * Returns <code>true</code> if this WikiPage exists in the repository and is of any content
     * type other than {@link ContentManager#JSPWIKI_CONTENT_TYPE}; <code>false</code> otherwise.
     */
    public boolean isAttachment() throws ProviderException
    {
        String contentType = getContentType();
        boolean exists = m_engine.getContentManager().pageExists( getPath() );
        if( !exists || contentType == null || ContentManager.JSPWIKI_CONTENT_TYPE.equals( contentType ) ) return false;
        
        return true;
    }

    /**
     *  {@inheritDoc}
     */
    public List<WikiPage> getChildren() throws ProviderException
    {
        ArrayList<WikiPage> pages = new ArrayList<WikiPage>();
        
        try
        {
            NodeIterator iter = getJCRNode().getNodes();
        
            while( iter.hasNext() )
            {
                Node n = iter.nextNode();
                
                //
                //  We will not count any page which has a namespace as
                //  a child of this WikiPage.
                //
                if( n.getName().indexOf( ':' ) == -1 )
                {                
                    pages.add( new JCRWikiPage( m_engine, iter.nextNode() ) );
                }
            }
        }
        catch( PathNotFoundException e )
        {
            return new ArrayList<WikiPage>();
        }
        catch( RepositoryException e )
        {
            throw new ProviderException("Unable to list children",e);
        }
        
        return pages;
    }

    // FIXME: This is really slow.  I mean, really, really slow, especially
    //        if you call it repeatedly.
    public JCRWikiPage getPredecessor() throws ProviderException, PageNotFoundException
    {
        List<WikiPage> versions = m_engine.getVersionHistory( getName() );

        int thisVersion = getVersion();

        WikiPage p = null;

        for( int i = 0; i < versions.size(); i++ )
        {
            if( versions.get( i ).getVersion() == thisVersion )
            {
                break;
            }
            p = versions.get( i );
        }
        
        if( p == null )
            throw new PageNotFoundException("No predecessor");
        
        return (JCRWikiPage)p;
    }

    public JCRWikiPage getCurrentVersion() throws ProviderException
    {
        try
        {
            return (JCRWikiPage) m_engine.getPage( getPath() );
        }
        catch( PageNotFoundException e )
        {
            throw new ProviderException("version cannot access current page - this can be serious",e);
        }
    }
    

}