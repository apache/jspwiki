/*
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright 2008 The Apache Software Foundation 
    
    Licensed under the Apache License, Version 2.0 (the "License"); 
    you may not use this file except in compliance with the License. 
    You may obtain a copy of the License at 
    
      http://www.apache.org/licenses/LICENSE-2.0 
      
    Unless required by applicable law or agreed to in writing, software 
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
    See the License for the specific language governing permissions and 
    limitations under the License.    
 */
package com.ecyrd.jspwiki;

import java.util.Properties;
import java.io.IOException;

/**
 *  A generic Wiki provider for all sorts of things that the Wiki can
 *  store.
 *
 *  @since 2.0
 */
public interface WikiProvider
{
    /**
     *  Passing this to any method should get the latest version
     */
    public static final int LATEST_VERSION = -1;

    /**
     *  Initializes the page provider.
     *  
     *  @param engine WikiEngine to own this provider
     *  @param properties A set of properties used to initialize this provider
     *  @throws NoRequiredPropertyException If the provider needs a property which is not found in the property set
     *  @throws IOException If there is an IO problem
     */
    public void initialize( WikiEngine engine, Properties properties ) 
        throws NoRequiredPropertyException,
               IOException;

    /**
     *  Return a valid HTML string for information.  May
     *  be anything.
     *  @since 1.6.4
     *  @return A string describing the provider.
     */

    public String getProviderInfo();
}


