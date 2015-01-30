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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.apache.commons.lang.StringUtils;

import com.google.gson.Gson;

public class AjaxUtil extends HttpServlet {
	private static final long serialVersionUID = 3170439306358345408L;
	private static Gson gson = new Gson();
	
	public static String toJson(Object input) {
		String result = "";
		if (input != null) {
			result = gson.toJson(input);
		}
		return result;
	}
	
	public static String getNextPathPart(String path, String lastPart) throws ServletException {
        String result = null;
        if (StringUtils.isBlank(path)) {
            return result;
        }
        if (!lastPart.endsWith("/")) {
        	lastPart += "/";
        }
        int index = path.indexOf(lastPart);
        if (index<0) {
        	lastPart = lastPart.substring(0,lastPart.length()-1);
        	index = path.indexOf(lastPart);
        	if (index<0) {
        		throw new ServletException("Invalid path provided " + path + " does not contain '" + lastPart + "'");
        	}
        }
        path = path.substring(index+lastPart.length());
        index = path.indexOf("/");
        if (index == -1) {
            index = path.indexOf("#");
            if (index == -1) {
                index = path.indexOf("?");
            }
        }
        if (index == -1) {
            result = path;
        }
        else {
            result = path.substring(0,index);
        }

        return result;
    }
	
}
