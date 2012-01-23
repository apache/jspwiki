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
package org.apache.wiki.ui.stripes;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.Collection;
import java.util.Locale;

import net.sourceforge.stripes.config.Configuration;
import net.sourceforge.stripes.controller.StripesFilter;
import net.sourceforge.stripes.validation.LocalizableError;
import net.sourceforge.stripes.validation.SimpleError;
import net.sourceforge.stripes.validation.TypeConverter;
import net.sourceforge.stripes.validation.ValidationError;

import org.apache.wiki.InternalWikiException;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.api.WikiPage;
import org.apache.wiki.content.ContentManager;
import org.apache.wiki.content.PageAlreadyExistsException;
import org.apache.wiki.content.PageNotFoundException;
import org.apache.wiki.content.WikiPath;
import org.apache.wiki.providers.ProviderException;

/**
 * Stripes type converter that converts a WikiPage name, expressed as a String,
 * into an {@link org.apache.wiki.api.WikiPage} object. This converter is looked
 * up and returned by the Stripes
 * {@link net.sourceforge.stripes.validation.TypeConverterFactory} for HTTP
 * request parameters that need to be bound to ActionBean properties of type
 * WikiPage. Stripes executes this TypeConverter during the
 * {@link net.sourceforge.stripes.controller.LifecycleStage#BindingAndValidation}
 * stage of request processing.
 * 
 */
public class WikiPageTypeConverter implements TypeConverter<WikiPage>
{
    /**
     * Converts a named wiki page into a valid WikiPage object by retrieving the
     * latest version via the WikiEngine. If the page cannot be found (perhaps because it
     * does not exist) and it is not a special page, this method return a newly 
     * instantiated WikiPage that has not yet been saved to the repository.
     * In other words: for non-<code>null</code> values of
     * <code>pageName</code> where the page does not correspond to a
     * special page, this method is guaranteed to return a WikiPage. The only
     * time this method will return <code>null</code> is if the string
     * corresponds to a special page.
     * 
     * @param pageName the name of the WikiPage to retrieve
     * @param targetType the type to return, which will always be of type
     *            {@link org.apache.wiki.api.WikiPage}
     * @param errors the current Collection of validation errors for this field
     * @return the WikiPage
     */
    public WikiPage convert( String pageName, Class<? extends WikiPage> targetType, Collection<ValidationError> errors )
    {
        Configuration config = StripesFilter.getConfiguration();
        WikiEngine engine = WikiEngine.getInstance( config.getServletContext(), null );
        WikiPage page = null;
        
        // Decode the page name
        try
        {
            String decodedName = URLDecoder.decode( pageName, "UTF-8" );
            if ( decodedName != null )
            {
                pageName = decodedName;
            }
        }
        catch( UnsupportedEncodingException e1 )
        {
            throw new InternalWikiException( "Impossible! UTF-8 must be supported." );
        }
        
        // Is this a special page?
        URI uri = engine.getSpecialPageReference( pageName );
        if( uri != null )
        {
            errors.add( new LocalizableError( "edit.specialPage" ) );
            return null;
        }
        
        // Not a special page. Let's go get (or create) the page...
        try
        {
            page = engine.getPage( pageName );
        }
        catch( PageNotFoundException e )
        {
            try
            {
                page = getFinalPage( engine, pageName );
                if ( page == null )
                {
                    ContentManager cm = engine.getContentManager();
                    page = cm.addPage( WikiPath.valueOf( pageName ), ContentManager.JSPWIKI_CONTENT_TYPE );
                    cm.release();
                }
            }
            catch( PageAlreadyExistsException e2 )
            {
                // If content manager can't add a new page (should not happen!)
                errors.add( new SimpleError( e2.getMessage() ) );
            }
            catch( ProviderException e2 )
            {
                errors.add( new SimpleError( e2.getMessage() ) );
            }
        }
        catch( ProviderException e )
        {
            errors.add( new SimpleError( "Provider exception: " + e.getMessage() ) );
        }
        return page;
    }

    /**
     * Looks up and returns the WikiPage matching the supplied page name, trying all possible
     * variations as defined by {@link WikiEngine#getFinalPageName(WikiPath)}.
     * @param engine the wiki engine
     * @param pageName the page name to find
     * @return the WikiPage, if contained in the repository
     * @throws ProviderException in unusual cases; this should never happen
     */
    private WikiPage getFinalPage( WikiEngine engine, String pageName ) throws ProviderException
    {
        WikiPath finalName = engine.getFinalPageName( WikiPath.valueOf( pageName ) );
        if ( finalName != null )
        {
            try
            {
                return engine.getPage( finalName );
            }
            catch( PageNotFoundException pnf )
            {
                // This should never happen, because getFinalPageName always verifies the page exists!
                pnf.printStackTrace();
                throw new ProviderException( "Could not find WikiPage " + finalName + " even though we just found it. Odd!" );
            }
        }
        return null;
    }
    
    /**
     * No-op method that does nothing, because setting the Locale has no effect on the conversion.
     */
    public void setLocale( Locale locale )
    {
    }
}
