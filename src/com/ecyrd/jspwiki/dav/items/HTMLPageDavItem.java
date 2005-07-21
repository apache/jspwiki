/*
 * (C) Janne Jalkanen 2005
 * 
 */
package com.ecyrd.jspwiki.dav.items;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Collection;

import org.jdom.Element;

import com.ecyrd.jspwiki.TranslatorReader;
import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiPage;
import com.ecyrd.jspwiki.dav.DavProvider;
import com.ecyrd.jspwiki.dav.WikiDavProvider;
import com.sun.rsasign.r;

/**
 *  @author jalkanen
 *
 *  @since 
 */
public class HTMLPageDavItem extends PageDavItem
{
    private long m_cachedLength = -1;
    
    /**
     * @param engine
     * @param page
     */
    public HTMLPageDavItem( DavProvider provider, WikiPage page )
    {
        super( provider, page );
    }

    
    public String getHref()
    {
        return m_provider.getURL( m_page.getName()+".html" );    
    }
 
    public String getContentType()
    {
        return "text/html; charset=UTF-8";
    }

    private byte[] getText()
    {
        WikiEngine engine = ((WikiDavProvider)m_provider).getEngine();
        
        WikiContext context = new WikiContext( engine, m_page );
        context.setRequestContext( WikiContext.VIEW );

        context.setVariable( TranslatorReader.PROP_RUNPLUGINS, "false" );
        context.setVariable( WikiEngine.PROP_RUNFILTERS, "false" );

        String text = engine.getHTML( context, m_page );
        
        try
        {
            return text.getBytes("UTF-8");
        }
        catch( UnsupportedEncodingException e ) { return null; } // Should never happen
    }
    
    public InputStream getInputStream()
    {
        byte[] text = getText();
        
        ByteArrayInputStream in = new ByteArrayInputStream( text );
            
        return in;
    }

    public long getLength()
    {
        if( m_cachedLength == -1 )
        {       
            byte[] text = getText();
        
            m_cachedLength = text.length;
        }
        
        return m_cachedLength;
    }
    
    public Collection getPropertySet()
    {
        Collection set = getCommonProperties();

        //
        //  Rendering the page for every single time is not really a very good idea.
        //

        set.add( new Element("getcontentlength",m_davns).setText( Long.toString(getLength()) ) );
        set.add( new Element("getcontenttype",m_davns).setText("text/html; charset=\"UTF-8\""));

        return set;
    }
}
