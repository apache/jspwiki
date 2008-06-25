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
package com.ecyrd.jspwiki.plugin;

import com.ecyrd.jspwiki.*;
import com.ecyrd.jspwiki.auth.*;
import com.ecyrd.jspwiki.auth.permissions.PermissionFactory;

import java.util.*;

/**
 *  Inserts page contents.  Muchos thanks to Scott Hurlbert for the initial code.
 *
 *  @since 2.1.37
 *  @author Scott Hurlbert
 */
public class InsertPage
    implements WikiPlugin
{
    private static final String PARAM_PAGENAME  = "page";
    private static final String PARAM_STYLE     = "style";
    private static final String PARAM_MAXLENGTH = "maxlength";
    private static final String PARAM_CLASS     = "class";
    private static final String PARAM_SECTION   = "section";
    private static final String PARAM_DEFAULT   = "default";

    private static final String DEFAULT_STYLE = "";

    /** This attribute is stashed in the WikiContext to make sure that we don't
     *  have circular references.
     */
    public static final String ATTR_RECURSE    = "com.ecyrd.jspwiki.plugin.InsertPage.recurseCheck";
    
    /**
     *  {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public String execute( WikiContext context, Map params )
        throws PluginException
    {
        WikiEngine engine = context.getEngine();

        StringBuffer res = new StringBuffer();

        String clazz        = (String) params.get( PARAM_CLASS );
        String includedPage = (String) params.get( PARAM_PAGENAME );
        String style        = (String) params.get( PARAM_STYLE );
        String defaultstr   = (String) params.get( PARAM_DEFAULT );
        int    section      = TextUtil.parseIntParameter((String) params.get( PARAM_SECTION ), 
                                                         -1 );
        int    maxlen       = TextUtil.parseIntParameter((String) params.get( PARAM_MAXLENGTH ),
                                                         -1 );

        if( style == null ) style = DEFAULT_STYLE;

        if( maxlen == -1 ) maxlen = Integer.MAX_VALUE;

        if( includedPage != null )
        {
            WikiPage page = engine.getPage( includedPage );

            
            if( page != null )
            {
                //
                //  Check for recursivity
                //
                
                List<String> previousIncludes = (List)context.getVariable( ATTR_RECURSE );
                
                if( previousIncludes != null )
                {
                    if( previousIncludes.contains( page.getName() ) )
                    {
                        return "<span class=\"error\">Error: Circular reference - you can't include a page in itself!</span>";
                    }
                }
                else
                {
                    previousIncludes = new ArrayList<String>();
                }
               
                previousIncludes.add( page.getName() );
                context.setVariable( ATTR_RECURSE, previousIncludes );
                
                //
                // Check for permissions
                //
                AuthorizationManager mgr = engine.getAuthorizationManager();

                if( !mgr.checkPermission( context.getWikiSession(),
                                          PermissionFactory.getPagePermission( page, "view") ) )
                {
                    res.append("<span class=\"error\">You do not have permission to view this included page.</span>");
                    return res.toString();
                }

                /**
                 *  We want inclusion to occur within the context of
                 *  its own page, because we need the links to be correct.
                 */
                
                WikiContext includedContext = (WikiContext) context.clone();
                includedContext.setPage( page );

                String pageData = engine.getPureText( page );
                String moreLink = "";

                if( section != -1 )
                {
                    try
                    {
                        pageData = TextUtil.getSection( pageData, section );
                    }
                    catch( IllegalArgumentException e )
                    {
                        throw new PluginException( e.getMessage() );
                    }
                }

                if( pageData.length() > maxlen ) 
                {
                    pageData = pageData.substring( 0, maxlen )+" ...";
                    moreLink = "<p><a href=\""+context.getURL(WikiContext.VIEW,includedPage)+"\">More...</a></p>";
                }

                res.append("<div style=\""+style+"\""+(clazz != null ? " class=\""+clazz+"\"" : "")+">");
                res.append( engine.textToHTML( includedContext, pageData ) );
                res.append( moreLink );
                res.append("</div>");
                
                //
                //  Remove the name from the stack; we're now done with this.
                //
                previousIncludes.remove( page.getName() );
                context.setVariable( ATTR_RECURSE, previousIncludes );
            }
            else
            {
                if( defaultstr != null ) 
                {
                    res.append( defaultstr );
                }
                else
                {
                    res.append("There is no page called '"+includedPage+"'.  Would you like to ");
                    res.append("<a href=\""+context.getURL( WikiContext.EDIT, includedPage )+"\">create it?</a>");
                }
            }
        }
        else
        {
            res.append("<span class=\"error\">");
            res.append("You have to define a page!");
            res.append("</span>");
        }

        return res.toString();
    }

}
