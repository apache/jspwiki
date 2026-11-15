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

import jakarta.mail.MessagingException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.auth.user.UserProfile;
import org.apache.wiki.i18n.InternationalizationManager;
import org.apache.wiki.util.MailUtil;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;


/**
 * Tells the owners of the accounts by e-mail what {@link AccountLifecycle} does to them. The texts are the
 * <code>notification.credentials.*</code> ones of the templates bundle. Nobody is told who has no e-mail address.
 *
 * @since 3.0.1
 */
public final class AccountLifecycleMailer implements AccountLifecycle.Notifier {

    private static final Logger LOG = LogManager.getLogger( AccountLifecycleMailer.class );

    /** Sends a message, as {@link MailUtil#sendMessage(Properties, String, String, String)} does. */
    @FunctionalInterface
    interface Mail {

        void send( Properties props, String to, String subject, String content ) throws MessagingException;

    }

    private final Engine engine;
    private final Locale locale;
    private final Mail mail;

    /**
     * Creates a mailer that sends the messages with the mail settings of the wiki, in the language of the server.
     *
     * @param engine the wiki engine
     */
    public AccountLifecycleMailer( final Engine engine ) {
        this( engine, Locale.getDefault(), MailUtil::sendMessage );
    }

    AccountLifecycleMailer( final Engine engine, final Locale locale, final Mail mail ) {
        this.engine = engine;
        this.locale = locale;
        this.mail = mail;
    }

    /** {@inheritDoc} */
    @Override
    public void passwordWillExpire( final UserProfile profile, final Date expires ) {
        send( profile, "expiring", format( expires ) );
    }

    /** {@inheritDoc} */
    @Override
    public void accountLocked( final UserProfile profile, final Date deleted ) {
        if( deleted == null ) {
            send( profile, "locked" );
        } else {
            send( profile, "lockedDeleted", format( deleted ) );
        }
    }

    /** {@inheritDoc} */
    @Override
    public void accountDeleted( final UserProfile profile ) {
        send( profile, "deleted" );
    }

    private String format( final Date date ) {
        return DateFormat.getDateInstance( DateFormat.LONG, locale ).format( date );
    }

    /**
     * Sends the message of a template. Its subject is called <code>notification.credentials.NAME.subject</code>, with the name of the
     * wiki as argument, and its content <code>notification.credentials.NAME.content</code>, with the name of the wiki, the full name and
     * the login name of the owner and the extra arguments.
     */
    private void send( final UserProfile profile, final String name, final Object... extra ) {
        if( StringUtils.isBlank( profile.getEmail() ) ) {
            LOG.debug( "{} has no e-mail address, so it is not told about {}", profile.getLoginName(), name );
            return;
        }

        final InternationalizationManager i18n = engine.getManager( InternationalizationManager.class );
        final String app = engine.getApplicationName();
        final Object[] arguments = new Object[ 3 + extra.length ];
        arguments[ 0 ] = app;
        arguments[ 1 ] = profile.getFullname() == null ? profile.getLoginName() : profile.getFullname();
        arguments[ 2 ] = profile.getLoginName();
        System.arraycopy( extra, 0, arguments, 3, extra.length );

        final String subject = i18n.get( InternationalizationManager.DEF_TEMPLATE, locale, "notification.credentials." + name + ".subject", app );
        final String content = i18n.get( InternationalizationManager.DEF_TEMPLATE, locale, "notification.credentials." + name + ".content", arguments );
        try {
            mail.send( engine.getWikiProperties(), profile.getEmail(), subject, content );
        } catch( final MessagingException e ) {
            throw new IllegalStateException( "Could not send the e-mail about " + name + " to " + profile.getLoginName() + ": " + e.getMessage(), e );
        }
    }

}
