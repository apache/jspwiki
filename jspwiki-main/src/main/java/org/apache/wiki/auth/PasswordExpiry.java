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
package org.apache.wiki.auth;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.wiki.auth.user.UserProfile;
import org.apache.wiki.util.TextUtil;

import java.util.Date;
import java.util.Properties;
import java.util.concurrent.TimeUnit;


/**
 * Decides when a password is too old, as set by {@value #PROP_EXPIRY_DAYS}. It is off unless a number of days is configured. Only the
 * profiles of the wiki's user database have a password that can expire; it is the date the password was last changed
 * ({@link UserProfile#getPasswordChanged()}) that counts.
 *
 * @since 3.0.1
 */
public final class PasswordExpiry {

    private static final Logger LOG = LogManager.getLogger( PasswordExpiry.class );

    /** Property with the number of days after which a password expires. Zero, the default, means that passwords do not expire. */
    public static final String PROP_EXPIRY_DAYS = "jspwiki.credentials.expiry.days";

    private final long expiryMillis;

    /**
     * Reads {@value #PROP_EXPIRY_DAYS}. A value that is not a number, or is negative, is logged and passwords do not expire.
     *
     * @param props the wiki properties
     */
    public PasswordExpiry( final Properties props ) {
        long millis = 0;
        final String value = TextUtil.getStringProperty( props, PROP_EXPIRY_DAYS, "0" ).trim();
        try {
            final long days = Long.parseLong( value );
            if( days < 0 ) {
                LOG.error( "{}={} is negative; passwords do not expire", PROP_EXPIRY_DAYS, value );
            } else {
                millis = TimeUnit.DAYS.toMillis( days );
            }
        } catch( final NumberFormatException e ) {
            LOG.error( "{}={} is not a number; passwords do not expire", PROP_EXPIRY_DAYS, value );
        }
        expiryMillis = millis;
    }

    /**
     * Tells if passwords expire.
     *
     * @return {@code true} if a number of days is configured
     */
    public boolean isEnabled() {
        return expiryMillis > 0;
    }

    /**
     * Tells if the password of a profile is older than the configured number of days. The password of a profile that does not know
     * when it was changed does not expire.
     *
     * @param profile the profile
     * @param now the current time
     * @return {@code true} if passwords expire and the password of the profile has
     */
    public boolean isExpired( final UserProfile profile, final Date now ) {
        final Date changed = profile.getPasswordChanged();
        return isEnabled() && changed != null && now.getTime() - changed.getTime() > expiryMillis;
    }

}
