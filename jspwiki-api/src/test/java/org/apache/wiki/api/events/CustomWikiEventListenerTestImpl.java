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
package org.apache.wiki.api.events;

import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.exceptions.WikiException;
import org.apache.wiki.event.WikiEvent;

import java.util.Properties;

public class CustomWikiEventListenerTestImpl implements CustomWikiEventListener< Engine > {

    Engine e;
    Properties properties;

    @Override
    public void initialize( final Engine engine, final Properties properties ) throws WikiException {
        this.e = engine;
        this.properties = properties;
        this.properties.put( "test1", "initialize" );
    }

    @Override
    public Engine client() {
        properties.put( "test2", "client" );
        return e;
    }

    @Override
    public void actionPerformed( final WikiEvent event ) {
        properties.put( "test3", "actionPerformed" );
    }

}
