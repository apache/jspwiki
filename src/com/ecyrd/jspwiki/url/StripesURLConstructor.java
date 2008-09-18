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
package com.ecyrd.jspwiki.url;

import java.io.File;
import java.io.FileInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.Map.Entry;

import net.sourceforge.stripes.action.ActionBean;
import net.sourceforge.stripes.controller.UrlBinding;
import net.sourceforge.stripes.controller.UrlBindingFactory;
import net.sourceforge.stripes.controller.UrlBindingParameter;
import net.sourceforge.stripes.util.UrlBuilder;
import net.sourceforge.stripes.util.bean.ParseException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.action.*;

/**
 * Implements the default URL constructor using links directly to the JSP pages.
 * This is what JSPWiki by default is using. For example, WikiContext.VIEW
 * points at "Wiki.jsp", etc.
 * 
 * @since 2.2
 */
public class StripesURLConstructor extends DefaultURLConstructor
{
    private static final Logger log = Logger.getLogger( StripesURLConstructor.class );
    
    /**
     * Contains the absolute path of the JSPWiki Web application without the
     * actual servlet; in other words, the absolute or relative path to this
     * webapp's root path. If no base URL is specified in
     * <code>jspwiki.properties</code>, the value will be an empty string.
     * Note that the trailing slash is removed.
     */
    private String m_pathPrefix;

    /**
     * Contains the base URL of the JSPWiki Web application before the
     * servlet context, with trailing slash removed.
     */
    private String m_baseUrl;
    
    /**
     * Returns the URL for accessing a named wiki page via a particular
     * ActionBean, with a set of appended parameters. The URL returned will be
     * absolute if the WikiEngine was configured to return absolute URLs;
     * otherwise, the URL will be relative to the webapp context root. If the
     * ActionBean class is not a WikiContext subclass, the value of the
     * <code>page</code> parameter is ignored.
     * 
     * @param context the wiki request context to use
     * @param name the target of the action, typically the wiki page
     * @param absolute If <code>true</code>, will generate an absolute URL
     *            regardless of properties setting.
     * @param parameters the query parameters to append to the end of the URL; may
     *            be <code>null</code> if no parameters
     * @return the URL
     */
    @Override
    public String makeURL( String context, String name, boolean absolute, String parameters )
    {
        // Get the path prefix
        String pathPrefix;
        UrlBuilder urlBuilder;
        boolean engineAbsolute = "absolute".equals( m_engine.getWikiProperties().getProperty( WikiEngine.PROP_REFSTYLE ) );
        pathPrefix = ( absolute | engineAbsolute ) ? m_baseUrl + m_pathPrefix : m_pathPrefix ;
        
        // Split up the parameters
        Map<String,String> params = splitParamString(parameters);

        // For the NONE context, initialize UrlBuilder with root path
        String baseUrl;
        if ( WikiContext.NONE.equals( context ) )
        {
            urlBuilder = new UrlBuilder( null, "/" + name, false );
        }
        
        // For the other contexts, initialize UrlBuilder with the WikiActionBean's URLBinding
        else
        {
            HandlerInfo handler = m_engine.getWikiActionBeanFactory().findEventHandler( context );
            Class<? extends WikiActionBean> beanClass = handler.getActionBeanClass();
            UrlBinding mapping = UrlBindingFactory.getInstance().getBindingPrototype(beanClass);
            baseUrl = mapping == null ? null : mapping.getPath();
            urlBuilder = new UrlBuilder( null, baseUrl, false );

            // Append the "name" parameter for page/group action beans (argh; we have to do stupid if/else tricks)
            if ( name != null )
            {
                if( AttachActionBean.class.isAssignableFrom( beanClass ) )
                {
                    int slashAt = name.indexOf( '/' );
                    if ( slashAt == -1 )
                    {
                        urlBuilder.addParameter( "page",name );
                    }
                    else
                    {
                        urlBuilder.addParameter( "page", name.substring( 0,  slashAt ) );
                        urlBuilder.addParameter( "attachment", name.substring( slashAt + 1,  name.length() ) );
                    }
                }
                else if( WikiContext.class.isAssignableFrom( beanClass ) )
                {
                    urlBuilder.addParameter( "page", name );
                }
                else if( GroupActionBean.class.isAssignableFrom( beanClass ) )
                {
                    urlBuilder.addParameter( "group", name );
                }
            }
            
//            // Set the event handler if not the default
//            if ( !handler.equals( HandlerInfo.getDefaultHandlerInfo( beanClass ) ) )
//            {
//                urlBuilder.setEvent( handler.getEventName() );
//            }
        }

        // Append the other parameters
        if( params != null )
        {
            for( Entry<String, String> param : params.entrySet() )
            {
                if ( param.getValue() != null && param.getValue().length() > 0 )
                {
                    urlBuilder.addParameter( param.getKey(), param.getValue() );
                }
            }
        }
        
        // Get the URLl; replace delimiters
        String url = pathPrefix + urlBuilder.toString();
        url = StringUtils.replace( url, "+", "%20" );
        url = StringUtils.replace( url, "%2F", "/" );

        return url;
    }

    /**
     * {@inheritDoc}
     */
    public void initialize( WikiEngine engine, Properties properties )
    {
        super.initialize( engine, properties );

        // Load the URL patterns from the config file
        File file = new File( "WEB-INF/urlpattern.properties" );
        Properties props = new Properties();
        try
        {
            props.load( new FileInputStream( file ) );
        }
        catch( Exception e )
        {
            // throw new WikiException( e.getMessage() );
        }

        for( Map.Entry<Object, Object> entry : props.entrySet() )
        {
            String beanClassName = ((String) entry.getKey()).trim();
            String urlPattern = ((String) entry.getKey()).trim();
            Class<? extends WikiActionBean> beanClass;
            try
            {
                beanClass = (Class<? extends WikiActionBean>) Class.forName( beanClassName );
                UrlBindingFactory urlFactory = UrlBindingFactory.getInstance();
                UrlBinding bindingPrototype = parseUrlBinding( beanClass, urlPattern );
                urlFactory.addBinding( beanClass, bindingPrototype );
            }
            catch( ClassNotFoundException e )
            {
                log.error( "Could not instantiate class " + beanClassName );
            }
        }

        m_pathPrefix = getContextPath( engine );
        m_baseUrl = engine.getBaseURL();
        if ( m_baseUrl.endsWith( "/" ) )
        {
            m_baseUrl = m_baseUrl.substring( 0, m_baseUrl.length() - 1 );
        }
    }

    public static String getContextPath( WikiEngine engine )
    {
        // Initialize the path prefix for building URLs
        // NB this was stolen and adapted from the old URLConstructor code...
        String baseurl = engine.getBaseURL();
        String contextPath = "/JSPWiki"; // Just a guess.
        if( baseurl != null && baseurl.length() > 0 )
        {
            try
            {
                URL url = new URL( baseurl );
                contextPath = url.getPath();
                if ( contextPath.endsWith( "/" ) )
                {
                    contextPath = contextPath.substring( 0, contextPath.length() - 1 );
                }
            }
            catch( MalformedURLException e )
            {
            }
        }
        return contextPath;
    }

    /**
     * Splits a set of parameters, supplied in a single String, into its
     * component parts.
     * 
     * @param appendedParams
     * @return the map containing parameter names (keys) and their values
     *         (values).
     */
    private Map<String, String> splitParamString( String appendedParams )
    {
        Map<String, String> params = new HashMap<String, String>();
        if( appendedParams != null )
        {
            String[] kvs = appendedParams.split( String.valueOf( "&" ) );
            for( String kv : kvs )
            {
                int equals = kv.lastIndexOf( '=', kv.length() - 1 );
                switch( equals )
                {
                    case 0:
                        break;
                    case -1:
                        params.put( kv, "" );
                        break;
                    default:
                        String key = kv.substring( 0, equals );
                        String value = kv.substring( equals + 1, kv.length() );
                        params.put( key, value );
                }
            }
        }
        return params;
    }
    
    private UrlBinding parseUrlBinding( Class<? extends ActionBean> beanType, String pattern )
    {
        // This is copied line-for-line from Stripes 1.5
        
        // parse the pattern
        String path = null;
        List<Object> components = new ArrayList<Object>();
        int braceLevel = 0;
        boolean escape = false;
        char[] chars = pattern.toCharArray();
        StringBuilder buf = new StringBuilder(pattern.length());
        char c = 0;
        for (int i = 0; i < chars.length; i++) {
            c = chars[i];
            if (!escape) {
                switch (c) {
                case '{':
                    ++braceLevel;
                    if (braceLevel == 1) {
                        if (path == null) {
                            // extract trailing non-alphanum chars as a literal to trim the path
                            int end = buf.length() - 1;
                            while (end >= 0 && !Character.isJavaIdentifierPart(buf.charAt(end)))
                                --end;
                            if (end < 0) {
                                path = buf.toString();
                            }
                            else {
                                ++end;
                                path = buf.substring(0, end);
                                components.add(buf.substring(end));
                            }
                        }
                        else {
                            components.add(buf.toString());
                        }
                        buf.setLength(0);
                        continue;
                    }
                    break;
                case '}':
                    if (braceLevel > 0) {
                        --braceLevel;
                    }
                    if (braceLevel == 0) {
                        components.add(parseUrlBindingParameter(beanType, buf.toString()));
                        buf.setLength(0);
                        continue;
                    }
                    break;
                case '\\':
                    escape = true;
                    continue;
                }
            }

            // append the char
            buf.append(c);
            escape = false;
        }

        // handle whatever is left
        if (buf.length() > 0) {
            if (escape)
                throw new ParseException(pattern, "Expression must not end with escape character");
            else if (braceLevel > 0)
                throw new ParseException(pattern, "Unterminated left brace ('{') in expression");
            else if (path == null)
                path = buf.toString();
            else if (c == '}')
                components.add(parseUrlBindingParameter(beanType, buf.toString()));
            else
                components.add(buf.toString());
        }

        return new UrlBinding(beanType, path, components);
    }
    
    /**
     * Parses a parameter specification into name and default value and returns a
     * {@link UrlBindingParameter} with the corresponding name and default value properties set
     * accordingly.
     * 
     * @param beanClass the bean class to which the binding applies
     * @param string the parameter string
     * @return a parameter object
     * @throws ParseException if the pattern cannot be parsed
     */
    protected static UrlBindingParameter parseUrlBindingParameter(
            Class<? extends ActionBean> beanClass, String string) {
        char[] chars = string.toCharArray();
        char c = 0;
        boolean escape = false;
        StringBuilder name = new StringBuilder();
        StringBuilder defaultValue = new StringBuilder();
        StringBuilder current = name;
        for (int i = 0; i < chars.length; i++) {
            c = chars[i];
            if (!escape) {
                switch (c) {
                case '\\':
                    escape = true;
                    continue;
                case '=':
                    current = defaultValue;
                    continue;
                }
            }

            current.append(c);
            escape = false;
        }

        String dflt = defaultValue.length() < 1 ? null : defaultValue.toString();
        if (dflt != null && UrlBindingParameter.PARAMETER_NAME_EVENT.equals(name.toString())) {
            throw new ParseException(string, "In ActionBean class " + beanClass.getName()
                    + ", the " + UrlBindingParameter.PARAMETER_NAME_EVENT
                    + " parameter may not be assigned a default value. Its default value is"
                    + " determined by the @DefaultHandler annotation.");
        }
        return new UrlBindingParameter(beanClass, name.toString(), null, dflt) {
            @Override
            public String getValue() {
                throw new UnsupportedOperationException(
                        "getValue() is not implemented for URL parameter prototypes");
            }
        };
    }

}
