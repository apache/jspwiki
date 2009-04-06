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

import java.util.Collection;
import java.util.Locale;

import org.apache.wiki.WikiEngine;
import org.apache.wiki.api.WikiPage;
import org.apache.wiki.content.PageNotFoundException;
import org.apache.wiki.content.WikiName;
import org.apache.wiki.providers.ProviderException;

import net.sourceforge.stripes.controller.StripesFilter;
import net.sourceforge.stripes.validation.LocalizableError;
import net.sourceforge.stripes.validation.SimpleError;
import net.sourceforge.stripes.validation.TypeConverter;
import net.sourceforge.stripes.validation.ValidationError;

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
 * @author Andrew Jaquith
 */
public class WikiPageTypeConverter implements TypeConverter<WikiPage>
{
    /**
     * Converts a named wiki page into a valid WikiPage object by retrieving the
     * latest version via the WikiEngine. If the page cannot be found (perhaps because it
     * does not exist), this method will add a validation error to the supplied
     * Collection of errors and return <code>null</code>. The error will be
     * of type {@link net.sourceforge.stripes.validation.LocalizableError} and
     * will have a message key of <code>common.nopage</code> and a single
     * parameter (equal to the value passed for <code>pageName</code>).
     * 
     * @param pageName the name of the WikiPage to retrieve
     * @param targetType the type to return, which will always be of type
     *            {@link org.apache.wiki.api.WikiPage}
     * @param errors the current Collection of validation errors for this field
     * @return the
     */
    public WikiPage convert( String pageName, Class<? extends WikiPage> targetType, Collection<ValidationError> errors )
    {
        WikiRuntimeConfiguration config = (WikiRuntimeConfiguration) StripesFilter.getConfiguration();
        WikiEngine engine = config.getEngine();
        WikiPage page = null;
        try
        {
            page = engine.getPage( pageName );
        }
        catch( PageNotFoundException e )
        {
            try
            {
                WikiName finalName = engine.getFinalPageName( WikiName.valueOf( pageName ) );
                if ( finalName == null )
                {
                    errors.add( new LocalizableError( "common.nopage", pageName ) );
                }
                else
                {
                    try
                    {
                        return engine.getPage( finalName );
                    }
                    catch( PageNotFoundException pnf )
                    {
                        // This should never happen, because getFinalPageName always verifies the page exists!
                        pnf.printStackTrace();
                    }
                }
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

    public void setLocale( Locale locale )
    {
    }
}
