/*
 * (C) Janne Jalkanen 2005
 * 
 */
package com.ecyrd.jspwiki.dav.items;

import java.util.Collection;
import java.util.Iterator;

import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiPage;
import com.ecyrd.jspwiki.attachment.Attachment;
import com.ecyrd.jspwiki.attachment.AttachmentManager;
import com.ecyrd.jspwiki.dav.DavContext;
import com.ecyrd.jspwiki.providers.ProviderException;

/**
 *  @author jalkanen
 *
 *  @since 
 */
public class DavItemFactory
{
    private WikiEngine m_engine;
    Logger log = Logger.getLogger( DavItemFactory.class );

    
    public DavItemFactory( WikiEngine engine )
    {
        m_engine = engine;
    }

    private DavItem getRawItem( DavContext dc )
    {
        String pagename = dc.m_page;
        
        if( pagename == null || pagename.length() == 0 )
        {
            // DirectoryItem di = new DirectoryItem( m_engine, dc.m_davcontext );
            DirectoryItem di = new DirectoryItem( null, dc.m_davcontext );
            
            try
            {
                Collection c = m_engine.getPageManager().getAllPages();
                
                for( Iterator i = c.iterator(); i.hasNext(); )
                {
                    WikiPage p = (WikiPage) i.next();
                    
                    PageDavItem dip = new PageDavItem( null, p );
                    
                    di.addDavItem( dip );
                }
            }
            catch( ProviderException e )
            {
                log.error( "Failed to get page list", e );
                return null;
            }
            
            return di;
        }
        else
        {
            if( pagename.endsWith(".txt") )
            {
                pagename = pagename.substring(0,pagename.length()-4);
                
                WikiPage p = m_engine.getPage( pagename );
                
                if( p != null )
                {
                    PageDavItem di = new PageDavItem( null, p );
                    
                    return di;
                }
            }
            // TODO: add attachments
        }
        return null;
    }

    private DavItem getAttachmentItem( DavContext dc )
    {
        String attname = dc.m_page;
        
        if( attname == null || attname.length() == 0 )
        {
            // User wants to have a listing of pages
            
            // DirectoryItem di = new DirectoryItem( m_engine, dc.m_davcontext );
          
            DirectoryItem di = new DirectoryItem( null, dc.m_davcontext );
            
            AttachmentManager mgr = m_engine.getAttachmentManager();
                
            try
            {
                Collection all = mgr.getAllAttachments();
                
                for( Iterator i = all.iterator(); i.hasNext(); )
                {
                    Attachment att = (Attachment)i.next();
                    
                    DirectoryItem dia = new AttachmentDirectoryItem( null, att.getParentName() );
           
                    di.addDavItem( dia );
                }
            }
            catch( ProviderException e )
            {
                log.error("Unable to get listing of attachments: ",e);
            }
            
            return di;
        }
        else if( attname.indexOf("/") == -1 )
        {
            // No attachment; user wants to have a directory of a particular page
            // FIXME: This does not work for subpages
            
            AttachmentDirectoryItem di = new AttachmentDirectoryItem( null, attname );
            
            return di;
        }
        else
        {
            try
            {
                Attachment att = m_engine.getAttachmentManager().getAttachmentInfo( attname );
            
                if( att != null )
                {
                    AttachmentItem ai = new AttachmentItem( null, att );
                
                    return ai;
                }
            }
            catch( ProviderException e )
            {
                log.error("Unable to get attachment info for "+attname, e );
            }
        }
        
        return null;
    }
    
    private DavItem getHTMLItem( DavContext dc )
    {
        String pagename = dc.m_page;
        
        if( pagename == null || pagename.length() == 0 )
        {
        //    DirectoryItem di = new DirectoryItem( m_engine, dc.m_davcontext );
        
            DirectoryItem di = new DirectoryItem( null, dc.m_davcontext );
            
            try
            {
                Collection c = m_engine.getPageManager().getAllPages();
                
                for( Iterator i = c.iterator(); i.hasNext(); )
                {
                    WikiPage p = (WikiPage) i.next();
                    
                    HTMLPageDavItem dip = new HTMLPageDavItem( null, p );
                    
                    di.addDavItem( dip );
                }
            }
            catch( ProviderException e )
            {
                log.error( "Failed to get page list", e );
                return null;
            }
            
            return di;
        }
        else
        {
            if( pagename.endsWith(".html") )
            {
                pagename = pagename.substring(0,pagename.length()-5);
                
                WikiPage p = m_engine.getPage( pagename );
                
                if( p != null )
                {
                    HTMLPageDavItem di = new HTMLPageDavItem( null, p );
                    
                    return di;
                }
            }
            // TODO: add attachments
        }
        return null;
    }

    
    public DavItem newItem( DavContext dc )
    {
        if( dc.m_davcontext.length() == 0 )
        {
            return new TopLevelDavItem( null );
        }
        else if( dc.m_davcontext.equals("raw") )
        {
            return getRawItem( dc );
        }
        else if( dc.m_davcontext.equals("html") )
        {
            return getHTMLItem( dc );
        }
        else if( dc.m_davcontext.equals("attach") )
        {
            return getAttachmentItem( dc );
        }
        return null;
        
    }
}
