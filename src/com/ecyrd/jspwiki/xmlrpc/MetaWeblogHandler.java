/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2004 Janne Jalkanen (Janne.Jalkanen@iki.fi)

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation; either version 2.1 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.ecyrd.jspwiki.xmlrpc;

import java.io.*;
import org.apache.log4j.Category;
import com.ecyrd.jspwiki.*;
import com.ecyrd.jspwiki.attachment.Attachment;
import com.ecyrd.jspwiki.attachment.AttachmentManager;
import com.ecyrd.jspwiki.plugin.WeblogEntryPlugin;
import com.ecyrd.jspwiki.plugin.WeblogPlugin;
import com.ecyrd.jspwiki.plugin.PluginException;
import com.ecyrd.jspwiki.providers.ProviderException;
import com.ecyrd.jspwiki.auth.AuthorizationManager;
import com.ecyrd.jspwiki.auth.UserProfile;
import java.util.*;
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
 *  @author Janne Jalkanen
 *  @since 2.1.7
 */

public class MetaWeblogHandler
    implements WikiRPCHandler
{
    Category log = Category.getInstance( MetaWeblogHandler.class ); 

    private WikiEngine m_engine;

    public void initialize( WikiEngine engine )
    {
        m_engine = engine;
    }

    /**
     *  Does a quick check against the current user
     *  and does he have permissions to do the stuff
     *  that he really wants to.
     */
    private void checkPermissions( WikiPage page, 
                                   String username,
                                   String password,
                                   String permission )
        throws XmlRpcException
    {
        AuthorizationManager mgr = m_engine.getAuthorizationManager();
        UserProfile currentUser  = m_engine.getUserManager().getUserProfile( username );
        currentUser.setPassword( password );

        boolean isValid = m_engine.getUserManager().getAuthenticator().authenticate( currentUser );
        
        if( isValid )
        {
            if( !mgr.checkPermission( page,
                                      currentUser,
                                      permission ) )
            {
                return;
            }
            else
            {
                String msg = "Insufficient permissions to do "+permission+" on "+page.getName();
                log.error( msg );
                throw new XmlRpcException(0, msg );
            }
        }
        else 
        {
            log.error( "Username '"+username+"' or password not valid." );
            throw new XmlRpcException(0, "Password or username not valid.");
        }
    }

    /**
     *  JSPWiki does not support categories, therefore JSPWiki
     *  always returns an empty list for categories.
     */
    public Hashtable getCategories( String blogid,
                                    String username,
                                    String password )
        throws XmlRpcException
    {
        WikiPage page = m_engine.getPage( blogid );

        checkPermissions( page, username, password, "view" );

        Hashtable ht = new Hashtable();

        return ht;
    }

    /**
     *  Takes a wiki page, and creates a metaWeblog struct
     *  out of it.
     *  @param page The actual entry page
     *  @return A metaWeblog entry struct.
     */
    private Hashtable makeEntry( WikiPage page )
    {
        Hashtable ht = new Hashtable();

        WikiPage firstVersion = m_engine.getPage( page.getName(), 1 );

        ht.put("dateCreated", firstVersion.getLastModified());
        ht.put("link", m_engine.getViewURL(page.getName()));
        ht.put("permaLink", m_engine.getViewURL(page.getName()));
        ht.put("postid", page.getName());
        ht.put("userid", page.getAuthor());

        String pageText = m_engine.getText(page.getName());
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

        return ht;
    }

    /**
     *  Returns a list of the recent posts to this weblog.
     */

    // FIXME: The implementation is suboptimal, as it
    //        goes through all of the blog entries.

    public Hashtable getRecentPosts( String blogid,
                                     String username,
                                     String password,
                                     int numberOfPosts)
        throws XmlRpcException
    {
        Hashtable result = new Hashtable();

        log.info( "metaWeblog.getRecentPosts() called");

        WikiPage page = m_engine.getPage( blogid );

        checkPermissions( page, username, password, "view" );

        try
        {
            WeblogPlugin plugin = new WeblogPlugin();

            List changed = plugin.findBlogEntries(m_engine.getPageManager(), 
                                                  blogid,
                                                  new Date(0L),
                                                  new Date());

            Collections.sort( changed, new PageTimeComparator() );

            int items = 0;
            for( Iterator i = changed.iterator(); i.hasNext() && items < numberOfPosts; items++ )
            {
                WikiPage p = (WikiPage) i.next();

                result.put( "entry", makeEntry( p ) );
            }

        }
        catch( ProviderException e )
        {
            log.error( "Failed to list recent posts", e );

            throw new XmlRpcException( 0, e.getMessage() );
        }

        return result;
    }

    /**
     *  Adds a new post to the blog.
     *  @param publish This parameter is ignored for JSPWiki.
     */
    public String newPost( String blogid,
                           String username,
                           String password,
                           Hashtable content,
                           boolean publish )
        throws XmlRpcException
    {
        log.info("metaWeblog.newPost() called");
        
        WikiPage page = m_engine.getPage( blogid );
        checkPermissions( page, username, password, "create" );

        try
        {
            WeblogEntryPlugin plugin = new WeblogEntryPlugin();

            String pageName = plugin.getNewEntryPage( m_engine, blogid );

            WikiPage entryPage = new WikiPage( pageName );
            entryPage.setAuthor( username );

            WikiContext context = new WikiContext( m_engine, entryPage );

            StringBuffer text = new StringBuffer();
            text.append( "!"+content.get("title") );
            text.append( "\n\n" );
            text.append( content.get("description") );

            log.debug("Writing entry: "+text);

            m_engine.saveText( context, text.toString() );
        }
        catch( Exception e )
        {
            log.error("Failed to create weblog entry",e);
            throw new XmlRpcException( 0, "Failed to create weblog entry: "+e.getMessage() );
        }

        return ""; // FIXME:
    }

    /**
     *  Creates an attachment and adds it to the blog.  The attachment
     *  is created into the main blog page, not the actual post page,
     *  because we do not know it at this point.
     */
    public Hashtable newMediaObject( String blogid, 
                                     String username,
                                     String password,
                                     Hashtable content )
        throws XmlRpcException
    {
        String url = "";

        log.info("metaWeblog.newMediaObject() called");

        WikiPage page = m_engine.getPage( blogid );
        checkPermissions( page, username, password, "upload" );

        String name = (String) content.get( "name" );
        byte[] data = (byte[]) content.get( "bits" );

        AttachmentManager attmgr = m_engine.getAttachmentManager();

        try
        {
            Attachment att = new Attachment( blogid, name );
            att.setAuthor( username );
            attmgr.storeAttachment( att, new ByteArrayInputStream( data ) );

            url = m_engine.getAttachmentURL(att.getName());
        }
        catch( Exception e )
        {
            log.error( "Failed to upload attachment", e );
            throw new XmlRpcException( 0, "Failed to upload media object: "+e.getMessage() );
        }

        Hashtable result = new Hashtable();
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
        log.info("metaWeblog.editPost("+postid+") called");

        // FIXME: Is postid correct?  Should we determine it from the page name?
        WikiPage page = m_engine.getPage( postid );
        checkPermissions( page, username, password, "edit" );

        try
        {
            WikiPage entryPage = (WikiPage)page.clone();
            entryPage.setAuthor( username );

            WikiContext context = new WikiContext( m_engine, entryPage );

            StringBuffer text = new StringBuffer();
            text.append( "!"+content.get("title") );
            text.append( "\n\n" );
            text.append( content.get("description") );

            log.debug("Updating entry: "+text);

            m_engine.saveText( context, text.toString() );
        }
        catch( Exception e )
        {
            log.error("Failed to create weblog entry",e);
            throw new XmlRpcException( 0, "Failed to update weblog entry: "+e.getMessage() );
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

        WikiPage page = m_engine.getPage( wikiname );

        checkPermissions( page, username, password, "view" );

        return makeEntry( page );
    }
}
