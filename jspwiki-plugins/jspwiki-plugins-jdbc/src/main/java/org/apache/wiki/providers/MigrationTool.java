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

import java.nio.file.Path;
import java.util.Collection;
import java.util.Properties;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.core.Page;

/**
 * this needs testing
 */
public class MigrationTool {

    //migrates from the file system into a jdbc provider
    public static void ingestFileSystemDirectory(final Engine engine, final Path fsRoot, final JdbcPageProvider target)
            throws Exception {

        VersioningFileProvider existingFiles = new VersioningFileProvider();
        Properties properties = new Properties();
        properties.setProperty(FileSystemProvider.PROP_PAGEDIR, fsRoot.toString());
        existingFiles.initialize(engine, properties);
        Collection<Page> allPages = existingFiles.getAllPages();
        for (Page page : allPages) {
            for (int i = page.getVersion(); i >= 0; i++) {
                target.putPageText(page, existingFiles.getPageText(page.getName(), i));
            }
        }

    }
}
