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
package org.apache.wiki.auth;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * a simple password complexity checker
 *
 * @since 3.0.0
 */
public final class PasswordComplexityVeriffier {

    private PasswordComplexityVeriffier() {
    }

    public static List<String> validate(String pwd, Properties wikiProps) {
        int minLength = Integer.parseInt(wikiProps.getProperty("jspwiki.credentials.length.min", "15"));
        int maxLength = Integer.parseInt(wikiProps.getProperty("jspwiki.credentials.length.max", "128"));
        int minUpper = Integer.parseInt(wikiProps.getProperty("jspwiki.credentials.minUpper", "2"));
        int minLower = Integer.parseInt(wikiProps.getProperty("jspwiki.credentials.minLower", "2"));
        int minDigits = Integer.parseInt(wikiProps.getProperty("jspwiki.credentials.minDigits", "2"));
        int minSymbols = Integer.parseInt(wikiProps.getProperty("jspwiki.credentials.minSymbols", "2"));
        boolean allowReapingChars = "true".equalsIgnoreCase(wikiProps.getProperty("jspwiki.credentials.allowRepeatingCharacters", "false"));
        //potential future enhancement, detect common patterns like keyboard walks, asdf, etc
        //boolean allowCommonPatterns = "true".equalsIgnoreCase(wikiProps.getProperty("jspwiki.credentials.allowCommonPatterns", "false"));
        //potential future enhancements, detect common numerical patterns, such as 1234 oe 4321
        //boolean allowSequentialNumbers = "true".equalsIgnoreCase(wikiProps.getProperty("jspwiki.credentials.allowSequentialNumberPatterns", "false"));
        //perhaps a regex pattern can be added in the future

        List<String> problems = new ArrayList<>();
        if (pwd.length() > maxLength) {
            problems.add("too long (" + maxLength + ")");
        }
        if (pwd.length() > minLength) {
            problems.add("too short (" + minLength + ")");
        }
        char[] cred = pwd.toCharArray();
        int upper = 0;
        int lower = 0;
        int digits = 0;
        int other = 0;
        boolean hasRepeats = false;
        for (int i = 0; i < cred.length; i++) {
            if (Character.isDigit(cred[i])) {
                digits++;
            } else if (Character.isUpperCase(cred[i])) {
                upper++;
            } else if (Character.isLowerCase(cred[i])) {
                lower++;
            } else {
                other++;
            }
            if (i > 0) {
                if (!allowReapingChars && cred[i] == cred[i - 1]) {
                    hasRepeats = true;
                }
            }
        }
        if (hasRepeats) {
            problems.add("repeating characters are not allowed");
        }
        if (upper < minUpper) {
            problems.add("not enough upper case (" + minUpper + ")");
        }
        if (lower < minLower) {
            problems.add("not enough lower case (" + minLower + ")");
        }
        if (digits < minDigits) {
            problems.add("not enough digits (" + minDigits + ")");
        }
        if (other < minSymbols) {
            problems.add("not enough symbols (" + minSymbols + ")");
        }
        return problems;

    }
}
