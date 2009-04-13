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
package org.apache.wiki.api;

import java.io.InputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.wiki.auth.acl.Acl;
import org.apache.wiki.content.PageNotFoundException;
import org.apache.wiki.content.WikiPath;
import org.apache.wiki.providers.ProviderException;


/**
 *  Simple wrapper class for the Wiki page attributes.  The Wiki page
 *  content is moved around in Strings, though.
 */
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
     *  Returns the name of the page without the space identifier.
     *  
     *  @return The page name.
     */
    public String getName();
    
    /**
     *  Returns the full, qualified, name of the WikiPage that includes the wiki space.
     *  In most cases, you should use this method.
     * 
     *  @return the qualified page name, for example <code>mywiki:Main</code>
     */
    public WikiPath getPath();

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
    public Serializable getAttribute( String key );

    /**
     *  Sets an metadata attribute.
     *  
     *  @see #getAttribute(String)
     *  @param key The key for the attribute used to fetch the attribute later on.
     *  @param attribute The attribute value
     */
    public void setAttribute( String key, Serializable attribute );

    /**
     * Returns the full attributes Map, in case external code needs
     * to iterate through the attributes.
     * 
     * @deprecated
     * @return The attribute Map.  Please note that this is a direct
     *         reference, not a copy.
     */
    public Map<String,Serializable> getAttributes();

    /**
     *  Removes an attribute from the page, if it exists.
     *  
     *  @param  key The key for the attribute
     *  @return If the attribute existed, returns the object.
     *  @since 2.1.111
     */
    public Serializable removeAttribute( String key );

    /**
     *  Returns the date when this page was last modified.
     *  
     *  @return The last modification date
     */
    public Date getLastModified();

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
     * {@link org.apache.wiki.auth.acl.AclManager#setPermissions(WikiPage, Acl)}.
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
 
    /**
     *  Returns the content of the WikiPage markup as a String.
     *  
     *  @return String depicting the contents.
     *  @throws ProviderException If the page has no contents or fetching the content failed.
     */
    public String getContentAsString() throws ProviderException;

    /**
     *  Returns the content of the WikiPage as an InputStream.
     *  
     *  @return The content of the page as an InputStream.
     *  @throws ProviderException If the page contents cannot be retrieved.
     */
    public InputStream getContentAsStream() throws ProviderException;
    
    /**
     *  Stores the state of the page, creating a new version in the
     *  repository.
     *  
     *  @throws ProviderException If the save cannot be completed.
     */
    public void save() throws ProviderException;
    
    /**
     *  Set the content of the page to a given String.  This directly writes
     *  to the wiki:content attribute.
     *  
     *  @param content New content of the page.
     *  @throws ProviderException If the page contents are illegal or cannot be stored.
     */
    public void setContent(String content) throws ProviderException;

    /**
     *  Sets the contents of the page to the contents of the given InputStream.
     * 
     *  @param in InputStream to read from.
     *  @throws ProviderException If the page cannot be stored.
     */
    public void setContent( InputStream in ) throws ProviderException;

    /**
     *  Returns a collection of all the WikiNames that this page refers to.
     *  
     *  @return A collection of WikiNames. May be an empty collection if this
     *          page refers to no other pages.
     *  @throws ProviderException If the references cannot be fetched.
     */
    public Collection<WikiPath> getRefersTo() throws ProviderException;
    
    /**
     *  Returns the parent of the page. 
     *  
     *  @return The parent of the page.
     *  @throws ProviderException If the parent page cannot be determined.
     *  @throws PageNotFoundException in case there is no parent page. 
     */
    public WikiPage getParent() throws PageNotFoundException, ProviderException;

    /**
     *  Returns a list of all subpages and attachments of this WikiPage.
     *  In case there are no subpages or attachments, returns an empty
     *  list.
     *  
     *  @return An ordered List of WikiPage objects.
     *  @throws ProviderException If something goes wrong.
     */
    public List<WikiPage> getChildren() throws ProviderException;
    
    /**
     *  Returns true, if this page is an attachment (that is, does not
     *  contain wikimarkup and has a parent page).
     *  <p>
     *  As of 3.0, you should not do an instanceof Attachment test to
     *  test for attachmentness, since Attachment is now an interface.
     *  Use this method instead.
     *  
     *  @return True, if this is an attachment. False otherwise.
     *  @throws ProviderException If the attachmentness cannot be determined.
     */
    public boolean isAttachment() throws ProviderException;
}
