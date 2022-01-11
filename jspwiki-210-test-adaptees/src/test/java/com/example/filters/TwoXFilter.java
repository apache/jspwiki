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
package com.example.filters;

import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.api.exceptions.FilterException;
import org.apache.wiki.api.filters.BasicPageFilter;

import java.util.Properties;


public class TwoXFilter extends BasicPageFilter {

    String newContent = "";
    int invocations = 0;

    /** {@inheritDoc} */
    @Override
    public void initialize( final WikiEngine engine, final Properties properties ) throws FilterException {
        super.initialize( engine, properties );
        invocations++;
    }

    @Override
    public String preTranslate( final WikiContext wikiContext, final String content ) throws FilterException {
        invocations++;
        return content;
    }

    /** {@inheritDoc} */
    @Override
    public String postTranslate( final WikiContext wikiContext, final String htmlContent ) {
        invocations++;
        newContent = "see how I care about yor content - hmmm...";
        return newContent;
    }

    @Override
    public String preSave( final WikiContext wikiContext, final String content ) throws FilterException {
        invocations++;
        return content;
    }

    @Override
    public void postSave( final WikiContext wikiContext, final String content ) throws FilterException {
        invocations++;
    }

    public int invocations() {
        return invocations;
    }

}
