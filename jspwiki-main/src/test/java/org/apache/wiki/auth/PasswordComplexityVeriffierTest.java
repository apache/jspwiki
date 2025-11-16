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

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.Properties;
import org.apache.commons.lang3.StringUtils;
import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiPage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PasswordComplexityVeriffierTest {

    TestEngine engine;
    WikiContext context;

    public PasswordComplexityVeriffierTest() throws Exception {
        Properties props = new Properties();
        props.load(new FileInputStream(new File("src/main/resources/ini/jspwiki.properties")));
        // 8 is the default. 15 or more is recommended for high security systems
        props.setProperty("jspwiki.credentials.length.min", "8");
// 64 is the default
        props.setProperty("jspwiki.credentials.length.max", "20");
// 1 is the default, 2 or more is recommended for high security systems
        props.setProperty("jspwiki.credentials.minUpper", "1");
// 1 is the default, 2 or more is recommended for high security systems
        props.setProperty("jspwiki.credentials.minLower", "1");
// 1 is the default, 2 or more is recommended for high security systems
        props.setProperty("jspwiki.credentials.minDigits=", "1");
// 1 is the default, 2 or more is recommended for high security systems
        props.setProperty("jspwiki.credentials.minSymbols", "1");
// allow X repeating characters but no more. 1 is the default.
// i.e. 1 with "password" is ok but "passsword" is not
        props.setProperty("jspwiki.credentials.repeatingCharacters", "1");
        engine = TestEngine.build(props);
        context = new WikiContext(engine,
                new WikiPage(engine, "test"));
    }

    @Test
    public void testValidate() {

        List<String> result = PasswordComplexityVeriffier.validate("e,#Em1KG1!tez8EYmi?,", context);
        Assertions.assertTrue(result.isEmpty(), StringUtils.join(result));

        //too long
        result = PasswordComplexityVeriffier.validate("e,#Em1KG1!tez8EYmi?,1234", context);
        Assertions.assertTrue(!result.isEmpty(), StringUtils.join(result));

        //no upper, digits symbols
        result = PasswordComplexityVeriffier.validate("abcedefghi", context);
        Assertions.assertTrue(!result.isEmpty(), StringUtils.join(result));

        //too short
        result = PasswordComplexityVeriffier.validate("abc", context);
        Assertions.assertTrue(!result.isEmpty(), StringUtils.join(result));

        //no lower, digits, symbols
        result = PasswordComplexityVeriffier.validate("ABCEDEFGHI", context);
        Assertions.assertTrue(!result.isEmpty(), StringUtils.join(result));

        //too many repeating
        result = PasswordComplexityVeriffier.validate("a@CEDEFGHI222", context);
        Assertions.assertTrue(!result.isEmpty(), StringUtils.join(result));

        result = PasswordComplexityVeriffier.validate("a@CEDEFGHI222a", context);
        Assertions.assertTrue(!result.isEmpty(), StringUtils.join(result));

        result = PasswordComplexityVeriffier.validate("aaa@CEDEFGHI2a", context);
        Assertions.assertTrue(!result.isEmpty(), StringUtils.join(result));

        result = PasswordComplexityVeriffier.validate("aaa@CEbbbbGHI2a", context);
        Assertions.assertTrue(!result.isEmpty(), StringUtils.join(result));

        result = PasswordComplexityVeriffier.validate("a@CEbbGH4444", context);
        Assertions.assertTrue(!result.isEmpty(), StringUtils.join(result));

        result = PasswordComplexityVeriffier.validate("aaaa@CEbbGH444", context);
        Assertions.assertTrue(!result.isEmpty(), StringUtils.join(result));
    }

}
