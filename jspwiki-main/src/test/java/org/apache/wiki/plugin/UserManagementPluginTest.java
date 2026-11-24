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
package org.apache.wiki.plugin;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.Principal;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.ContextEnum;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.core.Session;
import org.apache.wiki.api.spi.ContextDSL;
import org.apache.wiki.api.spi.Wiki;
import org.apache.wiki.auth.AuthenticationManager;
import org.apache.wiki.auth.AuthorizationManager;
import org.apache.wiki.auth.NoSuchPrincipalException;
import org.apache.wiki.auth.SessionMonitor;
import org.apache.wiki.auth.UserManager;
import org.apache.wiki.auth.permissions.AllPermission;
import org.apache.wiki.auth.user.UserDatabase;
import org.apache.wiki.auth.user.UserProfile;
import org.apache.wiki.event.WikiSecurityEvent;
import org.apache.wiki.i18n.InternationalizationManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ai generated tests
 */
@ExtendWith(MockitoExtension.class)
class UserManagementPluginTest {

    private static final String APP_NAME = "JSPWiki";

    @Mock private Engine engine;
    @Mock private Context context;
    @Mock private Session session;
    @Mock private AuthorizationManager authorizationManager;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserManager userManager;
    @Mock private UserDatabase userDatabase;
    @Mock private InternationalizationManager i18n;
    @Mock private SessionMonitor sessionMonitor;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;

    private MockedStatic<Wiki> wikiStaticMock;
    private MockedStatic<SessionMonitor> sessionMonitorStaticMock;
    private ContextDSL contextDsl;
    private StringWriter responseWriter;
    private UserManagementPlugin plugin;

    @BeforeEach
    void setUp() throws Exception {
        plugin = new UserManagementPlugin(engine);

        lenient().when(engine.getApplicationName()).thenReturn(APP_NAME);
        lenient().when(engine.getManager(AuthorizationManager.class)).thenReturn(authorizationManager);
        lenient().when(engine.getManager(AuthenticationManager.class)).thenReturn(authenticationManager);
        lenient().when(engine.getManager(UserManager.class)).thenReturn(userManager);
        lenient().when(engine.getManager(InternationalizationManager.class)).thenReturn(i18n);

        lenient().when(context.getEngine()).thenReturn(engine);
        lenient().when(context.getWikiSession()).thenReturn(session);
        lenient().when(session.getLocale()).thenReturn(Locale.ENGLISH);

        lenient().when(userManager.getUserDatabase()).thenReturn(userDatabase);
        lenient().when(authorizationManager.checkPermission(eq(session), any(AllPermission.class))).thenReturn(true);
        lenient().when(authenticationManager.isContainerAuthenticated()).thenReturn(false);

        // Echo i18n keys by default so assertions are readable and deterministic
        lenient().when(i18n.get(eq(InternationalizationManager.CORE_BUNDLE), any(Locale.class), anyString()))
                .thenAnswer(inv -> inv.getArgument(2));

        responseWriter = new StringWriter();
        lenient().when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));

        contextDsl = mock(ContextDSL.class);
        lenient().when(contextDsl.create(engine, request, ContextEnum.PAGE_VIEW.getRequestContext()))
                .thenReturn(context);

        wikiStaticMock = mockStatic(Wiki.class);
        wikiStaticMock.when(Wiki::context).thenReturn(contextDsl);

        sessionMonitorStaticMock = mockStatic(SessionMonitor.class);
        sessionMonitorStaticMock.when(() -> SessionMonitor.getInstance(engine)).thenReturn(sessionMonitor);
        lenient().when(sessionMonitor.findOtherSessionsByUsername(anyString())).thenReturn(List.of());
    }

    @AfterEach
    void tearDown() {
        sessionMonitorStaticMock.close();
        wikiStaticMock.close();
    }

    private UserProfile createProfile(String loginName, String wikiName, boolean locked, boolean pwExpired) {
        UserProfile profile = mock(UserProfile.class);
        lenient().when(profile.getLoginName()).thenReturn(loginName);
        lenient().when(profile.getWikiName()).thenReturn(wikiName);
        lenient().when(profile.getEmail()).thenReturn(loginName + "@example.com");
        lenient().when(profile.getFullname()).thenReturn("Full " + wikiName);
        lenient().when(profile.isLocked()).thenReturn(locked);
        lenient().when(profile.getCreated()).thenReturn(new Date(1_700_000_000_000L));
        lenient().when(profile.getLastModified()).thenReturn(new Date(1_700_000_100_000L));
        lenient().when(profile.getPasswordChanged()).thenReturn(new Date(1_700_000_200_000L));
        lenient().when(profile.getAttributes()).thenReturn(Map.of(
                UserProfile.ATTR_CURRENT_LOGIN_TIMESTAMP, 1_700_000_300_000L,
                UserProfile.ATTR_CURRENT_LOGIN_IP, "127.0.0.1"
        ));
        lenient().when(authenticationManager.isPasswordExpiredByLoginName(loginName)).thenReturn(pwExpired);
        return profile;
    }

    @Test
    void metadataAndIdHelpers() {
        assertEquals("User Management", plugin.getDisplayName(Locale.ENGLISH));
        assertEquals(UserManagementPlugin.PATH, plugin.getServletMapping());

        String id1 = UserManagementPlugin.idCssSafeId();
        String id2 = UserManagementPlugin.idCssSafeId();
        assertTrue(id1.startsWith("id-"));
        assertFalse(id1.substring(3).contains("-"));
        assertNotEquals(id1, id2);
    }

    @Nested
    class ExecuteTests {

        @Test
        void returnsEmptyStringWhenUserLacksAllPermission() throws Exception {
            when(authorizationManager.checkPermission(eq(session), any(AllPermission.class))).thenReturn(false);

            String html = plugin.execute(context, Map.of());

            assertEquals("", html);
            verify(userManager, never()).getUserDatabase();
        }

        @Test
        void returnsNotSupportedMessageWhenContainerAuthenticated() throws Exception {
            when(authenticationManager.isContainerAuthenticated()).thenReturn(true);

            String html = plugin.execute(context, Map.of());

            assertTrue(html.contains("action.notsupported"));
            verify(userManager, never()).getUserDatabase();
        }

        @Test
        void rendersUserTableAndEscapesHtmlAndScriptValues() throws Exception {
            Principal principal = () -> "Alice<script>";
            UserProfile alice = createProfile("alice", "Alice<script>", false, false);
            when(userDatabase.getWikiNames()).thenReturn(new Principal[]{principal});
            when(userDatabase.findByWikiName("Alice<script>")).thenReturn(alice);

            String html = plugin.execute(context, Map.of());

            assertTrue(html.contains("<table class='wikitable'>"));
            assertTrue(html.contains("userinfo.status.ok"));
            assertTrue(html.contains("userinfo.actions.expireall"));
            assertFalse(html.contains("<td title=\"userinfo.wikiname: Alice<script>"));
            assertTrue(html.contains("Alice&lt;script&gt;"));
        }

        @Test
        void reflectsLockedAndPasswordExpiredStates() throws Exception {
            Principal p1 = () -> "LockedUser";
            Principal p2 = () -> "ExpiredUser";
            UserProfile lockedProfile = createProfile("lockeduser", "LockedUser", true, false);
            UserProfile expiredProfile = createProfile("expireduser", "ExpiredUser", false, true);

            when(userDatabase.getWikiNames()).thenReturn(new Principal[]{p1, p2});
            when(userDatabase.findByWikiName("LockedUser")).thenReturn(lockedProfile);
            when(userDatabase.findByWikiName("ExpiredUser")).thenReturn(expiredProfile);

            String html = plugin.execute(context, Map.of());

            assertNotNull(html);
            assertTrue(html.contains("userinfo.status.locked"));
            assertTrue(html.contains("userinfo.status.pwexpired"));
        }
    }

    @Nested
    class ServiceTests {

        @Test
        void deniesUnauthorizedRequestWith400AndFiresAccessDeniedEvent() throws Exception {
            when(authorizationManager.checkPermission(eq(session), any(AllPermission.class))).thenReturn(false);

            plugin.service(request, response, "lock", List.of("Alice"));

            verify(response).setStatus(400);
            assertTrue(responseWriter.toString().contains("access.deny"));
            verify(userManager).fireEvent(eq(WikiSecurityEvent.ACCESS_DENIED), eq(session), any());
            verify(userDatabase, never()).save(any());
        }

        @Test
        void rejectsContainerAuthenticatedRequestWith400() throws Exception {
            when(authenticationManager.isContainerAuthenticated()).thenReturn(true);

            plugin.service(request, response, "lock", List.of("Alice"));

            verify(response).setStatus(400);
            assertTrue(responseWriter.toString().contains("action.notsupported"));
        }

        @Test
        void returns400WhenTargetUserDoesNotExist() throws Exception {
            when(userDatabase.findByWikiName("Unknown")).thenThrow(new NoSuchPrincipalException("missing"));

            plugin.service(request, response, "lock", List.of("Unknown"));

            verify(response).setStatus(400);
            assertTrue(responseWriter.toString().contains("errors.unknownuser"));
        }

        @Test
        void locksUnlockedUserSavesProfileAndInvalidatesOtherSessions() throws Exception {
            UserProfile alice = createProfile("alice", "Alice", false, false);
            Session otherSession = mock(Session.class);
            when(userDatabase.findByWikiName("Alice")).thenReturn(alice);
            when(sessionMonitor.findOtherSessionsByUsername("alice")).thenReturn(List.of(otherSession));

            plugin.service(request, response, "lock", List.of("Alice", "5000"));

            verify(alice).setLockExpiry(any(Date.class));
            verify(userDatabase).save(alice);
            verify(userManager).fireEvent(eq(WikiSecurityEvent.LOGIN_LOCKED), eq(session), eq(alice));
            verify(otherSession).invalidate();
            verify(response, never()).setStatus(400);
            verify(response, never()).setStatus(500);
        }

        @Test
        void returns400WhenLockingAlreadyLockedUser() throws Exception {
            UserProfile alice = createProfile("alice", "Alice", true, false);
            when(userDatabase.findByWikiName("Alice")).thenReturn(alice);

            plugin.service(request, response, "lock", List.of("Alice"));

            verify(response).setStatus(400);
            assertTrue(responseWriter.toString().contains("errors.accountalreadylocked"));
            verify(userDatabase, never()).save(any());
        }

        @Test
        void unlocksLockedUserAndClearsLockExpiry() throws Exception {
            UserProfile alice = createProfile("alice", "Alice", true, false);
            when(userDatabase.findByWikiName("Alice")).thenReturn(alice);

            plugin.service(request, response, "unlock", List.of("Alice"));

            verify(alice).setLockExpiry(null);
            verify(userDatabase).save(alice);
            verify(userManager).fireEvent(eq(WikiSecurityEvent.LOGIN_UNLOCKED), eq(session), eq(engine));
        }

        @Test
        void returns400WhenUnlockingAlreadyUnlockedUser() throws Exception {
            UserProfile alice = createProfile("alice", "Alice", false, false);
            when(userDatabase.findByWikiName("Alice")).thenReturn(alice);

            plugin.service(request, response, "unlock", List.of("Alice"));

            verify(response).setStatus(400);
            assertTrue(responseWriter.toString().contains("errors.accountalreadyunlocked"));
            verify(userDatabase, never()).save(any());
        }

        @Test
        void expiresSingleUserPasswordAndInvalidatesOtherSessions() throws Exception {
            UserProfile alice = createProfile("alice", "Alice", false, false);
            Session otherSession = mock(Session.class);
            when(userDatabase.findByWikiName("Alice")).thenReturn(alice);
            when(sessionMonitor.findOtherSessionsByUsername("alice")).thenReturn(List.of(otherSession));

            plugin.service(request, response, "expire", List.of("Alice"));

            verify(alice).setPasswordChanged(new Date(0));
            verify(userDatabase).save(alice);
            verify(otherSession).invalidate();
            verify(userManager).fireEvent(eq(WikiSecurityEvent.LOGIN_CREDENTIAL_EXPIRED), eq(session), eq(alice));
        }

        @Test
        void expiresAllUsersPasswords() throws Exception {
            Principal p1 = () -> "Alice";
            Principal p2 = () -> "Bob";
            UserProfile alice = createProfile("alice", "Alice", false, false);
            UserProfile bob = createProfile("bob", "Bob", false, false);
            when(userDatabase.getWikiNames()).thenReturn(new Principal[]{p1, p2});
            when(userDatabase.findByWikiName("Alice")).thenReturn(alice);
            when(userDatabase.findByWikiName("Bob")).thenReturn(bob);

            plugin.service(request, response, "expireAll", List.of());

            verify(alice).setPasswordChanged(new Date(0));
            verify(bob).setPasswordChanged(new Date(0));
            verify(userDatabase).save(alice);
            verify(userDatabase).save(bob);
        }

        @Test
        void deletesUserByLoginNameAndFiresDeletedEvent() throws Exception {
            UserProfile alice = createProfile("alice", "Alice", false, false);
            when(userDatabase.findByWikiName("Alice")).thenReturn(alice);

            plugin.service(request, response, "delete", List.of("Alice"));

            verify(userDatabase).deleteByLoginName("alice");
            verify(userManager).fireEvent(eq(WikiSecurityEvent.LOGIN_DELETED), eq(session), eq(alice));
        }

        @Test
        void returns400ForUnknownAction() throws Exception {
            plugin.service(request, response, "unknownAction", List.of());

            verify(response).setStatus(400);
            assertTrue(responseWriter.toString().contains("errors.badrequest"));
        }
    }
}