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
package org.apache.wiki.url;

import java.io.UnsupportedEncodingException;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

import org.apache.wiki.InternalWikiException;
import org.apache.wiki.TextUtil;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;


/**
 *  Provides a way to do short URLs of the form /wiki/PageName.
 *
 *
 *  @since 2.2
 */
public class ShortURLConstructor
    extends DefaultURLConstructor
{
    private static final String DEFAULT_PREFIX = "wiki/";

    static Logger log = Logger.getLogger( ShortURLConstructor.class );
    
    /**
     *  Contains the path part after the JSPWiki base URL
     */
    protected String m_urlPrefix = "";
    
    /**
     *  This corresponds to your WikiServlet path.  By default, it is assumed to
     *  be "wiki/", but you can set it to whatever you like - including an empty
     *  name.
     */
    public static final String PROP_PREFIX = "jspwiki.shortURLConstructor.prefix";
    
    /** {@inheritDoc} */
    public void initialize( WikiEngine engine, 
                            Properties properties )
    {
        super.initialize( engine, properties );
        
        m_urlPrefix = TextUtil.getStringProperty( properties, PROP_PREFIX, null );
        
        if( m_urlPrefix == null )
        {
            m_urlPrefix = DEFAULT_PREFIX;
        }

        log.info("Short URL prefix path="+m_urlPrefix+" (You can use "+PROP_PREFIX+" to override this)");
    }

    /**
     *  Constructs the actual URL based on the context.
     */
    private String makeURL( String context,
                            String name,
                            boolean absolute )
    {
        String viewurl = "%p"+m_urlPrefix+"%n";

        if( absolute ) 
            viewurl = "%u"+m_urlPrefix+"%n";

        if( context.equals(WikiContext.VIEW) )
        {
            if( name == null ) return doReplacement("%u","",absolute);
            return doReplacement( viewurl, name, absolute );
        }
        else if( context.equals(WikiContext.PREVIEW) )
        {
            if( name == null ) return doReplacement("%u","",absolute);
            return doReplacement( viewurl+"?do=Preview", name, absolute);
        }
        else if( context.equals(WikiContext.EDIT) )
        {
            return doReplacement( viewurl+"?do=Edit", name, absolute );
        }
        else if( context.equals(WikiContext.ATTACH) )
        {
            return doReplacement( "%uattach/%n", name, absolute );
        }
        else if( context.equals(WikiContext.INFO) )
        {
            return doReplacement( viewurl+"?do=PageInfo", name, absolute );
        }
        else if( context.equals(WikiContext.DIFF) )
        {
            return doReplacement( viewurl+"?do=Diff", name, absolute );
        }
        else if( context.equals(WikiContext.NONE) )
        {
            return doReplacement( "%u%n", name, absolute );
        }
        else if( context.equals(WikiContext.UPLOAD) )
        {
            return doReplacement( viewurl+"?do=Upload", name, absolute ); 
        }
        else if( context.equals(WikiContext.COMMENT) )
        {
            return doReplacement( viewurl+"?do=Comment", name, absolute ); 
        }
        else if( context.equals(WikiContext.LOGIN) )
        {
            String loginUrl = absolute ? "%uLogin.jsp?redirect=%n" : "%pLogin.jsp?redirect=%n";
            return doReplacement( loginUrl, name, absolute ); 
        }
        else if( context.equals(WikiContext.DELETE) )
        {
            return doReplacement( viewurl+"?do=Delete", name, absolute ); 
        }
        else if( context.equals(WikiContext.CONFLICT) )
        {
            return doReplacement( viewurl+"?do=PageModified", name, absolute ); 
        }
        else if( context.equals(WikiContext.PREFS) )
        {
            return doReplacement( viewurl+"?do=UserPreferences", name, absolute ); 
        }
        else if( context.equals(WikiContext.FIND) )
        {
            return doReplacement( viewurl+"?do=Search", name, absolute ); 
        }
        else if( context.equals(WikiContext.ERROR) )
        {
            return doReplacement( "%uError.jsp", name, absolute );
        }
        else if( context.equals(WikiContext.CREATE_GROUP) )
        {
            return doReplacement( viewurl+"?do=NewGroup", name, absolute );
        }
        else if( context.equals(WikiContext.DELETE_GROUP) )
        {
            return doReplacement( viewurl+"?do=DeleteGroup", name, absolute );
        }        
        else if( context.equals(WikiContext.EDIT_GROUP) )
        {
            return doReplacement( viewurl+"?do=EditGroup", name, absolute );
        }
        else if( context.equals(WikiContext.VIEW_GROUP) )
        {
            return doReplacement( viewurl+"?do=Group&group=%n", name, absolute );
        }
        
        throw new InternalWikiException("Requested unsupported context "+context);
    }

    /**
     *  {@inheritDoc}
     */
    public String makeURL( String context,
                           String name,
                           boolean absolute,
                           String parameters )
    {
        if( parameters != null && parameters.length() > 0 )
        {            
            if( context.equals(WikiContext.ATTACH) || context.equals(WikiContext.VIEW) )
            {
                parameters = "?"+parameters;
            }
            else if( context.equals(WikiContext.NONE) )
            {
                parameters = (name.indexOf('?') != -1 ) ? "&amp;" : "?" + parameters;
            }
            else
            {
                parameters = "&amp;"+parameters;
            }
        }
        else
        {
            parameters = "";
        }
        return makeURL( context, name, absolute )+parameters;
    }

    /**
     * {@inheritDoc}
     */
    public String parsePage( String context,
                             HttpServletRequest request,
                             String encoding )
        throws UnsupportedEncodingException
    {
        String pagereq = request.getParameter( "page" );

        if( pagereq == null )
        {
            pagereq = parsePageFromURL( request, encoding );
        }

        return pagereq;
    }

    /**
     *  {@inheritDoc}
     */
    public String getForwardPage( HttpServletRequest req )
    {
        String jspPage = req.getParameter( "do" );
        if( jspPage == null ) jspPage = "Wiki";
    
        return jspPage+".jsp";
    }
}
