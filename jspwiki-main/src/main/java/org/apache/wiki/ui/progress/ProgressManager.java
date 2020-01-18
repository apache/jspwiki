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
 *  Manages progressing items.  In general this class is used whenever JSPWiki is doing something which may require a long time.
 *  In addition, this manager provides a JSON interface for finding remotely what the progress is.  The JSON object name is
 *  JSON_PROGRESSTRACKER = "{@value #JSON_PROGRESSTRACKER}".
 *
 *  @since  2.6
 */
public interface ProgressManager {

    String JSON_PROGRESSTRACKER = "progressTracker";

    /**
     *  You can use this to get an unique process identifier.
     *
     *  @return A new random value
     */
    String getNewProgressIdentifier();

    /**
     *  Call this method to get your ProgressItem into the ProgressManager queue. The ProgressItem will be moved to state STARTED.
     *
     *  @param pi ProgressItem to start
     *  @param id The progress identifier
     */
    void startProgress( ProgressItem pi, String id );

    /**
     *  Call this method to remove your ProgressItem from the queue (after which getProgress() will no longer find it.
     *  The ProgressItem will be moved to state STOPPED.
     *
     *  @param id The progress identifier
     */
    void stopProgress( String id );

    /**
     *  Get the progress in percents.
     *
     *  @param id The progress identifier.
     *  @return a value between 0 to 100 indicating the progress
     *  @throws IllegalArgumentException If no such progress item exists.
     */
    int getProgress( String id ) throws IllegalArgumentException;

}
