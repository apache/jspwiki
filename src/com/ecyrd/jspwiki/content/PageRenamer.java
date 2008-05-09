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
package com.ecyrd.jspwiki.content;

import com.ecyrd.jspwiki.*;
import com.ecyrd.jspwiki.parser.MarkupParser;

/**
 *  Provides page renaming functionality.  Note that there used to be
 *  a similarly named class in 2.6, but due to unclear copyright, the
 *  class was completely rewritten from scratch for 2.8.
 *
 *  @since 2.8
 */
public class PageRenamer
{

    /**
     *  Renames a page.
     *  
     *  @param context The current context.
     *  @param renameFrom The name from which to rename.
     *  @param renameTo The new name.
     *  @param changeReferrers If true, also changes all the referrers.
     *  @return The final new name (in case it had to be modified)
     *  @throws WikiException If the page cannot be renamed.
     */
    public String renamePage( WikiContext context, 
                              String renameFrom, 
                              String renameTo, 
                              boolean changeReferrers )
        throws WikiException
    {
        //
        //  Sanity checks first
        //
        if( renameFrom == null || renameFrom.length() == 0 )
        {
            throw new WikiException( "From name may not be null or empty" );
        }
        if( renameTo == null || renameTo.length() == 0 )
        {
            throw new WikiException( "To name may not be null or empty" );
        }
        
        if( renameTo.equals(renameFrom) )
        {
            throw new WikiException( "You cannot rename the page to itself" );
        }
       
        //
        //  Clean up the "to" -name so that it does not contain anything illegal
        //
        
        renameTo = MarkupParser.cleanLink( renameTo.trim() );
        
        //
        //  Preconditions: "from" page must exist, and "to" page must not yet exist.
        //
        WikiEngine engine = context.getEngine();
        WikiPage fromPage = engine.getPage( renameFrom );
        
        if( fromPage == null )
        {
            throw new WikiException("No such page "+renameFrom);
        }
        
        WikiPage toPage = engine.getPage( renameTo );
        
        if( toPage != null )
        {
            throw new WikiException("Page already exists "+renameTo);
        }
        
        //  FIXME: Temporary
        if( changeReferrers ) throw new WikiException("Referrer change not yet supported.");
        
        //
        //  Do the actual rename by changing from the frompage to the topage
        //
        
        engine.getPageManager().getProvider().movePage( renameFrom, renameTo );
        
        engine.getAttachmentManager().getCurrentProvider().moveAttachmentsForPage( renameFrom, renameTo );
        
        toPage = engine.getPage( renameTo );
        
        if( toPage == null ) throw new InternalWikiException("Rename seems to have failed for some strange reason - please check logs!");
        
        engine.getReferenceManager().pageRemoved( fromPage );
        engine.getReferenceManager().updateReferences( renameTo, 
                                                       engine.scanWikiLinks( toPage, engine.getPureText( toPage )) );
        
        //
        //  Done, return the new name.
        //
        return renameTo;
    }

}
