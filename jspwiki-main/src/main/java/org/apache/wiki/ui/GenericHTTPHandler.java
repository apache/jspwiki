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
package org.apache.wiki.ui;

import org.apache.wiki.api.core.Context;


/**
 *  Provides a generic HTTP handler interface.
 *
 */
public interface GenericHTTPHandler {
    
    /**
     *  Get an identifier for this particular AdminBean. This id MUST conform to URI rules. The ID must also be unique across all HTTPHandlers.
     *  
     *  @return the identifier for the bean
     */
    String getId();
    
    /**
     *  Return basic HTML.
     *  
     *  @param context associated WikiContext
     *  @return the HTML for the bean
     */
    String doGet( Context context );
    
    /**
     *  Handles a POST response.
     *  @param context associated WikiContext
     *  @return the response string resulting from the POST
     */
    String doPost( Context context );

}
