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

import org.apache.wiki.WikiPage;
import org.apache.wiki.attachment.Attachment;

/**
 *  Writes a link to a parent of a Wiki page.
 *
 *  <P><B>Attributes</B></P>
 *  <UL>
 *    <LI>page - Page name to refer to.  Default is the current page.
 *    <LI>format - either "anchor" or "url" to output either an <A>... or just the HREF part of one.
 *  </UL>
 *
 *  @since 2.0
 */
public class LinkToParentTag
    extends LinkToTag
{
    private static final long serialVersionUID = 0L;
    
    public int doWikiStartTag()
        throws IOException
    {
        WikiPage p = m_wikiContext.getPage();

        //
        //  We just simply set the page to be our parent page
        //  and call the superclass.
        //
        if( p instanceof Attachment )
        {
            setPage( ((Attachment)p).getParentName() );
        }
        else
        {
            String name = p.getName();

            int entrystart = name.indexOf("_blogentry_");

            if( entrystart != -1 )
            {
                setPage( name.substring( 0, entrystart ) );
            }

            int commentstart = name.indexOf("_comments_");
                
            if( commentstart != -1 )
            {
                setPage( name.substring( 0, commentstart ) );
            }
        }

        return super.doWikiStartTag();
    }

}
