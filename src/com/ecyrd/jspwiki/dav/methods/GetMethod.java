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
package com.ecyrd.jspwiki.dav.methods;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ecyrd.jspwiki.FileUtil;
import com.ecyrd.jspwiki.dav.DavPath;
import com.ecyrd.jspwiki.dav.DavProvider;
import com.ecyrd.jspwiki.dav.items.DavItem;

/**
 *
 *  @since 
 */
public class GetMethod extends DavMethod
{

    /**
     * 
     */
    public GetMethod( DavProvider provider )
    {
        super( provider );
    }
    
    public void execute( HttpServletRequest req, HttpServletResponse res, DavPath dp )
        throws IOException
    {        
        DavItem di = m_provider.getItem( dp );
        
        if( di != null )
        {
            String mime = di.getContentType();
            res.setContentType( mime );
        
            long length = di.getLength();
        
            if( length >= 0 )
            {
                res.setContentLength( (int)di.getLength() );
            }
        
            InputStream in = di.getInputStream();
            
            if( in != null )
            {
                FileUtil.copyContents( in, res.getOutputStream() );
                
                in.close();
            }
            else
            {
                res.sendError( HttpServletResponse.SC_NO_CONTENT ); // FIXME: probably not correct
            }
            
        }
        else
        {
            res.sendError( HttpServletResponse.SC_NOT_FOUND );
        }
    }
}
