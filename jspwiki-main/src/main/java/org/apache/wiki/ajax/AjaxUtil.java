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
package org.apache.wiki.ajax;

import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

/**
 * Helpful utilities for the Ajax functions.
 * 
 * @since 2.10.2-svn12
 */
public class AjaxUtil extends HttpServlet {
	private static final long serialVersionUID = 3170439306358345408L;
	private static Gson gson = new Gson();

	/**
	 * Uses Google Gson (https://code.google.com/p/google-gson/) to convert to JSON
	 *
	 * @param input the object to be converted to JSON
	 * @return the JSON string of the object
	 */
	public static String toJson( final Object input ) {
		if( input != null ) {
			return gson.toJson( input );
		}
		return "";
	}
	
	/**
	 * Given a requestUri path, find the next uri "fragment" after the "/lastPart/" one.
	 * E.g. given url "/test/abc/travel", and lastPart "abc", this will return "travel". Given lastPart "test" will return "abc".
	 * 
	 * This could be done better using a <a href="http://en.wikipedia.org/wiki/URL_Template">URITemplate</a>
	 * (as <a href="https://tools.ietf.org/html/rfc6570">RFC6570</a>) 
	 * 
	 * @param path the RequestURI to search usually done by calling request.getRequestUri().
	 * @param lastPart the previousPart of the path to search after.
	 * @return the next part of the path.
	 * @throws ServletException if {@code path} does not contain {@code lastPart}
	 */
	public static String getNextPathPart( String path, String lastPart ) throws ServletException {
        if( StringUtils.isBlank( path ) ) {
			return null;
		}
		if( !lastPart.endsWith( "/" ) ) {
			lastPart += "/";
		}
		int index = path.indexOf( lastPart );
		if( index < 0 ) {
			lastPart = lastPart.substring( 0, lastPart.length() - 1 );
			index = path.indexOf( lastPart );
			if( index < 0 ) {
				throw new ServletException( "Invalid path provided " + path + " does not contain '" + lastPart + "'" );
			}
		}
		path = path.substring( index + lastPart.length() );
		index = path.indexOf( "/" );
		if( index == -1 ) {
			index = path.indexOf( "#" );
			if( index == -1 ) {
				index = path.indexOf( "?" );
			}
		}

		final String result;
		if( index == -1 ) {
			result = path;
        } else {
            result = path.substring( 0, index );
        }

        return result;
    }
	
}
