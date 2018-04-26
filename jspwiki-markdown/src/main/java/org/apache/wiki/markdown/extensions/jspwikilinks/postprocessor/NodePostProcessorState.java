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
package org.apache.wiki.markdown.extensions.jspwikilinks.postprocessor;

import com.vladsch.flexmark.ast.Node;
import com.vladsch.flexmark.util.NodeTracker;


/**
 * Encapsulates different node's post-process for different kinds of nodes.
 */
public interface NodePostProcessorState < T extends Node > {

    /**
     * performs further processing before rendering.
     *
     * @param state to record node addition/deletion
     * @param link the specific node in which the post-processing is taking place.
     */
    void process( NodeTracker state, T node );

}
