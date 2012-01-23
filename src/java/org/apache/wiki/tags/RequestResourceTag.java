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
package org.apache.wiki.tags;

/**
 *  <p>In JSPWiki 2.8, this tag provided a way to instruct JSPWiki to insert
 *  resource requests into JSPs. <em>This tag has been deprecated because it
 *  is unsafe.</em> The Stripes layout tags should be used instead. See the default
 *  template {@code layout/DefaultLayout.jsp} for instructions on how
 *  to include scripts and other resources in JSPs.</p>
 *  @deprecated use the Stripes <code>layout-component</code> tags instead
 */
public class RequestResourceTag extends WikiTagBase
{
    private static final long serialVersionUID = 0L;

    /**
     *  {@inheritDoc}
     */
    @Override
    public void initTag()
    {
        super.initTag();
    }
    
    /**
     *  {@inheritDoc}
     */
    @Override
    public int doWikiStartTag() throws Exception
    {   
        return SKIP_BODY;
    }

    /**
     *  Always returns the empty string.
     *  
     *  @return The resource name.
     */
    public String getResource()
    {
        return "";
    }

    /**
     *  This method does nothing.
     *  
     *  @param r Resource identifier.
     */
    public void setResource(String r)
    {
        // No-op.
    }

    /**
     *  Always returns the empty string.
     *  
     *  @return The type of the resource.
     */
    public String getType()
    {
        return "";
    }
    
    /**
     *  This method does nothing.
     * 
     *  @param type The type to be set.
     */

    public void setType(String type)
    {
    }

}
