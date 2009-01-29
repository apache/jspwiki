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
package com.ecyrd.jspwiki.ui.admin;

import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.ui.GenericHTTPHandler;

/**
 *  Describes an administrative bean.
 *  
 *  @since  2.5.52
 */
public interface AdminBean
    extends GenericHTTPHandler
{
    public static final int UNKNOWN = 0;
    public static final int CORE    = 1;
    public static final int EDITOR  = 2;
    
    public void initialize( WikiEngine engine );
    
    /**
     *  Return a human-readable title for this AdminBean.
     *  
     *  @return the bean's title
     */
    public String getTitle();
    
    /**
     *  Returns a type (UNKNOWN, EDITOR, etc).
     *  
     *  @return the bean's type
     */
    public int getType();
}
