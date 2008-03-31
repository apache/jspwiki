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

/**
 *  Defines an interface for transforming strings within a Wiki context.
 *
 *  @since 1.6.4
 */
public interface StringTransmutator
{
    /**
     *  Returns a changed String, suitable for Wiki context.
     *  
     *  @param context WikiContext in which mutation is to be done
     *  @param source  The source string.
     *  @return The mutated string.
     */
    public String mutate( WikiContext context, String source );
}
