/*
    JSPWiki - a JSP-based WikiWiki clone.

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
package com.ecyrd.jspwiki.ui.progress;

/**
 *  Provides access to an progress item.
 *
 *  @since  2.6
 */
public abstract class ProgressItem
{
    public static final int CREATED  = 0;
    public static final int STARTED  = 1;
    public static final int STOPPED  = 2;
    public static final int FINISHED = 3;

    protected int m_state = CREATED;

    public int getState()
    {
        return m_state;
    }

    public void setState( int state )
    {
        m_state = state;
    }

    /**
     *  Returns the progress in percents.
     *  @return An integer 0-100.
     */
    public abstract int getProgress();
}
