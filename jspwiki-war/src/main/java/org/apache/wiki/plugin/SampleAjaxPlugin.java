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

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.wiki.WikiContext;
import org.apache.wiki.ajax.WikiAjaxServlet;
import org.apache.wiki.api.exceptions.PluginException;
import org.apache.wiki.api.plugin.WikiPlugin;

/**
 * @since 2.10.2-svn10
 */
public class SampleAjaxPlugin implements WikiPlugin, WikiAjaxServlet {
	
	private static final String SERVLET_MAPPING = "SampleAjaxPlugin";

	@Override
    public String execute(WikiContext context, Map<String, String> params) throws PluginException {
    	String id = Integer.toString(this.hashCode());
        String html= "<div onclick='Wiki.ajaxHtmlCall(\"/"+SERVLET_MAPPING+"/ajaxAction\",[12,45],\"result"+id+"\",\"Loading...\")' style='color: blue; cursor: pointer'>Press Me</div>\n"+
                        "<div id='result"+id+"'></div>";
        return html;
    }

	@Override
	public String getServletMapping() {
		return SERVLET_MAPPING;
	}
	
	@Override
	public void service(HttpServletRequest request, HttpServletResponse response, String actionName, List<String> params) throws ServletException, IOException {
        try {
            Thread.sleep(5000); // Wait 5 seconds
        } catch (Exception e) {}
        response.getWriter().print("You called! actionName="+actionName+" params="+params);		
	}



}
