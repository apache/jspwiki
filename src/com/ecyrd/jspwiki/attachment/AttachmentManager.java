/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2002 Janne Jalkanen (Janne.Jalkanen@iki.fi)

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.ecyrd.jspwiki.attachment;

import java.io.IOException;
import java.util.Properties;
import org.apache.log4j.Category;

import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiProvider;
import com.ecyrd.jspwiki.NoRequiredPropertyException;
import com.ecyrd.jspwiki.providers.WikiAttachmentProvider;

/**
 *  Provides facilities for handling attachments.
 *  
 *  @author Janne Jalkanen
 *  @since 1.9.28
 */
public class AttachmentManager
{
    public static final String  PROP_PROVIDER = "jspwiki.attachmentProvider";

    static Category log = Category.getInstance( AttachmentManager.class );
    private WikiAttachmentProvider m_provider;

    /**
     *  Creates a new AttachmentManager.  Note that creation will never fail,
     *  but it's quite likely that attachments do not function.
     */

    // FIXME: Perhaps this should fail somehow.
    public AttachmentManager( Properties props )
    {
        String classname = props.getProperty( PROP_PROVIDER );

        //
        //  If no class defined, then will just simply fail.
        //
        if( classname == null )
        {
            log.info( "No attachment provider defined - disabling attachment support." );
            return;
        }

        //
        //  Create and initialize the provider.
        //
        try
        {
            Class providerclass = WikiEngine.findWikiClass( classname, 
                                                            "com.ecyrd.jspwiki.providers" );

            m_provider = (WikiAttachmentProvider)providerclass.newInstance();

            m_provider.initialize( props );
        }
        catch( ClassNotFoundException e )
        {
            log.error( "Attachment provider class not found",e);
        }
        catch( InstantiationException e )
        {
            log.error( "Attachment provider could not be created", e );
        }
        catch( IllegalAccessException e )
        {
            log.error( "You may not access the attachment provider class", e );
        }
        catch( NoRequiredPropertyException e )
        {
            log.error( "Attachment provider did not find a property that it needed: "+e.getMessage(), e );
            m_provider = null; // No, it did not work.
        }
        catch( IOException e )
        {
            log.error( "Attachment provider reports IO error", e );
            m_provider = null;
        }
    }

    /**
     *  Returns true, if attachments are enabled and running.
     */
    public boolean attachmentsEnabled()
    {
        return m_provider != null;
    }

    public void addAttachment( Attachment att )
    {
    }

    public Attachment getAttachmentInfo( String name )
    {
        return m_provider.getAttachmentInfo( name, WikiProvider.LATEST_VERSION );
    }

    public WikiAttachmentProvider getCurrentProvider()
    {
        return m_provider;
    }
}
