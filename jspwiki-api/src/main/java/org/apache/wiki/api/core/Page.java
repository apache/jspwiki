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
package org.apache.wiki.api.core;

import java.util.Date;
import java.util.Map;


public interface Page extends Cloneable {

    /** "Summary" is a short summary of the page.  It is a String. */
    String DESCRIPTION = "summary";

    /** A special variable name for storing a page alias. */
    String ALIAS = "alias";

    /** A special variable name for storing a redirect note */
    String REDIRECT = "redirect";

    /** A special variable name for storing the author. */
    String AUTHOR = "author";

    /** A special variable name for storing a changenote. */
    String CHANGENOTE = "changenote";

    /** A special variable name for storing a viewcount. */
    String VIEWCOUNT = "viewcount";

    /**
     *  Returns the name of the page.
     *
     *  @return The page name.
     */
    String getName();

    /**
     *  Returns the date when this page was last modified.
     *
     *  @return The last modification date
     */
    Date getLastModified();

    /**
     *  Sets the last modification date.  In general, this is only changed by the provider.
     *
     *  @param date The date
     */
    void setLastModified( Date date );

    /**
     *  Sets the page version.  In general, this is only changed by the provider.
     *
     *  @param version The version number
     */
    void setVersion( int version );

    /**
     *  Returns the version that this WikiPage instance represents.
     *
     *  @return the version number of this page.
     */
    int getVersion();

    /**
     *  Returns the size of the page.
     *
     *  @return the size of the page. 
     *  @since 2.1.109
     */
    long getSize();

    /**
     *  Sets the size.  Typically called by the provider only.
     *
     *  @param size The size of the page.
     *  @since 2.1.109
     */
    void setSize( long size );

    /**
     *  Sets the author of the page.  Typically called only by the provider.
     *
     *  @param author The author name.
     */
    void setAuthor( String author );

    /**
     *  Returns author name, or null, if no author has been defined.
     *
     *  @return Author name, or possibly null.
     */
    String getAuthor();

    /**
     *  Returns the wiki name for this page
     *
     *  @return The name of the wiki.
     */
    String getWiki();

    /** This method will remove all metadata from the page. */
    void invalidateMetadata();

    /**
     *  Returns <code>true</code> if the page has valid metadata; that is, it has been parsed. Note that this method is a kludge to
     *  support our pre-3.0 metadata system, and as such will go away with the new API.
     *
     *  @return true, if the page has metadata.
     */
    boolean hasMetadata();

    /** Sets the metadata flag to true.  Never call. */
    void setHasMetadata();

    /**
     *  A WikiPage may have a number of attributes, which might or might not be  available.  Typically attributes are things that do not
     *  need to be stored with the wiki page to the page repository, but are generated on-the-fly.  A provider is not required to save 
     *  them, but they can do that if they really want.
     *
     *  @param key The key using which the attribute is fetched
     *  @return The attribute.  If the attribute has not been set, returns null.
     */
    < T > T getAttribute( String key );

    /**
     *  Sets an metadata attribute.
     *
     *  @see #getAttribute(String)
     *  @param key The key for the attribute used to fetch the attribute later on.
     *  @param attribute The attribute value
     */
    void setAttribute( String key, Object attribute );

    /**
     *  Removes an attribute from the page, if it exists.
     *
     *  @param  key The key for the attribute
     *  @return If the attribute existed, returns the object.
     *  @since 2.1.111
     */
    < T > T removeAttribute( String key );

    /**
     * Returns the full attributes Map, in case external code needs to iterate through the attributes.
     *
     * @return The attribute Map.  Please note that this is a direct reference, not a copy.
     */
    Map< String, Object > getAttributes();

    /**
     *  Returns the Acl for this page.  May return <code>null</code>, in case there is no Acl defined, or it has not
     *  yet been set by {@link #setAcl(Acl)}.
     *
     *  @return The access control list.  May return null, if there is  no acl.
     */
    //Acl getAcl();

    /**
     * Sets the Acl for this page. Note that method does <em>not</em> persist the Acl itself to back-end storage or in page markup;
     * it merely sets the internal field that stores the Acl. To persist the Acl, callers should invoke 
     * {@link org.apache.wiki.auth.acl.AclManager#setPermissions(org.apache.wiki.api.core.Page, org.apache.wiki.api.core.Acl)}.
     *
     * @param acl The Acl to set
     */
    //void setAcl( Acl acl );

}
