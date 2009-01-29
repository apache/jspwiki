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
package com.ecyrd.jspwiki.tags;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.tagext.BodyTagSupport;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.action.WikiContextFactory;
import com.ecyrd.jspwiki.log.Logger;
import com.ecyrd.jspwiki.log.LoggerFactory;

/**
 *  Converts the body text into HTML content.
 *
 *  @since 2.0
 */
public class TranslateTag
    extends BodyTagSupport
{
    private static final long serialVersionUID = 0L;
    
    static    Logger    log = LoggerFactory.getLogger( TranslateTag.class );

    public final int doAfterBody()
        throws JspException
    {
        try
        {
            WikiContext context = WikiContextFactory.findContext( pageContext );

            //
            //  Because the TranslateTag should not affect any of the real page attributes
            //  we have to make a clone here.
            //
            
            context = context.deepClone();
            
            //
            //  Get the page data.
            //
            BodyContent bc = getBodyContent();
            String wikiText = bc.getString();
            bc.clearBody();

            if( wikiText != null )
            {
                wikiText = wikiText.trim();
            
                String result = context.getEngine().textToHTML( context, wikiText );

                getPreviousOut().write( result );
            }
        }
        catch( Exception e )
        {
            log.error( "Tag failed", e );
            throw new JspException( "Tag failed, check logs: "+e.getMessage() );
        }

        return SKIP_BODY;
    }
}
