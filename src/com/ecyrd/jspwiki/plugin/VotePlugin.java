/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2003 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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
package com.ecyrd.jspwiki.plugin;

import java.util.*;
import java.io.*;
import com.ecyrd.jspwiki.*;
import com.ecyrd.jspwiki.attachment.AttachmentManager;
import com.ecyrd.jspwiki.attachment.Attachment;
import com.ecyrd.jspwiki.providers.ProviderException;

import org.apache.log4j.Logger;

/**
 *  Implements a simple voting system.  WARNING: The storage method is
 *  still experimental; I will probably change it at some point.
 */

public class VotePlugin
    implements WikiPlugin
{
    static Logger log = Logger.getLogger( VotePlugin.class );

    public static String ATTACHMENT_NAME = "VotePlugin.properties";

    public static String VAR_VOTES = "VotePlugin.votes";

    /**
     *  +1 for yes, -1 for no.
     *
     *  @return number of votes, or -1 if an error occurred.
     */
    public int vote( WikiContext context, int vote )
    {
        Properties props = getVotes( context );

        if( vote > 0 )
        {
            int nVotes = getYesVotes( context );

            putVotes( context, "yes", ++nVotes );

            return nVotes;
        }
        else if( vote < 0 )
        {
            int nVotes = getNoVotes( context );

            putVotes( context, "no", ++nVotes );

            return nVotes;
        }

        return -1; // Error
    }

    private void putVotes( WikiContext context, String yesno, int nVotes )
    {
        WikiEngine engine = context.getEngine();
        WikiPage   page   = context.getPage();

        Properties props = getVotes( context );

        props.setProperty( yesno, Integer.toString(nVotes) );

        page.setAttribute( VAR_VOTES, props );

        storeAttachment( context, props );
    }

    private void storeAttachment( WikiContext context, Properties props )
    {
        try
        {            
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            props.store( out, "JSPWiki Votes plugin stores its votes here.  Don't modify!" );

            out.close();

            AttachmentManager attmgr = context.getEngine().getAttachmentManager();

            Attachment att = findAttachment( context );

            InputStream in = new ByteArrayInputStream( out.toByteArray() );

            attmgr.storeAttachment( att, in );

            in.close();
        }
        catch( Exception ex )
        {
            log.error("Unable to write properties", ex);
        }
    }

    private Attachment findAttachment( WikiContext context )
        throws ProviderException
    {
        Attachment att = context.getEngine().getAttachmentManager().getAttachmentInfo( context, ATTACHMENT_NAME );

        if( att == null )
        {
            att = new Attachment( context.getPage().getName(),
                                  ATTACHMENT_NAME );
        }

        return att;
    }

    private Properties getVotes( WikiContext context )
    {
        WikiPage page = context.getPage();

        Properties props = (Properties) page.getAttribute( VAR_VOTES );

        //
        //  Not loaded yet
        //
        if( props == null )
        {
            props = new Properties();

            AttachmentManager attmgr = context.getEngine().getAttachmentManager();

            try
            {
                Attachment att = attmgr.getAttachmentInfo( context,
                                                           ATTACHMENT_NAME );

                if( att != null )
                {
                    props.load( attmgr.getAttachmentStream( att ) );
                }
            }
            catch( Exception e )
            {
                log.error( "Unable to load attachment ", e );
            }
        }

        return props;
    }

    private int getYesVotes( WikiContext context )
    {
        Properties props = getVotes( context );

        return TextUtil.getIntegerProperty( props, "yes", 0 );
    }

    private int getNoVotes( WikiContext context )
    {
        Properties props = getVotes( context );

        return TextUtil.getIntegerProperty( props, "no", 0 );
    }

    public String execute( WikiContext context, Map params )
        throws PluginException
    {
        WikiEngine engine = context.getEngine();

        String posneg = (String) params.get( "value" );

        if( TextUtil.isPositive(posneg) )
        {
            return Integer.toString( getYesVotes( context ) );
        }

        return Integer.toString( getNoVotes( context ) );
    }
}
