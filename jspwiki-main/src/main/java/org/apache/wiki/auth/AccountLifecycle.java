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
import org.apache.wiki.auth.user.UserDatabase;
import org.apache.wiki.auth.user.UserProfile;
import org.apache.wiki.util.TextUtil;

import java.io.Serializable;
import java.security.Principal;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;


/**
 * What happens to a user account as the password of its owner gets older, on top of {@link PasswordExpiry}. Each stage is optional and
 * is counted in days from the last time the password was changed ({@link UserProfile#getPasswordChanged()}):
 * <ul>
 * <li>{@value #PROP_WARN_DAYS}: the owner is told that the password is going to expire. Only if {@value PasswordExpiry#PROP_EXPIRY_DAYS}
 * is set, and later than this.</li>
 * <li>{@value #PROP_LOCK_DAYS}: the account is locked, and its owner told. It stays locked until an administrator unlocks it.</li>
 * <li>{@value #PROP_DELETE_DAYS}: the account is deleted, and its owner told.</li>
 * </ul>
 * A stage has to come after the ones before it, in the order of the list and of the expiry of the password; if it does not, it is
 * logged and the stage is off. {@link #apply(Date)} goes through all the accounts once. The password that has no date yet gets it, which
 * is when its clock starts.
 *
 * @since 3.0.1
 */
public final class AccountLifecycle {

    private static final Logger LOG = LogManager.getLogger( AccountLifecycle.class );

    /** Property with the number of days after which the owner of an account is told that the password is going to expire. */
    public static final String PROP_WARN_DAYS = "jspwiki.credentials.expiry.warnDays";

    /** Property with the number of days after which an account is locked. */
    public static final String PROP_LOCK_DAYS = "jspwiki.credentials.lock.days";

    /** Property with the number of days after which an account is deleted. */
    public static final String PROP_DELETE_DAYS = "jspwiki.credentials.delete.days";

    /** Attribute of a profile with the time the owner was told that the password is going to expire. */
    static final String ATTR_REMINDER_SENT = "jspwiki.credentials.reminderSent";

    /** The end of the year 9999: far enough to be forever, and a date that every user database can keep. */
    static final Date LOCKED_UNTIL = new Date( 253_402_300_799_000L );

    /** Tells the owners of the accounts what happens to them. */
    public interface Notifier {

        /**
         * Tells the owner that the password is going to expire. If this throws, the owner is told again at the next pass.
         *
         * @param profile the profile of the account
         * @param expires the day the password expires
         */
        void passwordWillExpire( UserProfile profile, Date expires );

        /**
         * Tells the owner that the account has been locked.
         *
         * @param profile the profile of the account
         * @param deleted the day the account is deleted, or {@code null} if it is not
         */
        void accountLocked( UserProfile profile, Date deleted );

        /**
         * Tells the owner that the account has been deleted.
         *
         * @param profile the profile of the account that has been deleted
         */
        void accountDeleted( UserProfile profile );

    }

    private final Supplier< UserDatabase > databases;
    private final Notifier notifier;
    private final int expiryDays;
    private final int warnDays;
    private final int lockDays;
    private final int deleteDays;

    /**
     * Reads the stages from the properties.
     *
     * @param props the wiki properties
     * @param databases gives the user database, which is not needed before a pass
     * @param notifier tells the owners
     */
    public AccountLifecycle( final Properties props, final Supplier< UserDatabase > databases, final Notifier notifier ) {
        this.databases = databases;
        this.notifier = notifier;

        expiryDays = new PasswordExpiry( props ).getDays();
        final int warn = days( props, PROP_WARN_DAYS );
        if( warn > 0 && expiryDays == 0 ) {
            LOG.error( "{} needs {} to be set; the stage is off", PROP_WARN_DAYS, PasswordExpiry.PROP_EXPIRY_DAYS );
            warnDays = 0;
        } else {
            warnDays = inOrder( PROP_WARN_DAYS, warn, 0, expiryDays );
        }
        lockDays = inOrder( PROP_LOCK_DAYS, days( props, PROP_LOCK_DAYS ), expiryDays, Integer.MAX_VALUE );
        deleteDays = inOrder( PROP_DELETE_DAYS, days( props, PROP_DELETE_DAYS ), Math.max( expiryDays, lockDays ), Integer.MAX_VALUE );
    }

    /**
     * Reads a number of days.
     *
     * @return the days, or 0 if the property is not set to a positive number
     */
    private static int days( final Properties props, final String key ) {
        final String value = TextUtil.getStringProperty( props, key, "0" ).trim();
        try {
            final int days = Integer.parseInt( value );
            if( days < 0 ) {
                LOG.error( "{}={} is negative; the stage is off", key, value );
                return 0;
            }
            return days;
        } catch( final NumberFormatException e ) {
            LOG.error( "{}={} is not a number; the stage is off", key, value );
            return 0;
        }
    }

    /**
     * Checks that the days of a stage are after the stage before it and before the one after it.
     *
     * @return the days, or 0 if they are not in order or are not set
     */
    private static int inOrder( final String key, final int days, final int after, final int before ) {
        if( days > after && days < before ) {
            return days;
        }
        if( days > 0 ) {
            LOG.error( "{}={} is not after {} days and before {} days; the stage is off", key, days, after, before );
        }
        return 0;
    }

    /**
     * Tells if any stage is on.
     *
     * @return {@code true} if there is something to do for the accounts
     */
    public boolean isEnabled() {
        return warnDays > 0 || lockDays > 0 || deleteDays > 0;
    }

    /**
     * Returns the days after which the owner is told that the password is going to expire.
     *
     * @return the days, or 0 if the owner is not told
     */
    public int getWarnDays() {
        return warnDays;
    }

    /**
     * Returns the days after which an account is locked.
     *
     * @return the days, or 0 if accounts are not locked
     */
    public int getLockDays() {
        return lockDays;
    }

    /**
     * Returns the days after which an account is deleted.
     *
     * @return the days, or 0 if accounts are not deleted
     */
    public int getDeleteDays() {
        return deleteDays;
    }

    /**
     * Goes through all the accounts once and does what their age calls for. An account that cannot be dealt with is logged and left for
     * the next pass.
     *
     * @param now the time to count the age from
     */
    public void apply( final Date now ) {
        final UserDatabase db = databases.get();
        final Principal[] wikiNames;
        try {
            wikiNames = db.getWikiNames();
        } catch( final WikiSecurityException e ) {
            LOG.error( "Could not list the user accounts: {}", e.getMessage(), e );
            return;
        }

        for( final Principal wikiName : wikiNames ) {
            try {
                apply( db, db.findByWikiName( wikiName.getName() ), now );
            } catch( final NoSuchPrincipalException e ) {
                LOG.debug( "The account of {} is gone", wikiName.getName(), e );
            } catch( final WikiSecurityException | RuntimeException e ) {
                LOG.error( "Could not apply the account policy to {}: {}", wikiName.getName(), e.getMessage(), e );
            }
        }
    }

    private void apply( final UserDatabase db, final UserProfile profile, final Date now ) throws WikiSecurityException {
        final Date changed = profile.getPasswordChanged();
        if( changed == null ) {
            // The clock of the password starts here
            profile.setPasswordChanged( now );
            db.save( profile );
            return;
        }

        final long age = now.getTime() - changed.getTime();
        if( deleteDays > 0 && age > TimeUnit.DAYS.toMillis( deleteDays ) ) {
            delete( db, profile );
        } else if( lockDays > 0 && age > TimeUnit.DAYS.toMillis( lockDays ) ) {
            if( !profile.isLocked() ) {
                lock( db, profile, changed );
            }
        } else if( warnDays > 0 && age > TimeUnit.DAYS.toMillis( warnDays ) && !profile.isLocked() && !reminderSent( profile, changed ) ) {
            warn( db, profile, changed, now );
        }
    }

    private boolean reminderSent( final UserProfile profile, final Date changed ) {
        final Serializable sent = profile.getAttributes().get( ATTR_REMINDER_SENT );
        return sent instanceof Date && !( ( Date )sent ).before( changed );
    }

    private void warn( final UserDatabase db, final UserProfile profile, final Date changed, final Date now ) throws WikiSecurityException {
        final Date expires = new Date( changed.getTime() + TimeUnit.DAYS.toMillis( expiryDays ) );
        notifier.passwordWillExpire( profile, expires );
        profile.getAttributes().put( ATTR_REMINDER_SENT, now );
        db.save( profile );
    }

    private void lock( final UserDatabase db, final UserProfile profile, final Date changed ) throws WikiSecurityException {
        profile.setLockExpiry( LOCKED_UNTIL );
        db.save( profile );
        LOG.warn( "AUDIT - The account of {} is locked: its password is older than {} days", profile.getLoginName(), lockDays );
        final Date deleted = deleteDays > 0 ? new Date( changed.getTime() + TimeUnit.DAYS.toMillis( deleteDays ) ) : null;
        tell( () -> notifier.accountLocked( profile, deleted ), profile );
    }

    private void delete( final UserDatabase db, final UserProfile profile ) throws WikiSecurityException {
        db.deleteByLoginName( profile.getLoginName() );
        LOG.warn( "AUDIT - The account of {} is deleted: its password is older than {} days", profile.getLoginName(), deleteDays );
        tell( () -> notifier.accountDeleted( profile ), profile );
    }

    /** What has been done to an account stays done if the owner cannot be told. */
    private void tell( final Runnable notification, final UserProfile profile ) {
        try {
            notification.run();
        } catch( final RuntimeException e ) {
            LOG.warn( "Could not tell {} about the change of the account: {}", profile.getLoginName(), e.getMessage(), e );
        }
    }

}
