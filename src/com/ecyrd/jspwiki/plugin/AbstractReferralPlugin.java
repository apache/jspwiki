/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2005 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.oro.text.GlobCompiler;
import org.apache.oro.text.regex.*;

import com.ecyrd.jspwiki.*;
import com.ecyrd.jspwiki.parser.MarkupParser;
import com.ecyrd.jspwiki.parser.WikiDocument;
import com.ecyrd.jspwiki.render.RenderingManager;

/**
 *  This is a base class for all plugins using referral things.
 *
 *  <p>Parameters:<br>
 *  maxwidth: maximum width of generated links<br>
 *  separator: separator between generated links (wikitext)<br>
 *  after: output after the link
 *  before: output before the link
 *  @author Janne Jalkanen
 */
public abstract class AbstractReferralPlugin
    implements WikiPlugin
{
    private static Logger log = Logger.getLogger( AbstractReferralPlugin.class );

    public static final int    ALL_ITEMS       = -1;
    public static final String PARAM_MAXWIDTH  = "maxwidth";
    public static final String PARAM_SEPARATOR = "separator";
    public static final String PARAM_AFTER     = "after";
    public static final String PARAM_BEFORE    = "before";
 
    public static final String PARAM_EXCLUDE   = "exclude";
    
    protected           int      m_maxwidth = Integer.MAX_VALUE;
    protected           String   m_before = ""; // null not blank
    protected           String   m_separator = ""; // null not blank 
    protected           String   m_after = "\\\\";
    
    protected           Pattern[]  m_exclude;
    
    protected           WikiEngine m_engine;

    /**
     *  Used to initialize some things.  All plugins must call this first.
     *
     *  @since 1.6.4
     */

    // FIXME: The compiled pattern strings should really be cached somehow.
    
    public void initialize( WikiContext context, Map params )
        throws PluginException
    {
        m_engine = context.getEngine();
        m_maxwidth = TextUtil.parseIntParameter( (String)params.get( PARAM_MAXWIDTH ), Integer.MAX_VALUE ); 
        if( m_maxwidth < 0 ) m_maxwidth = 0;

        String s = (String) params.get( PARAM_SEPARATOR );

        if( s != null )
        {
            m_separator = s;
            // pre-2.1.145 there was a separator at the end of the list
            // if they set the parameters, we use the new format of 
            // before Item1 after separator before Item2 after separator before Item3 after
            m_after = "";
        }

        s = (String) params.get( PARAM_BEFORE );
        
        if( s != null )
        {
            m_before = s;
        }

        s = (String) params.get( PARAM_AFTER );
        
        if( s != null )
        {
            m_after = s;
        }
  
        s = (String) params.get( PARAM_EXCLUDE );
        
        if( s != null )
        {
            try
            {
                PatternCompiler pc = new GlobCompiler();

                String[] ptrns = StringUtils.split( s, "," );
                
                m_exclude = new Pattern[ptrns.length];
                
                for( int i = 0; i < ptrns.length; i++ )
                {
                    m_exclude[i] = pc.compile( ptrns[i] );
                }
            }
            catch( MalformedPatternException e )
            {
                throw new PluginException("Exclude-parameter has a malformed pattern: "+e.getMessage());
            }
        }
        
        // log.debug( "Requested maximum width is "+m_maxwidth );
    }
    
    protected Collection filterCollection( Collection c )
    {
        if( m_exclude == null || m_exclude.length == 0 ) return c;
        
        PatternMatcher pm = new Perl5Matcher();
        
        for( Iterator i = c.iterator(); i.hasNext(); )
        {
            String pageName = (String) i.next();
            
            for( int j = 0; j < m_exclude.length; j++ )
            {
                if( pm.matches( pageName, m_exclude[j] ) )
                {
                    i.remove();
                    break; // The inner loop, continue on the next item
                }
            }
        }
        
        return c;
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
        if( links == null || links.isEmpty() )
            return( "" );

        StringBuffer output = new StringBuffer();
        
        Iterator it     = links.iterator();
        int      count  = 0;
        
        //
        //  The output will be B Item[1] A S B Item[2] A S B Item[3] A
        //
        while( it.hasNext() && ( (count < numItems) || ( numItems == ALL_ITEMS ) ) )
        {
            String value = (String)it.next();

            if( count > 0 )
            {
                output.append( m_after );
                output.append( m_separator );
            }
            
            output.append( m_before );
            
            // Make a Wiki markup link. See TranslatorReader.
            output.append( "[" + m_engine.beautifyTitle(value) + "]" );
            count++;
        }

        // 
        //  Output final item - if there have been none, no "after" is printed
        //
        if( count > 0 ) output.append( m_after );
        
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
        
        RenderingManager mgr = m_engine.getRenderingManager();
        
        try
        {
            MarkupParser parser = mgr.getParser(context, wikitext);
            
            parser.addLinkTransmutator( new CutMutator(m_maxwidth) );
            parser.enableImageInlining( false );
            
            WikiDocument doc = parser.parse();
            
            result = mgr.getHTML( context, doc );
        }
        catch( IOException e )
        {
            log.error("Failed to convert page data to HTML", e);
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
