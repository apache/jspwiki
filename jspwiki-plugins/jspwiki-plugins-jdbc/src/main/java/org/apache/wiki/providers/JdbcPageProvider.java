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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.core.Page;
import org.apache.wiki.api.exceptions.NoRequiredPropertyException;
import org.apache.wiki.api.exceptions.ProviderException;
import org.apache.wiki.api.providers.PageProvider;
import org.apache.wiki.api.search.QueryItem;
import org.apache.wiki.api.search.SearchResult;
import org.apache.wiki.api.spi.Wiki;
import org.apache.wiki.search.SearchMatcher;
import org.apache.wiki.search.SearchResultComparator;
import org.apache.wiki.util.TextUtil;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;

/**
 * JDBC-backed {@link PageProvider} supporting SQLite, PostgreSQL, MariaDB, and
 * Microsoft SQL Server without an ORM.
 */
public class JdbcPageProvider implements PageProvider {

    private static final Logger LOG = LogManager.getLogger(JdbcPageProvider.class);

    public static final String PROP_JDBC_URL = "jspwiki.jdbcPageProvider.url";
    public static final String PROP_JDBC_USER = "jspwiki.jdbcPageProvider.user";
    public static final String PROP_JDBC_PASSWORD = "jspwiki.jdbcPageProvider.password";
    public static final String PROP_JDBC_DRIVER = "jspwiki.jdbcPageProvider.driver";
    public static final String PROP_JDBC_JNDI = "jspwiki.jdbcPageProvider.jndiName";
    public static final String PROP_AUTO_CREATE_SCHEMA = "jspwiki.jdbcPageProvider.autoCreateSchema";

    private enum Dialect {
        SQLITE, POSTGRESQL, MARIADB, MSSQL, GENERIC;

        static Dialect detect(final String url, final String productName) {
            final String u = url != null ? url.toLowerCase() : "";
            final String p = productName != null ? productName.toLowerCase() : "";
            if (u.startsWith("jdbc:sqlite:") || p.contains("sqlite")) {
                return SQLITE;
            }
            if (u.startsWith("jdbc:postgresql:") || p.contains("postgresql")) {
                return POSTGRESQL;
            }
            if (u.startsWith("jdbc:mariadb:") || u.startsWith("jdbc:mysql:")
                    || p.contains("mariadb") || p.contains("mysql")) {
                return MARIADB;
            }
            if (u.startsWith("jdbc:sqlserver:") || p.contains("sql server") || p.contains("microsoft")) {
                return MSSQL;
            }
            return GENERIC;
        }
    }

    private Engine m_engine;
    private DataSource m_dataSource;
    private String m_jdbcUrl;
    private String m_jdbcUser;
    private String m_jdbcPassword;
    private Dialect m_dialect = Dialect.GENERIC;
    private String m_dbProductName = "JDBC";
    public static final String PROP_SEARCH_PREFILTER = "jspwiki.jdbcPageProvider.searchPreFilter";
    private boolean m_searchPreFilter = true;
    private int m_maxPropLimit = AbstractFileProvider.DEFAULT_MAX_PROPLIMIT;
    private int m_maxPropKeyLength = AbstractFileProvider.DEFAULT_MAX_PROPKEYLENGTH;
    private int m_maxPropValueLength = AbstractFileProvider.DEFAULT_MAX_PROPVALUELENGTH;

    @Override
    public void initialize(final Engine engine, final Properties properties)
            throws NoRequiredPropertyException, IOException {
        m_engine = engine;

        m_maxPropLimit = TextUtil.getIntegerProperty(
                properties, AbstractFileProvider.PROP_CUSTOMPROP_MAXLIMIT, AbstractFileProvider.DEFAULT_MAX_PROPLIMIT);
        m_maxPropKeyLength = TextUtil.getIntegerProperty(
                properties, AbstractFileProvider.PROP_CUSTOMPROP_MAXKEYLENGTH, AbstractFileProvider.DEFAULT_MAX_PROPKEYLENGTH);
        m_maxPropValueLength = TextUtil.getIntegerProperty(
                properties, AbstractFileProvider.PROP_CUSTOMPROP_MAXVALUELENGTH, AbstractFileProvider.DEFAULT_MAX_PROPVALUELENGTH);
        m_searchPreFilter = "true".equalsIgnoreCase(
                properties.getProperty(PROP_SEARCH_PREFILTER, "true"));
        final String jndiName = properties.getProperty(PROP_JDBC_JNDI);
        if (StringUtils.isNotBlank(jndiName)) {
            try {
                final InitialContext ctx = new InitialContext();
                m_dataSource = (DataSource) ctx.lookup(jndiName);
            } catch (final NamingException e) {
                throw new IOException("Failed to look up JNDI DataSource '" + jndiName + "'", e);
            }
        } else {
            m_jdbcUrl = properties.getProperty(PROP_JDBC_URL);
            if (StringUtils.isBlank(m_jdbcUrl)) {
                throw new NoRequiredPropertyException("Missing required property: " + PROP_JDBC_URL, PROP_JDBC_URL);
            }
            m_jdbcUser = properties.getProperty(PROP_JDBC_USER);
            m_jdbcPassword = properties.getProperty(PROP_JDBC_PASSWORD);

            final String driverClass = properties.getProperty(PROP_JDBC_DRIVER);
            if (StringUtils.isNotBlank(driverClass)) {
                try {
                    Class.forName(driverClass);
                } catch (final ClassNotFoundException e) {
                    throw new IOException("JDBC driver class not found: " + driverClass, e);
                }
            }
        }

        try (final Connection conn = openConnection()) {
            m_dbProductName = conn.getMetaData().getDatabaseProductName();
            m_dialect = Dialect.detect(m_jdbcUrl, m_dbProductName);

            final boolean autoCreate = Boolean.parseBoolean(
                    properties.getProperty(PROP_AUTO_CREATE_SCHEMA, "false"));
            if (autoCreate) {
                createSchemaIfNeeded(conn);
            }
        } catch (final SQLException e) {
            throw new IOException("Failed to initialize JDBC connection for JdbcPageProvider", e);
        }

        LOG.info("Initialized JdbcPageProvider ({}) using {}", m_dbProductName, m_dialect);
    }

    private Connection openConnection() throws SQLException {
        final Connection conn;
        if (m_dataSource != null) {
            conn = m_dataSource.getConnection();
        } else if (StringUtils.isNotBlank(m_jdbcUser)) {
            conn = DriverManager.getConnection(m_jdbcUrl, m_jdbcUser, m_jdbcPassword);
        } else {
            conn = DriverManager.getConnection(m_jdbcUrl);
        }
        if (m_dialect == Dialect.SQLITE) {
            try (final Statement st = conn.createStatement()) {
                st.execute("PRAGMA foreign_keys = ON");
            }
        }
        return conn;
    }

    private void createSchemaIfNeeded(final Connection conn) throws SQLException {
        final String textType;
        final String noteType;
        final String propValType;
        switch (m_dialect) {
            case MARIADB:
                textType = "LONGTEXT";
                noteType = "VARCHAR(1024)";
                propValType = "VARCHAR(4096)";
                break;
            case MSSQL:
                textType = "NVARCHAR(MAX)";
                noteType = "NVARCHAR(1024)";
                propValType = "NVARCHAR(4000)";
                break;
            case POSTGRESQL:
            case SQLITE:
            case GENERIC:
            default:
                textType = "TEXT";
                noteType = "VARCHAR(1024)";
                propValType = "VARCHAR(4096)";
                break;
        }

        final String pagesSql = "CREATE TABLE IF NOT EXISTS wiki_pages ("
                + "page_name VARCHAR(255) NOT NULL PRIMARY KEY, "
                + "latest_version INTEGER NOT NULL, "
                + "author VARCHAR(255), "
                + "change_note " + noteType + ", "
                + "view_count BIGINT, "
                + "last_modified BIGINT NOT NULL, "
                + "page_text " + textType + " NOT NULL)";

        final String versionsSql = "CREATE TABLE IF NOT EXISTS wiki_page_versions ("
                + "page_name VARCHAR(255) NOT NULL, "
                + "version_num INTEGER NOT NULL, "
                + "author VARCHAR(255), "
                + "change_note " + noteType + ", "
                + "view_count BIGINT, "
                + "last_modified BIGINT NOT NULL, "
                + "page_text " + textType + " NOT NULL, "
                + "PRIMARY KEY (page_name, version_num), "
                + "CONSTRAINT fk_versions_page FOREIGN KEY (page_name) "
                + "REFERENCES wiki_pages (page_name) ON DELETE CASCADE)";

        final String propsSql = "CREATE TABLE IF NOT EXISTS wiki_page_properties ("
                + "page_name VARCHAR(255) NOT NULL, "
                + "version_num INTEGER NOT NULL, "
                + "prop_key VARCHAR(255) NOT NULL, "
                + "prop_value " + propValType + " NOT NULL, "
                + "PRIMARY KEY (page_name, version_num, prop_key), "
                + "CONSTRAINT fk_props_version FOREIGN KEY (page_name, version_num) "
                + "REFERENCES wiki_page_versions (page_name, version_num) ON DELETE CASCADE)";

        final String idxSql = "CREATE INDEX IF NOT EXISTS idx_wiki_pages_modified ON wiki_pages (last_modified)";

        try (final Statement st = conn.createStatement()) {
            if (m_dialect == Dialect.MSSQL) {
                st.execute("IF OBJECT_ID('dbo.wiki_pages', 'U') IS NULL " + pagesSql.replace("IF NOT EXISTS ", ""));
                st.execute("IF OBJECT_ID('dbo.wiki_page_versions', 'U') IS NULL " + versionsSql.replace("IF NOT EXISTS ", ""));
                st.execute("IF OBJECT_ID('dbo.wiki_page_properties', 'U') IS NULL " + propsSql.replace("IF NOT EXISTS ", ""));
                st.execute("IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_wiki_pages_modified' AND object_id = OBJECT_ID('dbo.wiki_pages')) "
                        + "CREATE INDEX idx_wiki_pages_modified ON wiki_pages (last_modified)");
            } else {
                st.execute(pagesSql);
                st.execute(versionsSql);
                st.execute(propsSql);
                st.execute(idxSql);
            }
        }
    }

    /**
     * Hook for optional encryption/obfuscation before persisting page text.
     */
    public String obfuscate(final String pageContent) {
        return pageContent;
    }

    /**
     * Hook for optional decryption/deobfuscation after reading page text.
     */
    public String deobfuscate(final String pageContent) {
        return pageContent;
    }

    @Override
    public boolean pageExists(final String page) {
        if (page == null) {
            return false;
        }
        final String sql = "SELECT 1 FROM wiki_pages WHERE page_name = ?";
        try (final Connection conn = openConnection(); final PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, page);
            try (final ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (final SQLException e) {
            LOG.error("Failed checking existence for page '{}'", page, e);
            return false;
        }
    }

    @Override
    public boolean pageExists(final String page, final int version) {
        if (page == null) {
            return false;
        }
        if (version == PageProvider.LATEST_VERSION) {
            return pageExists(page);
        }
        if (version <= 0) {
            return false;
        }
        final String sql = "SELECT 1 FROM wiki_page_versions WHERE page_name = ? AND version_num = ?";
        try (final Connection conn = openConnection(); final PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, page);
            ps.setInt(2, version);
            try (final ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (final SQLException e) {
            LOG.error("Failed checking existence for page '{}' version {}", page, version, e);
            return false;
        }
    }

    @Override
    public String getPageText(final String page, final int version) throws ProviderException {
        if (page == null) {
            throw new ProviderException("Page name must not be null");
        }
        try (final Connection conn = openConnection()) {
            final int latest = getLatestVersion(conn, page);
            if (latest <= 0) {
                return null;
            }
            final int targetVersion = resolveVersion(page, version, latest);

            final String sql = (targetVersion == latest)
                    ? "SELECT page_text FROM wiki_pages WHERE page_name = ?"
                    : "SELECT page_text FROM wiki_page_versions WHERE page_name = ? AND version_num = ?";

            try (final PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, page);
                if (targetVersion != latest) {
                    ps.setInt(2, targetVersion);
                }
                try (final ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new NoSuchVersionException("Page '" + page + "', version=" + targetVersion);
                    }
                    return deobfuscate(rs.getString(1));
                }
            }
        } catch (final ProviderException e) {
            throw e;
        } catch (final SQLException e) {
            throw new ProviderException("Failed reading page text for '" + page + "' version " + version, e);
        }
    }

    @Override
    public void putPageText(final Page page, final String text) throws ProviderException {
        if (page == null || StringUtils.isBlank(page.getName())) {
            throw new ProviderException("Page or page name must not be null/blank");
        }

        final String pageName = page.getName();
        final String storedText = obfuscate(text != null ? text : "");
        final long nowMillis = (page.getLastModified() != null)
                ? page.getLastModified().getTime()
                : System.currentTimeMillis();

        final String author = page.getAuthor();
        final String changeNote = page.getAttribute(Page.CHANGENOTE) != null
                ? page.getAttribute(Page.CHANGENOTE).toString()
                : null;
        final Long viewCount = parseLongOrNull(page.getAttribute(Page.VIEWCOUNT));

        final Properties customProps = extractAndValidateCustomProperties(page);

        try (final Connection conn = openConnection()) {
            conn.setAutoCommit(false);
            try {
                final int currentLatest = getLatestVersion(conn, pageName);
                final int nextVersion = (currentLatest > 0) ? currentLatest + 1 : 1;
                final Date modDate = new Date(nowMillis);
                page.setLastModified(modDate);
                page.setVersion(nextVersion);

                if (currentLatest <= 0) {
                    insertPageRow(conn, pageName, nextVersion, author, changeNote, viewCount, nowMillis, storedText);
                } else {
                    updatePageRow(conn, pageName, nextVersion, author, changeNote, viewCount, nowMillis, storedText);
                }

                insertVersionRow(conn, pageName, nextVersion, author, changeNote, viewCount, nowMillis, storedText);
                insertCustomProperties(conn, pageName, nextVersion, customProps);

                conn.commit();
            } catch (final Exception ex) {
                rollbackQuietly(conn);
                if (ex instanceof ProviderException) {
                    throw (ProviderException) ex;
                }
                throw new ProviderException("Failed saving page '" + pageName + "'", ex);
            } finally {
                resetAutoCommitQuietly(conn);
            }
        } catch (final SQLException e) {
            throw new ProviderException("Database error saving page '" + pageName + "'", e);
        }
    }

    @Override
    public Page getPageInfo(final String page, final int version) throws ProviderException {
        if (page == null) {
            return null;
        }
        try (final Connection conn = openConnection()) {
            final int latest = getLatestVersion(conn, page);
            if (latest <= 0) {
                return null;
            }
            final int targetVersion = resolveVersion(page, version, latest);

            final String sql = (targetVersion == latest)
                    ? "SELECT latest_version, author, change_note, view_count, last_modified FROM wiki_pages WHERE page_name = ?"
                    : "SELECT version_num, author, change_note, view_count, last_modified FROM wiki_page_versions WHERE page_name = ? AND version_num = ?";

            try (final PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, page);
                if (targetVersion != latest) {
                    ps.setInt(2, targetVersion);
                }
                try (final ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return null;
                    }
                    final Page p = Wiki.contents().page(m_engine, page);
                    p.setVersion(targetVersion);
                    p.setAuthor(rs.getString(2));
                    final String note = rs.getString(3);
                    if (note != null) {
                        p.setAttribute(Page.CHANGENOTE, note);
                    }
                    final long vc = rs.getLong(4);
                    if (!rs.wasNull()) {
                        p.setAttribute(Page.VIEWCOUNT, Long.toString(vc));
                    }
                    p.setLastModified(new Date(rs.getLong(5)));

                    loadCustomProperties(conn, page, targetVersion, p);
                    return p;
                }
            }
        } catch (final NoSuchVersionException e) {
            return null;
        } catch (final SQLException e) {
            throw new ProviderException("Failed retrieving page info for '" + page + "' version " + version, e);
        }
    }

    @Override
    public List<Page> getVersionHistory(final String page) throws ProviderException {
        if (page == null) {
            return Collections.emptyList();
        }
        final String sql = "SELECT version_num, author, change_note, view_count, last_modified "
                + "FROM wiki_page_versions WHERE page_name = ? ORDER BY version_num DESC";
        final List<Page> history = new ArrayList<>();
        try (final Connection conn = openConnection(); final PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, page);
            try (final ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    final int ver = rs.getInt("version_num");
                    final Page p = Wiki.contents().page(m_engine, page);
                    p.setVersion(ver);
                    p.setAuthor(rs.getString("author"));
                    final String note = rs.getString("change_note");
                    if (note != null) {
                        p.setAttribute(Page.CHANGENOTE, note);
                    }
                    final long vc = rs.getLong("view_count");
                    if (!rs.wasNull()) {
                        p.setAttribute(Page.VIEWCOUNT, Long.toString(vc));
                    }
                    p.setLastModified(new Date(rs.getLong("last_modified")));
                    loadCustomProperties(conn, page, ver, p);
                    history.add(p);
                }
            }
            return history;
        } catch (final SQLException e) {
            throw new ProviderException("Failed retrieving version history for '" + page + "'", e);
        }
    }

    @Override
    public Collection<Page> getAllPages() throws ProviderException {
        final String sql = "SELECT page_name, latest_version, author, change_note, view_count, last_modified "
                + "FROM wiki_pages ORDER BY page_name";
        final List<Page> pages = new ArrayList<>();
        try (final Connection conn = openConnection(); final PreparedStatement ps = conn.prepareStatement(sql); final ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                final String name = rs.getString("page_name");
                final int ver = rs.getInt("latest_version");
                final Page p = Wiki.contents().page(m_engine, name);
                p.setVersion(ver);
                p.setAuthor(rs.getString("author"));
                final String note = rs.getString("change_note");
                if (note != null) {
                    p.setAttribute(Page.CHANGENOTE, note);
                }
                final long vc = rs.getLong("view_count");
                if (!rs.wasNull()) {
                    p.setAttribute(Page.VIEWCOUNT, Long.toString(vc));
                }
                p.setLastModified(new Date(rs.getLong("last_modified")));
                loadCustomProperties(conn, name, ver, p);
                pages.add(p);
            }
            return pages;
        } catch (final SQLException e) {
            throw new ProviderException("Failed listing all pages", e);
        }
    }

    @Override
    public Collection<Page> getAllChangedSince(final Date date) {
        if (date == null) {
            return Collections.emptyList();
        }
        final String sql = "SELECT page_name, latest_version, author, change_note, view_count, last_modified "
                + "FROM wiki_pages WHERE last_modified > ? ORDER BY last_modified DESC";
        final List<Page> changed = new ArrayList<>();
        try (final Connection conn = openConnection(); final PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, date.getTime());
            try (final ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    final String name = rs.getString("page_name");
                    final int ver = rs.getInt("latest_version");
                    final Page p = Wiki.contents().page(m_engine, name);
                    p.setVersion(ver);
                    p.setAuthor(rs.getString("author"));
                    final String note = rs.getString("change_note");
                    if (note != null) {
                        p.setAttribute(Page.CHANGENOTE, note);
                    }
                    final long vc = rs.getLong("view_count");
                    if (!rs.wasNull()) {
                        p.setAttribute(Page.VIEWCOUNT, Long.toString(vc));
                    }
                    p.setLastModified(new Date(rs.getLong("last_modified")));
                    loadCustomProperties(conn, name, ver, p);
                    changed.add(p);
                }
            }
        } catch (final SQLException e) {
            LOG.error("Failed querying pages changed since {}", date, e);
        }
        return changed;
    }

    @Override
    public int getPageCount() {
        final String sql = "SELECT COUNT(*) FROM wiki_pages";
        try (final Connection conn = openConnection(); final PreparedStatement ps = conn.prepareStatement(sql); final ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (final SQLException e) {
            LOG.error("Failed counting pages", e);
            return 0;
        }
    }

    @Override
    public Collection<SearchResult> findPages(final QueryItem[] query) {
        final TreeSet<SearchResult> results = new TreeSet<>(new SearchResultComparator());
        if (query == null || query.length == 0) {
            return results;
        }

        final List<String> positiveTerms = extractPositiveSearchTerms(query);
        final StringBuilder sql = new StringBuilder("SELECT page_name, page_text FROM wiki_pages");
        final List<String> params = new ArrayList<>();

        if (m_searchPreFilter && !positiveTerms.isEmpty()) {
            sql.append(" WHERE ");
            for (int i = 0; i < positiveTerms.size(); i++) {
                if (i > 0) {
                    sql.append(" OR ");
                }
                sql.append("(LOWER(page_name) LIKE ? ESCAPE '\\' OR LOWER(page_text) LIKE ? ESCAPE '\\')");
                final String pattern = "%" + escapeLikePattern(positiveTerms.get(i).toLowerCase()) + "%";
                params.add(pattern);
                params.add(pattern);
            }
        }

        final SearchMatcher matcher = new SearchMatcher(m_engine, query);
        try (final Connection conn = openConnection(); final PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setString(i + 1, params.get(i));
            }
            try (final ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    final String pageName = rs.getString("page_name");
                    final String pageText = deobfuscate(rs.getString("page_text"));
                    final SearchResult match = matcher.matchPageContent(pageName, pageText);
                    if (match != null) {
                        results.add(match);
                    }
                }
            }
        } catch (final Exception e) {
            LOG.error("Failed searching pages", e);
        }
        return results;
    }

    private static List<String> extractPositiveSearchTerms(final QueryItem[] query) {
        final List<String> terms = new ArrayList<>();
        for (final QueryItem item : query) {
            if (item != null
                    && item.type != QueryItem.FORBIDDEN
                    && (item.type == QueryItem.REQUIRED || item.type == QueryItem.REQUESTED)
                    && StringUtils.isNotBlank(item.word)) {
                terms.add(item.word.trim());
            }
        }
        return terms;
    }

    private static String escapeLikePattern(final String input) {
        final StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            final char c = input.charAt(i);
            if (c == '\\' || c == '%' || c == '_') {
                sb.append('\\');
            }
            sb.append(c);
        }
        return sb.toString();
    }

    @Override
    public void deleteVersion(final String pageName, final int version) throws ProviderException {
        if (pageName == null) {
            return;
        }
        try (final Connection conn = openConnection()) {
            conn.setAutoCommit(false);
            try {
                final int latest = getLatestVersion(conn, pageName);
                if (latest <= 0) {
                    conn.commit();
                    return;
                }
                final int targetVersion = resolveVersion(pageName, version, latest);

                deletePropertiesForVersion(conn, pageName, targetVersion);
                try (final PreparedStatement delVer = conn.prepareStatement(
                        "DELETE FROM wiki_page_versions WHERE page_name = ? AND version_num = ?")) {
                    delVer.setString(1, pageName);
                    delVer.setInt(2, targetVersion);
                    final int deleted = delVer.executeUpdate();
                    if (deleted == 0) {
                        throw new NoSuchVersionException("Page '" + pageName + "', version=" + targetVersion);
                    }
                }

                if (targetVersion == latest) {
                    // Promote remaining highest version, or delete page if none remain
                    try (final PreparedStatement findPrev = conn.prepareStatement(
                            "SELECT version_num, author, change_note, view_count, last_modified, page_text "
                            + "FROM wiki_page_versions WHERE page_name = ? ORDER BY version_num DESC")) {
                        findPrev.setString(1, pageName);
                        try (final ResultSet rs = findPrev.executeQuery()) {
                            if (rs.next()) {
                                final int prevVer = rs.getInt("version_num");
                                final String author = rs.getString("author");
                                final String note = rs.getString("change_note");
                                final long vcLong = rs.getLong("view_count");
                                final Long vc = rs.wasNull() ? null : vcLong;
                                final long mod = rs.getLong("last_modified");
                                final String text = rs.getString("page_text");
                                updatePageRow(conn, pageName, prevVer, author, note, vc, mod, text);
                            } else {
                                try (final PreparedStatement delPage = conn.prepareStatement(
                                        "DELETE FROM wiki_pages WHERE page_name = ?")) {
                                    delPage.setString(1, pageName);
                                    delPage.executeUpdate();
                                }
                            }
                        }
                    }
                }
                conn.commit();
            } catch (final Exception ex) {
                rollbackQuietly(conn);
                if (ex instanceof ProviderException) {
                    throw (ProviderException) ex;
                }
                throw new ProviderException("Failed deleting version " + version + " of page '" + pageName + "'", ex);
            } finally {
                resetAutoCommitQuietly(conn);
            }
        } catch (final SQLException e) {
            throw new ProviderException("Database error deleting version of page '" + pageName + "'", e);
        }
    }

    @Override
    public void deletePage(final String pageName) throws ProviderException {
        if (pageName == null) {
            return;
        }
        try (final Connection conn = openConnection()) {
            conn.setAutoCommit(false);
            try {
                try (final PreparedStatement p1 = conn.prepareStatement(
                        "DELETE FROM wiki_page_properties WHERE page_name = ?")) {
                    p1.setString(1, pageName);
                    p1.executeUpdate();
                }
                try (final PreparedStatement p2 = conn.prepareStatement(
                        "DELETE FROM wiki_page_versions WHERE page_name = ?")) {
                    p2.setString(1, pageName);
                    p2.executeUpdate();
                }
                try (final PreparedStatement p3 = conn.prepareStatement(
                        "DELETE FROM wiki_pages WHERE page_name = ?")) {
                    p3.setString(1, pageName);
                    p3.executeUpdate();
                }
                conn.commit();
            } catch (final SQLException ex) {
                rollbackQuietly(conn);
                throw new ProviderException("Failed deleting page '" + pageName + "'", ex);
            } finally {
                resetAutoCommitQuietly(conn);
            }
        } catch (final SQLException e) {
            throw new ProviderException("Database error deleting page '" + pageName + "'", e);
        }
    }

    @Override
    public void movePage(final String from, final String to) throws ProviderException {
        if (StringUtils.isBlank(from) || StringUtils.isBlank(to) || from.equals(to)) {
            return;
        }
        try (final Connection conn = openConnection()) {
            conn.setAutoCommit(false);
            try {
                // Ensure target does not already exist
                if (getLatestVersion(conn, to) > 0) {
                    throw new ProviderException("Target page '" + to + "' already exists");

                }

                try (final PreparedStatement checkSource = conn.prepareStatement(
                        "SELECT 1 FROM wiki_page_versions WHERE page_name = ? AND version_num = ?")) {
                    final int sourceLatest = getLatestVersion(conn, from);
                    if (sourceLatest <= 0) {
                        throw new ProviderException("Source page '" + from + "' does not exist");
                    }
                    checkSource.setString(1, from);
                    checkSource.setInt(2, sourceLatest);
                    try (final ResultSet rs = checkSource.executeQuery()) {
                        if (!rs.next()) {
                            throw new ProviderException(
                                    "Source page '" + from + "' is missing version snapshot " + sourceLatest);
                        }
                    }
                }

                try (final PreparedStatement copyPage = conn.prepareStatement(
                        "INSERT INTO wiki_pages (page_name, latest_version, author, change_note, view_count, last_modified, page_text) "
                        + "SELECT ?, latest_version, author, change_note, view_count, last_modified, page_text "
                        + "FROM wiki_pages WHERE page_name = ?")) {
                    copyPage.setString(1, to);
                    copyPage.setString(2, from);
                    final int inserted = copyPage.executeUpdate();
                    if (inserted == 0) {
                        throw new ProviderException("Source page '" + from + "' does not exist");
                    }
                }

                try (final PreparedStatement copyVers = conn.prepareStatement(
                        "INSERT INTO wiki_page_versions (page_name, version_num, author, change_note, view_count, last_modified, page_text) "
                        + "SELECT ?, version_num, author, change_note, view_count, last_modified, page_text "
                        + "FROM wiki_page_versions WHERE page_name = ?")) {
                    copyVers.setString(1, to);
                    copyVers.setString(2, from);
                    copyVers.executeUpdate();
                }

                try (final PreparedStatement copyProps = conn.prepareStatement(
                        "INSERT INTO wiki_page_properties (page_name, version_num, prop_key, prop_value) "
                        + "SELECT ?, version_num, prop_key, prop_value "
                        + "FROM wiki_page_properties WHERE page_name = ?")) {
                    copyProps.setString(1, to);
                    copyProps.setString(2, from);
                    copyProps.executeUpdate();
                }

                try (final PreparedStatement delProps = conn.prepareStatement(
                        "DELETE FROM wiki_page_properties WHERE page_name = ?")) {
                    delProps.setString(1, from);
                    delProps.executeUpdate();
                }
                try (final PreparedStatement delVers = conn.prepareStatement(
                        "DELETE FROM wiki_page_versions WHERE page_name = ?")) {
                    delVers.setString(1, from);
                    delVers.executeUpdate();
                }
                try (final PreparedStatement delPage = conn.prepareStatement(
                        "DELETE FROM wiki_pages WHERE page_name = ?")) {
                    delPage.setString(1, from);
                    delPage.executeUpdate();
                }

                conn.commit();
            } catch (final Exception ex) {
                rollbackQuietly(conn);
                if (ex instanceof ProviderException) {
                    throw (ProviderException) ex;
                }
                throw new ProviderException("Failed moving page from '" + from + "' to '" + to + "'", ex);
            } finally {
                resetAutoCommitQuietly(conn);
            }
        } catch (final SQLException e) {
            throw new ProviderException("Database error moving page from '" + from + "' to '" + to + "'", e);
        }
    }

    @Override
    public String getProviderInfo() {
        return "JdbcPageProvider (" + m_dbProductName + ", dialect=" + m_dialect + ")";
    }

    // --- Internal JDBC Helpers ---
    private int getLatestVersion(final Connection conn, final String pageName) throws SQLException {
        final String sql = "SELECT latest_version FROM wiki_pages WHERE page_name = ?";
        try (final PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pageName);
            try (final ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }

    private int resolveVersion(final String pageName, final int requestedVersion, final int latest)
            throws NoSuchVersionException {
        if (requestedVersion == PageProvider.LATEST_VERSION) {
            return latest;
        }
        if (requestedVersion <= 0 || requestedVersion > latest) {
            throw new NoSuchVersionException(
                    "Requested version " + requestedVersion + " for page '" + pageName + "', latest is " + latest);
        }
        return requestedVersion;
    }

    private void insertPageRow(final Connection conn, final String pageName, final int version,
            final String author, final String changeNote, final Long viewCount,
            final long lastModified, final String text) throws SQLException {
        final String sql = "INSERT INTO wiki_pages "
                + "(page_name, latest_version, author, change_note, view_count, last_modified, page_text) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (final PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pageName);
            ps.setInt(2, version);
            ps.setString(3, author);
            ps.setString(4, changeNote);
            if (viewCount != null) {
                ps.setLong(5, viewCount);
            } else {
                ps.setNull(5, Types.BIGINT);
            }
            ps.setLong(6, lastModified);
            ps.setString(7, text);
            ps.executeUpdate();
        }
    }

    private void updatePageRow(final Connection conn, final String pageName, final int version,
            final String author, final String changeNote, final Long viewCount,
            final long lastModified, final String text) throws SQLException {
        final String sql = "UPDATE wiki_pages SET latest_version = ?, author = ?, change_note = ?, "
                + "view_count = ?, last_modified = ?, page_text = ? WHERE page_name = ?";
        try (final PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, version);
            ps.setString(2, author);
            ps.setString(3, changeNote);
            if (viewCount != null) {
                ps.setLong(4, viewCount);
            } else {
                ps.setNull(4, Types.BIGINT);
            }
            ps.setLong(5, lastModified);
            ps.setString(6, text);
            ps.setString(7, pageName);
            ps.executeUpdate();
        }
    }

    private void insertVersionRow(final Connection conn, final String pageName, final int version,
            final String author, final String changeNote, final Long viewCount,
            final long lastModified, final String text) throws SQLException {
        final String sql = "INSERT INTO wiki_page_versions "
                + "(page_name, version_num, author, change_note, view_count, last_modified, page_text) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (final PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pageName);
            ps.setInt(2, version);
            ps.setString(3, author);
            ps.setString(4, changeNote);
            if (viewCount != null) {
                ps.setLong(5, viewCount);
            } else {
                ps.setNull(5, Types.BIGINT);
            }
            ps.setLong(6, lastModified);
            ps.setString(7, text);
            ps.executeUpdate();
        }
    }

    private Properties extractAndValidateCustomProperties(final Page page) throws ProviderException {
        final Properties props = new Properties();
        final Map<String, Object> atts = page.getAttributes();
        if (atts != null) {
            for (final Map.Entry<String, Object> entry : atts.entrySet()) {
                final String key = entry.getKey();
                final Object val = entry.getValue();
                if (key != null && key.startsWith("@") && val != null) {
                    props.setProperty(key, val.toString());
                }
            }
        }
        if (!props.isEmpty()) {
            if (props.size() > m_maxPropLimit) {
                throw new ProviderException("Too many custom properties (" + props.size() + " > " + m_maxPropLimit + ")");
            }
            final Enumeration<?> names = props.propertyNames();
            while (names.hasMoreElements()) {
                final String key = (String) names.nextElement();
                final String val = props.getProperty(key);
                if (key.length() > m_maxPropKeyLength || !StringUtils.isAsciiPrintable(key)) {
                    throw new ProviderException("Invalid custom property key: " + key);
                }
                if (val.length() > m_maxPropValueLength || !StringUtils.isAsciiPrintable(val)) {
                    throw new ProviderException("Invalid custom property value for key: " + key);
                }
            }
        }
        return props;
    }

    private void insertCustomProperties(final Connection conn, final String pageName,
            final int versionNum, final Properties props) throws SQLException {
        if (props.isEmpty()) {
            return;
        }
        final String sql = "INSERT INTO wiki_page_properties (page_name, version_num, prop_key, prop_value) "
                + "VALUES (?, ?, ?, ?)";
        try (final PreparedStatement ps = conn.prepareStatement(sql)) {
            final Enumeration<?> names = props.propertyNames();
            while (names.hasMoreElements()) {
                final String key = (String) names.nextElement();
                ps.setString(1, pageName);
                ps.setInt(2, versionNum);
                ps.setString(3, key);
                ps.setString(4, props.getProperty(key));
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void loadCustomProperties(final Connection conn, final String pageName,
            final int versionNum, final Page page) throws SQLException {
        final String sql = "SELECT prop_key, prop_value FROM wiki_page_properties "
                + "WHERE page_name = ? AND version_num = ?";
        try (final PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pageName);
            ps.setInt(2, versionNum);
            try (final ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    page.setAttribute(rs.getString(1), rs.getString(2));
                }
            }
        }
    }

    private void deletePropertiesForVersion(final Connection conn, final String pageName, final int versionNum)
            throws SQLException {
        try (final PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM wiki_page_properties WHERE page_name = ? AND version_num = ?")) {
            ps.setString(1, pageName);
            ps.setInt(2, versionNum);
            ps.executeUpdate();
        }
    }

    private static Long parseLongOrNull(final Object val) {
        if (val == null) {
            return null;
        }
        if (val instanceof Number) {
            return ((Number) val).longValue();
        }
        try {
            return Long.parseLong(val.toString());
        } catch (final NumberFormatException e) {
            return null;
        }
    }

    private static void rollbackQuietly(final Connection conn) {
        try {
            conn.rollback();
        } catch (final SQLException ignored) {
            // Best-effort rollback
        }
    }

    private static void resetAutoCommitQuietly(final Connection conn) {
        try {
            if (!conn.getAutoCommit()) {
                conn.setAutoCommit(true);
            }
        } catch (final SQLException ignored) {
            // Best-effort reset
        }
    }
}
