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
package org.apache.wiki.xmlrpc;

import org.apache.wiki.api.core.Context;


/**
 *  Any wiki RPC handler should implement this so that they can be properly initialized and recognized by JSPWiki.
 *
 *  @since 2.1.7
 */
// FIXME3.0: This class is fast becoming obsolete.  It should be moved to the "rpc" package in 3.0
public interface WikiRPCHandler {

    void initialize( Context context );

}
