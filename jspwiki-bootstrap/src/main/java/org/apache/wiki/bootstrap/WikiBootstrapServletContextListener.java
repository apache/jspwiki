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
package org.apache.wiki.bootstrap;

import java.util.Properties;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.wiki.api.spi.Wiki;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WikiBootstrapServletContextListener implements ServletContextListener {

	private static final Logger LOG = LoggerFactory.getLogger(WikiBootstrapServletContextListener.class);

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void contextInitialized(final ServletContextEvent sce) {
	}

	/**
	 * Locate and init JSPWiki SPIs' implementations
	 *
	 * @param sce associated servlet context.
	 * @return JSPWiki configuration properties.
	 */
	Properties initWikiSPIs(final ServletContextEvent sce) {
		return Wiki.init(sce.getServletContext());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void contextDestroyed(final ServletContextEvent sce) {
	}
}
