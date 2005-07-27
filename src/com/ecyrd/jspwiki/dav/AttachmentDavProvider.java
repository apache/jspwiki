package com.ecyrd.jspwiki.dav;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiPage;
import com.ecyrd.jspwiki.attachment.Attachment;
import com.ecyrd.jspwiki.dav.items.AttachmentItem;
import com.ecyrd.jspwiki.dav.items.DavItem;
import com.ecyrd.jspwiki.dav.items.DirectoryItem;
import com.ecyrd.jspwiki.providers.ProviderException;

public class AttachmentDavProvider implements DavProvider
{
    protected WikiEngine m_engine;
    protected static Logger log = Logger.getLogger( AttachmentDavProvider.class );
    
    public AttachmentDavProvider( WikiEngine engine )
    {
        m_engine = engine;
    }

    public WikiEngine getEngine()
    {
        return m_engine;
    }

    private Collection listAllPagesWithAttachments()
    {
        ArrayList pageNames = new ArrayList();
        
        try
        {
            Collection atts = m_engine.getAttachmentManager().getAllAttachments();
            
            for( Iterator i = atts.iterator(); i.hasNext(); )
            {
                Attachment att = (Attachment)i.next();
                
                String pageName = att.getParentName();
                
                if( !pageNames.contains(pageName) )
                    pageNames.add( pageName );
            }
        }
        catch (ProviderException e)
        {
            log.error("Unable to get all attachments",e);
        }
        
        Collections.sort( pageNames );
        
        ArrayList result = new ArrayList();
        
        for( Iterator i = pageNames.iterator(); i.hasNext(); )
        {
            DirectoryItem di = new DirectoryItem( this, new DavPath( (String)i.next() ));
            
            result.add( di );
        }
        return result;
    }

    protected Collection listAttachmentsOfPage( DavPath path )
    {
        String pageName = path.getName();
        
        log.debug("Listing attachments for page "+pageName);
        
        ArrayList result = new ArrayList();
        try
        {
            WikiPage page = m_engine.getPage( pageName );
            Collection attachments = m_engine.getAttachmentManager().listAttachments(page);
            
            for( Iterator i = attachments.iterator(); i.hasNext(); )
            {
                Attachment att = (Attachment) i.next();
            
                DavPath thisPath = new DavPath( "/" );
                
                thisPath.append( att.getName() );
            
                AttachmentItem ai = new AttachmentItem( this, thisPath, att );
                
                result.add( ai );
            }
        }
        catch( ProviderException e )
        {
            log.error("Unable to list attachments, returning what I got",e);
            // FIXME: Not a good way to handle errors
        }
        
        return result;
    }
    
    public DavItem getItem(DavPath path)
    {
        if( path.isRoot() )
        {
            DirectoryItem di = new DirectoryItem( this, new DavPath("") );
            
            di.addDavItems( listAllPagesWithAttachments() );
            return di;
        }
        else if( path.isDirectory() )
        {
            DirectoryItem di = new DirectoryItem( this, path );
            
            di.addDavItems( listAttachmentsOfPage(path) );
            
            return di;
        }
        else
        {
            String attName = path.getPath();
            
            try
            {
                Attachment att = m_engine.getAttachmentManager().getAttachmentInfo( attName );
            
                if( att != null )
                {
                    AttachmentItem ai = new AttachmentItem( this, path, att );
                
                    return ai;
                }
            }
            catch( ProviderException e )
            {
                log.error("Unable to get the attachment data for "+attName,e);
            }
        }
        return null;
    }

    public void setItem(DavPath path, DavItem item)
    {
        // TODO Auto-generated method stub

    }

    public String getURL(DavPath path)
    {
        String p = path.getPath();
        
        if( p.startsWith("/") ) p = p.substring( 1 );
        
        return m_engine.getURL( WikiContext.ATTACH, p, null, true );
    }

}
