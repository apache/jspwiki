package org.apache.wiki.providers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.wiki.api.core.Attachment;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.core.Page;
import org.apache.wiki.api.exceptions.NoRequiredPropertyException;
import org.apache.wiki.api.exceptions.ProviderException;
import org.apache.wiki.api.providers.AttachmentProvider;
import org.apache.wiki.api.providers.WikiProvider;
import org.apache.wiki.api.search.QueryItem;
import org.apache.wiki.pages.PageTimeComparator;
import org.apache.wiki.util.FileUtil;
import org.apache.wiki.util.TextUtil;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.regex.Pattern;

public class JdbcAttachmentProvider implements AttachmentProvider {

    private static final Logger LOG = LogManager.getLogger(JdbcAttachmentProvider.class);

    public static final String PROP_DATASOURCE = "jspwiki.jdbcAttachmentProvider.dataSource";
    public static final String PROP_AUTO_CREATE = "jspwiki.jdbcAttachmentProvider.autoCreateTable";
    public static final String PROP_DISABLECACHE = BasicAttachmentProvider.PROP_DISABLECACHE;

    private enum DbDialect {
        POSTGRES, MARIADB, MSSQL, SQLITE, OTHER
    }

    private Engine m_engine;
    private DataSource m_dataSource;
    private DbDialect m_dialect = DbDialect.OTHER;
    private Pattern m_disableCache;

    @Override
    public void initialize(final Engine engine, final Properties properties) throws NoRequiredPropertyException, IOException {
        m_engine = engine;

        final String jndiName = properties.getProperty(PROP_DATASOURCE);
        if (jndiName == null || jndiName.isBlank()) {
            throw new NoRequiredPropertyException("Missing required property " + PROP_DATASOURCE, PROP_DATASOURCE);
        }

        try {
            final InitialContext ctx = new InitialContext();
            final Object lookedUp = ctx.lookup(jndiName);
            if (!(lookedUp instanceof DataSource)) {
                throw new IOException("JNDI resource '" + jndiName + "' is not a javax.sql.DataSource");
            }
            m_dataSource = (DataSource) lookedUp;
        } catch (final NamingException e) {
            throw new IOException("Failed to look up DataSource '" + jndiName + "'", e);
        }

        try (final Connection conn = m_dataSource.getConnection()) {
            m_dialect = detectDialect(conn.getMetaData());
            if (m_dialect == DbDialect.SQLITE) {
                applySqlitePragmas(conn);
            }
            final boolean autoCreate = TextUtil.getBooleanProperty(properties, PROP_AUTO_CREATE, false);
            if (autoCreate) {
                createSchemaIfMissing(conn, m_dialect);
            }
        } catch (final SQLException e) {
            throw new IOException("Failed to initialize JDBC metadata/schema", e);
        }

        final String patternString = properties.getProperty(PROP_DISABLECACHE);
        if (patternString != null && !patternString.isBlank()) {
            m_disableCache = Pattern.compile(patternString);
        }
    }

    private DbDialect detectDialect(final DatabaseMetaData meta) throws SQLException {
        final String product = meta.getDatabaseProductName();
        final String lower = (product != null) ? product.toLowerCase(Locale.ROOT) : "";
        if (lower.contains("postgres")) {
            return DbDialect.POSTGRES;
        }
        if (lower.contains("mariadb") || lower.contains("mysql")) {
            return DbDialect.MARIADB;
        }
        if (lower.contains("microsoft sql server") || lower.contains("sql server")) {
            return DbDialect.MSSQL;
        }
        if (lower.contains("sqlite")) {
            return DbDialect.SQLITE;
        }
        return DbDialect.OTHER;
    }

    private void applySqlitePragmas(final Connection conn) throws SQLException {
        try (final Statement st = conn.createStatement()) {
            st.execute("PRAGMA busy_timeout = 5000");
            st.execute("PRAGMA journal_mode = WAL");
        }
    }

    private void createSchemaIfMissing(final Connection conn, final DbDialect dialect) throws SQLException {
        final String ddl;
        switch (dialect) {
            case POSTGRES ->
                ddl = """
                CREATE TABLE IF NOT EXISTS wiki_attachment_versions (
                    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                    parent_page VARCHAR(255) NOT NULL,
                    file_name VARCHAR(255) NOT NULL,
                    version INT NOT NULL,
                    author VARCHAR(255),
                    change_note VARCHAR(1024),
                    file_size BIGINT NOT NULL,
                    last_modified BIGINT NOT NULL,
                    content BYTEA NOT NULL,
                    CONSTRAINT uq_att_page_file_ver UNIQUE (parent_page, file_name, version)
                )
                """;
            case OTHER ->
                ddl = """
                CREATE TABLE IF NOT EXISTS wiki_attachment_versions (
                    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                    parent_page VARCHAR(255) NOT NULL,
                    file_name VARCHAR(255) NOT NULL,
                    version INT NOT NULL,
                    author VARCHAR(255),
                    change_note VARCHAR(1024),
                    file_size BIGINT NOT NULL,
                    last_modified BIGINT NOT NULL,
                    content VARBINARY NOT NULL,
                    CONSTRAINT uq_att_page_file_ver UNIQUE (parent_page, file_name, version)
                )
                """;
            case MARIADB ->
                ddl = """
                CREATE TABLE IF NOT EXISTS wiki_attachment_versions (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    parent_page VARCHAR(255) NOT NULL,
                    file_name VARCHAR(255) NOT NULL,
                    version INT NOT NULL,
                    author VARCHAR(255),
                    change_note VARCHAR(1024),
                    file_size BIGINT NOT NULL,
                    last_modified BIGINT NOT NULL,
                    content LONGBLOB NOT NULL,
                    CONSTRAINT uq_att_page_file_ver UNIQUE (parent_page, file_name, version)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """;
            case MSSQL ->
                ddl = """
                IF OBJECT_ID(N'dbo.wiki_attachment_versions', N'U') IS NULL
                CREATE TABLE dbo.wiki_attachment_versions (
                    id BIGINT IDENTITY(1,1) PRIMARY KEY,
                    parent_page NVARCHAR(255) NOT NULL,
                    file_name NVARCHAR(255) NOT NULL,
                    version INT NOT NULL,
                    author NVARCHAR(255) NULL,
                    change_note NVARCHAR(1024) NULL,
                    file_size BIGINT NOT NULL,
                    last_modified BIGINT NOT NULL,
                    content VARBINARY(MAX) NOT NULL,
                    CONSTRAINT uq_att_page_file_ver UNIQUE (parent_page, file_name, version)
                    )
                                """;
            case SQLITE ->
                ddl = """
                CREATE TABLE IF NOT EXISTS wiki_attachment_versions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    parent_page TEXT NOT NULL,
                    file_name TEXT NOT NULL,
                    version INTEGER NOT NULL,
                    author TEXT,
                    change_note TEXT,
                    file_size INTEGER NOT NULL,
                    last_modified INTEGER NOT NULL,
                    content BLOB NOT NULL,
                    UNIQUE (parent_page, file_name, version)
                    )
                """;
            default ->
                throw new SQLException("unknown or unsupported database type ");
        }

        try (final Statement st = conn.createStatement()) {
            st.execute(ddl);
            if (dialect != DbDialect.MSSQL) {
                st.execute("CREATE INDEX IF NOT EXISTS idx_att_page_file ON wiki_attachment_versions (parent_page, file_name)");
                st.execute("CREATE INDEX IF NOT EXISTS idx_att_last_mod ON wiki_attachment_versions (last_modified)");
            }
        }
    }

    @Override
    public String getProviderInfo() {
        return "JdbcAttachmentProvider (" + m_dialect + ")";
    }

    @Override
    public void putAttachmentData(final Attachment att, final InputStream data) throws ProviderException, IOException {
        final String parentPage = att.getParentName();
        final String fileName = att.getFileName();
        final String author = (att.getAuthor() != null) ? att.getAuthor() : "unknown";
        final Object noteObj = att.getAttribute(Page.CHANGENOTE);
        final String changeNote = (noteObj != null) ? noteObj.toString() : null;
        final Date now = new Date();

        final Path tempFile = Files.createTempFile("jspwiki-att-", ".bin");
        try {
            try (final OutputStream out = Files.newOutputStream(tempFile)) {
                FileUtil.copyContents(data, out);
            }
            final long fileSize = Files.size(tempFile);

            final String maxVerSql = "SELECT COALESCE(MAX(version), 0) FROM wiki_attachment_versions WHERE parent_page = ? AND file_name = ?";
            final String insertSql = "INSERT INTO wiki_attachment_versions "
                    + "(parent_page, file_name, version, author, change_note, file_size, last_modified, content) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

            try (final Connection conn = m_dataSource.getConnection()) {
                if (m_dialect == DbDialect.SQLITE) {
                    applySqlitePragmas(conn);
                }
                conn.setAutoCommit(false);
                try {
                    int nextVersion;
                    try (final PreparedStatement psMax = conn.prepareStatement(maxVerSql)) {
                        psMax.setString(1, parentPage);
                        psMax.setString(2, fileName);
                        try (final ResultSet rs = psMax.executeQuery()) {
                            nextVersion = rs.next() ? rs.getInt(1) + 1 : 1;
                        }
                    }

                    try (final PreparedStatement psIns = conn.prepareStatement(insertSql); final InputStream fileIn = Files.newInputStream(tempFile)) {
                        psIns.setString(1, parentPage);
                        psIns.setString(2, fileName);
                        psIns.setInt(3, nextVersion);
                        psIns.setString(4, author);
                        if (changeNote != null) {
                            psIns.setString(5, changeNote);
                        } else {
                            psIns.setNull(5, Types.VARCHAR);
                        }
                        psIns.setLong(6, fileSize);
                        psIns.setLong(7, now.getTime());
                        psIns.setBinaryStream(8, fileIn, fileSize);
                        psIns.executeUpdate();
                    }

                    conn.commit();
                    att.setVersion(nextVersion);
                    att.setSize(fileSize);
                    att.setLastModified(now);
                } catch (final SQLException e) {
                    conn.rollback();
                    throw new ProviderException("Failed to store attachment version for " + parentPage + "/" + fileName, e);
                } finally {
                    conn.setAutoCommit(true);
                }
            } catch (final SQLException e) {
                throw new ProviderException("Database error storing attachment", e);
            }
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Override
    public InputStream getAttachmentData(final Attachment att) throws IOException, ProviderException {
        final int resolvedVersion = resolveVersion(att.getParentName(), att.getFileName(), att.getVersion());
        if (resolvedVersion <= 0) {
            throw new ProviderException("Attachment not found: " + att.getName());
        }

        final String sql = "SELECT content FROM wiki_attachment_versions "
                + "WHERE parent_page = ? AND file_name = ? AND version = ?";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = m_dataSource.getConnection();
            if (m_dialect == DbDialect.SQLITE) {
                applySqlitePragmas(conn);
            }
            ps = conn.prepareStatement(sql);
            ps.setString(1, att.getParentName());
            ps.setString(2, att.getFileName());
            ps.setInt(3, resolvedVersion);
            rs = ps.executeQuery();

            if (!rs.next()) {
                closeQuietly(rs, ps, conn);
                throw new ProviderException("Attachment version not found: " + att.getName() + " v" + resolvedVersion);
            }

            final InputStream rawStream = rs.getBinaryStream(1);
            if (rawStream == null) {
                closeQuietly(rs, ps, conn);
                throw new ProviderException("Attachment content is null for: " + att.getName());
            }

            final ResultSet fRs = rs;
            final PreparedStatement fPs = ps;
            final Connection fConn = conn;
            return new InputStream() {
                @Override
                public int read() throws IOException {
                    return rawStream.read();
                }

                @Override
                public int read(final byte[] b, final int off, final int len) throws IOException {
                    return rawStream.read(b, off, len);
                }

                @Override
                public void close() throws IOException {
                    try {
                        rawStream.close();
                    } finally {
                        closeQuietly(fRs, fPs, fConn);
                    }
                }
            };
        } catch (final SQLException e) {
            closeQuietly(rs, ps, conn);
            throw new ProviderException("Failed to read attachment data for " + att.getName(), e);
        }
    }

    @Override
    public List< Attachment> listAttachments(final Page page) throws ProviderException {
        final String sql = "SELECT v.file_name, v.version, v.author, v.change_note, v.file_size, v.last_modified "
                + "FROM wiki_attachment_versions v "
                + "INNER JOIN ("
                + "  SELECT parent_page, file_name, MAX(version) AS max_ver "
                + "  FROM wiki_attachment_versions "
                + "  WHERE parent_page = ? "
                + "  GROUP BY parent_page, file_name"
                + ") latest ON v.parent_page = latest.parent_page "
                + "AND v.file_name = latest.file_name "
                + "AND v.version = latest.max_ver "
                + "ORDER BY v.file_name";

        final List< Attachment> results = new ArrayList<>();
        try (final Connection conn = m_dataSource.getConnection(); final PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, page.getName());
            try (final ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapAttachment(page.getName(), rs));
                }
            }
        } catch (final SQLException e) {
            throw new ProviderException("Failed to list attachments for page " + page.getName(), e);
        }
        return results;
    }

    @Override
    public Collection< Attachment> findAttachments(final QueryItem[] query) {
        return Collections.emptyList();
    }

    @Override
    public List< Attachment> listAllChanged(final Date timestamp) throws ProviderException {
        final String sql = "SELECT v.parent_page, v.file_name, v.version, v.author, v.change_note, v.file_size, v.last_modified "
                + "FROM wiki_attachment_versions v "
                + "INNER JOIN ("
                + "  SELECT parent_page, file_name, MAX(version) AS max_ver "
                + "  FROM wiki_attachment_versions "
                + "  GROUP BY parent_page, file_name"
                + ") latest ON v.parent_page = latest.parent_page "
                + "AND v.file_name = latest.file_name "
                + "AND v.version = latest.max_ver "
                + "WHERE v.last_modified > ?";

        final List< Attachment> list = new ArrayList<>();
        try (final Connection conn = m_dataSource.getConnection(); final PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, timestamp.getTime());
            try (final ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    final String parentPage = rs.getString("parent_page");
                    list.add(mapAttachment(parentPage, rs));
                }
            }
        } catch (final SQLException e) {
            throw new ProviderException("Failed to list changed attachments since " + timestamp, e);
        }

        list.sort(new PageTimeComparator());
        return list;
    }

    @Override
    public Attachment getAttachmentInfo(final Page page, final String name, int version) throws ProviderException {
        final String parentPage = page.getName();
        if (version == WikiProvider.LATEST_VERSION) {
            version = resolveVersion(parentPage, name, WikiProvider.LATEST_VERSION);
            if (version <= 0) {
                return null;
            }
        }

        final String sql = "SELECT file_name, version, author, change_note, file_size, last_modified "
                + "FROM wiki_attachment_versions "
                + "WHERE parent_page = ? AND file_name = ? AND version = ?";

        try (final Connection conn = m_dataSource.getConnection(); final PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, parentPage);
            ps.setString(2, name);
            ps.setInt(3, version);
            try (final ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return mapAttachment(parentPage, rs);
            }
        } catch (final SQLException e) {
            throw new ProviderException("Failed to get attachment info for " + parentPage + "/" + name + " v" + version, e);
        }
    }

    @Override
    public List< Attachment> getVersionHistory(final Attachment att) {
        final String sql = "SELECT file_name, version, author, change_note, file_size, last_modified "
                + "FROM wiki_attachment_versions "
                + "WHERE parent_page = ? AND file_name = ? "
                + "ORDER BY version DESC";

        final List< Attachment> history = new ArrayList<>();
        try (final Connection conn = m_dataSource.getConnection(); final PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, att.getParentName());
            ps.setString(2, att.getFileName());
            try (final ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    history.add(mapAttachment(att.getParentName(), rs));
                }
            }
        } catch (final SQLException e) {
            LOG.error("Failed to get version history for {}", att, e);
        }
        return history;
    }

    @Override
    public void deleteVersion(final Attachment att) throws ProviderException {
        final int targetVersion = (att.getVersion() == WikiProvider.LATEST_VERSION)
                ? resolveVersion(att.getParentName(), att.getFileName(), WikiProvider.LATEST_VERSION)
                : att.getVersion();

        if (targetVersion <= 0) {
            return;
        }

        final String sql = "DELETE FROM wiki_attachment_versions WHERE parent_page = ? AND file_name = ? AND version = ?";
        try (final Connection conn = m_dataSource.getConnection(); final PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, att.getParentName());
            ps.setString(2, att.getFileName());
            ps.setInt(3, targetVersion);
            ps.executeUpdate();
        } catch (final SQLException e) {
            throw new ProviderException("Failed to delete version " + targetVersion + " of " + att.getName(), e);
        }
    }

    @Override
    public void deleteAttachment(final Attachment att) throws ProviderException {
        final String sql = "DELETE FROM wiki_attachment_versions WHERE parent_page = ? AND file_name = ?";
        try (final Connection conn = m_dataSource.getConnection(); final PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, att.getParentName());
            ps.setString(2, att.getFileName());
            ps.executeUpdate();
        } catch (final SQLException e) {
            throw new ProviderException("Failed to delete attachment " + att.getName(), e);
        }
    }

    @Override
    public void moveAttachmentsForPage(final String oldParent, final String newParent) throws ProviderException {
        final String sql = "UPDATE wiki_attachment_versions SET parent_page = ? WHERE parent_page = ?";
        try (final Connection conn = m_dataSource.getConnection(); final PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newParent);
            ps.setString(2, oldParent);
            ps.executeUpdate();
        } catch (final SQLException e) {
            throw new ProviderException("Failed to move attachments from " + oldParent + " to " + newParent, e);
        }
    }

    private int resolveVersion(final String parentPage, final String fileName, final int requestedVersion) throws ProviderException {
        if (requestedVersion != WikiProvider.LATEST_VERSION) {
            return requestedVersion;
        }
        final String sql = "SELECT MAX(version) FROM wiki_attachment_versions WHERE parent_page = ? AND file_name = ?";
        try (final Connection conn = m_dataSource.getConnection(); final PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, parentPage);
            ps.setString(2, fileName);
            try (final ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (final SQLException e) {
            throw new ProviderException("Failed to resolve latest version for " + parentPage + "/" + fileName, e);
        }
    }

    private Attachment mapAttachment(final String parentPage, final ResultSet rs) throws SQLException {
        final String fileName = rs.getString("file_name");
        final Attachment att = new org.apache.wiki.attachment.Attachment(m_engine, parentPage, fileName);
        att.setVersion(rs.getInt("version"));
        att.setAuthor(rs.getString("author"));
        final String changeNote = rs.getString("change_note");
        if (changeNote != null) {
            att.setAttribute(Page.CHANGENOTE, changeNote);
        }
        att.setSize(rs.getLong("file_size"));
        final long epochMillis = rs.getLong("last_modified");
        if (!rs.wasNull()) {
            att.setLastModified(new Date(epochMillis));
        }
        if (m_disableCache != null && m_disableCache.matcher(fileName).matches()) {
            att.setCacheable(false);
        }
        return att;
    }

    private static void closeQuietly(final AutoCloseable... closeables) {
        for (final AutoCloseable c : closeables) {
            if (c != null) {
                try {
                    c.close();
                } catch (final Exception ignored) {
                }
            }
        }
    }
}
