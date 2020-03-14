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

import org.apache.log4j.Logger;
import org.apache.wiki.WikiContext;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.exceptions.PluginException;
import org.apache.wiki.api.exceptions.ProviderException;
import org.apache.wiki.api.plugin.Plugin;
import org.apache.wiki.pages.PageManager;
import org.apache.wiki.references.ReferenceManager;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 *  A Plugin that creates an index of pages according to a certain pattern.
 *  <br />
 *  The default is to include all pages.
 *  <p>
 *  This is a rewrite of the earlier JSPWiki IndexPlugin using JDOM2.
 8  </p>
 *  <p>
 *  Parameters (from AbstractReferralPlugin):
 *  </p>
 *  <ul>
 *    <li><b>include</b> - A regexp pattern for marking which pages should be included.</li>
 *    <li><b>exclude</b> - A regexp pattern for marking which pages should be excluded.</li>
 *  </ul>
 *  
 * @author Ichiro Furusato
 */
public class IndexPlugin extends AbstractReferralPlugin implements Plugin {

    private static final Logger log = Logger.getLogger(IndexPlugin.class);

    private Namespace xmlns_XHTML = Namespace.getNamespace("http://www.w3.org/1999/xhtml");
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String execute( final Context context, final Map<String,String> params ) throws PluginException {
        final String include = params.get(PARAM_INCLUDE);
        final String exclude = params.get(PARAM_EXCLUDE);
        
        final Element masterDiv = getElement("div","index");
        final Element indexDiv = getElement("div","header");
        masterDiv.addContent(indexDiv);
        try {
            final List<String> pages = listPages(context,include,exclude);
            context.getEngine().getManager( PageManager.class ).getPageSorter().sort(pages);
            char initialChar = ' ';
            Element currentDiv = new Element("div",xmlns_XHTML);            
            for ( final String name : pages ) {
                if ( name.charAt(0) != initialChar ) {
                    if ( initialChar != ' ' ) {
                        indexDiv.addContent(" - ");
                    }                    
                    initialChar = name.charAt(0);
                    masterDiv.addContent(makeHeader(String.valueOf(initialChar)));
                    currentDiv = getElement("div","body");
                    masterDiv.addContent(currentDiv);
                    indexDiv.addContent(getLink("#"+initialChar,String.valueOf(initialChar)));
                } else {
                    currentDiv.addContent(", ");
                }
                currentDiv.addContent(getLink(context.getURL(WikiContext.VIEW,name),name));
            }
            
        } catch( final ProviderException e ) {
            log.warn("could not load page index",e);
            throw new PluginException( e.getMessage() );
        }
        // serialize to raw format string (no changes to whitespace)
        final XMLOutputter out = new XMLOutputter(Format.getRawFormat());
        return out.outputString(masterDiv);
    }

    private Element getLink( final String href, final String content ) {
        final Element a = new Element( "a", xmlns_XHTML );
        a.setAttribute( "href", href );
        a.addContent( content );
        return a;
    }

    private Element makeHeader( final String initialChar ) {
        final Element span = getElement( "span", "section" );
        final Element a = new Element( "a", xmlns_XHTML );
        a.setAttribute( "id", initialChar );
        a.addContent( initialChar );
        span.addContent( a );
        return span;
    }

    private Element getElement( final String gi, final String classValue ) {
        final Element elt = new Element( gi, xmlns_XHTML );
        elt.setAttribute( "class", classValue );
        return elt;
    }

    /**
     *  Grabs a list of all pages and filters them according to the include/exclude patterns.
     *  
     * @param context
     * @param include
     * @param exclude
     * @return A list containing page names which matched the filters.
     * @throws ProviderException
     */
    private List<String> listPages( final Context context, final String include, final String exclude ) throws ProviderException {
        final Pattern includePtrn = include != null ? Pattern.compile( include ) : Pattern.compile(".*");
        final Pattern excludePtrn = exclude != null ? Pattern.compile( exclude ) : Pattern.compile("\\p{Cntrl}"); // there are no control characters in page names
        final List< String > result = new ArrayList<>();
        final Set< String > pages = context.getEngine().getManager( ReferenceManager.class ).findCreated();
        for( final String pageName : pages ) {
            if( excludePtrn.matcher( pageName ).matches() ) {
                continue;
            }
            if( includePtrn.matcher( pageName ).matches() ) {
                result.add( pageName );
            }
        }
        return result;
    }

}