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

package org.apache.wiki.modules;

/**
 *  This is a simple interface which is implemented by a number of JSPWiki
 *  components to signal that they should not be included in things like
 *  module listings and so on.  Because JSPWiki reuses its internal module
 *  mechanisms for its own use as well (e.g. RenderingManager uses a PageFilter
 *  to catch page saves), it's sort of dumb to have these all appear in the
 *  "installed filters" list.
 *  <p>
 *  A plugin developer should never implement this interface.
 *  
 *  @since 2.4
 */
public interface InternalModule
{
}
