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


/**
 * Simple Value Object to hold all necessary information to perform a migration.  
 */
public class MigrationVO
{
    /** directory holding the repository */
    private String repoDir;
    
    /** encoding of wiki pages; not actually used, but could be useful when developing a custom MigrationManager impl. */
    private String encoding;

    /**
     * getter for {@link #repoDir}.
     * 
     * @return {@link #repoDir}.
     */
    public String getRepoDir()
    {
        return repoDir;
    }

    /**
     * setter for {@link #repoDir}.
     * 
     * @param repoDir the {@link #repoDir} to set.
     * @return the current instance, with it's {@link #repoDir} updated.
     */
    public MigrationVO setRepoDir( String repoDir )
    {
        this.repoDir = repoDir;
        return this;
    }

    /**
     * getter for {@link #encoding}.
     * 
     * @return {@link #encoding}.
     */
    public String getEncoding()
    {
        return encoding;
    }

    /**
     * setter for {@link #encoding}.
     * 
     * @param repoDir the {@link #encoding} to set.
     * @return the current instance, with it's {@link #encoding} updated.
     */
    public MigrationVO setEncoding( String encoding )
    {
        this.encoding = encoding;
        return this;
    }
    
}
