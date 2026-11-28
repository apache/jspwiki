-- Current page state (also covers comment pages stored as wiki pages)
CREATE TABLE wiki_pages (
    page_name       VARCHAR(255) NOT NULL PRIMARY KEY,
    latest_version  INTEGER      NOT NULL,
    author          VARCHAR(255),
    change_note     VARCHAR(1024),
    view_count      BIGINT,
    last_modified   BIGINT       NOT NULL, -- UTC epoch milliseconds
    page_text       CLOB         NOT NULL  -- TEXT / NVARCHAR(MAX) in vendor DDL
);

CREATE INDEX idx_wiki_pages_modified ON wiki_pages (last_modified);

-- Historical versions (1..latest_version)
CREATE TABLE wiki_page_versions (
    page_name       VARCHAR(255) NOT NULL,
    version_num     INTEGER      NOT NULL,
    author          VARCHAR(255),
    change_note     VARCHAR(1024),
    view_count      BIGINT,
    last_modified   BIGINT       NOT NULL, -- UTC epoch milliseconds
    page_text       CLOB         NOT NULL,
    PRIMARY KEY (page_name, version_num),
    CONSTRAINT fk_versions_page FOREIGN KEY (page_name)
        REFERENCES wiki_pages (page_name) ON DELETE CASCADE
);

-- Custom page properties (e.g., '@' attributes) per version
CREATE TABLE wiki_page_properties (
    page_name       VARCHAR(255) NOT NULL,
    version_num     INTEGER      NOT NULL,
    prop_key        VARCHAR(255) NOT NULL,
    prop_value      VARCHAR(4096) NOT NULL,
    PRIMARY KEY (page_name, version_num, prop_key),
    CONSTRAINT fk_props_version FOREIGN KEY (page_name, version_num)
        REFERENCES wiki_page_versions (page_name, version_num) ON DELETE CASCADE
);
