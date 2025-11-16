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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.ResourceBundle;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.i18n.InternationalizationManager;

/**
 * a simple password complexity checker
 *
 * @since 3.0.0
 */
public final class PasswordComplexityVeriffier {

    private PasswordComplexityVeriffier() {
    }

    /**
     * validates a password, returns a list of size 0 is all the checks
     * pass.list of size > 0 = password is invalid with each item in the list
     * containing the rule that failed.suitable to direct display to the user.
     * i.e. the password was too short, or has too many repeating characters,
     * etc.
     *
     * @param pwd
     * @param context
     * @return see above
     */
    public static List<String> validate(String pwd, Context context) {
        Properties wikiProps = context.getEngine().getWikiProperties();
        final ResourceBundle rb = ResourceBundle.getBundle(InternationalizationManager.CORE_BUNDLE, context.getWikiSession().getLocale());

        int minLength = Integer.parseInt(wikiProps.getProperty("jspwiki.credentials.length.min", "8"));
        int maxLength = Integer.parseInt(wikiProps.getProperty("jspwiki.credentials.length.max", "64"));
        int minUpper = Integer.parseInt(wikiProps.getProperty("jspwiki.credentials.minUpper", "1"));
        int minLower = Integer.parseInt(wikiProps.getProperty("jspwiki.credentials.minLower", "1"));
        int minDigits = Integer.parseInt(wikiProps.getProperty("jspwiki.credentials.minDigits", "1"));
        int minSymbols = Integer.parseInt(wikiProps.getProperty("jspwiki.credentials.minSymbols", "1"));
        int maxRepeats = Integer.parseInt(wikiProps.getProperty("jspwiki.credentials.repeatingCharacters", "1"));
        //potential future enhancement, detect common patterns like keyboard walks, asdf, etc
        //boolean allowCommonPatterns = "true".equalsIgnoreCase(wikiProps.getProperty("jspwiki.credentials.allowCommonPatterns", "false"));
        //potential future enhancements, detect common numerical patterns, such as 1234 oe 4321
        //boolean allowSequentialNumbers = "true".equalsIgnoreCase(wikiProps.getProperty("jspwiki.credentials.allowSequentialNumberPatterns", "false"));
        //perhaps a regex pattern can be added in the future

        List<String> problems = new ArrayList<>();
        if (pwd.length() > maxLength) {
            problems.add(MessageFormat.format(rb.getString("pwdcheck.toolong"), maxLength));
        }
        if (pwd.length() < minLength) {
            problems.add(MessageFormat.format(rb.getString("pwdcheck.tooshort"), minLength));
        }
        char[] cred = pwd.toCharArray();
        int upper = 0;
        int lower = 0;
        int digits = 0;
        int other = 0;
        //the higest number of repeats
        int repeats = 0;
        int localrepeats = 0;
        boolean repeatCheck = false;
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
                if (cred[i] == cred[i - 1]) {
                    //ok we have a repeat
                    if (repeatCheck) {
                        //existing sequence
                        localrepeats++;
                    } else {
                        //this is a new sequence
                        repeatCheck = true;
                        localrepeats = 1;
                    }

                } else {
                    repeatCheck = false;
                    if (localrepeats > repeats) {
                        repeats = localrepeats;
                    }
                    localrepeats = 0;
                }
            }
        }

        if (repeatCheck) {
            if (localrepeats > repeats) {
                repeats = localrepeats;
            }
        }

        if (maxRepeats > 0 && repeats > maxRepeats) {
            problems.add(MessageFormat.format(rb.getString("pwdcheck.repeats"), maxRepeats));
        }
        if (minUpper > 0 && upper < minUpper) {
            problems.add(MessageFormat.format(rb.getString("pwdcheck.minUpper"), minUpper));
        }
        if (minLower > 0 && lower < minLower) {
            problems.add(MessageFormat.format(rb.getString("pwdcheck.minLower"), minUpper));
        }
        if (minDigits > 0 && digits < minDigits) {
            problems.add(MessageFormat.format(rb.getString("pwdcheck.minDigits"), minUpper));
        }
        if (minSymbols > 0 && other < minSymbols) {
            problems.add(MessageFormat.format(rb.getString("pwdcheck.minOther"), minSymbols));
        }
        return problems;

    }
}
