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
package org.apache.wiki.api.plugin;

import org.apache.wiki.WikiEngine;
import org.apache.wiki.api.exceptions.PluginException;

/**
 *  If a plugin defines this interface, it is called exactly once
 *  prior to the actual execute() routine.  If the plugin has its
 *  own declaration in jspwiki_modules.xml, then it is called during
 *  startup - otherwise it is called the first time the plugin is
 *  encountered.
 *  <p>
 *  This method did not actually work until 2.5.30.  The method signature
 *  has been changed in 2.6 to reflect the new operation.
 */
public interface InitializablePlugin
{
    /**
     *  Called whenever the plugin is being instantiated for
     *  the first time.
     *  
     *  @param engine The WikiEngine.
     *  @throws PluginException If something goes wrong.
     */

    void initialize( WikiEngine engine ) throws PluginException;
}
