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
package org.apache.wiki.providers;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.wiki.api.core.Attachment;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.core.Page;
import org.apache.wiki.api.exceptions.ProviderException;
import org.apache.wiki.api.providers.AttachmentProvider;
import org.apache.wiki.api.providers.PageProvider;
import org.apache.wiki.api.spi.Wiki;

/**
 * this needs testing
 */
public class MigrationTool {

    private static final Logger LOG = LogManager.getLogger(MigrationTool.class);

    //migrates from the file system into a jdbc provider
    public static void migratePageProviders(final Engine engine, final PageProvider existing, final PageProvider target)
            throws Exception {

        final Collection<Page> allPages = existing.getAllPages();
        int migratedPages = 0;
        for (final Page latestPage : allPages) {
            final String pageName = latestPage.getName();
            //if (skipExisting && target.pageExists(pageName)) {
            //    LOG.info("Skipping already-present page '{}'", pageName);
            //    continue;
            //}

            final int maxVersion = Math.max(1, latestPage.getVersion());
            for (int v = 1; v <= maxVersion; v++) {
                final Page versionInfo = existing.getPageInfo(pageName, v);
                final String text = existing.getPageText(pageName, v);
                if (text == null && versionInfo == null) {
                    continue;
                }
                final Page toWrite = (versionInfo != null) ? versionInfo : latestPage;
                toWrite.setVersion(v);
                target.putPageText(toWrite, text != null ? text : "");
            }

            migratedPages++;
            if (migratedPages % 100 == 0) {
                LOG.info("Migrated {} pages from ", migratedPages);
            }
        }
        LOG.info("Completed file-system ingestion from {}", migratedPages);

    }

    public static class MigrationStats {

        public int pagesScanned;
        public int attachmentsMigrated;
        public int versionsMigrated;
        public int skipped;
        public int errors;
    }

    public static MigrationStats migrateAttachmentProvider(
            final Engine engine,
            final AttachmentProvider sourceProvider,
            final AttachmentProvider targetJdbcProvider,
            final boolean dryRun,
            final boolean skipExisting) throws Exception {

        final MigrationStats stats = new MigrationStats();
        Objects.requireNonNull(sourceProvider, "sourceProvider must not be null");
        Objects.requireNonNull(targetJdbcProvider, "targetJdbcProvider must not be null");

        // Enumerate attachments across the source provider from epoch 0
        final List<Attachment> allLatest = sourceProvider.listAllChanged(new Date(0L));
        final Set<String> seenParents = new HashSet<>();

        for (final Attachment latestAtt : allLatest) {
            if (latestAtt == null || latestAtt.getParentName() == null || latestAtt.getFileName() == null) {
                continue;
            }
            seenParents.add(latestAtt.getParentName());

            final Page parentPage = Wiki.contents().page(engine, latestAtt.getParentName());

            try {
                if (skipExisting && targetJdbcProvider.getAttachmentInfo(
                        parentPage, latestAtt.getFileName(), AttachmentProvider.LATEST_VERSION) != null) {
                    LOG.info("Skipping existing attachment '{}/{}'",
                            latestAtt.getParentName(), latestAtt.getFileName());
                    stats.skipped++;
                    continue;
                }

                List<Attachment> history = sourceProvider.getVersionHistory(latestAtt);
                if (history == null || history.isEmpty()) {
                    history = List.of(latestAtt);
                } else {
                    // getVersionHistory returns newest-first; sort ascending for sequential ingestion
                    history = new ArrayList<>(history);
                    history.sort(Comparator.comparingInt(Attachment::getVersion));
                }

                for (final Attachment verMeta : history) {
                    final Attachment toWrite = new org.apache.wiki.attachment.Attachment(
                            engine, latestAtt.getParentName(), latestAtt.getFileName());
                    toWrite.setVersion(verMeta.getVersion());
                    toWrite.setSize(verMeta.getSize());
                    if (verMeta.getLastModified() != null) {
                        toWrite.setLastModified(verMeta.getLastModified());
                    }
                    if (verMeta.getAuthor() != null) {
                        toWrite.setAuthor(verMeta.getAuthor());
                    }
                    final String changeNote = verMeta.getAttribute(Page.CHANGENOTE);
                    if (changeNote != null) {
                        toWrite.setAttribute(Page.CHANGENOTE, changeNote);
                    }

                    if (!dryRun) {
                        try (final InputStream in = sourceProvider.getAttachmentData(verMeta)) {
                            targetJdbcProvider.putAttachmentData(toWrite, in);
                        }
                    }
                    stats.versionsMigrated++;
                }

                stats.attachmentsMigrated++;
                if (stats.attachmentsMigrated % 100 == 0) {
                    LOG.info("Migrated {} attachments ({} versions) so far...",
                            stats.attachmentsMigrated, stats.versionsMigrated);
                }
            } catch (final ProviderException | IOException e) {
                stats.errors++;
                LOG.error("Failed migrating attachment '{}' for page '{}'",
                        latestAtt.getFileName(), latestAtt.getParentName(), e);
            }
        }

        stats.pagesScanned = seenParents.size();
        LOG.info("Attachment migration complete (dryRun={}): pages={}, attachments={}, versions={}, skipped={}, errors={}",
                dryRun, stats.pagesScanned, stats.attachmentsMigrated, stats.versionsMigrated, stats.skipped, stats.errors);
        return stats;
    }

}
