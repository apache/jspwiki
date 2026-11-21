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
package org.apache.wiki.providers;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.core.Page;
import org.apache.wiki.api.exceptions.ProviderException;
import org.apache.wiki.api.providers.PageProvider;
import org.apache.wiki.api.search.QueryItem;
import org.apache.wiki.api.search.SearchResult;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Stream;
import org.apache.wiki.TestEngine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;
import org.junit.platform.commons.util.StringUtils;

@ExtendWith(JdbcPageProviderTest.DatabaseMatrixProvider.class)
class JdbcPageProviderTest {

    private static final String LOCAL_CONFIG_RESOURCE = "jdbc-test-local.properties";
    private static final String CONFIG_PATH_SYS_PROP = "jspwiki.test.jdbc.config";
    private static final String CONFIG_PATH_ENV_VAR = "JSPWIKI_TEST_JDBC_CONFIG";

    record DbTarget(String name, String url, String user, String password, String driver, boolean sqlite) {

        boolean isConfigured() {
            return sqlite || StringUtils.isNotBlank(url);
        }
    }

    static class TestContext {

        final DbTarget target;
        final Engine engine;
        final JdbcPageProvider provider;
        private Connection sqliteKeepAlive;
        private Path tempSqliteFile;

        TestContext(final DbTarget target) throws Exception {
            this.engine = TestEngine.build();
            Mockito.mock(Engine.class);
            //Mockito.when(this.engine.getApplicationName()).thenReturn("JSPWiki");
            if (target.sqlite()) {
                this.tempSqliteFile = Files.createTempFile("jspwiki-jdbc-test-", ".db");
                final String sqliteUrl = "jdbc:sqlite:" + this.tempSqliteFile.toAbsolutePath();
                this.target = new DbTarget(
                        target.name(), sqliteUrl, null, null, target.driver(), true);
                this.provider = initProvider(this.target, true);
            } else {
                this.target = target;
                this.provider = initProvider(target, true);
                cleanExternalTables();
            }
        }

        JdbcPageProvider initProvider(final DbTarget db, final boolean searchPreFilter) throws Exception {
            final Properties props = new Properties();
            props.setProperty(JdbcPageProvider.PROP_JDBC_URL, db.url());
            if (StringUtils.isNotBlank(db.user())) {
                props.setProperty(JdbcPageProvider.PROP_JDBC_USER, db.user());
            }
            if (StringUtils.isNotBlank(db.password())) {
                props.setProperty(JdbcPageProvider.PROP_JDBC_PASSWORD, db.password());
            }
            if (StringUtils.isNotBlank(db.driver())) {
                props.setProperty(JdbcPageProvider.PROP_JDBC_DRIVER, db.driver());
            }
            props.setProperty(JdbcPageProvider.PROP_AUTO_CREATE_SCHEMA, "true");
            props.setProperty(JdbcPageProvider.PROP_SEARCH_PREFILTER, Boolean.toString(searchPreFilter));

            final JdbcPageProvider p = new JdbcPageProvider();
            p.initialize(this.engine, props);
            return p;
        }

        private void cleanExternalTables() throws Exception {
            try (final Connection conn = openRawConnection(); final Statement st = conn.createStatement()) {
                st.executeUpdate("DELETE FROM wiki_page_properties");
                st.executeUpdate("DELETE FROM wiki_page_versions");
                st.executeUpdate("DELETE FROM wiki_pages");
            }
        }

        Connection openRawConnection() throws Exception {
            if (StringUtils.isNotBlank(target.driver())) {
                Class.forName(target.driver());
            }
            if (StringUtils.isNotBlank(target.user())) {
                return DriverManager.getConnection(target.url(), target.user(), target.password());
            }
            return DriverManager.getConnection(target.url());
        }

        void close() throws Exception {
            if (sqliteKeepAlive != null && !sqliteKeepAlive.isClosed()) {
                sqliteKeepAlive.close();
            }
        }
    }

    static class DatabaseMatrixProvider implements TestTemplateInvocationContextProvider {

        @Override
        public boolean supportsTestTemplate(final ExtensionContext context) {
            return true;
        }

        @Override
        public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(
                final ExtensionContext context) {
            final Properties localProps = loadLocalConfig();
            final List<DbTarget> targets = new ArrayList<>();

            targets.add(new DbTarget("SQLite", "jdbc:sqlite::memory:", null, null, null, true));

            addIfConfigured(targets, localProps, "PostgreSQL", "pg", "PG");
            addIfConfigured(targets, localProps, "MariaDB", "mariadb", "MARIADB");
            addIfConfigured(targets, localProps, "MSSQL", "mssql", "MSSQL");

            return targets.stream().map(this::invocationContextFor);
        }

        private void addIfConfigured(final List<DbTarget> targets, final Properties localProps,
                final String displayName, final String propPrefix, final String envPrefix) {
            final String url = resolve(localProps, propPrefix + ".url", "JSPWIKI_TEST_" + envPrefix + "_URL");
            if (StringUtils.isBlank(url)) {
                return;
            }
            final String user = resolve(localProps, propPrefix + ".user", "JSPWIKI_TEST_" + envPrefix + "_USER");
            final String password = resolve(localProps, propPrefix + ".password", "JSPWIKI_TEST_" + envPrefix + "_PASSWORD");
            final String driver = resolve(localProps, propPrefix + ".driver", "JSPWIKI_TEST_" + envPrefix + "_DRIVER");
            targets.add(new DbTarget(displayName, url, user, password, driver, false));
        }

        private static String resolve(final Properties localProps, final String propKey, final String envKey) {
            final String envVal = System.getenv(envKey);
            if (StringUtils.isNotBlank(envVal)) {
                return envVal.trim();
            }
            final String propVal = localProps.getProperty(propKey);
            return StringUtils.isNotBlank(propVal) ? propVal.trim() : null;
        }

        private static Properties loadLocalConfig() {
            final Properties props = new Properties();

            final String customPath = firstNonBlank(
                    System.getProperty(CONFIG_PATH_SYS_PROP),
                    System.getenv(CONFIG_PATH_ENV_VAR));
            if (StringUtils.isNotBlank(customPath)) {
                final Path p = Path.of(customPath);
                if (Files.isRegularFile(p)) {
                    try (final InputStream in = Files.newInputStream(p)) {
                        props.load(in);
                        return props;
                    } catch (final IOException e) {
                        throw new IllegalStateException("Failed reading JDBC test config at " + p, e);
                    }
                }
            }

            try (final InputStream in = DatabaseMatrixProvider.class
                    .getClassLoader().getResourceAsStream(LOCAL_CONFIG_RESOURCE)) {
                if (in != null) {
                    props.load(in);
                }
            } catch (final IOException e) {
                throw new IllegalStateException("Failed reading classpath resource " + LOCAL_CONFIG_RESOURCE, e);
            }
            return props;
        }

        private static String firstNonBlank(final String a, final String b) {
            return StringUtils.isNotBlank(a) ? a : b;
        }

        private TestTemplateInvocationContext invocationContextFor(final DbTarget target) {
            return new TestTemplateInvocationContext() {
                @Override
                public String getDisplayName(final int invocationIndex) {
                    return target.name();
                }

                @Override
                public List<Extension> getAdditionalExtensions() {
                    final String ctxKey = TestContext.class.getName() + "#" + target.name();

                    return List.of(
                            new BeforeEachCallback() {
                        @Override
                        public void beforeEach(final ExtensionContext extCtx) throws Exception {
                            extCtx.getStore(ExtensionContext.Namespace.create(JdbcPageProviderTest.class))
                                    .put(ctxKey, new TestContext(target));
                        }
                    },
                            new AfterEachCallback() {
                        @Override
                        public void afterEach(final ExtensionContext extCtx) throws Exception {
                            final TestContext tc = extCtx.getStore(
                                    ExtensionContext.Namespace.create(JdbcPageProviderTest.class))
                                    .remove(ctxKey, TestContext.class);
                            if (tc != null) {
                                tc.close();
                            }
                        }
                    },
                            new ParameterResolver() {
                        @Override
                        public boolean supportsParameter(final ParameterContext paramCtx,
                                final ExtensionContext extCtx) {
                            return paramCtx.getParameter().getType() == TestContext.class;
                        }

                        @Override
                        public Object resolveParameter(final ParameterContext paramCtx,
                                final ExtensionContext extCtx) {
                            return extCtx.getStore(ExtensionContext.Namespace.create(JdbcPageProviderTest.class))
                                    .get(ctxKey, TestContext.class);
                        }
                    }
                    );
                }
            };
        }
    }

    private JdbcPageProvider createAndInitProvider(final TestContext ctx, final boolean searchPreFilter) throws Exception {
        DbTarget db = ctx.target;
        final Properties props = new Properties();
        props.setProperty(JdbcPageProvider.PROP_JDBC_URL, Objects.requireNonNull(db.url(), "url must not be null"));
        if (StringUtils.isNotBlank(db.user())) {
            props.setProperty(JdbcPageProvider.PROP_JDBC_USER, db.user());
        }
        if (StringUtils.isNotBlank(db.password())) {
            props.setProperty(JdbcPageProvider.PROP_JDBC_PASSWORD, db.password());
        }
        if (StringUtils.isNotBlank(db.driver())) {
            props.setProperty(JdbcPageProvider.PROP_JDBC_DRIVER, db.driver());
        }
        props.setProperty(JdbcPageProvider.PROP_AUTO_CREATE_SCHEMA, "true");
        props.setProperty(JdbcPageProvider.PROP_SEARCH_PREFILTER, Boolean.toString(searchPreFilter));

        final JdbcPageProvider provider = new JdbcPageProvider();
        provider.initialize(ctx.engine, props);
        return provider;
    }

    private Page newPage(final String name, final String author, final String changeNote, final Date modified) {

        final Page page = Mockito.mock(Page.class);
        final java.util.Map<String, Object> attrs = new java.util.HashMap<>();
        final String[] holder = new String[1];
        holder[0] = author;

        Mockito.when(page.getName()).thenReturn(name);
        Mockito.when(page.getAuthor()).thenAnswer(inv -> holder[0]);
        Mockito.doAnswer(inv -> {
            holder[0] = inv.getArgument(0);
            return null;
        }).when(page).setAuthor(Mockito.anyString());

        final int[] ver = new int[1];
        Mockito.when(page.getVersion()).thenAnswer(inv -> ver[0]);
        Mockito.doAnswer(inv -> {
            ver[0] = inv.getArgument(0);
            return null;
        }).when(page).setVersion(Mockito.anyInt());

        final Date[] mod = new Date[]{modified};
        Mockito.when(page.getLastModified()).thenAnswer(inv -> mod[0]);
        Mockito.doAnswer(inv -> {
            mod[0] = inv.getArgument(0);
            return null;
        }).when(page).setLastModified(Mockito.any(Date.class));

        if (changeNote != null) {
            attrs.put(Page.CHANGENOTE, changeNote);
        }
        Mockito.when(page.getAttributes()).thenReturn(attrs);
        Mockito.when(page.getAttribute(Mockito.anyString())).thenAnswer(inv -> attrs.get(inv.getArgument(0)));
        Mockito.doAnswer(inv -> {
            attrs.put(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(page).setAttribute(Mockito.anyString(), Mockito.any());

        return page;
    }

    @TestTemplate
    void testPutGetAndVersioning(final TestContext ctx) throws Exception {
        final JdbcPageProvider m_provider = ctx.provider;
        final Page p1 = newPage("Main", "alice", "initial draft", new Date(1_700_000_000_000L));
        m_provider.putPageText(p1, "Version 1 content");

        final Page p2 = newPage("Main", "bob", "second edit", new Date(1_700_000_100_000L));
        m_provider.putPageText(p2, "Version 2 content");

        assertTrue(m_provider.pageExists("Main"));
        assertTrue(m_provider.pageExists("Main", 1));
        assertTrue(m_provider.pageExists("Main", 2));
        assertFalse(m_provider.pageExists("Main", 3));
        assertEquals(1, m_provider.getPageCount());

        assertEquals("Version 1 content", m_provider.getPageText("Main", 1));
        assertEquals("Version 2 content", m_provider.getPageText("Main", 2));
        assertEquals("Version 2 content", m_provider.getPageText("Main", PageProvider.LATEST_VERSION));

        final Page info1 = m_provider.getPageInfo("Main", 1);
        assertNotNull(info1);
        assertEquals("alice", info1.getAuthor());
        assertEquals("initial draft", info1.getAttribute(Page.CHANGENOTE));

        final Page info2 = m_provider.getPageInfo("Main", PageProvider.LATEST_VERSION);
        assertNotNull(info2);
        assertEquals("bob", info2.getAuthor());
        assertEquals("second edit", info2.getAttribute(Page.CHANGENOTE));

        final List<Page> history = m_provider.getVersionHistory("Main");
        assertEquals(2, history.size());
        assertEquals(2, history.get(0).getVersion());
        assertEquals(1, history.get(1).getVersion());

        assertThrows(NoSuchVersionException.class, () -> m_provider.getPageText("Main", 99));
    }

    @TestTemplate
    void testCustomPropertiesRoundTripAndValidation(final TestContext ctx) throws Exception {
        final JdbcPageProvider m_provider = ctx.provider;
        final Page p = newPage("PropPage", "alice", null, new Date(1_700_000_000_000L));
        p.setAttribute("@category", "homelab");
        p.setAttribute("@priority", "1");
        p.setAttribute("nonCustomAttr", "ignored");

        m_provider.putPageText(p, "Body");

        final Page loaded = m_provider.getPageInfo("PropPage", PageProvider.LATEST_VERSION);
        assertNotNull(loaded);
        assertEquals("homelab", loaded.getAttribute("@category"));
        assertEquals("1", loaded.getAttribute("@priority"));
        assertNull(loaded.getAttribute("nonCustomAttr"));

        // Non-ASCII value in custom property should fail validation
        final Page bad = newPage("BadPropPage", "alice", null, new Date(1_700_000_000_000L));
        bad.setAttribute("@emoji", "\u2603");
        assertThrows(ProviderException.class, () -> m_provider.putPageText(bad, "Body"));
    }

    @TestTemplate
    void testGetAllChangedSince(final TestContext ctx) throws Exception {
        final JdbcPageProvider m_provider = ctx.provider;
        m_provider.putPageText(newPage("OldPage", "alice", null, new Date(1_000L)), "old");
        m_provider.putPageText(newPage("MidPage", "alice", null, new Date(5_000L)), "mid");
        m_provider.putPageText(newPage("NewPage", "alice", null, new Date(9_000L)), "new");

        final Collection<Page> changed = m_provider.getAllChangedSince(new Date(5_000L));
        assertEquals(1, changed.size());
        assertEquals("NewPage", changed.iterator().next().getName());
    }

    @TestTemplate
    void testDeleteLatestVersionPromotesPreviousVersion(final TestContext ctx) throws Exception {
        final JdbcPageProvider m_provider = ctx.provider;
        m_provider.putPageText(newPage("Doc", "alice", "v1", new Date(1_000L)), "first");
        m_provider.putPageText(newPage("Doc", "bob", "v2", new Date(2_000L)), "second");

        m_provider.deleteVersion("Doc", 2);

        assertTrue(m_provider.pageExists("Doc"));
        assertFalse(m_provider.pageExists("Doc", 2));
        assertTrue(m_provider.pageExists("Doc", 1));
        assertEquals("first", m_provider.getPageText("Doc", PageProvider.LATEST_VERSION));
        assertEquals("alice", m_provider.getPageInfo("Doc", PageProvider.LATEST_VERSION).getAuthor());
    }

    @TestTemplate
    void testMovePageAndDeletePage(final TestContext ctx) throws Exception {
        final JdbcPageProvider m_provider = ctx.provider;
        m_provider.putPageText(newPage("DraftNote", "alice", "v1", new Date(1_000L)), "v1 body");
        m_provider.putPageText(newPage("DraftNote", "alice", "v2", new Date(2_000L)), "v2 body");

        m_provider.movePage("DraftNote", "PublishedNote");

        assertFalse(m_provider.pageExists("DraftNote"));
        assertFalse(m_provider.pageExists("DraftNote", 1));
        assertTrue(m_provider.pageExists("PublishedNote"));
        assertTrue(m_provider.pageExists("PublishedNote", 1));
        assertTrue(m_provider.pageExists("PublishedNote", 2));
        assertEquals("v1 body", m_provider.getPageText("PublishedNote", 1));
        assertEquals("v2 body", m_provider.getPageText("PublishedNote", 2));

        m_provider.deletePage("PublishedNote");
        assertFalse(m_provider.pageExists("PublishedNote"));
        assertEquals(0, m_provider.getPageCount());
    }

    @TestTemplate
    void testFindPagesWithWildcardsAndPreFilterToggle(final TestContext ctx) throws Exception {
        final JdbcPageProvider m_provider = ctx.provider;
        m_provider.putPageText(
                newPage("PoolGuide", "alice", null, new Date(1_000L)),
                "RAIDZ1 pool expansion on 100% healthy drives with sub_dir notes");
        m_provider.putPageText(
                newPage("Unrelated", "alice", null, new Date(2_000L)),
                "Completely different content about cooking");

        // Search for literal "100%" (must escape '%' so it doesn't match "100" alone)
        final QueryItem literalPercent = new QueryItem();
        literalPercent.word = "100%";
        literalPercent.type = QueryItem.REQUESTED;

        final Collection<SearchResult> percentMatches = m_provider.findPages(new QueryItem[]{literalPercent});
        assertEquals(1, percentMatches.size());
        assertEquals("PoolGuide", percentMatches.iterator().next().getPage().getName());

        // Search for literal "sub_dir" (must escape '_' so it doesn't match "subXdir")
        final QueryItem literalUnderscore = new QueryItem();
        literalUnderscore.word = "sub_dir";
        literalUnderscore.type = QueryItem.REQUESTED;

        final Collection<SearchResult> underscoreMatches = m_provider.findPages(new QueryItem[]{literalUnderscore});
        assertEquals(1, underscoreMatches.size());
        assertEquals("PoolGuide", underscoreMatches.iterator().next().getPage().getName());

        // Verify identical results with pre-filter disabled
        final JdbcPageProvider noFilterProvider = createAndInitProvider(ctx, false);
        final Collection<SearchResult> noFilterMatches
                = noFilterProvider.findPages(new QueryItem[]{literalPercent});
        assertEquals(1, noFilterMatches.size());
        assertEquals("PoolGuide", noFilterMatches.iterator().next().getPage().getName());
    }

    @TestTemplate
    void testFindPagesCombiningRequiredAndForbiddenTerms(final TestContext ctx) throws Exception {
        final JdbcPageProvider m_provider = ctx.provider;
        m_provider.putPageText(
                newPage("ActiveSetup", "alice", null, new Date(1_000L)),
                "Current homelab guide using ZFS and container networking");
        m_provider.putPageText(
                newPage("LegacySetup", "alice", null, new Date(2_000L)),
                "Old homelab guide marked deprecated for removal");
        m_provider.putPageText(
                newPage("NoMatchPage", "alice", null, new Date(3_000L)),
                "Unrelated notes with deprecated mentioned alone");

        final QueryItem requiredHomelab = new QueryItem();
        requiredHomelab.word = "homelab";
        requiredHomelab.type = QueryItem.REQUIRED;

        final QueryItem forbiddenDeprecated = new QueryItem();
        forbiddenDeprecated.word = "deprecated";
        forbiddenDeprecated.type = QueryItem.FORBIDDEN;

        final QueryItem requestedZfs = new QueryItem();
        requestedZfs.word = "zfs";
        requestedZfs.type = QueryItem.REQUESTED;

        final Collection<SearchResult> matches = m_provider.findPages(
                new QueryItem[]{requiredHomelab, forbiddenDeprecated, requestedZfs});

        assertEquals(1, matches.size());
        assertEquals("ActiveSetup", matches.iterator().next().getPage().getName());
    }

    @TestTemplate
    void testMovePageTransfersVersionsAndProperties(final TestContext ctx) throws Exception {
        final JdbcPageProvider m_provider = ctx.provider;
        final Page v1 = newPage("Scratchpad", "alice", "first draft", new Date(1_000L));
        v1.setAttribute("@status", "draft");
        m_provider.putPageText(v1, "Draft v1 text");

        final Page v2 = newPage("Scratchpad", "bob", "added section", new Date(2_000L));
        v2.setAttribute("@status", "ready");
        m_provider.putPageText(v2, "Draft v2 text");

        m_provider.movePage("Scratchpad", "ArchitectureNotes");

        // Source page and versions should be gone
        assertFalse(m_provider.pageExists("Scratchpad"));
        assertFalse(m_provider.pageExists("Scratchpad", 1));
        assertFalse(m_provider.pageExists("Scratchpad", 2));
        assertNull(m_provider.getPageInfo("Scratchpad", PageProvider.LATEST_VERSION));

        // Destination page and both versions should exist with content and metadata
        assertTrue(m_provider.pageExists("ArchitectureNotes"));
        assertTrue(m_provider.pageExists("ArchitectureNotes", 1));
        assertTrue(m_provider.pageExists("ArchitectureNotes", 2));
        assertEquals("Draft v1 text", m_provider.getPageText("ArchitectureNotes", 1));
        assertEquals("Draft v2 text", m_provider.getPageText("ArchitectureNotes", 2));
        assertEquals("Draft v2 text", m_provider.getPageText("ArchitectureNotes", PageProvider.LATEST_VERSION));

        final Page movedV1 = m_provider.getPageInfo("ArchitectureNotes", 1);
        assertNotNull(movedV1);
        assertEquals("alice", movedV1.getAuthor());
        assertEquals("first draft", movedV1.getAttribute(Page.CHANGENOTE));
        assertEquals("draft", movedV1.getAttribute("@status"));

        final Page movedLatest = m_provider.getPageInfo("ArchitectureNotes", PageProvider.LATEST_VERSION);
        assertNotNull(movedLatest);
        assertEquals("bob", movedLatest.getAuthor());
        assertEquals("added section", movedLatest.getAttribute(Page.CHANGENOTE));
        assertEquals("ready", movedLatest.getAttribute("@status"));

        final List<Page> history = m_provider.getVersionHistory("ArchitectureNotes");
        assertEquals(2, history.size());
        assertEquals(2, history.get(0).getVersion());
        assertEquals(1, history.get(1).getVersion());

        // Moving onto an already-existing page should fail with ProviderException
        m_provider.putPageText(newPage("ExistingTarget", "alice", null, new Date(3_000L)), "keep me");
        assertThrows(ProviderException.class,
                () -> m_provider.movePage("ArchitectureNotes", "ExistingTarget"));
        assertTrue(m_provider.pageExists("ArchitectureNotes"));
        assertEquals("keep me", m_provider.getPageText("ExistingTarget", PageProvider.LATEST_VERSION));
    }

    @TestTemplate
    void testDeletePageRemovesVersionsAndCustomProperties(final TestContext ctx) throws Exception {
        final JdbcPageProvider m_provider = ctx.provider;
        final Page targetV1 = newPage("TempSpec", "alice", "v1 note", new Date(1_000L));
        targetV1.setAttribute("@owner", "alice");
        m_provider.putPageText(targetV1, "Spec v1 content");

        final Page targetV2 = newPage("TempSpec", "bob", "v2 note", new Date(2_000L));
        targetV2.setAttribute("@owner", "bob");
        targetV2.setAttribute("@reviewed", "true");
        m_provider.putPageText(targetV2, "Spec v2 content");

        final Page bystander = newPage("KeepMe", "alice", "kept", new Date(1_500L));
        bystander.setAttribute("@owner", "alice");
        m_provider.putPageText(bystander, "Do not delete");

        assertEquals(2, m_provider.getPageCount());

        m_provider.deletePage("TempSpec");

        assertEquals(1, m_provider.getPageCount());
        assertFalse(m_provider.pageExists("TempSpec"));
        assertFalse(m_provider.pageExists("TempSpec", 1));
        assertFalse(m_provider.pageExists("TempSpec", 2));
        assertNull(m_provider.getPageInfo("TempSpec", PageProvider.LATEST_VERSION));
        assertNull(m_provider.getPageInfo("TempSpec", 1));
        assertTrue(m_provider.getVersionHistory("TempSpec").isEmpty());
        //assertThrows(NoSuchVersionException.class, () -> m_provider.getPageText("TempSpec", 1));
        assertNull(m_provider.getPageText("TempSpec", 1));
        assertNull(m_provider.getPageText("TempSpec", 2));
        assertNull(m_provider.getPageText("TempSpec", PageProvider.LATEST_VERSION));

        // Bystander page and its metadata remain intact
        assertTrue(m_provider.pageExists("KeepMe"));
        assertEquals("Do not delete", m_provider.getPageText("KeepMe", PageProvider.LATEST_VERSION));
        final Page keptInfo = m_provider.getPageInfo("KeepMe", PageProvider.LATEST_VERSION);
        assertNotNull(keptInfo);
        assertEquals("alice", keptInfo.getAttribute("@owner"));

        // Re-creating a page with the same name starts fresh at version 1 with no stale @reviewed attribute
        final Page recreated = newPage("TempSpec", "charlie", "fresh start", new Date(3_000L));
        m_provider.putPageText(recreated, "Fresh content");
        assertEquals(1, m_provider.getVersionHistory("TempSpec").size());
        final Page recreatedInfo = m_provider.getPageInfo("TempSpec", PageProvider.LATEST_VERSION);
        assertNotNull(recreatedInfo);
        assertEquals(1, recreatedInfo.getVersion());
        assertNull(recreatedInfo.getAttribute("@reviewed"));
    }

    @TestTemplate
    void testGetAllPagesOrderingAndLatestMetadata(final TestContext ctx) throws Exception {
        final JdbcPageProvider m_provider = ctx.provider;
        assertTrue(m_provider.getAllPages().isEmpty());

        // Insert out of alphabetical order with multiple versions on some pages
        final Page zulu = newPage("ZuluPage", "alice", "zulu note", new Date(5_000L));
        zulu.setAttribute("@section", "ops");
        m_provider.putPageText(zulu, "Zulu body");

        final Page alphaV1 = newPage("AlphaPage", "alice", "alpha v1", new Date(1_000L));
        alphaV1.setAttribute("@section", "old");
        m_provider.putPageText(alphaV1, "Alpha v1");

        final Page alphaV2 = newPage("AlphaPage", "bob", "alpha v2", new Date(9_000L));
        alphaV2.setAttribute("@section", "core");
        m_provider.putPageText(alphaV2, "Alpha v2");

        final Page main = newPage("Main", "charlie", "home note", new Date(3_000L));
        main.setAttribute("@section", "landing");
        m_provider.putPageText(main, "Welcome");

        final List<Page> allPages = new ArrayList<>(m_provider.getAllPages());

        assertEquals(3, allPages.size());
        assertEquals(List.of("AlphaPage", "Main", "ZuluPage"),
                allPages.stream().map(Page::getName).toList());

        final Page first = allPages.get(0);
        assertEquals(2, first.getVersion());
        assertEquals("bob", first.getAuthor());
        assertEquals("alpha v2", first.getAttribute(Page.CHANGENOTE));
        assertEquals("core", first.getAttribute("@section"));
        assertEquals(new Date(9_000L), first.getLastModified());

        final Page second = allPages.get(1);
        assertEquals(1, second.getVersion());
        assertEquals("charlie", second.getAuthor());
        assertEquals("landing", second.getAttribute("@section"));

        final Page third = allPages.get(2);
        assertEquals(1, third.getVersion());
        assertEquals("alice", third.getAuthor());
        assertEquals("ops", third.getAttribute("@section"));
    }

    @TestTemplate
    void testDeleteNonLatestVersionLeavesLatestAndHistoryIntact(final TestContext ctx) throws Exception {
        final JdbcPageProvider m_provider = ctx.provider;
        final Page v1 = newPage("Runbook", "alice", "v1 draft", new Date(1_000L));
        v1.setAttribute("@env", "staging");
        m_provider.putPageText(v1, "Runbook v1");

        final Page v2 = newPage("Runbook", "bob", "v2 refinement", new Date(2_000L));
        v2.setAttribute("@env", "preprod");
        m_provider.putPageText(v2, "Runbook v2");

        final Page v3 = newPage("Runbook", "charlie", "v3 release", new Date(3_000L));
        v3.setAttribute("@env", "prod");
        m_provider.putPageText(v3, "Runbook v3");

        m_provider.deleteVersion("Runbook", 2);

        assertTrue(m_provider.pageExists("Runbook"));
        assertTrue(m_provider.pageExists("Runbook", 1));
        assertFalse(m_provider.pageExists("Runbook", 2));
        assertTrue(m_provider.pageExists("Runbook", 3));

        assertEquals("Runbook v1", m_provider.getPageText("Runbook", 1));
        assertThrows(NoSuchVersionException.class, () -> m_provider.getPageText("Runbook", 2));
        assertEquals("Runbook v3", m_provider.getPageText("Runbook", 3));
        assertEquals("Runbook v3", m_provider.getPageText("Runbook", PageProvider.LATEST_VERSION));

        final Page latestInfo = m_provider.getPageInfo("Runbook", PageProvider.LATEST_VERSION);
        assertNotNull(latestInfo);
        assertEquals(3, latestInfo.getVersion());
        assertEquals("charlie", latestInfo.getAuthor());
        assertEquals("prod", latestInfo.getAttribute("@env"));
    }

    @TestTemplate
    void testDeleteLatestVersionPromotesHighestRemainingVersion(final TestContext ctx) throws Exception {
        final JdbcPageProvider m_provider = ctx.provider;
        final Page v1 = newPage("Checklist", "alice", "initial", new Date(1_000L));
        v1.setAttribute("@phase", "plan");
        m_provider.putPageText(v1, "Step A");

        final Page v2 = newPage("Checklist", "bob", "expanded", new Date(2_000L));
        v2.setAttribute("@phase", "execute");
        m_provider.putPageText(v2, "Step A + Step B");

        final Page v3 = newPage("Checklist", "charlie", "mistaken edit", new Date(3_000L));
        v3.setAttribute("@phase", "broken");
        m_provider.putPageText(v3, "Accidental overwrite");

        // Delete latest (v3) -> promotes v2 to current page state
        m_provider.deleteVersion("Checklist", PageProvider.LATEST_VERSION);

        assertTrue(m_provider.pageExists("Checklist"));
        assertFalse(m_provider.pageExists("Checklist", 3));
        assertTrue(m_provider.pageExists("Checklist", 2));
        assertTrue(m_provider.pageExists("Checklist", 1));

        assertEquals("Step A + Step B", m_provider.getPageText("Checklist", PageProvider.LATEST_VERSION));
        assertEquals("Step A + Step B", m_provider.getPageText("Checklist", 2));
        assertEquals("Step A", m_provider.getPageText("Checklist", 1));

        final Page promoted = m_provider.getPageInfo("Checklist", PageProvider.LATEST_VERSION);
        assertNotNull(promoted);
        assertEquals(2, promoted.getVersion());
        assertEquals("bob", promoted.getAuthor());
        //assertEquals("expanded", promoted.getAttribute("@phase"));
        assertEquals("execute", promoted.getAttribute("@phase"));
        assertEquals("expanded", promoted.getAttribute(Page.CHANGENOTE));
    }

    @TestTemplate
    void testDeleteOnlyVersionRemovesPageCompletely(final TestContext ctx) throws Exception {
        final JdbcPageProvider m_provider = ctx.provider;
        m_provider.putPageText(newPage("SingleShot", "alice", "only rev", new Date(1_000L)), "Only content");

        m_provider.deleteVersion("SingleShot", 1);

        assertFalse(m_provider.pageExists("SingleShot"));
        assertFalse(m_provider.pageExists("SingleShot", 1));
        assertEquals(0, m_provider.getPageCount());
        assertNull(m_provider.getPageInfo("SingleShot", PageProvider.LATEST_VERSION));
    }
}
