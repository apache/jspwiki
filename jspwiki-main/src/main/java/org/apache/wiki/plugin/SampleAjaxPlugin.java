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

import org.apache.wiki.ajax.WikiAjaxServlet;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.exceptions.PluginException;
import org.apache.wiki.api.plugin.Plugin;
import org.apache.wiki.util.TextUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @since 2.10.2-svn10
 *
 * updated according to https://issues.apache.org/jira/browse/JSPWIKI-1195
 */

public class SampleAjaxPlugin implements Plugin, WikiAjaxServlet {
	
	private static final String SERVLET_MAPPING = "SampleAjaxPlugin";

	@Override
	public String execute( final Context context, final Map<String, String> params) throws PluginException {
    		var id = Long.toString(System.currentTimeMillis());

		var url = "/" + SERVLET_MAPPING + "/ajaxAction";

		var ajaxParams = jsStringEscape( params.get("params") );

		var js = String.format("$('result%s').value='Loading…';"+
			"var token = Wiki.CsrfProtection;"+
			"new Request.HTML({"+
				"url: Wiki.JsonUrl + '%s', "+
				"update:$('result%s'),"+
				"onSuccess: function(data){console.log('Success',data);},"+
				"onError: function(err){console.log('Error',err);}"+
			"}).post('params=%s&X-XSRF-TOKEN='+token)", id, url, id, ajaxParams);

		return String.format("<div onclick='%s' style='color: blue; cursor: pointer'>Press Me</div><div id='result%s'></div>", js, id);
	}

	/**
	 * Escapes a value for safe embedding inside a single-quoted javascript string that itself
	 * lives inside a single-quoted HTML event-handler attribute: every character other than an
	 * ASCII letter or digit is emitted as a javascript unicode escape, so the result can
	 * terminate neither the enclosing javascript string nor the enclosing attribute.
	 *
	 * @param s the raw parameter value, may be {@code null}.
	 * @return the escaped value, or an empty string if {@code s} was {@code null}.
	 */
	static String jsStringEscape( final String s ) {
		if( s == null ) {
			return "";
		}
		final StringBuilder sb = new StringBuilder( s.length() );
		for( int i = 0; i < s.length(); i++ ) {
			final char c = s.charAt( i );
			if( ( c >= 'a' && c <= 'z' ) || ( c >= 'A' && c <= 'Z' ) || ( c >= '0' && c <= '9' ) ) {
				sb.append( c );
			} else {
				sb.append( String.format( "\\u%04x", ( int )c ) );
			}
		}
		return sb.toString();
	}

	@Override
	public String getServletMapping() {
		return SERVLET_MAPPING;
	}

	@Override
	public void service( final HttpServletRequest request, final HttpServletResponse response, final String actionName, final List< String > params )
			throws ServletException, IOException {
		try {
			Thread.sleep( 5000 ); // Wait 5 seconds
		} catch( final Exception e ) {
		}
		response.getWriter().print( "You called! actionName=" + TextUtil.replaceEntities( actionName )
				+ " params=" + TextUtil.replaceEntities( String.valueOf( params ) ) );
	}

}
