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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.wiki.api.core.Acl;
import org.apache.wiki.api.core.ContextEnum;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.core.Page;
import org.apache.wiki.api.engine.Initializable;
import org.apache.wiki.api.exceptions.WikiException;
import org.apache.wiki.auth.UserManager;
import org.apache.wiki.auth.acl.AclManager;
import org.apache.wiki.auth.permissions.PermissionFactory;
import org.apache.wiki.auth.user.UserDatabase;
import org.apache.wiki.auth.user.UserProfile;
import org.apache.wiki.event.WikiEvent;
import org.apache.wiki.event.WikiEventListener;
import org.apache.wiki.event.WikiEventManager;
import org.apache.wiki.event.WikiPageEvent;
import org.apache.wiki.filters.FilterManager;
import org.apache.wiki.i18n.InternationalizationManager;
import org.apache.wiki.pages.PageManager;
import org.apache.wiki.util.MailUtil;
import org.apache.wiki.util.TextUtil;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Default implementation of {@link SubscriptionManager}. It listens to the end of the post-save filtering and sends an
 * e-mail to every user that is subscribed to the saved page and has an e-mail address in the profile.
 * <p>
 * The mails are sent from a single background thread, so saving a page never waits for the mail server. The mail does not
 * contain the text of the page. The links in the mails are made absolute with the property {@value #PROP_BASEURL}, since no
 * request is available here. Pages with an ACL on the view permission are not notified, since the view permission of an
 * offline user cannot be evaluated here.
 *
 * @since 3.0.1
 */
public class DefaultSubscriptionManager implements SubscriptionManager, Initializable, WikiEventListener {

    private static final Logger LOG = LogManager.getLogger( DefaultSubscriptionManager.class );

    /** Separates the page names in the profile attribute; it cannot be part of a page name. */
    private static final String SEPARATOR = "\n";

    /** Scheme and host of the links in the mails, like <tt>https://wiki.example.org</tt>. Value is <tt>{@value}</tt>. */
    public static final String PROP_BASEURL = "jspwiki.subscription.baseURL";

    private Engine m_engine;
    private ExecutorService m_executor;
    private String m_baseUrl;

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize( final Engine engine, final Properties props ) throws WikiException {
        m_engine = engine;
        m_baseUrl = TextUtil.getStringProperty( props, PROP_BASEURL, "" ).replaceAll( "/+$", "" );
        m_executor = Executors.newSingleThreadExecutor( r -> {
            final Thread t = new Thread( r, "JSPWiki subscription notifier" );
            t.setDaemon( true );
            return t;
        } );
        // the FilterManager is the source of the save events and it is set up before this component
        WikiEventManager.addWikiEventListener( engine.getManager( FilterManager.class ), this );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void subscribe( final String loginName, final String pageName ) throws WikiException {
        final UserProfile profile = findProfile( loginName );
        final Set< String > pages = readSubscriptions( profile );
        if( pages.add( pageName ) ) {
            storeSubscriptions( profile, pages );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unsubscribe( final String loginName, final String pageName ) throws WikiException {
        final UserProfile profile = findProfile( loginName );
        final Set< String > pages = readSubscriptions( profile );
        if( pages.remove( pageName ) ) {
            storeSubscriptions( profile, pages );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSubscribed( final String loginName, final String pageName ) {
        return getSubscriptions( loginName ).contains( pageName );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set< String > getSubscriptions( final String loginName ) {
        try {
            return Collections.unmodifiableSet( readSubscriptions( findProfile( loginName ) ) );
        } catch( final WikiException e ) {
            return Collections.emptySet();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed( final WikiEvent event ) {
        if( event instanceof WikiPageEvent && event.getType() == WikiPageEvent.POST_SAVE_END ) {
            final String pageName = ( ( WikiPageEvent )event ).getPageName();
            m_executor.execute( () -> notifySubscribers( pageName ) );
        }
    }

    void notifySubscribers( final String pageName ) {
        try {
            final UserDatabase db = m_engine.getManager( UserManager.class ).getUserDatabase();
            final List< UserProfile > recipients = new ArrayList<>();
            for( final Principal wikiName : db.getWikiNames() ) {
                final UserProfile profile = db.findByWikiName( wikiName.getName() );
                final String email = profile.getEmail();
                if( email != null && !email.isEmpty() && readSubscriptions( profile ).contains( pageName ) ) {
                    recipients.add( profile );
                }
            }
            // looking up the ACL renders the page, so it is only done when someone is going to be notified
            final Page page = m_engine.getManager( PageManager.class ).getPage( pageName );
            if( recipients.isEmpty() || page == null || isViewRestricted( page ) ) {
                return;
            }
            for( final UserProfile profile : recipients ) {
                sendMail( profile, page );
            }
        } catch( final Exception e ) {
            LOG.error( "Could not notify the subscribers of " + pageName, e );
        }
    }

    private boolean isViewRestricted( final Page page ) {
        final Acl acl = m_engine.getManager( AclManager.class ).getPermissions( page );
        return acl != null && acl.findPrincipals( PermissionFactory.getPagePermission( page, "view" ) ).length > 0;
    }

    private void sendMail( final UserProfile profile, final Page page ) {
        try {
            final InternationalizationManager i18n = m_engine.getManager( InternationalizationManager.class );
            final Locale locale = Locale.getDefault();
            final String app = m_engine.getApplicationName();
            final String url = m_engine.getURL( ContextEnum.PAGE_VIEW.getRequestContext(), page.getName(), null );
            final String author = page.getAuthor() == null ? "" : page.getAuthor();
            final String subject = i18n.get( InternationalizationManager.DEF_TEMPLATE, locale,
                                             "notification.pageChanged.subject", app, page.getName() );
            final String content = i18n.get( InternationalizationManager.DEF_TEMPLATE, locale,
                                             "notification.pageChanged.content", app, page.getName(), author, m_baseUrl + url );
            MailUtil.sendMessage( m_engine.getWikiProperties(), profile.getEmail(), subject, content );
        } catch( final Exception e ) {
            LOG.warn( "Could not send the change notification of {} to {}: {}", page.getName(), profile.getLoginName(), e.getMessage() );
        }
    }

    private UserProfile findProfile( final String loginName ) throws WikiException {
        return m_engine.getManager( UserManager.class ).getUserDatabase().findByLoginName( loginName );
    }

    private static Set< String > readSubscriptions( final UserProfile profile ) {
        final Set< String > pages = new LinkedHashSet<>();
        final Object stored = profile.getAttributes().get( PROFILE_ATTR_SUBSCRIPTIONS );
        if( stored instanceof String && !( ( String )stored ).isEmpty() ) {
            Collections.addAll( pages, ( ( String )stored ).split( SEPARATOR ) );
        }
        return pages;
    }

    private void storeSubscriptions( final UserProfile profile, final Set< String > pages ) throws WikiException {
        if( pages.isEmpty() ) {
            profile.getAttributes().remove( PROFILE_ATTR_SUBSCRIPTIONS );
        } else {
            profile.getAttributes().put( PROFILE_ATTR_SUBSCRIPTIONS, String.join( SEPARATOR, pages ) );
        }
        m_engine.getManager( UserManager.class ).getUserDatabase().save( profile );
    }

}
