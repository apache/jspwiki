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
package org.apache.wiki.api.engine;

import org.apache.wiki.api.core.Engine;

import java.util.Properties;

/**
 * <p>SPI used to notify JSPWiki extensions about {@link Engine}'s initialization & shutdown, without having to deep
 * dive on {@link Engine}'s internals.</p>
 *
 * <p>Examples of {@code EngineLifecycleExtension}'s use cases:
 * <ul>
 *   <li>Appending a plugin's classpath to {@code jspwiki.plugin.searchPath}, so it can be used as a drop-in, without further configuration.</li>
 *   <li>Providing default parameters for a custom extension.</li>
 *   <li>Setting up that expensive singleton, or disposing resources, without having to use or register a {@link org.apache.wiki.event.WikiEventListener WikiEventListener}.</li>
 * </ul>
 * </p>
 *
 * <p>As a concrete example, the markdown module uses an {@code EngineLifecycleExtension} to set up all the required properties
 * if {@code jspwiki.syntax=markdown} is provided on the {@code jspwiki[-custom].properties file}.</p>
 *
 * <p>All methods are provided with a {@code default}, do-nothing implementation, so specific EngineLifecycleExtensions only have
 * to provide implementations for the methods they're really interested in.</p>
 */
public interface EngineLifecycleExtension {

    /**
     * Called before {@link Engine} initialization, after the wiki properties have been sought out.
     *
     * @param properties wiki configuration properties.
     */
    default void onInit( final Properties properties ) {}

    /**
     * Called after {@link Engine} initialization.
     *
     * @param properties wiki configuration properties.
     * @param e JSPWiki's ready to be used {@link Engine}.
     */
    default void onStart( final Engine e, final Properties properties ) {}

    /**
     * Called before {@link Engine} shutdown.
     * @param e JSPWiki's running {@link Engine}.
     * @param properties wiki configuration properties.
     */
    default void onShutdown( final Engine e, final Properties properties ) {}

}
