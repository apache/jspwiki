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
package org.apache.wiki.parser;

import org.apache.log4j.Logger;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.Perl5Matcher;
import org.apache.wiki.WikiContext;
import org.apache.wiki.api.exceptions.ProviderException;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;


/**
 * Link parsing operations.
 *
 * @since 2.10.3
 */
public class LinkParsingOperations {

    private static Logger log = Logger.getLogger( LinkParsingOperations.class );
    private final WikiContext wikiContext;

    /**
     *  This list contains all IANA registered URI protocol
     *  types as of September 2004 + a few well-known extra types.
     *
     *  JSPWiki recognises all of them as external links.
     *
     *  This array is sorted during class load, so you can just dump
     *  here whatever you want in whatever order you want.
     */
    static final String[] EXTERNAL_LINKS = {
        "http:", "ftp:", "https:", "mailto:",
        "news:", "file:", "rtsp:", "mms:", "ldap:",
        "gopher:", "nntp:", "telnet:", "wais:",
        "prospero:", "z39.50s", "z39.50r", "vemmi:",
        "imap:", "nfs:", "acap:", "tip:", "pop:",
        "dav:", "opaquelocktoken:", "sip:", "sips:",
        "tel:", "fax:", "modem:", "soap.beep:", "soap.beeps",
        "xmlrpc.beep", "xmlrpc.beeps", "urn:", "go:",
        "h323:", "ipp:", "tftp:", "mupdate:", "pres:",
        "im:", "mtqp", "smb:"
    };

    static {
        Arrays.sort( EXTERNAL_LINKS );
    }

    public LinkParsingOperations( final WikiContext wikiContext ) {
    	this.wikiContext = wikiContext;
    }

    /**
     *  Returns true, if the link in question is an access rule.
     *
     * @param link The link text
     * @return {@code true}, if this represents an access rule.
     */
    public boolean isAccessRule( final String link ) {
        return link.startsWith("{ALLOW") || link.startsWith("{DENY");
    }

    /**
     *  Returns true if the link is really command to insert a plugin.
     *  <P>
     *  Currently we just check if the link starts with "{INSERT",
     *  or just plain "{" but not "{$".
     *
     *  @param link Link text, i.e. the contents of text between [].
     *  @return True, if this link seems to be a command to insert a plugin here.
     */
    public boolean isPluginLink( final String link ) {
        return link.startsWith( "{INSERT" ) ||
               ( link.startsWith( "{" ) && !link.startsWith( "{$" ) );
    }

    /**
     * Returns true if the link is a metadata link.
     *
     * @param link The link text
     * @return {@code true}, if this represents a metadata link.
     */
    public boolean isMetadata( final String link ) {
        return link.startsWith( "{SET" );
    }

    /**
     * Returns true if the link is really command to insert a variable.
     * <P>
     * Currently we just check if the link starts with "{$".
     *
     * @param link The link text
     * @return {@code true}, if this represents a variable link.
     */
    public boolean isVariableLink( final String link ) {
        return link.startsWith( "{$" );
    }

    /**
     * Returns true, if this Link represents an InterWiki link (of the form wiki:page).
     *
     * @return {@code true}, if this Link represents an InterWiki link, {@code false} otherwise.
     */
    public boolean isInterWikiLink( final String page ) {
        return interWikiLinkAt( page ) != -1;
    }

    /**
     * Returns true, if this Link represents an InterWiki link (of the form wiki:page).
     *
     * @return {@code true}, if this Link represents an InterWiki link, {@code false} otherwise.
     */
    public int interWikiLinkAt( final String page ) {
        return page.indexOf( ':' );
    }

    /**
     * Figures out if a link is an off-site link.  This recognizes
     * the most common protocols by checking how it starts.
     *
     * @param page The link to check.
     * @return true, if this is a link outside of this wiki.
     */
    public boolean isExternalLink( final String page ) {
        final int idx = Arrays.binarySearch( EXTERNAL_LINKS, page, new StartingComparator() );

        // We need to check here once again; otherwise we might get a match for something like "h".
        return idx >= 0 && page.startsWith( EXTERNAL_LINKS[ idx ] );
    }

    /**
     *  Matches the given link to the list of image name patterns to
     *  determine whether it should be treated as an inline image or not.
     */
    public boolean isImageLink( String link ) {
        if( wikiContext.getEngine().getRenderingManager().getParser( wikiContext, link ).isImageInlining() ) {
            link = link.toLowerCase();
            final List< Pattern > inlineImagePatterns = wikiContext.getEngine().getRenderingManager()
            	                                                   .getParser( wikiContext, link ).getInlineImagePatterns();

            for( final Pattern p : inlineImagePatterns ) {
                if( new Perl5Matcher().matches( link, p ) ) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Returns {@code true}, if the link name exists; otherwise it returns {@code false}.
     *
     * @param page link name
     * @return {@code true}, if the link name exists; otherwise it returns {@code false}.
     */
    public boolean linkExists( final String page ) {
        if( page == null || page.length() == 0 ) {
            return false;
        }
        try {
            return wikiContext.getEngine().getFinalPageName( page ) != null;
        } catch( final ProviderException e ) {
            log.warn( "TranslatorReader got a faulty page name [" + page + "]!", e );
            return false;
        }
    }

    /**
     * Returns link name, if it exists; otherwise it returns {@code null}.
     *
     * @param page link name
     * @return link name, if it exists; otherwise it returns {@code null}.
     */
    public String linkIfExists( final String page ) {
        if( page == null || page.length() == 0 ) {
            return null;
        }
        try {
            return wikiContext.getEngine().getFinalPageName( page );
        } catch( final ProviderException e ) {
            log.warn( "TranslatorReader got a faulty page name [" + page + "]!", e );
            return null;
        }
    }

    /**
     * Compares two Strings, and if one starts with the other, then returns 0. Otherwise just like the normal Comparator for strings.
     */
    private static class StartingComparator implements Comparator< String > {

        /**
         * {@inheritDoc}
         *
         * @see Comparator#compare(Object, Object)
         */
        @Override
        public int compare( final String s1, final String s2 ) {
            if( s1.length() > s2.length() ) {
                if( s1.startsWith( s2 ) && s2.length() > 1 ) {
                    return 0;
                }
            } else if( s2.startsWith( s1 ) && s1.length() > 1 ) {
                return 0;
            }

            return s1.compareTo( s2 );
        }

    }

}
