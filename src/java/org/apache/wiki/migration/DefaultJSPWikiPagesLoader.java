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
package org.apache.wiki.migration;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.List;

import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.api.WikiException;
import org.apache.wiki.api.WikiPage;
import org.apache.wiki.content.PageAlreadyExistsException;
import org.apache.wiki.content.PageNotFoundException;
import org.apache.wiki.content.WikiPath;
import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;
import org.apache.wiki.providers.ProviderException;
import org.apache.wiki.util.FileUtil;


/**
 * Migration Manager to load an initial set of 2.8 JSPWiki pages.
 * 
 * Could be expanded to import a 2.8 JSPWiki repository.
 */
public class DefaultJSPWikiPagesLoader implements MigrationManager
{
    
    /** class logger */
    private static final Logger LOG = LoggerFactory.getLogger( DefaultJSPWikiPagesLoader.class );
    
    /** 2.8 file wiki pages */
    private static final String TXT_SUFFIX = ".txt";
    
    /** filter for 2.8 wiki pages */
    private FilenameFilter filter = new FilenameFilter() 
    {
        /**
         * {@inheritDoc}
         */
        public boolean accept( File dir, String name ) 
        {
            return name.toLowerCase().endsWith( TXT_SUFFIX );
        }
    };
    
    /**
     * {@inheritDoc}
     */
    public void migrate( WikiEngine engine, MigrationVO vo ) 
    {
        if( vo != null && vo.getRepoDir() != null ) 
        {
            File repo = new File( vo.getRepoDir().trim() );
            if( repo.isDirectory() ) 
            {
                LOG.info( "migrating from " + vo );
                List< File > wikiPageFiles = Arrays.asList( repo.listFiles( filter ) );
                for( File wpf : wikiPageFiles ) 
                {
                    LOG.info( "migrating " + wpf.getName() );
                    migrateOlderVersionsFor( engine, repo, wpf.getName() );
                    migratePage( engine, wpf );
                }
            }
        }
    }
    
    /**
     * Migrates all the older versions of a given 2.8 WikiPage.
     * 
     * @param engine {@link WikiEngine} performing the migration.
     * @param repo directory holding the 2.8 repository.
     * @param page 2.8 WikiPage to be migrated.
     */
    void migrateOlderVersionsFor( WikiEngine engine, File repo, String page ) 
    {
        /* TODO
        List< File > wpVersioned = obtainOlderVersionsFor( page );
        for( File version : wpVersioned ) 
        {
            File oldVersion = obtainOldVersion( engine, repo, page );
            migratePage( engine, oldVersion );
        } 
        */
    }
    
    /**
     * Performs the migration of a simple 2.8 WikiPage.
     * 
     * @param engine {@link WikiEngine} performing the migration.
     * @param page Page to be loaded.
     */
    void migratePage( WikiEngine engine, File page ) 
    {
        WikiPage wp = getWikiPage( engine, page.getName().replaceAll( TXT_SUFFIX, "" ) );
        
        // TODO: add metadata held in the correponding page.properties file
        // TODO: add attachments
        WikiContext context = engine.getWikiContextFactory().newViewContext( null, null, wp );
        try
        {
            engine.saveText( context, FileUtil.getContentFrom( page.getAbsolutePath() ) );
        }
        catch( WikiException we )
        {
            LOG.error( "problems migrating " + page.getName(), we );
        }
    }
    
    /**
     * Retrieves a WikiPage or, if it doesn't exist, creates a new one with the given name.
     * 
     * @param engine {@link WikiEngine} responsible of grabbing the WikiPage. 
     * @param pageName Name of the WikiPage.
     * @return the asked WikiPage or a new one with the given name if it doesn't exist.
     */
    WikiPage getWikiPage( WikiEngine engine, String pageName ) 
    {
        WikiPage wpage = null;
        try
        {
            wpage = engine.getPage( pageName );
        }
        catch ( PageNotFoundException pnfe )
        { 
            LOG.info( "page " + pageName + " not found, will proceed to create a new one" );
        }
        catch( ProviderException pe )
        {
            LOG.error( pe.getMessage(), pe );
        }
        catch( IllegalArgumentException iae )
        {
            LOG.error( iae.getMessage(), iae );
        }
        if( wpage == null ) 
        {
             try
            {
                wpage = engine.createPage( WikiPath.valueOf( pageName ) );
            }
            catch( ProviderException pe )
            {
                LOG.error( pe.getMessage(), pe );
            }
            catch( IllegalArgumentException iae )
            {
                LOG.error( iae.getMessage(), iae );
            }
            catch( PageAlreadyExistsException paee )
            {
                LOG.error( paee.getMessage(), paee );
            }
        }
        return wpage;
    }
    
}