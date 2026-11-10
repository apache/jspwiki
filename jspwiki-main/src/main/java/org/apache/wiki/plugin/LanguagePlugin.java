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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

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

    /** The code of the block that is shown when none of the languages of the visitor has a block. */
    private static final String ELSE = "else";

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
        final Map< String, String > blocks = parseBlocks( body );
        String text = null;
        for( final String language : parse( header ) ) {
            text = blocks.get( language.toLowerCase( Locale.ROOT ) );
            if( text != null ) {
                break;
            }
        }
        if( text == null ) {
            text = blocks.get( ELSE );
            if( text == null ) {
                return "";
            }
        }
        return context.getEngine().getManager( RenderingManager.class ).textToHTML( context, text.trim() );
    }

    /**
     * Splits the body into its blocks, written as {@code :xx:text::}. The body is scanned once, without regular expressions, because
     * it is written by the authors of the page and a pattern with a lazy match would take a time that grows with the square of its
     * length on a crafted body. When a code occurs more than once, the first block is kept.
     *
     * @param body the body of the plugin.
     * @return the text of each block by its code in lower case, with the else block under {@code else}.
     */
    static Map< String, String > parseBlocks( final String body ) {
        final Map< String, String > blocks = new HashMap<>();
        int position = 0;
        while( position < body.length() ) {
            final int start = body.indexOf( ':', position );
            if( start < 0 ) {
                break;
            }
            final int codeEnd = body.indexOf( ':', start + 1 );
            if( codeEnd < 0 ) {
                break;
            }
            final String code = body.substring( start + 1, codeEnd );
            if( isCode( code ) ) {
                final int end = body.indexOf( "::", codeEnd + 1 );
                if( end < 0 ) {
                    break;
                }
                blocks.putIfAbsent( code.toLowerCase( Locale.ROOT ), body.substring( codeEnd + 1, end ) );
                position = end + 2;
            } else {
                position = start + 1;
            }
        }
        return blocks;
    }

    /** A code is a language of two or three letters, or the word else. */
    private static boolean isCode( final String code ) {
        if( ELSE.equals( code ) ) {
            return true;
        }
        if( code.length() < 2 || code.length() > 3 ) {
            return false;
        }
        for( int i = 0; i < code.length(); i++ ) {
            final char c = code.charAt( i );
            if( !( c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' ) ) {
                return false;
            }
        }
        return true;
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
