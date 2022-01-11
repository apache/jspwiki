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
package org.apache.wiki.api.spi;

import org.apache.wiki.api.core.Command;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.core.Page;

import javax.servlet.http.HttpServletRequest;


/**
 * SPI used to locate and provide {@link Context} instances.
 */
public interface ContextSPI {

    /**
     *  Create a new Context for the given Page.
     *
     *  @param engine The Engine that is handling the request.
     *  @param page The Page. If you want to create a Context for an older version of a page, you must use this method.
     */
    Context create( Engine engine, Page page );

    /**
     * <p>Creates a new Context for the given Engine, Command and HttpServletRequest.</p>
     * <p>This constructor will also look up the HttpSession associated with the request, and determine if a Session object is present.
     * If not, a new one is created.</p>
     * 
     * @param engine The Engine that is handling the request
     * @param request The HttpServletRequest that should be associated with this context. This parameter may be <code>null</code>.
     * @param command the command
     */
    Context create( Engine engine, HttpServletRequest request, Command command );

    /**
     * Creates a new Context for the given Engine, Page and HttpServletRequest.
     *
     * @param engine The Engine that is handling the request
     * @param request The HttpServletRequest that should be associated with this context. This parameter may be <code>null</code>.
     * @param page The WikiPage. If you want to create a WikiContext for an older version of a page, you must supply this parameter
     */
    Context create( Engine engine, HttpServletRequest request, Page page );

    /**
     *  Creates a new Context from a supplied HTTP request, using a default wiki context.
     *
     *  @param engine The Engine that is handling the request
     *  @param request the HTTP request
     *  @param requestContext the default context to use
     */
    Context create( Engine engine, HttpServletRequest request, String requestContext );

}
