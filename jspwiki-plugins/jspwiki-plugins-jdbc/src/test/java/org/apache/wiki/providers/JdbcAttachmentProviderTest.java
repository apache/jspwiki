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

import org.apache.commons.lang3.StringUtils;
import org.apache.wiki.api.core.Attachment;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.core.Page;
import org.apache.wiki.api.exceptions.NoRequiredPropertyException;
import org.apache.wiki.api.exceptions.ProviderException;
import org.apache.wiki.api.providers.WikiProvider;
import org.apache.wiki.api.spi.Wiki;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
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
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import javax.sql.DataSource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(JdbcAttachmentProviderTest.DatabaseMatrixProvider.class)
class JdbcAttachmentProviderTest {

    private static final String LOCAL_CONFIG_RESOURCE = "jdbc-test-local.properties";
    private static final String CONFIG_PATH_SYS_PROP = "jspwiki.test.jdbc.config";
    private static final String CONFIG_PATH_ENV_VAR = "JSPWIKI_TEST_JDBC_CONFIG";
    private static final String JNDI_NAME = "java:comp/env/jdbc/TestWikiAttachmentsDS";
    private static final String TABLE_NAME = "wiki_attachment_versions";

    private static MockedStatic<Wiki> wikiStaticMock;

    @BeforeAll
    static void initStaticMocks() {
        wikiStaticMock = Mockito.mockStatic(Wiki.class);
        final var contentsManager = mock(org.apache.wiki.api.spi.ContentsDSL.class);
        when(Wiki.contents()).thenReturn(contentsManager);
        when(contentsManager.page(any(Engine.class), anyString())).thenAnswer(inv -> {
            final Page p = mock(Page.class);
            when(p.getName()).thenReturn(inv.getArgument(1));
            return p;
        });
    }

    @AfterAll
    static void closeStaticMocks() {
        if (wikiStaticMock != null) {
            wikiStaticMock.close();
        }
    }

    record DbTarget(String name, String url, String user, String password, String driver, boolean sqlite) {

        boolean isConfigured() {
            return sqlite || StringUtils.isNotBlank(url);
        }
    }

    static class TestContext {

        final DbTarget target;
        final Engine engine;
        final JdbcAttachmentProvider provider;
        private Path tempSqliteFile;

        TestContext(final DbTarget target) throws Exception {
            this.engine = mock(Engine.class);
            if (target.sqlite()) {
                this.tempSqliteFile = Files.createTempFile("jspwiki-att-test-", ".db");
                final String sqliteUrl = "jdbc:sqlite:" + this.tempSqliteFile.toAbsolutePath();
                this.target = new DbTarget(target.name(), sqliteUrl, null, null, target.driver(), true);
            } else {
                this.target = target;
            }

            bindTargetToJndi(this.target);
            this.provider = initProvider(this.target, true, null);

            if (!this.target.sqlite()) {
                cleanExternalTable();
            }
        }

        JdbcAttachmentProvider initProvider(final DbTarget db, final boolean autoCreate, final String disableCache)
                throws Exception {
            bindTargetToJndi(db);
            final Properties props = new Properties();
            props.setProperty(JdbcAttachmentProvider.PROP_DATASOURCE, JNDI_NAME);
            props.setProperty(JdbcAttachmentProvider.PROP_AUTO_CREATE, Boolean.toString(autoCreate));
            if (StringUtils.isNotBlank(disableCache)) {
                props.setProperty(JdbcAttachmentProvider.PROP_DISABLECACHE, disableCache);
            }
            final JdbcAttachmentProvider p = new JdbcAttachmentProvider();
            p.initialize(this.engine, props);
            return p;
        }

        private void bindTargetToJndi(final DbTarget db) throws Exception {
            InMemoryJndiContextFactory.clear();
            InMemoryJndiContextFactory.bind(JNDI_NAME, new DriverManagerDataSource(db));
            System.setProperty(Context.INITIAL_CONTEXT_FACTORY, InMemoryJndiContextFactory.class.getName());
            new InitialContext();
        }

        private void cleanExternalTable() throws Exception {
            try (final Connection conn = openRawConnection(); final Statement st = conn.createStatement()) {
                st.executeUpdate("DELETE FROM " + TABLE_NAME);
            }
        }

        int countRowsForAttachment(final String parentPage, final String fileName) throws Exception {
            final String sql = "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE parent_page = ? AND file_name = ?";
            try (final Connection conn = openRawConnection(); final PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, parentPage);
                ps.setString(2, fileName);
                try (final ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getInt(1) : 0;
                }
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
            if (!target.sqlite()) {
                try {
                    cleanExternalTable();
                } catch (final Exception ignored) {
                }
            }
            InMemoryJndiContextFactory.clear();
            if (tempSqliteFile != null) {
                Files.deleteIfExists(tempSqliteFile);
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

            return targets.stream().filter(DbTarget::isConfigured).map(this::invocationContextFor);
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
                    final var namespace = ExtensionContext.Namespace.create(JdbcAttachmentProviderTest.class);
                    return List.of(
                            (BeforeEachCallback) extCtx
                            -> extCtx.getStore(namespace).put(ctxKey, new TestContext(target)),
                            (AfterEachCallback) extCtx -> {
                                final TestContext tc = extCtx.getStore(namespace).remove(ctxKey, TestContext.class);
                                if (tc != null) {
                                    tc.close();
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
                            return extCtx.getStore(namespace).get(ctxKey, TestContext.class);
                        }
                    }
                    );
                }
            };
        }
    }

    private Page newPage(final String name) {
        final Page p = mock(Page.class);
        when(p.getName()).thenReturn(name);
        return p;
    }

    private Attachment newAttachment(final String parentPage, final String fileName) {
        final Attachment att = mock(Attachment.class);
        final String[] author = {null};
        final int[] version = {WikiProvider.LATEST_VERSION};
        final long[] size = {0L};
        final Date[] lastMod = {null};
        final boolean[] cacheable = {true};
        final ConcurrentHashMap<String, Object> attrs = new ConcurrentHashMap<>();

        when(att.getParentName()).thenReturn(parentPage);
        when(att.getFileName()).thenReturn(fileName);
        when(att.getName()).thenAnswer(i -> parentPage + "/" + fileName);

        when(att.getAuthor()).thenAnswer(i -> author[0]);
        Mockito.doAnswer(i -> {
            author[0] = i.getArgument(0);
            return null;
        }).when(att).setAuthor(any());

        when(att.getVersion()).thenAnswer(i -> version[0]);
        Mockito.doAnswer(i -> {
            version[0] = i.getArgument(0);
            return null;
        }).when(att).setVersion(any(Integer.class));

        when(att.getSize()).thenAnswer(i -> size[0]);
        Mockito.doAnswer(i -> {
            size[0] = i.getArgument(0);
            return null;
        }).when(att).setSize(any(Long.class));

        when(att.getLastModified()).thenAnswer(i -> lastMod[0]);
        Mockito.doAnswer(i -> {
            lastMod[0] = i.getArgument(0);
            return null;
        }).when(att).setLastModified(any());

        when(att.isCacheable()).thenAnswer(i -> cacheable[0]);
        Mockito.doAnswer(i -> {
            cacheable[0] = i.getArgument(0);
            return null;
        }).when(att).setCacheable(any(Boolean.class));

        when(att.getAttribute(anyString())).thenAnswer(i -> attrs.get(i.getArgument(0)));
        Mockito.doAnswer(i -> {
            final Object v = i.getArgument(1);
            if (v == null) {
                attrs.remove(i.getArgument(0));
            } else {
                attrs.put(i.getArgument(0), v);
            }
            return null;
        }).when(att).setAttribute(anyString(), any());

        return att;
    }

    @Test
    void initializeMissingDataSourceThrows() {
        final JdbcAttachmentProvider p = new JdbcAttachmentProvider();
        assertThrows(NoRequiredPropertyException.class, () -> p.initialize(mock(Engine.class), new Properties()));
    }

    @TestTemplate
    void putAndGetAttachmentVersioningAndMetadata(final TestContext ctx) throws Exception {
        final byte[] v1Bytes = "version one content".getBytes(StandardCharsets.UTF_8);
        final byte[] v2Bytes = "version two updated content".getBytes(StandardCharsets.UTF_8);

        final Attachment upload1 = newAttachment("Main", "notes.txt");
        upload1.setAuthor("alice");
        upload1.setAttribute(Page.CHANGENOTE, "initial draft");
        ctx.provider.putAttachmentData(upload1, new ByteArrayInputStream(v1Bytes));
        assertEquals(1, upload1.getVersion());
        assertEquals(v1Bytes.length, upload1.getSize());

        final Attachment upload2 = newAttachment("Main", "notes.txt");
        upload2.setAuthor("bob");
        upload2.setAttribute(Page.CHANGENOTE, "added details");
        ctx.provider.putAttachmentData(upload2, new ByteArrayInputStream(v2Bytes));
        assertEquals(2, upload2.getVersion());
        assertEquals(v2Bytes.length, upload2.getSize());

        assertEquals(2, ctx.countRowsForAttachment("Main", "notes.txt"));

        final Attachment latestReq = newAttachment("Main", "notes.txt");
        latestReq.setVersion(WikiProvider.LATEST_VERSION);
        try (InputStream in = ctx.provider.getAttachmentData(latestReq)) {
            assertArrayEquals(v2Bytes, in.readAllBytes());
        }

        final Attachment v1Req = newAttachment("Main", "notes.txt");
        v1Req.setVersion(1);
        try (InputStream in = ctx.provider.getAttachmentData(v1Req)) {
            assertArrayEquals(v1Bytes, in.readAllBytes());
        }

        final Attachment infoV1 = ctx.provider.getAttachmentInfo(newPage("Main"), "notes.txt", 1);
        assertNotNull(infoV1);
        assertEquals("alice", infoV1.getAuthor());
        assertEquals("initial draft", infoV1.getAttribute(Page.CHANGENOTE));
        assertEquals(v1Bytes.length, infoV1.getSize());

        final List<Attachment> history = ctx.provider.getVersionHistory(upload2);
        assertEquals(2, history.size());
        assertEquals(2, history.get(0).getVersion());
        assertEquals(1, history.get(1).getVersion());
    }

    @TestTemplate
    void listAttachmentsAndListAllChanged(final TestContext ctx) throws Exception {
        final Date beforeAll = new Date(System.currentTimeMillis() - 10_000L);

        final Attachment a1 = newAttachment("PageA", "doc.pdf");
        ctx.provider.putAttachmentData(a1, new ByteArrayInputStream("pdf1".getBytes(StandardCharsets.UTF_8)));

        final Attachment a2 = newAttachment("PageA", "doc.pdf");
        ctx.provider.putAttachmentData(a2, new ByteArrayInputStream("pdf2".getBytes(StandardCharsets.UTF_8)));

        final Attachment b1 = newAttachment("PageB", "img.png");
        ctx.provider.putAttachmentData(b1, new ByteArrayInputStream("png1".getBytes(StandardCharsets.UTF_8)));

        final List<Attachment> pageAAtts = ctx.provider.listAttachments(newPage("PageA"));
        assertEquals(1, pageAAtts.size());
        assertEquals("doc.pdf", pageAAtts.get(0).getFileName());
        assertEquals(2, pageAAtts.get(0).getVersion());

        final List<Attachment> changed = ctx.provider.listAllChanged(beforeAll);
        assertEquals(2, changed.size());

        final Date afterAll = new Date(System.currentTimeMillis() + 10_000L);
        assertTrue(ctx.provider.listAllChanged(afterAll).isEmpty());
    }

    @TestTemplate
    void deleteVersionDeleteAttachmentAndMovePage(final TestContext ctx) throws Exception {
        final Attachment v1 = newAttachment("OldPage", "spec.md");
        ctx.provider.putAttachmentData(v1, new ByteArrayInputStream("v1".getBytes(StandardCharsets.UTF_8)));

        final Attachment v2 = newAttachment("OldPage", "spec.md");
        ctx.provider.putAttachmentData(v2, new ByteArrayInputStream("v2".getBytes(StandardCharsets.UTF_8)));

        final Attachment delV1 = newAttachment("OldPage", "spec.md");
        delV1.setVersion(1);
        ctx.provider.deleteVersion(delV1);
        assertNull(ctx.provider.getAttachmentInfo(newPage("OldPage"), "spec.md", 1));
        assertNotNull(ctx.provider.getAttachmentInfo(newPage("OldPage"), "spec.md", 2));

        ctx.provider.moveAttachmentsForPage("OldPage", "NewPage");
        assertTrue(ctx.provider.listAttachments(newPage("OldPage")).isEmpty());
        assertEquals(1, ctx.provider.listAttachments(newPage("NewPage")).size());

        ctx.provider.deleteAttachment(newAttachment("NewPage", "spec.md"));
        assertTrue(ctx.provider.listAttachments(newPage("NewPage")).isEmpty());
    }

    @TestTemplate
    void disableCachePatternSetsCacheableFalse(final TestContext ctx) throws Exception {
        final JdbcAttachmentProvider customProvider = ctx.initProvider(ctx.target, true, ".*\\.xlsx");

        final Attachment xlsx = newAttachment("Main", "budget.xlsx");
        customProvider.putAttachmentData(xlsx, new ByteArrayInputStream("data".getBytes(StandardCharsets.UTF_8)));

        final Attachment txt = newAttachment("Main", "readme.txt");
        customProvider.putAttachmentData(txt, new ByteArrayInputStream("text".getBytes(StandardCharsets.UTF_8)));

        final Attachment infoXlsx = customProvider.getAttachmentInfo(newPage("Main"), "budget.xlsx", WikiProvider.LATEST_VERSION);
        final Attachment infoTxt = customProvider.getAttachmentInfo(newPage("Main"), "readme.txt", WikiProvider.LATEST_VERSION);

        assertFalse(infoXlsx.isCacheable());
        assertTrue(infoTxt.isCacheable());
    }

    @TestTemplate
    void missingAttachmentThrowsProviderException(final TestContext ctx) {
        final Attachment missing = newAttachment("NoPage", "missing.bin");
        assertThrows(ProviderException.class, () -> ctx.provider.getAttachmentData(missing));
    }

    private static final class DriverManagerDataSource implements DataSource {

        private final DbTarget target;

        private DriverManagerDataSource(final DbTarget target) {
            this.target = target;
        }

        @Override
        public Connection getConnection() throws SQLException {
            if (StringUtils.isNotBlank(target.driver())) {
                try {
                    Class.forName(target.driver());
                } catch (final ClassNotFoundException e) {
                    throw new SQLException("JDBC driver not found: " + target.driver(), e);
                }
            }
            if (StringUtils.isNotBlank(target.user())) {
                return DriverManager.getConnection(target.url(), target.user(), target.password());
            }
            return DriverManager.getConnection(target.url());
        }

        @Override
        public Connection getConnection(final String username, final String password) throws SQLException {
            return DriverManager.getConnection(target.url(), username, password);
        }

        @Override
        public PrintWriter getLogWriter() {
            return null;
        }

        @Override
        public void setLogWriter(final PrintWriter out) {
        }

        @Override
        public void setLoginTimeout(final int seconds) {
        }

        @Override
        public int getLoginTimeout() {
            return 0;
        }

        @Override
        public java.util.logging.Logger getParentLogger() {
            return java.util.logging.Logger.getGlobal();
        }

        @Override
        public <T> T unwrap(final Class<T> iface) throws SQLException {
            if (iface.isInstance(this)) {
                return iface.cast(this);
            }
            throw new SQLException("Cannot unwrap to " + iface.getName());
        }

        @Override
        public boolean isWrapperFor(final Class<?> iface) {
            return iface.isInstance(this);
        }
    }

    public static class InMemoryJndiContextFactory implements InitialContextFactory {

        private static final ConcurrentHashMap<String, Object> BINDINGS = new ConcurrentHashMap<>();

        static void bind(final String name, final Object obj) {
            BINDINGS.put(name, obj);
        }

        static void clear() {
            BINDINGS.clear();
        }

        @Override
        public Context getInitialContext(final Hashtable<?, ?> environment) {
            final Context ctx = mock(Context.class, Mockito.RETURNS_DEEP_STUBS);
            try {
                when(ctx.lookup(anyString())).thenAnswer(inv -> {
                    final String key = inv.getArgument(0);
                    final Object val = BINDINGS.get(key);
                    if (val == null) {
                        throw new NameNotFoundException("Not bound: " + key);
                    }
                    return val;
                });
            } catch (final NamingException e) {
                throw new IllegalStateException(e);
            }
            return ctx;
        }
    }
}
