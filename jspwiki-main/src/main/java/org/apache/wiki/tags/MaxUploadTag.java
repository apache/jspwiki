/*
 * Copyright 2025 The Apache Software Foundation.
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
package org.apache.wiki.tags;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.wiki.util.TextUtil;

/**
 * Outputs the server's configured maximum file upload size.
 * 
 * @since 3.0.0
 */
public class MaxUploadTag extends WikiTagBase {

    private static final Logger LOG = LogManager.getLogger(MaxUploadTag.class);

    @Override
    public int doWikiStartTag() throws Exception {
        String maxUploadSize = m_wikiContext.getEngine().getWikiProperties().getProperty("jspwiki.attachment.maxsize");
        if (maxUploadSize != null) {
            try {
                long bytes = Long.parseLong(maxUploadSize);
                String humanFormat = humanReadableByteCountBin(bytes);
                pageContext.getOut().print(TextUtil.replaceEntities(humanFormat));
            } catch (NumberFormatException ex) {
                LOG.warn("Parse error from configuration setting jspwiki.attachment.maxsize " + ex.getMessage());
            }
        }
        return SKIP_BODY;
    }

    /*
    Binary (1 Ki = 1,024)
    from https://stackoverflow.com/a/3758880/1203182
     */
    public static String humanReadableByteCountBin(long bytes) {
        long absB = bytes == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(bytes);
        if (absB < 1024) {
            return bytes + " B";
        }
        long value = absB;
        CharacterIterator ci = new StringCharacterIterator("KMGTPE");
        for (int i = 40; i >= 0 && absB > 0xfffccccccccccccL >> i; i -= 10) {
            value >>= 10;
            ci.next();
        }
        value *= Long.signum(bytes);
        return String.format("%.1f %ciB", value / 1024.0, ci.current());
    }
}
