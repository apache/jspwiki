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
package org.apache.wiki.xmlrpc;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.wiki.api.core.Attachment;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.ContextEnum;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.core.Page;
import org.apache.wiki.api.spi.Wiki;
import org.apache.wiki.attachment.AttachmentManager;
import org.apache.wiki.auth.AuthenticationManager;
import org.apache.wiki.auth.AuthorizationManager;
import org.apache.wiki.auth.WikiSecurityException;
import org.apache.wiki.auth.permissions.PermissionFactory;
import org.apache.wiki.pages.PageManager;
import org.apache.wiki.pages.PageTimeComparator;
import org.apache.wiki.plugin.WeblogEntryPlugin;
import org.apache.wiki.plugin.WeblogPlugin;
import org.apache.xmlrpc.XmlRpcException;

import java.io.ByteArrayInputStream;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

/**
 *  Provides handlers for all RPC routines of the MetaWeblog API.
 *  <P>
 *  JSPWiki does not support categories, and therefore we always return
 *  an empty list for getCategories().  Note also that this API is not
 *  suitable for general Wiki editing, since JSPWiki formats the entries
 *  in a wiki-compatible manner.  And you cannot choose your page names
 *  either.  Since 2.1.94 the entire MetaWeblog API is supported.
 *
 *  @since 2.1.7
 */

public class MetaWeblogHandler implements WikiRPCHandler {

    private static final Logger LOG = LogManager.getLogger( MetaWeblogHandler.class );

    private Context m_context;

    /**
     *  {@inheritDoc}
     */
    @Override
    public void initialize( final Context context )
    {
        m_context = context;
    }

    /**
     *  Does a quick check against the current user
     *  and does he have permissions to do the stuff
     *  that he really wants to.
     *  <p>
     *  If there is no authentication enabled, returns normally.
     *
     *  @throws XmlRpcException with the correct error message, if auth fails.
     */
    private void checkPermissions( final Page page,
                                   final String username,
                                   final String password,
                                   final String permission ) throws XmlRpcException {
        try {
            final AuthenticationManager amm = m_context.getEngine().getManager( AuthenticationManager.class );
            final AuthorizationManager mgr = m_context.getEngine().getManager( AuthorizationManager.class );

            if( amm.login( m_context.getWikiSession(), m_context.getHttpRequest(), username, password ) ) {
                if( !mgr.checkPermission( m_context.getWikiSession(), PermissionFactory.getPagePermission( page, permission ) ) ) {
                    throw new XmlRpcException( 1, "No permission" );
                }
            } else {
                throw new XmlRpcException( 1, "Unknown login" );
            }
        } catch( final WikiSecurityException e ) {
            throw new XmlRpcException( 1, e.getMessage(), e );
        }
    }

    /**
     *  JSPWiki does not support categories, therefore JSPWiki always returns an empty list for categories.
     *
     *  @param blogid The id of the blog.
     *  @param username The username to use
     *  @param password The password
     *  @throws XmlRpcException If something goes wrong
     *  @return An empty hashtable.
     */
    public Hashtable< Object, Object > getCategories( final String blogid, final String username, final String password )  throws XmlRpcException {
        final Page page = m_context.getEngine().getManager( PageManager.class ).getPage( blogid );
        checkPermissions( page, username, password, "view" );
        return new Hashtable<>();
    }

    private String getURL( final String page ) {
        return m_context.getEngine().getURL( ContextEnum.PAGE_VIEW.getRequestContext(), page,null );
    }

    /**
     *  Takes a wiki page, and creates a metaWeblog struct out of it.
     *
     *  @param page The actual entry page
     *  @return A metaWeblog entry struct.
     */
    private Hashtable< String,Object > makeEntry( final Page page ) {
        final Page firstVersion = m_context.getEngine().getManager( PageManager.class ).getPage( page.getName(), 1 );
        final Hashtable< String, Object > ht = new Hashtable<>();
        ht.put( "dateCreated", firstVersion.getLastModified() );
        ht.put( "link", getURL(page.getName() ) );
        ht.put( "permaLink", getURL(page.getName() ) );
        ht.put( "postid", page.getName() );
        ht.put( "userid", page.getAuthor() );

        final String pageText = m_context.getEngine().getManager( PageManager.class ).getText(page.getName());
        final int firstLine = pageText.indexOf('\n');

        String title = "";
        if( firstLine > 0 ) {
            title = pageText.substring( 0, firstLine );
        }

        if( title.trim().isEmpty() ) {
            title = page.getName();
        }

        // Remove wiki formatting
        while( title.startsWith("!") ) {
            title = title.substring(1);
        }

        ht.put("title", title);
        ht.put("description", pageText);

        return ht;
    }

    /**
     *  Returns a list of the recent posts to this weblog.
     *
     *  @param blogid The id of the blog.
     *  @param username The username to use
     *  @param password The password
     *  @param numberOfPosts How many posts to find
     *  @throws XmlRpcException If something goes wrong
     *  @return As per MetaweblogAPI specification
     */
    // FIXME: The implementation is suboptimal, as it goes through all of the blog entries.
    public Hashtable getRecentPosts( final String blogid, final String username, final String password, final int numberOfPosts ) throws XmlRpcException {
        final Hashtable<String, Hashtable<String, Object>> result = new Hashtable<>();
        LOG.info( "metaWeblog.getRecentPosts() called");
        final Page page = m_context.getEngine().getManager( PageManager.class ).getPage( blogid );
        checkPermissions( page, username, password, "view" );

        final WeblogPlugin plugin = new WeblogPlugin();
        final List< Page > changed = plugin.findBlogEntries( m_context.getEngine(), blogid, new Date( 0L ), new Date() );
        changed.sort( new PageTimeComparator() );

        int items = 0;
        for( final Iterator< Page > i = changed.iterator(); i.hasNext() && items < numberOfPosts; items++ ) {
            final Page p = i.next();
            result.put( "entry", makeEntry( p ) );
        }

        return result;
    }

    /**
     *  Adds a new post to the blog.
     *
     *  @param blogid The id of the blog.
     *  @param username The username to use
     *  @param password The password
     *  @param content As per Metaweblogapi contract
     *  @param publish This parameter is ignored for JSPWiki.
     *  @return Returns an empty string
     *  @throws XmlRpcException If something goes wrong
     */
    public String newPost( final String blogid,
                           final String username,
                           final String password,
                           final Hashtable< String, Object > content,
                           final boolean publish ) throws XmlRpcException {
        LOG.info("metaWeblog.newPost() called");
        final Engine engine = m_context.getEngine();
        final Page page = engine.getManager( PageManager.class ).getPage( blogid );
        checkPermissions( page, username, password, "createPages" );

        try {
            final WeblogEntryPlugin plugin = new WeblogEntryPlugin();
            final String pageName = plugin.getNewEntryPage( engine, blogid );
            final Page entryPage = Wiki.contents().page( engine, pageName );
            entryPage.setAuthor( username );

            final Context context = Wiki.context().create( engine, entryPage );
            final StringBuilder text = new StringBuilder();
            text.append( "!" ).append( content.get( "title" ) );
            text.append( "\n\n" );
            text.append( content.get("description") );

            LOG.debug("Writing entry: "+text);

            engine.getManager( PageManager.class ).saveText( context, text.toString() );
        } catch( final Exception e ) {
            LOG.error("Failed to create weblog entry",e);
            throw new XmlRpcException( 0, "Failed to create weblog entry: "+e.getMessage() );
        }

        return ""; // FIXME:
    }

    /**
     *  Creates an attachment and adds it to the blog.  The attachment
     *  is created into the main blog page, not the actual post page,
     *  because we do not know it at this point.
     *
     *  @param blogid The id of the blog.
     *  @param username The username to use
     *  @param password The password
     *  @param content As per the MetaweblogAPI contract
     *  @return As per the MetaweblogAPI contract
     *  @throws XmlRpcException If something goes wrong
     */
    public Hashtable< String, Object > newMediaObject( final String blogid,
                                                       final String username,
                                                       final String password,
                                                       final Hashtable< String, Object > content ) throws XmlRpcException {
        final Engine engine = m_context.getEngine();
        final String url;

        LOG.info( "metaWeblog.newMediaObject() called" );

        final Page page = engine.getManager( PageManager.class ).getPage( blogid );
        checkPermissions( page, username, password, "upload" );

        final String name = (String) content.get( "name" );
        final byte[] data = (byte[]) content.get( "bits" );

        final AttachmentManager attmgr = engine.getManager( AttachmentManager.class );

        try {
            final Attachment att = Wiki.contents().attachment( engine, blogid, name );
            att.setAuthor( username );
            attmgr.storeAttachment( att, new ByteArrayInputStream( data ) );

            url = engine.getURL( ContextEnum.PAGE_ATTACH.getRequestContext(), att.getName(), null );
        } catch( final Exception e ) {
            LOG.error( "Failed to upload attachment", e );
            throw new XmlRpcException( 0, "Failed to upload media object: "+e.getMessage() );
        }

        final Hashtable< String, Object > result = new Hashtable<>();
        result.put("url", url);

        return result;
    }


    /**
     *  Allows the user to edit a post.  It does not allow general editability of wiki pages, because of the limitations of the metaWeblog API.
     */
    boolean editPost( final String postid,
                      final String username,
                      final String password,
                      final Hashtable< String,Object > content,
                      final boolean publish ) throws XmlRpcException {
        final Engine engine = m_context.getEngine();
        LOG.info("metaWeblog.editPost("+postid+") called");

        // FIXME: Is postid correct?  Should we determine it from the page name?
        final Page page = engine.getManager( PageManager.class ).getPage( postid );
        checkPermissions( page, username, password, "edit" );

        try {
            final Page entryPage = page.clone();
            entryPage.setAuthor( username );

            final Context context = Wiki.context().create( engine, entryPage );

            final StringBuilder text = new StringBuilder();
            text.append( "!" ).append( content.get( "title" ) );
            text.append( "\n\n" );
            text.append( content.get("description") );

            LOG.debug("Updating entry: "+text);

            engine.getManager( PageManager.class ).saveText( context, text.toString() );
        } catch( final Exception e ) {
            LOG.error("Failed to create weblog entry",e);
            throw new XmlRpcException( 0, "Failed to update weblog entry: "+e.getMessage() );
        }

        return true;
    }

    /**
     *  Gets the text of any page.  The title of the page is parsed
     *  (if any is provided).
     */
    Hashtable< String, Object > getPost( final String postid, final String username, final String password ) throws XmlRpcException {
        final String wikiname = "FIXME";
        final Page page = m_context.getEngine().getManager( PageManager.class ).getPage( wikiname );
        checkPermissions( page, username, password, "view" );
        return makeEntry( page );
    }

}
