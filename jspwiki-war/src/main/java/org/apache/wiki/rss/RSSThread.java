/* 
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
package org.apache.wiki.rss;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;

import org.apache.log4j.Logger;
import org.apache.wiki.WatchDog;
import org.apache.wiki.WikiBackgroundThread;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.util.FileUtil;

/**
 *  Runs the RSS generation thread.
 *  FIXME: MUST be somewhere else, this is not a good place.
 */
public class RSSThread extends WikiBackgroundThread
{
    static Logger              log = Logger.getLogger( RSSThread.class );
        
    private final File m_rssFile;
    private final RSSGenerator m_generator;
        
    private WatchDog m_watchdog;
    
    /**
     *  Create a new RSS thread.
     *  
     *  @param engine A WikiEngine to own this thread.
     *  @param rssFile A File to write the RSS data to.
     *  @param rssInterval How often the RSS should be generated.
     */
    public RSSThread( WikiEngine engine, File rssFile, int rssInterval )
    {
        super( engine, rssInterval );
        m_generator = engine.getRSSGenerator();
        m_rssFile = rssFile;
        setName("JSPWiki RSS Generator");
        log.debug( "RSS file will be at "+m_rssFile.getAbsolutePath() );
        log.debug( "RSS refresh interval (seconds): "+rssInterval );
    }
    
    /**
     *  {@inheritDoc}
     */
    @Override
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
     * @throws Exception All exceptions are thrown upwards.
     */
    @Override
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
