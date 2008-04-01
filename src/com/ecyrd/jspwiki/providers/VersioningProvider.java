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
package com.ecyrd.jspwiki.providers;

/**
 *  This is a provider interface which providers can implement, if they
 *  support fast checks of versions.
 *  <p>
 *  Note that this interface is pretty much a hack to support certain functionality
 *  before a complete refactoring of the complete provider interface.  Please
 *  don't bug me too much about it...
 *  
 *
 *  @since 2.3.29
 */
public interface VersioningProvider
{
    /**
     *  Return true, if page with a particular version exists.
     */

    public boolean pageExists( String page, int version );
}
