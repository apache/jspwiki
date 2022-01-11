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
package org.apache.wiki.ui.progress;

/**
 *  Provides access to an progress item.
 *
 *  @since  2.6
 */
public abstract class ProgressItem {

    /** Status: The PI is created. */
    public static final int CREATED  = 0;
    
    /** Status: The PI is started. */
    public static final int STARTED  = 1;
    
    /** Status: The PI is stopped. */
    public static final int STOPPED  = 2;
    
    /** Status: The PI is finished. */
    public static final int FINISHED = 3;

    protected int m_state = CREATED;

    /**
     *  Get the state of the ProgressItem.
     *
     *  @return CREATED, STARTED, STOPPED or FINISHED.
     */
    public int getState() {
        return m_state;
    }

    /**
     *  Sets the state of the ProgressItem.
     *  
     *  @param state One of the CREATED, STARTED, STOPPED or FINISHED.
     */
    public void setState( final int state ) {
        m_state = state;
    }

    /**
     *  Returns the progress in percents.
     *
     *  @return An integer 0-100.
     */
    public abstract int getProgress();

}
