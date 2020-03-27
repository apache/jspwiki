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

import org.apache.wiki.util.PropertyReader;
import org.apache.wiki.util.TextUtil;

import javax.servlet.ServletContext;
import java.nio.file.ProviderNotFoundException;
import java.util.Properties;
import java.util.ServiceLoader;


public class Wiki {

    private static final String PROP_PROVIDER_IMPL_ENGINE = "jspwiki.provider.impl.engine";
    private static final String DEFAULT_PROVIDER_IMPL_ENGINE = "org.apache.wiki.spi.EngineSPIDefaultImpl";

    private static EngineSPI engineSPI;

    static void init( final ServletContext context ) {
        final Properties properties = PropertyReader.loadWebAppProps( context );
        engineSPI = getSPI( EngineSPI.class, properties, PROP_PROVIDER_IMPL_ENGINE, DEFAULT_PROVIDER_IMPL_ENGINE );
    }

    public static EngineDSL engine() {
        return new EngineDSL( engineSPI );
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
