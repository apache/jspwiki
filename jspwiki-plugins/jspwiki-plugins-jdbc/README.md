# Installing the JDBC Page and Attachment Providers

1. Add the JAR from this module into your jspwiki/WEB-INF/lib folder
2. Add all the JDBC drivers from your database vendor of choice to jspwiki/WEB-INF/lib folder
3. Create the database and user account for jspwiki (vendor specific)
4. Add the following settings to your jspwiki-custom.properties file


    jspwiki.pageProvider = org.apache.wiki.providers.JdbcPageProvider
    jspwiki.jdbcPageProvider.autoCreateSchema = true

    jspwiki.attachment.provider = org.apache.wiki.providers.JdbcAttachmentProvider
    jspwiki.jdbcAttachmentProvider.dataSource = java:comp/env/jdbc/WikiDS
    # Optional:
    # jspwiki.basicAttachmentProvider.disableCache = .*\.xlsx

    # Option A: Direct JDBC URL (SQLite / PostgreSQL / MariaDB / MSSQL)
    jspwiki.jdbcPageProvider.url = jdbc:postgresql://localhost:5432/jspwiki
    jspwiki.jdbcPageProvider.user = jspwiki
    jspwiki.jdbcPageProvider.password = secret

    # Option B: Container/App-managed DataSource via JNDI (recommended for production pools)
    # so in tomcat, the connection specifics go in wiki/META-INF/context.xml
    # jspwiki.jdbcPageProvider.jndiName = java:comp/env/jdbc/JSPWikiDB

    # more efficient searches, however turn off if you use some kind of obfuscation algorithm
    jspwiki.jdbcPageProvider.searchPreFilter = true

5. Start up the server and test



# Local Testing and CI Jobs

Configuration data for databases is loaded from Environment variables first, then 
via a configuration file. see ./src/test/resources/jdbc-test-local.properties-sample.
Copy that file to ./src/test/resources/jdbc-test-local.properties and populate
what databases you have available and the required credentials for testing purposes.



Precedence & Configuration KeysEnvironment variables override local properties so
 CI jobs can inject credentials without touching local files:VendorEnv Vars (CI)Local Config Keys 
(src/test/resources/jdbc-test-local.properties)SQLite(built-in in-memory)(built-in in-memory)

PostgreSQL
| Env Var | Properties file|
| --- | --- |
|JSPWIKI_TEST_PG_URL | pg.url
|JSPWIKI_TEST_PG_USER | pg.user
|JSPWIKI_TEST_PG_PASSWORD | pg.password
|JSPWIKI_TEST_PG_DRIVER | pg.driver

MariaDB
| Env Var | Properties file|
| --- | --- |
|JSPWIKI_TEST_MARIADB_URL|mariadb.url
|JSPWIKI_TEST_MARIADB_USER|mariadb.user
|JSPWIKI_TEST_MARIADB_PASSWORD|mariadb.password 
|JSPWIKI_TEST_MARIADB_DRIVER|mariadb.driver


MSSQL
| Env Var | Properties file|
| --- | --- |
|JSPWIKI_TEST_MSSQL_URL|mssql.url
|JSPWIKI_TEST_MSSQL_USER|mssql.user
|JSPWIKI_TEST_MSSQL_PASSWORD|mssql.password 
|JSPWIKI_TEST_MSSQL_DRIVER| mssql.driver