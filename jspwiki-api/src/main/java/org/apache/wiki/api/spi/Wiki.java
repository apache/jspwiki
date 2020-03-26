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
package org.apache.wiki.api.spi;

import org.apache.wiki.api.core.Engine;
import org.apache.wiki.util.PropertyReader;
import org.apache.wiki.util.TextUtil;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import java.nio.file.ProviderNotFoundException;
import java.util.Properties;
import java.util.ServiceLoader;


public class Wiki {

    private static final String ATTR_ENGINE_SPI = "org.apache.wiki.api.core.Engine";
    private static final String PROP_ENGINE_PROVIDER_IMPL = "jspwiki.provider.impl.engine";
    private static final String DEFAULT_ENGINE_PROVIDER_IMPL = "org.apache.wiki.spi.EngineSPIDefaultImpl";

    public static Engine engine( final ServletConfig config ) {
        return engine( config.getServletContext(), null );
    }

    public static Engine engine( final ServletConfig config, final Properties props ) {
        return engine( config.getServletContext(), props );
    }

    public static Engine engine( final ServletContext context, final Properties props ) {
        final Engine engine = ( Engine )context.getAttribute( ATTR_ENGINE_SPI );
        if( engine == null ) {
            final Properties properties = loadPropertiesFrom( context, props );
            return getSPI( EngineSPI.class, properties, PROP_ENGINE_PROVIDER_IMPL, DEFAULT_ENGINE_PROVIDER_IMPL ).getInstance( context, props );
        }
        return engine;
    }

    static Properties loadPropertiesFrom( final ServletContext context, final Properties props ) {
        if( props == null ) {
            return PropertyReader.loadWebAppProps( context );
        }
        return props;
    }

    static < SPI > SPI getSPI( final Class< SPI > spi, final Properties props, final String prop, final String defValue ) {
        final String providerImpl = TextUtil.getStringProperty( props, prop, defValue );
        final ServiceLoader< SPI > loader = ServiceLoader.load( spi );
        for( final SPI provider : loader ) {
            if( providerImpl.equals( provider.getClass().getName() ) ) {
                return provider;
            }
        }
        throw new ProviderNotFoundException( spi.getName() + " provider not found" );
    }

}
