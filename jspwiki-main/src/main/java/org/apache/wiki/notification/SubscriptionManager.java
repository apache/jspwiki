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
package org.apache.wiki.notification;

import org.apache.wiki.api.exceptions.WikiException;

import java.util.Set;

/**
 * Keeps track of the pages that users want to be notified about by e-mail when they change.
 * <p>
 * The subscriptions of a user are kept in the attributes of the user profile, so they are stored by the configured
 * {@link org.apache.wiki.auth.user.UserDatabase} and need no additional storage.
 *
 * @since 3.0.1
 */
public interface SubscriptionManager {

    /** Name of the user profile attribute that holds the subscribed page names. Value is <tt>{@value}</tt>. */
    String PROFILE_ATTR_SUBSCRIPTIONS = "subscriptions";

    /**
     * Subscribes a user to the changes of a page. Subscribing twice has no effect.
     *
     * @param loginName login name of the user.
     * @param pageName name of the page.
     * @throws WikiException if the user does not exist or the profile cannot be saved.
     */
    void subscribe( String loginName, String pageName ) throws WikiException;

    /**
     * Removes the subscription of a user to a page. Removing a subscription that does not exist has no effect.
     *
     * @param loginName login name of the user.
     * @param pageName name of the page.
     * @throws WikiException if the user does not exist or the profile cannot be saved.
     */
    void unsubscribe( String loginName, String pageName ) throws WikiException;

    /**
     * Tells whether a user is subscribed to a page.
     *
     * @param loginName login name of the user.
     * @param pageName name of the page.
     * @return {@code true} if the user is subscribed to the page, {@code false} otherwise, also for an unknown user.
     */
    boolean isSubscribed( String loginName, String pageName );

    /**
     * Returns the names of the pages a user is subscribed to.
     *
     * @param loginName login name of the user.
     * @return an unmodifiable set of page names, empty for an unknown user.
     */
    Set< String > getSubscriptions( String loginName );

}
