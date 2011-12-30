/*
    JSPWiki - a JSP-based WikiWiki clone.

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
package org.apache.wiki.migration;

import org.apache.wiki.WikiEngine;


/**
 * Defines the common operations of a migration task. 
 */
public interface MigrationManager
{
    
    /**
     * Reads all wiki pages from <code>repoDir</code> and saves them through the given <code>engine</code>. 
     * 
     * @param engine current <code>WikiEngine</code>.
     * @param repoDir location of a <code>WikiPage</code>s repository.
     */
    void migrate( WikiEngine engine, MigrationVO repoDir );
    
}
