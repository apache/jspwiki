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
package org.apache.wiki.ui.stripes;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import net.sourceforge.stripes.action.ActionBean;
import net.sourceforge.stripes.config.Configuration;
import net.sourceforge.stripes.config.DontAutoLoad;
import net.sourceforge.stripes.controller.NameBasedActionResolver;
import net.sourceforge.stripes.controller.UrlBinding;
import net.sourceforge.stripes.controller.UrlBindingFactory;

import org.apache.wiki.api.WikiException;

/**
 * <p>
 * Resolves Stripes ActionBeans in an identical fashion to
 * {@link NameBasedActionResolver}, but allows UrlBindings to be defined in an
 * external file. At the moment, this file is hard-coded to the location
 * <code>WEB-INF/urlpattern.properties</code>. This will be changed later.
 * </p>
 * <p>
 * Because FileBasedActionResolver works slightly differently than the default
 * ActionResolver, this class is marked with the {@link DontAutoLoad} so that
 * Stripes's bootstrapper will not load it by default. To use
 * FileBasedActionResolver, the <code>web.xml</code> configuration parameters
 * for StripesFilter must specify it explicitly by setting the
 * <code>ActionResolver.Class</code> init parameter to this class name (e.g.,
 * <code>org.apache.wiki.ui.stripes.FileBasedActionResolver</code>).
 * 
 * @author Andrew Jaquith
 * @since 3.0
 */
@DontAutoLoad
public class FileBasedActionResolver extends NameBasedActionResolver
{

    private Set<Class<? extends ActionBean>> m_processed = new HashSet<Class<? extends ActionBean>>();

    private Properties m_urlBindings = new Properties();

    private static final String URL_BINDINGS = "/WEB-INF/urlpattern.properties";

    /**
     * <p>
     * Initializes the ActionResolver by loading the URL bindings from the
     * configuration file, then calling the parent class method
     * {@link NameBasedActionResolver#init(Configuration)}.
     * </p>
     */
    @Override
    public void init( Configuration configuration ) throws Exception
    {
        // FIXME: make this configurable
        // Get the url bindings file
        m_urlBindings.clear();
        InputStream in = configuration.getServletContext().getResourceAsStream( URL_BINDINGS );
        if( in == null )
        {
            throw new IOException( "Resource not returned by servlet context: " + URL_BINDINGS );
        }

        // Load the URL patterns from the config file
        try
        {
            m_urlBindings.load( in );
            in.close();
        }
        catch( IOException e )
        {
            throw new WikiException( "Could not find file " + URL_BINDINGS + ". Reason: " + e.getMessage(), e );
        }

        // Initialize the URL bindings factory
        super.init( configuration );
    }

    /**
     * Returns the {@link UrlBinding} for a specified ActionBean class. If an
     * UrlBinding was specified in the configuration file, it is returned.
     * Otherwise, the URLBinding calculated by {@link NameBasedActionResolver}
     * is returned.
     * 
     * @param clazz the ActionBean class for which the UrlBinding is needed
     * @return the UrlBinding
     */
    @Override
    public String getUrlBinding( Class<? extends ActionBean> clazz )
    {
        // See if we've processed the class first.
        if( m_processed.contains( clazz ) )
        {
            return super.getUrlBinding( clazz );
        }

        // Have we defined a binding string somewhere? If not, delegate to
        // Stripes
        String binding = m_urlBindings.getProperty( clazz.getName() );
        if( binding == null )
        {
            return super.getUrlBinding( clazz );
        }

        // Must be a new ActionBean we haven't processed yet, AND we have a
        // binding for it.
        UrlBinding prototype = UrlBindingFactory.parseUrlBinding( clazz, binding );
        UrlBindingFactory.getInstance().addBinding( clazz, prototype );
        m_processed.add( clazz );
        return prototype.toString();
    }

}
