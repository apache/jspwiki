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
package org.apache.wiki;

import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.exceptions.NoRequiredPropertyException;

import java.io.IOException;
import java.util.Properties;


/**
 * Hooks all WikiProviders not using the public API into it.
 *
 * @deprecated - implement directly {@link org.apache.wiki.api.providers.WikiProvider}.
 * @see org.apache.wiki.api.providers.WikiProvider
 */
@Deprecated
public interface WikiProvider extends org.apache.wiki.api.providers.WikiProvider {

    /**
     *  Initializes the page provider.
     *
     *  @param engine Engine to own this provider
     *  @param properties A set of properties used to initialize this provider
     *  @throws NoRequiredPropertyException If the provider needs a property which is not found in the property set
     *  @throws IOException If there is an IO problem
     */
    @Override
    default void initialize( final Engine engine, final Properties properties ) throws NoRequiredPropertyException, IOException {
        initialize( engine.adapt( WikiEngine.class ), properties );
    }

    /**
     *  Initializes the page provider.
     *
     *  @param engine Engine to own this provider
     *  @param properties A set of properties used to initialize this provider
     *  @throws NoRequiredPropertyException If the provider needs a property which is not found in the property set
     *  @throws IOException If there is an IO problem
     */
    default void initialize( final WikiEngine engine, final Properties properties ) throws NoRequiredPropertyException, IOException {}

}


