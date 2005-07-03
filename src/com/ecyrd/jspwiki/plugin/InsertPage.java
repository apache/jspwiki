/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2002 Janne Jalkanen (Janne.Jalkanen@iki.fi)

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation; either version 2.1 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.ecyrd.jspwiki.plugin;

import org.apache.log4j.Logger;
import com.ecyrd.jspwiki.*;
import com.ecyrd.jspwiki.auth.*;
import com.ecyrd.jspwiki.auth.permissions.PagePermission;
import java.util.*;

/**
 *  Inserts page contents.  Muchos thanks to Scott Hurlbert for the initial code.
 *
 *  @since 2.1.37
 *  @author Scott Hurlbert
 *  @author Janne Jalkanen
 */
public class InsertPage
    implements WikiPlugin
{
    private static Logger log = Logger.getLogger( InsertPage.class );

    public static final String PARAM_PAGENAME  = "page";
    public static final String PARAM_STYLE     = "style";
    public static final String PARAM_MAXLENGTH = "maxlength";
    public static final String PARAM_CLASS     = "class";
    public static final String PARAM_SECTION   = "section";
    public static final String PARAM_DEFAULT   = "default";

    private static final String DEFAULT_STYLE = "";

    public static final String ATTR_RECURSE    = "com.ecyrd.jspwiki.plugin.InsertPage.recurseCheck";
    
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
                AuthorizationManager mgr = engine.getAuthorizationManager();

                if( !mgr.checkPermission( context,
                                          new PagePermission( page, "view") ) )
                {
                    res.append("<span class=\"error\">You do not have permission to view this included page.</span>");
                    return res.toString();
                }
                   
                //
                //  Check for recursivity
                //
                
                List previousIncludes = (List)context.getVariable( ATTR_RECURSE );
                
                if( previousIncludes != null )
                {
                    if( previousIncludes.contains( page.getName() ) )
                    {
                        return "<span class=\"error\">Error: Circular reference - you can't include a page in itself!";
                    }
                }
                else
                {
                    previousIncludes = new ArrayList();
                }
               
                previousIncludes.add( page.getName() );
                context.setVariable( ATTR_RECURSE, previousIncludes );
                

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
