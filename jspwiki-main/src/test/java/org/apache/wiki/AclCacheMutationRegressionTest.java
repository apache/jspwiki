/*
 * Copyright 2026 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.wiki;

import java.security.Principal;
import java.util.Properties;

import org.apache.wiki.api.core.Acl;
import org.apache.wiki.api.core.AclEntry;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.core.Page;
import org.apache.wiki.api.core.Session;
import org.apache.wiki.auth.AuthorizationManager;
import org.apache.wiki.auth.WikiPrincipal;
import org.apache.wiki.auth.acl.AclEntryImpl;
import org.apache.wiki.auth.acl.AclImpl;
import org.apache.wiki.auth.permissions.PagePermission;
import org.apache.wiki.auth.permissions.PermissionFactory;
import org.apache.wiki.pages.PageManager;
import org.apache.wiki.plugin.InsertPage;
import org.apache.wiki.plugin.PluginManager;
import org.apache.wiki.render.RenderingManager;
import org.apache.wiki.ui.EditorManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.jsp.PageContext;
import org.apache.wiki.api.spi.Wiki;

class AclCacheMutationRegressionTest {

    private TestEngine engine;

    @BeforeEach
    void setUp() throws Exception {
        final Properties props = TestEngine.getTestProperties();
        engine = TestEngine.build( props );
    }

    @AfterEach
    void tearDown() {
        if( engine != null ) {
            engine.shutdown();
        }
    }

    @Test
    void testWikiPageCloneDeepCopiesAclEntries() {
        final Page original = new WikiPage( engine, "TestCloneAcl" );
        final Acl acl = new AclImpl();
        final AclEntry entry = new AclEntryImpl();
        final Principal all = WikiPrincipal.GUEST;
        entry.setPrincipal( all );
        entry.addPermission( PermissionFactory.getPagePermission( original, PagePermission.VIEW_ACTION ) );
        acl.addEntry( entry );
        original.setAcl( acl );

        final Page cloned = original.clone();
        final AclEntry clonedEntry = cloned.getAcl().getAclEntry( all );
        Assertions.assertNotNull( clonedEntry );
        Assertions.assertNotSame( entry, clonedEntry, "Cloned AclEntry must be a distinct instance" );

        clonedEntry.addPermission( PermissionFactory.getPagePermission( original, PagePermission.EDIT_ACTION ) );

        final AclEntry originalAfter = original.getAcl().getAclEntry( all );
        Assertions.assertFalse(
                originalAfter.checkPermission( PermissionFactory.getPagePermission( original, PagePermission.EDIT_ACTION ) ),
                "Mutating cloned AclEntry must not mutate original page ACL"
        );
    }

    @Test
    void testInsertPageSetsRealPageSoIncludedAclDoesNotMutateParent() throws Exception {
        engine.saveText( "PocSource", "PUBLIC SOURCE\n\n[{ALLOW edit testviewer}]" );
        engine.saveText( "PocParent", "[{ALLOW view testviewer}]\nRESTRICTED CONTENT\n[{InsertPage page='PocSource'}]" );

        final AuthorizationManager authMgr = engine.getManager( AuthorizationManager.class );
        final Session viewerSession = engine.guestSession();
        // Simulate authenticated user testviewer
        final Page parentPage = engine.getManager( PageManager.class ).getPage( "PocParent" );

        final Context context = Wiki.context().create( engine, parentPage );
        final PluginManager pluginMgr = engine.getManager( PluginManager.class );
        final RenderingManager renderMgr = engine.getManager( RenderingManager.class );
        final String rendered = renderMgr.getHTML( context, parentPage );

        Assertions.assertNotNull( rendered );
        final Page freshParent = engine.getManager( PageManager.class ).getPage( "PocParent" );
        final AclEntry viewerEntry = freshParent.getAcl() != null
                ? freshParent.getAcl().getAclEntry( new WikiPrincipal( "testviewer" ) )
                : null;

        if( viewerEntry != null ) {
            Assertions.assertFalse(
                    viewerEntry.checkPermission( PermissionFactory.getPagePermission( freshParent, PagePermission.EDIT_ACTION ) ),
                    "Included page ACL must not grant edit on parent page"
            );
        }
    }

    @Test
    void testGetEditedTextRejectsUnprivilegedGetParameterInjection() throws Exception {
        engine.saveText( "PocPreviewV1", "[{ALLOW view All}]\nSECRET CONTENT" );
        final Page page = engine.getManager( PageManager.class ).getPage( "PocPreviewV1" );
        final Context context = Wiki.context().create( engine, page );

        final HttpServletRequest request = Mockito.mock( HttpServletRequest.class );
        final HttpSession session = Mockito.mock( HttpSession.class );
        final PageContext pageContext = Mockito.mock( PageContext.class );

        Mockito.when( request.getMethod() ).thenReturn( "GET" );
        Mockito.when( request.getParameter( EditorManager.REQ_EDITEDTEXT ) ).thenReturn( "[{ALLOW edit All}]" );
        Mockito.when( pageContext.getRequest() ).thenReturn( request );
        Mockito.when( pageContext.getSession() ).thenReturn( session );
        Mockito.when( pageContext.getAttribute( Context.ATTR_CONTEXT ) ).thenReturn( context );
        Mockito.when( session.getAttribute( EditorManager.REQ_EDITEDTEXT ) ).thenReturn( null );

        final String edited = EditorManager.getEditedText( pageContext );
        Assertions.assertNull( edited, "Unprivileged GET _editedtext must not be returned as edited text" );

        final AuthorizationManager authMgr = engine.getManager( AuthorizationManager.class );
        final boolean canEdit = authMgr.checkPermission( context.getWikiSession(),
                PermissionFactory.getPagePermission( page, PagePermission.EDIT_ACTION ) );
        Assertions.assertFalse( canEdit, "Anonymous user must still lack edit permission" );
    }
}