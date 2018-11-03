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

import java.io.IOException;
import java.util.Collection;
import javax.servlet.jsp.PageContext;


/**
 *  Includes the body content, if there are any search results.
 *
 *  @since 2.0
 */
public class SearchResultsTag extends WikiTagBase {
    private static final long serialVersionUID = 0L;
    
    public final int doWikiStartTag() throws IOException {
        Collection< ? > list = ( Collection< ? > )pageContext.getAttribute( "searchresults", PageContext.REQUEST_SCOPE );
        
        if( list != null ) {
            return EVAL_BODY_INCLUDE;
        }

        String message = (String)pageContext.getAttribute( "err", PageContext.REQUEST_SCOPE );
            
        if( message != null ) {
            pageContext.getOut().print("<div class='error'>");
            pageContext.getOut().print( message );
            pageContext.getOut().println("</div>");
        }
        
        return SKIP_BODY;
    }

}
