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
package org.apache.wiki;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * This provides a simple ajax servlet for handling /ajax/<ClassName> requests.
 * Classes need to be registered using {@link WikiAjaxDispatcherServlet.register}
 *
 * @author David VIttor
 * @date 20/01/2015
 * @since 2.10.2-svn10
 */
public class WikiAjaxDispatcherServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static Map<String,HttpServlet> ajaxServlets = new HashMap<String,HttpServlet>();
    static final Logger log = Logger.getLogger(WikiAjaxDispatcherServlet.class.getName());
    private static final String PATH_AJAX = "/ajax/";

    /**
     * {@inheritDoc}
     */
    public void init(ServletConfig config)
            throws ServletException {
        super.init(config);

        log.info("WikiAjaxDispatcherServlet initialized.");
    }

    public static void register(HttpServlet servlet) {
    	log.info("WikiAjaxDispatcherServlet registering "+servlet.getClass().getSimpleName()+"="+servlet);
        ajaxServlets.put(servlet.getClass().getSimpleName(),servlet);
    }

    /**
     * Calls {@link this.performAction}
     */
    public void doPost(HttpServletRequest req, HttpServletResponse res)
            throws IOException, ServletException {
        performAction(req,res);
    }

    /**
     * Calls {@link this.performAction}
     */
    public void doGet(HttpServletRequest req, HttpServletResponse res)
            throws IOException, ServletException {
        performAction(req,res);
    }

    /**
     * The main method which get the requestURI "/ajax/<ServletName>", gets the 
     * {@link this.getServletName} and finds the servlet using {@link this.findServletByName}. 
     * It then calls servlet.service().
     * @param req the inbound request
     * @param res the outbound response
     * @throws IOException
     * @throws ServletException if no registered servlet can be found
     */
    private void performAction(HttpServletRequest req, HttpServletResponse res)
            throws IOException, ServletException {
        String path = req.getRequestURI();
        String servletName = getServletName(path);
        if (servletName!=null) {
            HttpServlet ajaxServlet = findServletByName(servletName);
            if (ajaxServlet != null) {
                ajaxServlet.service(req, res);
            } else {
                log.error("No registered class for servletName=" + servletName + " in path=" + path);
                throw new ServletException("No registered class for servletName=" + servletName);
            }
        }
    }

    /**
     * Get the name of the servlet given the requestURI.
     * @param path The requestURI, which must contains "/ajax/<ServletName>" in the path
     * @return The ServletName for the requestURI, or null
     * @throws ServletException if the path is invalid
     */
    public String getServletName(String path) throws ServletException {
        String result = null;
        if (StringUtils.isBlank(path)) {
            return result;
        }
        int index = path.indexOf(PATH_AJAX);
        if (index<0) {
            throw new ServletException("Invalid path provided " + path + " does not contain '" + PATH_AJAX + "'");
        }
        path = path.substring(index+6);
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

    /**
     * Find the servlet as registered in {@link WikiAjaxDispatcherServlet.register}.
     * 
     * @param servletName the name of the servlet from {@link this.getServletNamee}
     * @return The first servlet found, or null.
     */
    public HttpServlet findServletByName(String servletName) {
    	return ajaxServlets.get(servletName);
    }

}
