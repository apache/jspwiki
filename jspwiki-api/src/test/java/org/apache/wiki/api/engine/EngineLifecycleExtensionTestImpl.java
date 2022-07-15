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

public class EngineLifecycleExtensionTestImpl implements EngineLifecycleExtension {

    /** {@inheritDoc */
    @Override
    public void onInit(final Properties properties ) {
        properties.put( "test", "onInit" );
    }

    /** {@inheritDoc */
    @Override
    public void onStart(final Engine e, final Properties properties ) {
        properties.put( "test", "onStart" );
    }

    /** {@inheritDoc */
    @Override
    public void onShutdown(final Engine e, final Properties properties ) {
        properties.put( "test", "onShutdown" );
    }
    
}
