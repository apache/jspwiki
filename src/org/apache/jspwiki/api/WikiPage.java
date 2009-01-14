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
package org.apache.jspwiki.api;

import java.util.Date;
import java.util.Map;

import com.ecyrd.jspwiki.auth.acl.Acl;

/**
 *  Simple wrapper class for the Wiki page attributes.  The Wiki page
 *  content is moved around in Strings, though.
 */

// FIXME: We need to rethink how metadata is being used - probably the 
//        author, date, etc. should also be part of the metadata.  We also
//        need to figure out the metadata lifecycle.

public interface WikiPage
{
    /**
     *  "Summary" is a short summary of the page.  It is a String.
     */
    public static final String DESCRIPTION = "summary";

    /** A special variable name for storing a page alias. */
    public static final String ALIAS = "alias";
    
    /** A special variable name for storing a redirect note */
    public static final String REDIRECT = "redirect";

    /** A special variable name for storing a changenote. */
    public static final String CHANGENOTE = "changenote";
    

    /**
     *  Returns the name of the page.
     *  
     *  @return The page name.
     */
    public String getName();
    
    /**
     * Returns the full, qualified, name of the WikiPage that includes the wiki name.
     * Used by the {@link com.ecyrd.jspwiki.ui.stripes.HandlerInfo} class and
     * {@link com.ecyrd.jspwiki.ui.stripes.HandlerPermission} annotations.
     * @return the qualified page name, for example <code>mywiki:Main</code>
     */
    public String getQualifiedName();

    /**
     *  A WikiPage may have a number of attributes, which might or might not be 
     *  available.  Typically attributes are things that do not need to be stored
     *  with the wiki page to the page repository, but are generated
     *  on-the-fly.  A provider is not required to save them, but they
     *  can do that if they really want.
     *
     *  @param key The key to use for fetching the attribute
     *  @return The attribute.  If the attribute has not been set, returns null.
     */
    public Object getAttribute( String key );

    /**
     *  Sets an metadata attribute.
     *  
     *  @see #getAttribute(String)
     *  @param key The key for the attribute used to fetch the attribute later on.
     *  @param attribute The attribute value
     */
    public void setAttribute( String key, Object attribute );

    /**
     * Returns the full attributes Map, in case external code needs
     * to iterate through the attributes.
     * 
     * @return The attribute Map.  Please note that this is a direct
     *         reference, not a copy.
     */
    public Map getAttributes();

    /**
     *  Removes an attribute from the page, if it exists.
     *  
     *  @param  key The key for the attribute
     *  @return If the attribute existed, returns the object.
     *  @since 2.1.111
     */
    public Object removeAttribute( String key );

    /**
     *  Returns the date when this page was last modified.
     *  
     *  @return The last modification date
     */
    public Date getLastModified();

    /**
     *  Sets the last modification date.  In general, this is only
     *  changed by the provider.
     *  
     *  @param date The date
     */
    public void setLastModified( Date date );

    /**
     *  Sets the page version.  In general, this is only changed
     *  by the provider.
     *  
     *  @param version The version number
     */
    public void setVersion( int version );

    /**
     *  Returns the version that this WikiPage instance represents.
     *  
     *  @return the version number of this page.
     */
    public int getVersion();

    /**
     *  Returns the size of the page.
     *  
     *  @return the size of the page. 
     *  @since 2.1.109
     */
    public long getSize();

    /**
     *  Sets the size.  Typically called by the provider only.
     *  
     *  @param size The size of the page.
     *  @since 2.1.109
     */
    public void setSize( long size );

    /**
     *  Returns the Acl for this page.  May return <code>null</code>, 
     *  in case there is no Acl defined, or it has not
     *  yet been set by {@link #setAcl(Acl)}.
     *  
     *  @return The access control list.  May return null, if there is 
     *          no acl.
     */
    public Acl getAcl();

    /**
     * Sets the Acl for this page. Note that this method does <em>not</em>
     * persist the Acl itself to back-end storage or in page markup;
     * it merely sets the internal field that stores the Acl. To
     * persist the Acl, callers should invoke 
     * {@link com.ecyrd.jspwiki.auth.acl.AclManager#setPermissions(WikiPage, Acl)}.
     * @param acl The Acl to set
     */
    public void setAcl( Acl acl );

    /**
     *  Sets the author of the page.  Typically called only by the provider.
     *  
     *  @param author The author name.
     */
    public void setAuthor( String author );

    /**
     *  Returns author name, or null, if no author has been defined.
     *  
     *  @return Author name, or possibly null.
     */
    public String getAuthor();
    
    /**
     *  Returns the wiki name for this page
     *  
     *  @return The name of the wiki.
     */
    // FIXME: Should we rename this method?
    public String getWiki();

    /**
     *  This method will remove all metadata from the page.
     */
    public void invalidateMetadata();

    /**
     *  Returns <code>true</code> if the page has valid metadata; that is, it has been parsed.
     *  Note that this method is a kludge to support our pre-3.0 metadata system, and as such
     *  will go away with the new API.
     *  
     *  @return true, if the page has metadata.
     */
    public boolean hasMetadata();

    /**
     *  Sets the metadata flag to true.  Never call.
     */
    public void setHasMetadata();

    /**
     *  Creates a deep clone of a WikiPage.  Strings are not cloned, since
     *  they're immutable.  Attributes are not cloned, only the internal
     *  HashMap (so if you modify the contents of a value of an attribute,
     *  these will reflect back to everyone).
     *  
     *  @return A deep clone of the WikiPage
     */
    public Object clone();
    
    /**
     *  Compares a page with another.  The primary sorting order
     *  is according to page name, and if they have the same name,
     *  then according to the page version.
     *  
     *  @param o The object to compare against
     *  @return -1, 0 or 1
     */
    public int compareTo( Object o );
 
    public String getContentAsString() throws WikiException;

    public void save() throws WikiException;
    
    public void setContent(String content) throws WikiException;
}
