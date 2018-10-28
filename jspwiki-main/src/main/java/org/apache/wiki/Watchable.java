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
package org.apache.wiki;


/**
 *  A watchdog needs something to watch.  If you wish to be watched, implement this interface.
 */
public interface Watchable {

	/**
     *  This is a callback which is called whenever your expected completion time is exceeded.  The current state of the
     *  stack is available.
     *
     *  @param state The state in which your Watchable is currently.
     */
    void timeoutExceeded( String state );

    /**
     *  Returns a human-readable name of this Watchable.  Used in logging.
     *
     *  @return The name of the Watchable.
     */
    String getName();

    /**
     *  Returns <code>true</code>, if this Watchable is still alive and can be watched; otherwise <code>false</code>. 
     *  For example, a stopped Thread is not very interesting to watch.
     *
     *  @return the result
     */
    boolean isAlive();

}
