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
package com.ecyrd.jspwiki.plugin;

import org.apache.log4j.Category;
import com.ecyrd.jspwiki.*;
import java.util.*;
import java.io.StringReader;
import java.io.IOException;
import java.text.SimpleDateFormat;

/**
 *  This is a base class for all plugins using referral things.
 *
 *  @author Janne Jalkanen
 */
public abstract class AbstractReferralPlugin
    implements WikiPlugin
{
    private static Category log = Category.getInstance( AbstractReferralPlugin.class );

    public static final int    ALL_ITEMS       = -1;
    public static final String PARAM_MAXWIDTH  = "maxwidth";
    public static final String PARAM_SEPARATOR = "separator";

    protected           int    m_maxwidth = Integer.MAX_VALUE;
    protected           String m_separator = "\\\\";

    /**
     *  Used to initialize some things.  All plugins must call this first.
     *
     *  @since 1.6.4
     */
    public void initialize( WikiContext context, Map params )
        throws PluginException
    {
        m_maxwidth = TextUtil.parseIntParameter( (String)params.get( PARAM_MAXWIDTH ), Integer.MAX_VALUE ); 
        String s = (String) params.get( PARAM_SEPARATOR );

        if( s != null )
        {
            m_separator = s;
        }

        if( m_maxwidth < 0 ) m_maxwidth = 0;

        // log.debug( "Requested maximum width is "+m_maxwidth );
    }

    /**
     *  Makes WikiText from a Collection.
     *
     *  @param links Collection to make into WikiText.
     *  @param separator Separator string to use.
     *  @param numItems How many items to show.
     */
    protected String wikitizeCollection( Collection links, String separator, int numItems )
    {
        if(links == null || links.isEmpty() )
            return( "" );

        StringBuffer output = new StringBuffer();
        
        Iterator it     = links.iterator();
        int      count  = 0;
        
        while( it.hasNext() && ( (count < numItems) || ( numItems == ALL_ITEMS ) ) )
        {
            String value = (String)it.next();
            // Make a Wiki markup link. See TranslatorReader.
            output.append( "[" + value + "]" + separator +"\n");
            count++;
        }

        return( output.toString() );
    }

    /**
     *  Makes HTML with common parameters.
     *
     *  @since 1.6.4
     */
    protected String makeHTML( WikiContext context, String wikitext )
    {
        String result = "";
        TranslatorReader in = null;

        try
        {
            in     = new TranslatorReader( context,
                                           new StringReader( wikitext ) );
            in.addLinkTransmutator( new CutMutator(m_maxwidth) );

            result = FileUtil.readContents( in );
        }
        catch( IOException e )
        {
            log.error("Failed to convert page data to HTML", e);
        }
        finally
        {
            try
            {
                if( in != null ) in.close();
            }
            catch( Exception e ) 
            {
                log.fatal("Closing failed",e);
            }
        }

        return result;
    }
    
    /**
     *  A simple class that just cuts a String to a maximum
     *  length, adding three dots after the cutpoint.
     */
    private class CutMutator implements StringTransmutator
    {
        private int m_length;

        public CutMutator( int length )
        {
            m_length = length;
        }

        public String mutate( WikiContext context, String text )
        {
            if( text.length() > m_length )
            {
                return text.substring( 0, m_length ) + "...";
            }

            return text;
        }
    }
}
