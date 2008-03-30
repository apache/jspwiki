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

import java.util.ArrayList;
import java.util.Collection;

/**
 *  Just a simple class collecting all of the links
 *  that come in.
 */
public class LinkCollector
    implements StringTransmutator
{
    private ArrayList m_items = new ArrayList();

    /**
     * Returns a List of Strings representing links.
     * @return the link collection
     */
    public Collection getLinks()
    {
        return m_items;
    }

    /**
     * {@inheritDoc}
     */
    public String mutate( WikiContext context, String in )
    {
        m_items.add( in );

        return in;
    }
}

