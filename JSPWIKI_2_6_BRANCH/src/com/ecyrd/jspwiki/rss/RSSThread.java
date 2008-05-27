/*
   JSPWiki - a JSP-based WikiWiki clone.

   Copyright (C) 2005-2007 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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
package com.ecyrd.jspwiki.rss;


import java.io.BufferedWriter;
import java.io.File;


import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;

import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.FileUtil;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.util.WatchDog;
import com.ecyrd.jspwiki.util.WikiBackgroundThread;

/**
 *  Runs the RSS generation thread.
 *  FIXME: MUST be somewhere else, this is not a good place.
 */
public class RSSThread extends WikiBackgroundThread
{
    static Logger              log = Logger.getLogger( RSSThread.class );
        
    private final File m_rssFile;
    private final int m_rssInterval;
    private final RSSGenerator m_generator;
        
    private WatchDog m_watchdog;
    
    public RSSThread( WikiEngine engine, File rssFile, int rssInterval )
    {
        super( engine, rssInterval );
        m_generator = engine.getRSSGenerator();
        m_rssFile = rssFile;
        m_rssInterval = rssInterval;
        setName("JSPWiki RSS Generator");
        log.debug( "RSS file will be at "+m_rssFile.getAbsolutePath() );
        log.debug( "RSS refresh interval (seconds): "+m_rssInterval );
    }
    
    public void startupTask() throws Exception
    {
        m_watchdog = getEngine().getCurrentWatchDog();
    }
    
    /**
     * Runs the RSS generator thread.
     * If a previous RSS generation operation encountered a 
     * file I/O or other error, this method will turn off generation.
     * <code>false</code>.
     * @see java.lang.Thread#run()
     */
    public void backgroundTask() throws Exception
    {
        if ( m_generator.isEnabled() )
        {
            Writer out = null;
            Reader in  = null;

            m_watchdog.enterState( "Generating RSS feed", 60 );
            
            try
            {
                //
                //  Generate RSS file, output it to
                //  default "rss.rdf".
                //
                log.debug("Regenerating RSS feed to "+m_rssFile);

                String feed = m_generator.generate();

                in  = new StringReader(feed);
                out = new BufferedWriter( new OutputStreamWriter( new FileOutputStream( m_rssFile ), "UTF-8") );

                FileUtil.copyContents( in, out );
            }
            catch( IOException e )
            {
                log.error("Cannot generate RSS feed to "+m_rssFile.getAbsolutePath(), e );
                m_generator.setEnabled( false );
            }
            finally
            {
                try
                {
                    if( in != null )  in.close();
                    if( out != null ) out.close();
                }
                catch( IOException e )
                {
                    log.fatal("Could not close I/O for RSS", e );
                    m_generator.setEnabled( false );
                }
                m_watchdog.exitState();
            }

        }
    }
        
}
