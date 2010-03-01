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
 *  <p>This tag is used to include any programmatic includes into the
 *  output stream. In JSPWiki 2.8 and earlier, this tag emitted a
 *  tiny marker into the stream which was replaced by ServletFilter.
 *  <em>This tag has been deprecated because it is unsafe.</em> The
 *  Stripes layout tags should be used instead. See the default
 *  template {@code layout/DefaultLayout.jsp} for instructions on how
 *  to include scripts and other resources in JSPs.</p>
 *  @deprecated use the Stripes <code>layout-component</code> tags instead
 */
public class IncludeResourcesTag extends WikiTagBase
{
    private static final long serialVersionUID = 0L;
        
    public void initTag()
    {
        super.initTag();
    }
    
    public void setType( String type )
    {
    }
    
    public int doWikiStartTag() throws Exception
    {
        pageContext.getOut().println( "<!-- Please use the Stripes layout tags instead " +
        		"of IncludeResourcesTag. See layout/DefaultLayout.jsp for instructions. -->" );
        return SKIP_BODY;
    }

}
