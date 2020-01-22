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

import org.apache.log4j.Logger;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiPage;
import org.apache.wiki.attachment.Attachment;
import org.apache.wiki.attachment.AttachmentManager;
import org.apache.wiki.auth.AuthenticationManager;
import org.apache.wiki.auth.AuthorizationManager;
import org.apache.wiki.auth.WikiSecurityException;
import org.apache.wiki.auth.permissions.PermissionFactory;
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

    private static final Logger log = Logger.getLogger( MetaWeblogHandler.class );

    private WikiContext m_context;

    /**
     *  {@inheritDoc}
     */
    public void initialize( final WikiContext context )
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
    private void checkPermissions( final WikiPage page,
                                   final String username,
                                   final String password,
                                   final String permission ) throws XmlRpcException {
        try {
            final AuthenticationManager amm = m_context.getEngine().getAuthenticationManager();
            final AuthorizationManager mgr = m_context.getEngine().getAuthorizationManager();

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
        final WikiPage page = m_context.getEngine().getPageManager().getPage( blogid );
        checkPermissions( page, username, password, "view" );
        return new Hashtable<>();
    }

    private String getURL( final String page ) {
        return m_context.getEngine().getURL( WikiContext.VIEW, page,null );
    }

    /**
     *  Takes a wiki page, and creates a metaWeblog struct out of it.
     *
     *  @param page The actual entry page
     *  @return A metaWeblog entry struct.
     */
    private Hashtable< String,Object > makeEntry( final WikiPage page ) {
        final WikiPage firstVersion = m_context.getEngine().getPageManager().getPage( page.getName(), 1 );
        final Hashtable< String, Object > ht = new Hashtable<>();
        ht.put( "dateCreated", firstVersion.getLastModified() );
        ht.put( "link", getURL(page.getName() ) );
        ht.put( "permaLink", getURL(page.getName() ) );
        ht.put( "postid", page.getName() );
        ht.put( "userid", page.getAuthor() );

        final String pageText = m_context.getEngine().getPageManager().getText(page.getName());
        final int firstLine = pageText.indexOf('\n');

        String title = "";
        if( firstLine > 0 ) {
            title = pageText.substring( 0, firstLine );
        }

        if( title.trim().length() == 0 ) {
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
        log.info( "metaWeblog.getRecentPosts() called");
        final WikiPage page = m_context.getEngine().getPageManager().getPage( blogid );
        checkPermissions( page, username, password, "view" );

        final WeblogPlugin plugin = new WeblogPlugin();
        final List< WikiPage > changed = plugin.findBlogEntries( m_context.getEngine(), blogid, new Date( 0L ), new Date() );
        changed.sort( new PageTimeComparator() );

        int items = 0;
        for( final Iterator< WikiPage > i = changed.iterator(); i.hasNext() && items < numberOfPosts; items++ ) {
            final WikiPage p = i.next();
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
        log.info("metaWeblog.newPost() called");
        final WikiEngine engine = m_context.getEngine();
        final WikiPage page = engine.getPageManager().getPage( blogid );
        checkPermissions( page, username, password, "createPages" );

        try {
            final WeblogEntryPlugin plugin = new WeblogEntryPlugin();
            final String pageName = plugin.getNewEntryPage( engine, blogid );
            final WikiPage entryPage = new WikiPage( engine, pageName );
            entryPage.setAuthor( username );

            final WikiContext context = new WikiContext( engine, entryPage );
            final StringBuilder text = new StringBuilder();
            text.append( "!" ).append( content.get( "title" ) );
            text.append( "\n\n" );
            text.append( content.get("description") );

            log.debug("Writing entry: "+text);

            engine.getPageManager().saveText( context, text.toString() );
        } catch( final Exception e ) {
            log.error("Failed to create weblog entry",e);
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
        final WikiEngine engine = m_context.getEngine();
        final String url;

        log.info( "metaWeblog.newMediaObject() called" );

        final WikiPage page = engine.getPageManager().getPage( blogid );
        checkPermissions( page, username, password, "upload" );

        final String name = (String) content.get( "name" );
        final byte[] data = (byte[]) content.get( "bits" );

        final AttachmentManager attmgr = engine.getAttachmentManager();

        try {
            final Attachment att = new Attachment( engine, blogid, name );
            att.setAuthor( username );
            attmgr.storeAttachment( att, new ByteArrayInputStream( data ) );

            url = engine.getURL( WikiContext.ATTACH, att.getName(), null );
        } catch( final Exception e ) {
            log.error( "Failed to upload attachment", e );
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
        final WikiEngine engine = m_context.getEngine();
        log.info("metaWeblog.editPost("+postid+") called");

        // FIXME: Is postid correct?  Should we determine it from the page name?
        final WikiPage page = engine.getPageManager().getPage( postid );
        checkPermissions( page, username, password, "edit" );

        try {
            final WikiPage entryPage = (WikiPage)page.clone();
            entryPage.setAuthor( username );

            final WikiContext context = new WikiContext( engine, entryPage );

            final StringBuilder text = new StringBuilder();
            text.append( "!" ).append( content.get( "title" ) );
            text.append( "\n\n" );
            text.append( content.get("description") );

            log.debug("Updating entry: "+text);

            engine.getPageManager().saveText( context, text.toString() );
        } catch( final Exception e ) {
            log.error("Failed to create weblog entry",e);
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
        final WikiPage page = m_context.getEngine().getPageManager().getPage( wikiname );
        checkPermissions( page, username, password, "view" );
        return makeEntry( page );
    }

}
