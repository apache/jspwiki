/*
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

import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.core.Page;
import org.apache.wiki.api.providers.PageProvider;
import org.apache.wiki.auth.acl.Acl;
import org.apache.wiki.auth.acl.AclEntry;
import org.apache.wiki.auth.acl.AclImpl;
import org.apache.wiki.pages.PageManager;

import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;


/**
 *  Simple wrapper class for the Wiki page attributes.  The Wiki page content is moved around in Strings, though.
 */
// FIXME: We need to rethink how metadata is being used - probably the author, date, etc. should also be part of the metadata.  We also
//        need to figure out the metadata lifecycle.
public class WikiPage implements Page {

    private       String     m_name;
    private       Engine     m_engine;
    private       String     m_wiki;
    private Date             m_lastModified;
    private long             m_fileSize = -1;
    private int              m_version = PageProvider.LATEST_VERSION;
    private String           m_author = null;
    private final Map< String, Object > m_attributes = new HashMap<>();

    private Acl m_accessList = null;

    /**
     * Create a new WikiPage using a given engine and name.
     *
     * @param engine The Engine that owns this page.
     * @param name   The name of the page.
     */
    public WikiPage( final Engine engine, final String name ) {
        m_engine = engine;
        m_name = name;
        m_wiki = engine.getApplicationName();
    }

    /**
     * Create a new WikiPage using a given engine and name.
     *
     * @param engine The Engine that owns this page.
     * @param name   The name of the page.
     * @deprecated kept for compatibility with page/attachment providers not using public API. Use {@code WikiPage(Engine, String)} instead.
     */
    @Deprecated
    public WikiPage( final WikiEngine engine, final String name ) {
        this( ( Engine )engine, name );
    }

    /**
     * Returns the name of the page.
     *
     * @return The page name.
     */
    @Override
    public String getName() {
        return m_name;
    }

    /**
     * A WikiPage may have a number of attributes, which might or might not be available.  Typically attributes are things that do not need
     * to be stored with the wiki page to the page repository, but are generated on-the-fly.  A provider is not required to save them, but
     * they can do that if they really want.
     *
     * @param key The key using which the attribute is fetched
     * @return The attribute.  If the attribute has not been set, returns null.
     */
    @Override
    @SuppressWarnings( "unchecked" )
    public < T > T getAttribute( final String key ) {
        return ( T )m_attributes.get( key );
    }

    /**
     * Sets an metadata attribute.
     *
     * @param key       The key for the attribute used to fetch the attribute later on.
     * @param attribute The attribute value
     * @see #getAttribute(String)
     */
    @Override
    public void setAttribute( final String key, final Object attribute ) {
        m_attributes.put( key, attribute );
    }

    /**
     * Returns the full attributes Map, in case external code needs to iterate through the attributes.
     *
     * @return The attribute Map.  Please note that this is a direct
     * reference, not a copy.
     */
    @Override
    public Map< String, Object > getAttributes() {
        return m_attributes;
    }

    /**
     * Removes an attribute from the page, if it exists.
     *
     * @param key The key for the attribute
     * @return If the attribute existed, returns the object.
     * @since 2.1.111
     */
    @Override
    @SuppressWarnings( "unchecked" )
    public < T > T removeAttribute( final String key ) {
        return ( T )m_attributes.remove( key );
    }

    /**
     * Returns the date when this page was last modified.
     *
     * @return The last modification date
     */
    @Override
    public Date getLastModified() {
        return m_lastModified;
    }

    /**
     * Sets the last modification date.  In general, this is only changed by the provider.
     *
     * @param date The date
     */
    @Override
    public void setLastModified( final Date date ) {
        m_lastModified = date;
    }

    /**
     * Sets the page version.  In general, this is only changed by the provider.
     *
     * @param version The version number
     */
    @Override
    public void setVersion( final int version ) {
        m_version = version;
    }

    /**
     * Returns the version that this WikiPage instance represents.
     *
     * @return the version number of this page.
     */
    @Override
    public int getVersion() {
        return m_version;
    }

    /**
     * Returns the size of the page.
     *
     * @return the size of the page.
     * @since 2.1.109
     */
    @Override
    public long getSize() {
        return m_fileSize;
    }

    /**
     * Sets the size.  Typically called by the provider only.
     *
     * @param size The size of the page.
     * @since 2.1.109
     */
    @Override
    public void setSize( final long size ) {
        m_fileSize = size;
    }

    /**
     * Returns the Acl for this page.  May return <code>null</code>, in case there is no Acl defined, or it has not yet been set by
     * {@link #setAcl(Acl)}.
     *
     * @return The access control list.  May return null, if there is no acl.
     */
    public Acl getAcl() {
        return m_accessList;
    }

    /**
     * Sets the Acl for this page. Note that method does <em>not</em> persist the Acl itself to back-end storage or in page markup;
     * it merely sets the internal field that stores the Acl. To persist the Acl, callers should invoke
     * {@link org.apache.wiki.auth.acl.AclManager#setPermissions(Page, org.apache.wiki.api.core.Acl)}.
     *
     * @param acl The Acl to set
     * @deprecated use {@link #setAcl(org.apache.wiki.api.core.Acl)}
     * @see #setAcl(org.apache.wiki.api.core.Acl)
     */
    @Deprecated
    public void setAcl( final Acl acl ) {
        setAcl( ( org.apache.wiki.api.core.Acl )acl );
    }

    /**
     * Sets the Acl for this page. Note that method does <em>not</em> persist the Acl itself to back-end storage or in page markup;
     * it merely sets the internal field that stores the Acl. To persist the Acl, callers should invoke
     * {@link org.apache.wiki.auth.acl.AclManager#setPermissions(Page, org.apache.wiki.api.core.Acl)}.
     *
     * @param acl The Acl to set
     */
    public void setAcl( final org.apache.wiki.api.core.Acl acl ) {
        m_accessList = ( Acl )acl;
    }

    /**
     * Sets the author of the page.  Typically called only by the provider.
     *
     * @param author The author name.
     */
    @Override
    public void setAuthor( final String author ) {
        m_author = author;
    }

    /**
     * Returns author name, or null, if no author has been defined.
     *
     * @return Author name, or possibly null.
     */
    @Override
    public String getAuthor() {
        return m_author;
    }

    /**
     * Returns the wiki name for this page
     *
     * @return The name of the wiki.
     */
    @Override
    public String getWiki() {
        return m_wiki;
    }

    /**
     * This method will remove all metadata from the page.
     */
    @Override
    public void invalidateMetadata() {
        m_hasMetadata = false;
        setAcl( null );
        m_attributes.clear();
    }

    private boolean m_hasMetadata = false;

    /**
     * Returns <code>true</code> if the page has valid metadata; that is, it has been parsed. Note that this method is a kludge to
     * support our pre-3.0 metadata system, and as such will go away with the new API.
     *
     * @return true, if the page has metadata.
     */
    @Override
    public boolean hasMetadata() {
        return m_hasMetadata;
    }

    /**
     * Sets the metadata flag to true.  Never call.
     */
    @Override
    public void setHasMetadata() {
        m_hasMetadata = true;
    }

    /**
     * Returns a debug-suitable version of the page.
     *
     * @return A debug string.
     */
    @Override
    public String toString() {
        return "WikiPage [" + m_wiki + ":" + m_name + ",ver=" + m_version + ",mod=" + m_lastModified + "]";
    }

    /**
     *  Creates a deep clone of a WikiPage.  Strings are not cloned, since they're immutable.  Attributes are not cloned, only the internal
     *  HashMap (so if you modify the contents of a value of an attribute, these will reflect back to everyone).
     *  
     *  @return A deep clone of the WikiPage
     */
    @Override
    public WikiPage clone() {
        final WikiPage p = new WikiPage( m_engine, m_name );
        p.m_wiki         = m_wiki;
        p.m_author       = m_author;
        p.m_version      = m_version;
        p.m_lastModified = m_lastModified != null ? (Date)m_lastModified.clone() : null;
        p.m_fileSize     = m_fileSize;
        for( final Map.Entry< String, Object > entry : m_attributes.entrySet() ) {
            p.m_attributes.put( entry.getKey(), entry.getValue() );
        }

        if( m_accessList != null ) {
            p.m_accessList = new AclImpl();
            for( final Enumeration< AclEntry > entries = m_accessList.entries(); entries.hasMoreElements(); ) {
                final AclEntry e = entries.nextElement();
                p.m_accessList.addEntry( e );
            }
        }
            
        return p;
    }
    
    /**
     *  Compares a page with another by name using the defined PageNameComparator.  If the same name, compares their versions.
     *  
     *  @param page The page to compare against
     *  @return -1, 0 or 1
     */
    @Override
    public int compareTo( final Page page ) {
        if( this == page ) {
            return 0; // the same object
        }

        int res = m_engine.getManager( PageManager.class ).getPageSorter().compare( this.getName(), page.getName() );
        if( res == 0 ) {
            res = this.getVersion() - page.getVersion();
        }
        return res;
    }
    
    /**
     *  A page is equal to another page if its name and version are equal.
     *  
     *  {@inheritDoc}
     */
    public boolean equals( final Object o ) {
        if( o instanceof WikiPage ) {
            final WikiPage wp = ( WikiPage )o;
            if( wp.getName().equals( getName() ) ) {
                return wp.getVersion() == getVersion();
            }
        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return m_name.hashCode() * m_version;
    }

}
