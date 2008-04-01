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
// FIXME3.0 - remove, useless.

public class VotePlugin
    implements WikiPlugin
{
    static Logger log = Logger.getLogger( VotePlugin.class );

    public static final String ATTACHMENT_NAME = "VotePlugin.properties";

    public static final String VAR_VOTES = "VotePlugin.votes";

    /**
     *  +1 for yes, -1 for no.
     *
     *  @return number of votes, or -1 if an error occurred.
     */
    public int vote( WikiContext context, int vote )
    {
        getVotes( context );

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
            att = new Attachment( context.getEngine(),
                                  context.getPage().getName(),
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
        String posneg = (String) params.get( "value" );

        if( TextUtil.isPositive(posneg) )
        {
            return Integer.toString( getYesVotes( context ) );
        }

        return Integer.toString( getNoVotes( context ) );
    }
}
