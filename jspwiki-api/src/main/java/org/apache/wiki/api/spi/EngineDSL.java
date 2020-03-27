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

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import java.util.Properties;


public class EngineDSL {

    private final EngineSPI engineSPI;

    EngineDSL( final EngineSPI engineSPI ) {
        this.engineSPI = engineSPI;
    }

    /**
     * Locate, or build if necessary, a configured {@link Engine} instance.
     *
     * @param config servlet config holding the {@link Engine} instance.
     * @return a configured {@link Engine} instance.
     */
    public Engine find( final ServletConfig config ) {
        return find( config.getServletContext(), null );
    }

    /**
     * Locate, or build if necessary, a configured {@link Engine} instance.
     *
     * @param config servlet config holding the {@link Engine} instance.
     * @param props Engine configuration properties.
     * @return a configured {@link Engine} instance.
     */
    public Engine find( final ServletConfig config, final Properties props ) {
        return find( config.getServletContext(), props );
    }

    /**
     * Locate, or build if necessary, a configured {@link Engine} instance.
     *
     * @param context servlet context holding the {@link Engine} instance.
     * @param props Engine configuration properties.
     * @return a configured {@link Engine} instance.
     */
    public Engine find( final ServletContext context, final Properties props ) {
        return engineSPI.getInstance( context, props );
    }

}
