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

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
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

	private static final Logger log = Logger.getLogger( XmlUtil.class );
	private XmlUtil() {}
	
	/**
	 * Parses the given XML file and returns the requested nodes. If there's an error accessing or parsing the file, an
	 * empty list is returned.
	 * 
	 * @param xml file to parse; matches all resources from classpath, filters repeated items.
	 * @param requestedNodes requestd nodes on the xml file
	 * @return the requested nodes of the XML file.
	 */
	public static List<Element> parse( String xml, String requestedNodes )
	{
		if( StringUtils.isNotEmpty( xml ) && StringUtils.isNotEmpty( requestedNodes ) ) {
			Set<Element> readed = new HashSet<Element>();
			SAXBuilder builder = new SAXBuilder();
			try {
				Enumeration< URL > resources = XmlUtil.class.getClassLoader().getResources( xml );
				while( resources.hasMoreElements() ) {
	                URL resource = resources.nextElement();
	                log.debug( "reading " + resource.toString() );
	                Document doc = builder.build( resource );
	                XPathFactory xpfac = XPathFactory.instance();
	                XPathExpression<Element> xp = xpfac.compile( requestedNodes, Filters.element() );
	                readed.addAll( xp.evaluate( doc ) ); // filter out repeated items
	            }
				return new ArrayList<Element>( readed );
			} catch ( IOException ioe ) {
				log.error( "Couldn't load all " + xml + " resources", ioe );
			} catch ( JDOMException jdome ) {
				log.error( "error parsing " + xml + " resources", jdome );
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
	public static List< Element > parse( InputStream xmlStream, String requestedNodes ) {
		if( xmlStream != null && StringUtils.isNotEmpty( requestedNodes ) ) {
			SAXBuilder builder = new SAXBuilder();
			try {
                Document doc = builder.build( xmlStream );
                XPathFactory xpfac = XPathFactory.instance();
                XPathExpression< Element > xp = xpfac.compile( requestedNodes,Filters.element() );
				return xp.evaluate( doc );
			} catch ( IOException ioe ) {
				log.error( "Couldn't load all " + xmlStream + " resources", ioe );
			} catch ( JDOMException jdome ) {
				log.error( "error parsing " + xmlStream + " resources", jdome );
			}
		}		
		return Collections.emptyList();
	}

}