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
package org.apache.wiki.spi;

import org.apache.wiki.WikiContext;
import org.apache.wiki.api.core.Command;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.core.Page;
import org.apache.wiki.api.spi.ContextSPI;

import javax.servlet.http.HttpServletRequest;


/**
 * Default implementation for {@link ContextSPI}
 *
 * @see ContextSPI
 */
public class ContextSPIDefaultImpl implements ContextSPI {

    /**
     * {@inheritDoc}
     */
    @Override
    public Context create( final Engine engine, final Page page ) {
        return new WikiContext( engine, page );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Context create( final Engine engine, final HttpServletRequest request, final Command command ) {
        return new WikiContext( engine, request, command );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Context create( final Engine engine, final HttpServletRequest request, final Page page ) {
        return new WikiContext( engine, request, page );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Context create( final Engine engine, final HttpServletRequest request, final String requestContext ) {
        return new WikiContext( engine, request, requestContext );
    }

}
