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
package org.apache.wiki.tags;

import org.apache.wiki.ui.TemplateManager;

/**
 *  Provides easy access to TemplateManager.addResourceRequest().  You may use
 *  any of the request types defined there.
 *
 *  @see TemplateManager
 */
public class RequestResourceTag extends WikiTagBase
{
    private static final long serialVersionUID = 0L;
    
    private String m_type;
    private String m_resource;

    /**
     *  {@inheritDoc}
     */
    @Override
    public void initTag()
    {
        super.initTag();
        m_type = m_resource = null;
    }
    
    /**
     *  {@inheritDoc}
     */
    @Override
    public int doWikiStartTag() throws Exception
    {   
        if( m_type != null && m_resource != null )
        {
            TemplateManager.addResourceRequest( m_wikiContext, m_type, m_resource );
        }

        return SKIP_BODY;
    }

    /**
     *  Returns the resource that is to be added.
     *  
     *  @return The resource name.
     */
    public String getResource()
    {
        return m_resource;
    }

    /**
     *  Sets the resource name to be added.
     *  
     *  @param r Resource identifier.
     */
    public void setResource(String r)
    {
        m_resource = r;
    }

    /**
     *  Get the resource type. 
     *  
     *  @return The type of the resource.
     */
    public String getType()
    {
        return m_type;
    }
    
    /**
     *  Set the type of the resource to be included.  For example, "script".  Please
     *  see the TemplateManager class for more information about the different kinds
     *  of types you can use.
     *  
     *  @see TemplateManager
     * 
     *  @param type The type to be set.
     */

    public void setType(String type)
    {
        m_type = type;
    }

}
