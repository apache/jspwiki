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
package org.apache.wiki.dav;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

import javax.servlet.http.HttpServletResponse;

import org.apache.ecs.XhtmlDocument;
import org.apache.ecs.xhtml.li;
import org.apache.ecs.xhtml.ul;

import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiPage;

/**
 *
 *  @since 
 */
public class DavUtil
{
    public static String getCollectionInHTML( WikiContext context, Collection coll )
    {
        XhtmlDocument doc = new XhtmlDocument("UTF-8");
        
        ul content = new ul();
        
        for( Iterator i = coll.iterator(); i.hasNext(); )
        {
            Object o = i.next();
            
            if( o instanceof WikiPage )
            {
                WikiPage p = (WikiPage)o;
                content.addElement( new li().addElement( p.getName() ) );
            }
            else if( o instanceof String )
            {
                content.addElement( new li().addElement( o.toString() ));
            }
        }

        doc.appendBody( content );
        
        return doc.toString();
    }
    
    public static void sendHTMLResponse( HttpServletResponse res, String txt )
    throws IOException
    {
        res.setContentType( "text/html; charset=UTF-8" );
        res.setContentLength( txt.length() );
        
        res.getWriter().print( txt );
    }

    public static String combineURL( String part1, String part2 )
    {
        if( part1.endsWith("/") )
        {
            if( part2.startsWith("/") )
            {
                part2 = part2.substring(1);
            }
        }
        else
        {
            if( !part2.startsWith("/") )
            {
                return part1+"/"+part2;
            }
        }
        
        return part1+part2;
    }
}
