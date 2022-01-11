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

import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.core.Session;

import javax.servlet.http.HttpServletRequest;


/**
 * SPI used to locate and provide {@link Session} instances.
 */
public interface SessionSPI {

    /**
     * Removes the wiki session associated with the user's HTTP request from the cache of wiki sessions, typically as part of a logout process.
     *
     * @param engine the wiki engine
     * @param request the users's HTTP request
     */
    void remove( Engine engine, HttpServletRequest request );

    /**
     * <p>Returns the Session object associated with the current HTTP request. If not found, one is created.
     * This method is guaranteed to always return a Session, although the authentication status is unpredictable until the user
     * attempts to log in. If the servlet request parameter is <code>null</code>, a synthetic {@link #guest(Engine)} is
     * returned.</p>
     * <p>When a session is created, this method attaches a WikiEventListener to the GroupManager, UserManager and AuthenticationManager,
     * so that changes to users, groups, logins, etc. are detected automatically.</p>
     *
     * @param engine the engine
     * @param request the servlet request object
     * @return the existing (or newly created) session
     */
    Session find( Engine engine, HttpServletRequest request );

    /**
     * Creates a new "guest" session containing a single user Principal {@code org.apache.wiki.auth.WikiPrincipal#GUEST}, plus the role
     * principals {@code Role#ALL} and {@code Role#ANONYMOUS}. This method also adds the session as a listener for GroupManager,
     * AuthenticationManager and UserManager events.
     *
     * @param engine the wiki engine
     * @return the guest wiki session
     */
    Session guest( Engine engine );

}
