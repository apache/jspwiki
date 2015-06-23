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
package org.apache.wiki.auth.acl;

import java.util.Properties;

import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiPage;
import org.apache.wiki.auth.WikiSecurityException;

/**
 *  Specifies how to parse and return ACLs from wiki pages.
 *  @since 2.3
 */
public interface AclManager
{

    /**
     * Initializes the AclManager with a supplied wiki engine and properties.
     * @param engine the wiki engine
     * @param props the initialization properties
     */
    void initialize( WikiEngine engine, Properties props );

    /**
     * A helper method for parsing textual AccessControlLists. The line is in
     * form "(ALLOW) <permission><principal>, <principal>, <principal>". This
     * method was moved from Authorizer.
     * @param page The current wiki page. If the page already has an ACL, it
     *            will be used as a basis for this ACL in order to avoid the
     *            creation of a new one.
     * @param ruleLine The rule line, as described above.
     * @return A valid Access Control List. May be empty.
     * @throws WikiSecurityException if the ruleLine was faulty somehow.
     * @since 2.1.121
     */
    Acl parseAcl( WikiPage page, String ruleLine ) throws WikiSecurityException;

    /**
     * Returns the access control list for the page.
     * If the ACL has not been parsed yet, it is done
     * on-the-fly. If the page has a parent page, then that is tried also.
     * This method was moved from Authorizer;
     * it was consolidated with some code from AuthorizationManager.
     * @param page the wiki page
     * @since 2.2.121
     * @return the Acl representing permissions for the page
     */
    Acl getPermissions( WikiPage page );

    /**
     * Sets the access control list for the page and persists it.
     * @param page the wiki page
     * @param acl the access control list
     * @since 2.5
     * @throws WikiSecurityException if the ACL cannot be set or persisted
     */
    void setPermissions( WikiPage page, Acl acl ) throws WikiSecurityException;
}
