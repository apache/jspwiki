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
package org.apache.wiki.xmlrpc;

import java.io.ByteArrayInputStream;
import java.util.*;

import javax.security.auth.login.LoginException;


import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.api.WikiPage;
import org.apache.wiki.attachment.Attachment;
import org.apache.wiki.attachment.AttachmentManager;
import org.apache.wiki.auth.AuthenticationManager;
import org.apache.wiki.auth.AuthorizationManager;
import org.apache.wiki.auth.WikiSecurityException;
import org.apache.wiki.auth.permissions.PermissionFactory;
import org.apache.wiki.content.PageNotFoundException;
import org.apache.wiki.content.WikiPath;
import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;
import org.apache.wiki.plugin.WeblogEntryPlugin;
import org.apache.wiki.plugin.WeblogPlugin;
import org.apache.wiki.providers.ProviderException;
import org.apache.wiki.rpc.WikiRPCHandler;
import org.apache.xmlrpc.XmlRpcException;


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

public class MetaWeblogHandler
    implements WikiRPCHandler
{
    private static Logger log = LoggerFactory.getLogger( MetaWeblogHandler.class ); 

    private WikiContext m_context;
    
    private static final int ERR_NOTFOUND      = 404;
    private static final int ERR_SERVER_ERROR  = 500;
    private static final int ERR_UNAUTHORIZED  = 401;
    
    /**
     *  {@inheritDoc}
     */
    public void initialize( WikiContext context )
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
     *  @throw XmlRpcException with the correct error message, if auth fails.
     */
    private void checkPermissions( WikiPage page,
                                   String username,
                                   String password,
                                   String permission )
        throws XmlRpcException
    {
        try
        {
            AuthenticationManager amm = m_context.getEngine().getAuthenticationManager();
            AuthorizationManager mgr = m_context.getEngine().getAuthorizationManager();
        
            if( amm.login( m_context.getWikiSession(), m_context.getHttpRequest(), username, password ) )
            {
                if( !mgr.checkPermission( m_context.getWikiSession(), PermissionFactory.getPagePermission( page, permission ) ))
                {
                    throw new XmlRpcException( ERR_UNAUTHORIZED, "No permission" );
                }   
            }
            else
            {
                throw new XmlRpcException( ERR_UNAUTHORIZED, "Unknown login" );
            }
        }
        catch( WikiSecurityException e )
        {
            throw new XmlRpcException( ERR_SERVER_ERROR, e.getMessage(), e );
        }
        catch( LoginException e )
        {
            throw new XmlRpcException( ERR_SERVER_ERROR, e.getMessage(), e );
        }
        return;
    }

    /**
     *  JSPWiki does not support categories, therefore JSPWiki
     *  always returns an empty list for categories.
     *  
     *  @param blogid The id of the blog.
     *  @param username The username to use
     *  @param password The password
     *  @throws XmlRpcException If something goes wrong
     *  @return An empty hashtable.
     */
    public Hashtable getCategories( String blogid,
                                    String username,
                                    String password )
        throws XmlRpcException
    {
        WikiPage page;
        try
        {
            page = m_context.getEngine().getPage( blogid );

            checkPermissions( page, username, password, "view" );

            Hashtable ht = new Hashtable();

            return ht;
        }
        catch( PageNotFoundException e )
        {
            throw new XmlRpcException(ERR_NOTFOUND,"No such page");
        }
        catch( ProviderException e )
        {
            throw new XmlRpcException(ERR_SERVER_ERROR,"No such page");
        }
    }

    private String getURL( String page )
    {
        return m_context.getEngine().getURL( WikiContext.VIEW,
                                             page,
                                             null,
                                             true ); // Force absolute urls
    }

    /**
     *  Takes a wiki page, and creates a metaWeblog struct
     *  out of it.
     *  @param page The actual entry page
     *  @return A metaWeblog entry struct.
     */
    private Hashtable<String,Object> makeEntry( WikiPage page ) throws XmlRpcException
    {
        Hashtable<String, Object> ht = new Hashtable<String, Object>();

        try
        {
            WikiPage firstVersion = m_context.getEngine().getPage( page.getName(), 1 );

            ht.put("dateCreated", firstVersion.getLastModified());
            ht.put("link", getURL(page.getName()));
            ht.put("permaLink", getURL(page.getName()));
            ht.put("postid", page.getName());
            ht.put("userid", page.getAuthor());

            String pageText = m_context.getEngine().getText(page.getName());
            String title = "";
            int firstLine = pageText.indexOf('\n');

            if( firstLine > 0 )
            {
                title = pageText.substring( 0, firstLine );
            }
            
            if( title.trim().length() == 0 ) title = page.getName();

            // Remove wiki formatting
            while( title.startsWith("!") ) title = title.substring(1);

            ht.put("title", title);
            ht.put("description", pageText);
        }
        catch( PageNotFoundException e )
        {
            throw new XmlRpcException(ERR_NOTFOUND,"Not found");
        }
        catch( ProviderException e )
        {
            throw new XmlRpcException(ERR_SERVER_ERROR,e.getMessage());
        }
        
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

    // FIXME: The implementation is suboptimal, as it
    //        goes through all of the blog entries.

    @SuppressWarnings("unchecked")
    public Hashtable getRecentPosts( String blogid,
                                     String username,
                                     String password,
                                     int numberOfPosts)
        throws XmlRpcException
    {
        Hashtable<String, Hashtable<String, Object>> result = new Hashtable<String, Hashtable<String, Object>>();

        log.info( "metaWeblog.getRecentPosts() called");

        try
        {
            WikiPage page = m_context.getEngine().getPage( blogid );

            checkPermissions( page, username, password, "view" );

            WeblogPlugin plugin = new WeblogPlugin();

            SortedSet<WikiPage> changed = plugin.findBlogEntriesSorted(m_context.getEngine().getContentManager(), 
                                                            blogid,
                                                            new Date(0L),
                                                            new Date());

            int items = 0;
            for( Iterator i = changed.iterator(); i.hasNext() && items < numberOfPosts; items++ )
            {
                WikiPage p = (WikiPage) i.next();

                result.put( "entry", makeEntry( p ) );
            }

        }
        catch( PageNotFoundException e )
        {
            throw new XmlRpcException( ERR_NOTFOUND, e.getMessage() );
        }
        catch( ProviderException e )
        {
            log.error( "Failed to list recent posts", e );

            throw new XmlRpcException( ERR_SERVER_ERROR, e.getMessage() );
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
    public String newPost( String blogid,
                           String username,
                           String password,
                           Hashtable content,
                           boolean publish )
        throws XmlRpcException
    {
        log.info("metaWeblog.newPost() called");
        WikiEngine engine = m_context.getEngine();
        
        try
        {
            WikiPage page = engine.getPage( blogid );
            checkPermissions( page, username, password, "createPages" );
            
            WeblogEntryPlugin plugin = new WeblogEntryPlugin();

            String pageName = plugin.getNewEntryPage( engine, blogid );

            WikiPage entryPage = engine.createPage( WikiPath.valueOf( pageName ) );
            entryPage.setAuthor( username );

            WikiContext context = engine.getWikiContextFactory().newViewContext( entryPage );

            StringBuilder text = new StringBuilder();
            text.append( "!"+content.get("title") );
            text.append( "\n\n" );
            text.append( content.get("description") );

            log.debug("Writing entry: "+text);

            engine.saveText( context, text.toString() );
        }
        catch( PageNotFoundException e )
        {
            throw new XmlRpcException( ERR_NOTFOUND, "Not found" );
        }
        catch( Exception e )
        {
            log.error("Failed to create weblog entry",e);
            throw new XmlRpcException( ERR_SERVER_ERROR, "Failed to create weblog entry: "+e.getMessage() );
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
     *  
     */
    public Hashtable newMediaObject( String blogid, 
                                     String username,
                                     String password,
                                     Hashtable content )
        throws XmlRpcException
    {
        WikiEngine engine = m_context.getEngine();
        String url = "";

        log.info("metaWeblog.newMediaObject() called");

        try
        {
            WikiPage page = engine.getPage( blogid );
            checkPermissions( page, username, password, "upload" );

            String name = (String) content.get( "name" );
            byte[] data = (byte[]) content.get( "bits" );

            AttachmentManager attmgr = engine.getAttachmentManager();
            Attachment att = engine.getContentManager().addPage( WikiPath.valueOf( blogid ).resolve( name ),
                                                                 "application/octet-stream"); //FIXME! Needs a better guess
            att.setAuthor( username );

            att.setContent( new ByteArrayInputStream( data ) );
            
            engine.getContentManager().save( att );

            url = engine.getURL( WikiContext.ATTACH, att.getName(), null, true );
        }
        catch( PageNotFoundException e )
        {
            throw new XmlRpcException( ERR_NOTFOUND, "Not found" );
        }
        catch( Exception e )
        {
            log.error( "Failed to upload attachment", e );
            throw new XmlRpcException( ERR_SERVER_ERROR, "Failed to upload media object: "+e.getMessage() );
        }

        Hashtable<String, Object> result = new Hashtable<String, Object>();
        result.put("url", url);

        return result;
    }


    /**
     *  Allows the user to edit a post.  It does not allow general
     *   editability of wiki pages, because of the limitations of the
     *  metaWeblog API.
     */
    boolean editPost( String postid,
                      String username,
                      String password,
                      Hashtable content,
                      boolean publish )
        throws XmlRpcException
    {
        WikiEngine engine = m_context.getEngine();
        log.info("metaWeblog.editPost("+postid+") called");

        try
        {
            // FIXME: Is postid correct?  Should we determine it from the page name?
            WikiPage page = engine.getPage( postid );
            checkPermissions( page, username, password, "edit" );

            WikiPage entryPage = (WikiPage)page.clone();
            entryPage.setAuthor( username );

            WikiContext context = engine.getWikiContextFactory().newViewContext( entryPage );

            StringBuilder text = new StringBuilder();
            text.append( "!"+content.get("title") );
            text.append( "\n\n" );
            text.append( content.get("description") );

            log.debug("Updating entry: "+text);

            engine.saveText( context, text.toString() );
        }
        catch( PageNotFoundException e )
        {
            throw new XmlRpcException( ERR_NOTFOUND, "Not found" );
        }
        catch( Exception e )
        {
            log.error("Failed to create weblog entry",e);
            throw new XmlRpcException( ERR_SERVER_ERROR, "Failed to update weblog entry: "+e.getMessage() );
        }

        return true;
    }

    /**
     *  Gets the text of any page.  The title of the page is parsed
     *  (if any is provided).
     */
    Hashtable getPost( String postid,
                       String username,
                       String password )
        throws XmlRpcException
    {
        String wikiname = "FIXME";

        try
        {
            WikiPage page;
            page = m_context.getEngine().getPage( wikiname );

            checkPermissions( page, username, password, "view" );

            return makeEntry( page );
        }
        catch( PageNotFoundException e )
        {
            throw new XmlRpcException(ERR_NOTFOUND,"Not found");
        }
        catch( ProviderException e )
        {
            throw new XmlRpcException(ERR_SERVER_ERROR,e.getMessage());
        }
    }
}
