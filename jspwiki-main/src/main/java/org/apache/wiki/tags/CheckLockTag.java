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
package org.apache.wiki.tags;

import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.core.Page;
import org.apache.wiki.api.exceptions.ProviderException;
import org.apache.wiki.pages.PageLock;
import org.apache.wiki.pages.PageManager;

import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 *  Checks whether the page is locked for editing.  If the mode matches,
 *  the tag body is included.  The "mode" can be any of the following:
 *  
 *  <ul>
 *  <li><b>locked</b> - The page is currently locked, but the lock is owned by someone else.</li>
 *  <li><b>owned</b> - The page is currently locked and the current user is the owner of the lock.</li>
 *  <li><b>unlocked</b> - Nobody has locked the page.</li>
 *  </ul>
 *  
 *  @since 2.0
 */
public class CheckLockTag extends WikiTagBase {

    private static final long serialVersionUID = 1L;
    
    private enum LockState {
        LOCKED, NOTLOCKED, OWNED
    }

    private LockState m_mode;

    /**
     *  {@inheritDoc}
     */
    @Override
    public void initTag() {
        super.initTag();
        m_mode = LockState.NOTLOCKED;
    }

    /**
     *  Sets the mode to check for.
     *  
     *  @param arg A String for the mode.
     */
    public void setMode( final String arg ) {
        if( "locked".equals( arg ) ) {
            m_mode = LockState.LOCKED;
        } else if( "owned".equals( arg ) ) {
            m_mode = LockState.OWNED;
        } else {
            m_mode = LockState.NOTLOCKED;
        }
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public final int doWikiStartTag() throws IOException, ProviderException {
        final Engine engine = m_wikiContext.getEngine();
        final Page page = m_wikiContext.getPage();

        if( page != null ) {
            final PageManager mgr = engine.getManager( PageManager.class );
            final PageLock lock = mgr.getCurrentLock( page );
            final HttpSession session = pageContext.getSession();
            final PageLock userLock = ( PageLock )session.getAttribute( "lock-" + page.getName() );
            if( ( lock != null && m_mode == LockState.LOCKED && lock != userLock ) ||
                ( lock != null && m_mode == LockState.OWNED && lock == userLock )  ||
                ( lock == null && m_mode == LockState.NOTLOCKED ) ) {

                final String tid = getId();
                if( tid != null && lock != null ) {
                    pageContext.setAttribute( tid, lock );
                }

                return EVAL_BODY_INCLUDE;
            }
        }

        return SKIP_BODY;
    }

}
