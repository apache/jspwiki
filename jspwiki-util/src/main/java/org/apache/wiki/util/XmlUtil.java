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

package org.apache.wiki.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Text;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *  Utility class to parse XML files.
 *  <p>
 *  This uses JDOM2 as its backing implementation.
 *  </p>
 *  
 * @since 2.10
 */
public final class XmlUtil  {

	private static final String ALL_TEXT_NODES = "//text()";
	private static final Logger LOG = Logger.getLogger( XmlUtil.class );
	private XmlUtil() {}
	
	/**
	 * Parses the given XML file and returns the requested nodes. If there's an error accessing or parsing the file, an
	 * empty list is returned.
	 * 
	 * @param xml file to parse; matches all resources from classpath, filters repeated items.
	 * @param requestedNodes requested nodes on the xml file
	 * @return the requested nodes of the XML file.
	 */
	public static List<Element> parse( final String xml, final String requestedNodes )
	{
		if( StringUtils.isNotEmpty( xml ) && StringUtils.isNotEmpty( requestedNodes ) ) {
			final Set<Element> readed = new HashSet<>();
			final SAXBuilder builder = new SAXBuilder();
			try {
				final Enumeration< URL > resources = XmlUtil.class.getClassLoader().getResources( xml );
				while( resources.hasMoreElements() ) {
					final URL resource = resources.nextElement();
					LOG.debug( "reading " + resource.toString() );
					final Document doc = builder.build( resource );
					final XPathFactory xpfac = XPathFactory.instance();
					final XPathExpression<Element> xp = xpfac.compile( requestedNodes, Filters.element() );
	                readed.addAll( xp.evaluate( doc ) ); // filter out repeated items
	            }
				return new ArrayList<>( readed );
			} catch( final IOException ioe ) {
				LOG.error( "Couldn't load all " + xml + " resources", ioe );
			} catch( final JDOMException jdome ) {
				LOG.error( "error parsing " + xml + " resources", jdome );
			}
		}
		return Collections.emptyList();
	}
	
	/**
	 * Parses the given stream and returns the requested nodes. If there's an error accessing or parsing the stream, an
	 * empty list is returned.
	 * 
	 * @param xmlStream stream to parse.
	 * @param requestedNodes requestd nodes on the xml stream.
	 * @return the requested nodes of the XML stream.
	 */
	public static List< Element > parse( final InputStream xmlStream, final String requestedNodes ) {
		if( xmlStream != null && StringUtils.isNotEmpty( requestedNodes ) ) {
			final SAXBuilder builder = new SAXBuilder();
			try {
				final Document doc = builder.build( xmlStream );
				final XPathFactory xpfac = XPathFactory.instance();
				final XPathExpression< Element > xp = xpfac.compile( requestedNodes,Filters.element() );
				return xp.evaluate( doc );
			} catch( final IOException ioe ) {
				LOG.error( "Couldn't load all " + xmlStream + " resources", ioe );
			} catch( final JDOMException jdome ) {
				LOG.error( "error parsing " + xmlStream + " resources", jdome );
			}
		}		
		return Collections.emptyList();
	}

	/**
	 * Renders all the text() nodes from the DOM tree. This is very useful for cleaning away all of the XHTML.
	 *
	 * @param doc Dom tree
	 * @return String containing only the text from the provided Dom tree.
	 */
	public static String extractTextFromDocument( final Document doc ) {
		if( doc == null ) {
			return "";
		}
		final StringBuilder sb = new StringBuilder();
		final List< ? > nodes = XPathFactory.instance().compile( ALL_TEXT_NODES ).evaluate( doc );
		for( final Object el : nodes ) {
			if( el instanceof Text ) {
				sb.append( ( ( Text )el ).getValue() );
			}
		}

		return sb.toString();
	}

}