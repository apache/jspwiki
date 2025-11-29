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
package org.apache.wiki.diff;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.exceptions.NoRequiredPropertyException;

/**
 * SVN/Git style diff provider. Uses the DiffLib, ASF 2.0 licensed.
 *
 * @since 3.0.0
 */
public class SvnStyleDiffProvider implements DiffProvider {

    public static final String CSS_DIFF_ADDED = "<tr class=\"diffadd\"><td>";
    public static final String CSS_DIFF_REMOVED = "<tr class=\"diffrem\"><td>";
    public static final String CSS_DIFF_UNCHANGED = "<tr class=\"diff\"><td>";
    public static final String CSS_DIFF_CLOSE = "</td></tr>\n";
    public static final String CELL_CHANGE = "</td><td>";

    @Override
    public String makeDiffHtml(Context context, String originalText, String modifiedText) {

        List<String> original = originalText.lines().toList();
        List<String> modified = modifiedText.lines().toList();
        StringBuilder ret = new StringBuilder();
        ret.append("<table class=\"diff\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\">\n");

        Patch<String> patch = DiffUtils.diff(original, modified);
        int lineNumber = 1;
        int currentOriginalLine = 0;
        int currentModifiedLine = 0;

        for (AbstractDelta<String> delta : patch.getDeltas()) {
            int originalPosition = delta.getSource().getPosition();
            int modifiedPosition = delta.getTarget().getPosition();

            // Output unchanged lines before the delta
            while (currentOriginalLine < originalPosition && currentModifiedLine < modifiedPosition) {
                ret.append(CSS_DIFF_UNCHANGED);
                ret.append(lineNumber).append(CELL_CHANGE);
                ret.append(original.get(currentOriginalLine));
                ret.append(CSS_DIFF_CLOSE);
                //System.out.println("  " + lineNumber + " " + original.get(currentOriginalLine));
                lineNumber++;
                currentOriginalLine++;
                currentModifiedLine++;
            }

            List<String> originalLines = delta.getSource().getLines();
            List<String> revisedLines = delta.getTarget().getLines();

            for (String line : originalLines) {
                ret.append(CSS_DIFF_REMOVED);
                ret.append(lineNumber).append(CELL_CHANGE);
                ret.append(line);
                ret.append(CSS_DIFF_CLOSE);
                //System.out.println("- " + lineNumber + " " + line);
                lineNumber++;
                currentOriginalLine++;
            }

            for (String line : revisedLines) {
                ret.append(CSS_DIFF_ADDED);
                ret.append(lineNumber).append(CELL_CHANGE);
                ret.append(line);
                ret.append(CSS_DIFF_CLOSE);
                //System.out.println("+ " + lineNumber + " " + line);
                lineNumber++;
                currentModifiedLine++;
            }
        }

        // Output any remaining unchanged lines at the end
        while (currentOriginalLine < original.size() && currentModifiedLine < modified.size()) {
            ret.append(CSS_DIFF_UNCHANGED);
            ret.append(lineNumber).append(CELL_CHANGE);
            ret.append(original.get(currentOriginalLine));
            ret.append(CSS_DIFF_CLOSE);

            //System.out.println("  " + lineNumber + " " + original.get(currentOriginalLine));
            lineNumber++;
            currentOriginalLine++;
            currentModifiedLine++;
        }
        ret.append("</table>\n");
        return ret.toString();
    }

    @Override
    public void initialize(Engine engine, Properties properties) throws NoRequiredPropertyException, IOException {
    }

    @Override
    public String getProviderInfo() {
        return "SvnStyleDiffProvider";
    }

}
