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
import org.apache.wiki.api.exceptions.WikiException;

import java.util.Properties;


/**
 * Marker interface for Engine's components that can be initialized.
 */
public interface Initializable {

    /**
     * <p>Initializes this Engine component. Note that the engine is not fully initialized at this
     * point, so don't do anything fancy here - use lazy init, if you have to.<br/>&nbsp;</p>
     *
     * @param engine Engine performing the initialization.
     * @param props Properties for setup.
     * @throws WikiException if an exception occurs while initializing the component.
     */
    void initialize( Engine engine, Properties props ) throws WikiException;

}
