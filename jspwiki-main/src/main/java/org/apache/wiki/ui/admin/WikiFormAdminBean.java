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
package org.apache.wiki.ui.admin;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.wiki.WikiContext;
import org.apache.wiki.parser.WikiDocument;
import org.apache.wiki.render.RenderingManager;

/**
 *  This class is still experimental.
 *
 */
public abstract class WikiFormAdminBean
    implements AdminBean
{
    public abstract String getForm( WikiContext context );
    
    public abstract void handleResponse( WikiContext context, Map< ?, ? > params );

    public String doGet(WikiContext context)
    {
        String result = "";
        
        String wikiMarkup = getForm(context);
        
        RenderingManager mgr = context.getEngine().getRenderingManager();
        
        WikiDocument doc;
        try
        {
            doc = mgr.getParser( context, wikiMarkup ).parse();
            result = mgr.getHTML(context, doc);
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        return result;
    }

    public String handlePost(WikiContext context, HttpServletRequest req, HttpServletResponse resp)
    {
        return null;
        // FIXME: Not yet implemented
    }
}
