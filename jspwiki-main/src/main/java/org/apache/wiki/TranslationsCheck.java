/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.wiki;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;


/**
 * Simple utility that shows you a sorted list of property differences between
 * the 'default en' and a given i18n file. It also warns if there are any duplicates.
 * <p>
 * The first argument is the language, and it is mandatory.
 * The second argument is the path.  If the path is not defined, uses current path (.)
 * <p>
 * For example (if you're compiling your classes to "classes"):
 * <code>
 * java -cp classes TranslationsCheck fi
 * </code>
 */
public class TranslationsCheck {

    private final static String[] LANGS = { "de", "en", "es", "fi", "fr", "it", "nl", "pt_BR", "ru", "zh_CN" };
    private final static String SITE_I18N_ROW =
        "<tr%s>\n" +
        "  <td title=\"Available sets of core WikiPages for %s\"><a class=\"external\" href=\"https://search.maven.org/artifact/org.apache.jspwiki.wikipages/jspwiki-wikipages-%s\">%s</a></td>\n" +
        "  <td>%d%%</td>\n" +
        "  <td>%d</td>\n" +
        "  <td>%d</td>\n" +
        "</tr>\n";

    private final TreeSet< String > allProps = new TreeSet<>();  // sorted, no duplicates
    private final TreeSet< String > duplProps = new TreeSet<>();

    // Change these to your settings...
    String base = ".";
    String suffix;

    public static void main( final String[] args ) throws IOException {
        final TranslationsCheck translations = new TranslationsCheck();
        if( args.length == 0 ) {
            System.out.println( "Usage: java TranslationsCheck <language> [<path>]" );
            System.out.println( "Example: java TranslationsCheck nl [jspwiki-main/src/main/resources]" );
            System.out.println( "To output site i18n info use java TranslationsCheck site [<path>]" );
            return;
        }
        translations.suffix = args[ 0 ];
        if( args.length >= 2 ) {
            translations.base = args[ 1 ];
        }

        if( "site".equals( translations.suffix ) ) {
            final StringBuilder site = new StringBuilder();
            for( int i = 0; i < LANGS.length; i++ ) {
                translations.suffix = LANGS[ i ];
                site.append(translations.check(i));
            }
            site.append("</table>\n" + // close table and formatting divs
                    "</div>\n" + "</div>\n" + "</div>");
            Files.write( Paths.get( "./i18n-table.txt" ), site.toString().getBytes( StandardCharsets.UTF_8 ) );
        } else {
            translations.check( -1 );
        }
    }

    String check( final int lang ) throws IOException {
        // System.out.println( "Using code base " + Release.VERSTR );
        System.out.println( "Internationalization property file differences between 'default en' and '" + suffix + "' following:\n" );

        final String fileSuffix = ( "en".equals( suffix ) ) ? "" : "_" + suffix;
        final Map< String, Integer > coreMetrics = checkFile( "/CoreResources.properties", "/CoreResources" + fileSuffix + ".properties" );
        final Map< String, Integer > templateMetrics = checkFile( "/templates/default.properties", "/templates/default" + fileSuffix + ".properties" );
        final Map< String, Integer > pluginMetrics = checkFile( "/plugin/PluginResources.properties", "/plugin/PluginResources" + fileSuffix + ".properties" );

        if( lang >= 0 ) {
            final int expected = coreMetrics.get( "expected" ) + templateMetrics.get( "expected" ) + pluginMetrics.get( "expected" );
            final int missing = coreMetrics.get( "missing" ) + templateMetrics.get( "missing" ) + pluginMetrics.get( "missing" );
            final int completed = 100 * ( expected - missing ) / expected;
            final int outdated = coreMetrics.get( "outdated" ) + templateMetrics.get( "outdated" ) + pluginMetrics.get( "outdated" );
            final String odd = lang %2 == 0 ? " class=\"odd\"" : ""; // 0 first row

            return String.format( SITE_I18N_ROW, odd, suffix, suffix, suffix, completed, missing, outdated );
        }
        return "";
    }

    Map< String, Integer > checkFile( final String en, final String lang ) throws IOException {
        final Map< String, Integer > metrics = new HashMap<>();
        try {
            metrics.putAll( diff( en, lang ) );
            metrics.put( "duplicates", detectDuplicates( lang ) );
        } catch( final FileNotFoundException e ) {
            System.err.println( "Unable to locate " + lang );
        }
        System.out.println( "Duplicates overall (two or more occurences):" );
        System.out.println( "--------------------------------------------" );
        final Iterator< String > iter = duplProps.iterator();
        if( duplProps.size() == 0 ) {
            System.out.println( "(none)" );
        } else {
            while( iter.hasNext() ) {
                System.out.println( iter.next() );
            }
        }
        System.out.println( "" );
        return metrics;
    }

    public Map< String, Integer > diff( final String source1, final String source2 ) throws IOException {
        int missing = 0, outdated = 0;
        // Standard Properties
        final Properties p1 = new Properties();
        p1.load( getResourceAsStream( source1 ) );

        final Properties p2 = new Properties();
        p2.load( getResourceAsStream( source2 ) );

        final String msg = "Checking " + source2 + "...";
        System.out.println( msg );

        Iterator< String > iter = sortedNames( p1 ).iterator();
        while( iter.hasNext() ) {
            final String name = iter.next();
            final String value = p1.getProperty( name );

            if( p2.get( name ) == null ) {
                missing++;
                if( missing == 1 ) {
                    System.out.println( "\nMissing:" );
                    System.out.println( "--------" );
                }
                System.out.println( name + " = " + value );
            }
        }
        if( missing > 0 ) {
            System.out.println( "" );
        }

        iter = sortedNames( p2 ).iterator();
        while( iter.hasNext() ) {
            final String name = iter.next();
            final String value = p2.getProperty( name );

            if( p1.get( name ) == null ) {
                outdated++;
                if( outdated == 1 ) {
                    System.out.println( "\nOutdated or superfluous:" );
                    System.out.println( "------------------------" );
                }
                System.out.println( name + " = " + value );
            }
        }
        if( outdated > 0 ) {
            System.out.println( "" );
        }

        final Map< String, Integer > diff = new HashMap<>( 2 );
        diff.put( "expected", p1.size() );
        diff.put( "missing", missing );
        diff.put( "outdated", outdated );
        return diff;
    }

    private List< String > sortedNames( final Properties p ) {
        final List< String > list = new ArrayList<>();
        final Enumeration< ? > iter = p.propertyNames();
        while( iter.hasMoreElements() ) {
            list.add( ( String )iter.nextElement() );
        }

        Collections.sort( list );
        return list;
    }

    public int detectDuplicates( final String source ) throws IOException {
        final Properties p = new Properties();
        p.load( getResourceAsStream( source ) );
        final Enumeration< ? > iter = p.propertyNames();
        String currentStr;
        while( iter.hasMoreElements() ) {
            currentStr = ( String )iter.nextElement();
            if( !allProps.add( currentStr ) ) {
                duplProps.add( currentStr );
            }
        }
        return duplProps.size();
    }

    InputStream getResourceAsStream( final String source ) {
        return TranslationsCheck.class.getClassLoader().getResourceAsStream( base + source );
    }

}
