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
package org.apache.wiki.plugin;

import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.exceptions.PluginException;
import org.apache.wiki.api.plugin.Plugin;
import org.apache.wiki.render.RenderingManager;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shows the content that matches the language preference of the visitor, as the browser sends it in the {@code Accept-Language}
 * header. The body holds one block per language, written as {@code :xx:text::} with the language code between colons, and an optional
 * {@code :else:text::} block for visitors that none of the blocks suits:
 *
 * <pre>
 * [{LanguagePlugin
 *
 * :de:Willkommen::  :en:Welcome::  :else:Welcome / Willkommen::}]
 * </pre>
 *
 * <p>The languages of the visitor are tried in the order of their quality values, and a block matches when its code is the language of
 * the tag, so {@code de} matches {@code de-AT}. Only the language is compared, not the region. If no block matches and there is no
 * {@code :else:} block, nothing is shown. The text of the chosen block is rendered as wiki markup. A block ends at the first
 * {@code ::}, so the text of a block cannot contain it.</p>
 *
 * <p>Parameters: none.</p>
 *
 * @since 3.0.1
 */
public class LanguagePlugin implements Plugin {

    private static final Pattern BLOCK = Pattern.compile( ":([a-zA-Z]{2,3}):(.*?)::", Pattern.DOTALL );
    private static final Pattern ELSE_BLOCK = Pattern.compile( ":else:(.*?)::", Pattern.DOTALL );

    /** A language tag of the {@code Accept-Language} header with its quality value. */
    private static final class Preference {
        final String language;
        final double quality;

        Preference( final String language, final double quality ) {
            this.language = language;
            this.quality = quality;
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getDisplayName( final Locale locale ) {
        final ResourceBundle rb = ResourceBundle.getBundle( PluginManager.PLUGIN_I18N_RESOURCE, locale );
        return rb.getString( this.getClass().getSimpleName() );
    }

    /** {@inheritDoc} */
    @Override
    public String getSnipExample() {
        return "LanguagePlugin\n\n:de:{text}::  :en:{text}::  :else:{text}::\n";
    }

    /** {@inheritDoc} */
    @Override
    public String execute( final Context context, final Map< String, String > params ) throws PluginException {
        final String body = params.get( DefaultPluginManager.PARAM_BODY );
        if( body == null || body.isEmpty() ) {
            return "";
        }
        final HttpServletRequest request = context.getHttpRequest();
        final String header = request == null ? null : request.getHeader( "Accept-Language" );
        String text = null;
        for( final String language : parse( header ) ) {
            final Matcher blocks = BLOCK.matcher( body );
            while( text == null && blocks.find() ) {
                if( language.equalsIgnoreCase( blocks.group( 1 ) ) ) {
                    text = blocks.group( 2 );
                }
            }
            if( text != null ) {
                break;
            }
        }
        if( text == null ) {
            final Matcher fallback = ELSE_BLOCK.matcher( body );
            if( !fallback.find() ) {
                return "";
            }
            text = fallback.group( 1 );
        }
        return context.getEngine().getManager( RenderingManager.class ).textToHTML( context, text.trim() );
    }

    /**
     * Reads the languages of an {@code Accept-Language} header, most preferred first. Tags with a quality of zero and the wildcard are
     * left out, and so are entries that cannot be read.
     *
     * @param header the value of the header, may be {@code null}.
     * @return the languages, without region, in the order of preference.
     */
    static List< String > parse( final String header ) {
        final List< Preference > preferences = new ArrayList<>();
        if( header == null ) {
            return new ArrayList<>();
        }
        for( final String entry : header.split( "," ) ) {
            final String[] parts = entry.trim().split( ";" );
            final String tag = parts[ 0 ].trim();
            if( tag.isEmpty() || tag.equals( "*" ) ) {
                continue;
            }
            double quality = 1.0;
            for( int i = 1; i < parts.length; i++ ) {
                final String parameter = parts[ i ].trim();
                if( parameter.regionMatches( true, 0, "q=", 0, 2 ) ) {
                    try {
                        quality = Double.parseDouble( parameter.substring( 2 ).trim() );
                    } catch( final NumberFormatException e ) {
                        quality = 0;
                    }
                }
            }
            if( quality > 0 ) {
                final int dash = tag.indexOf( '-' );
                preferences.add( new Preference( dash < 0 ? tag : tag.substring( 0, dash ), quality ) );
            }
        }
        preferences.sort( Comparator.comparingDouble( ( final Preference p ) -> p.quality ).reversed() );
        final List< String > languages = new ArrayList<>();
        for( final Preference preference : preferences ) {
            languages.add( preference.language );
        }
        return languages;
    }

}
