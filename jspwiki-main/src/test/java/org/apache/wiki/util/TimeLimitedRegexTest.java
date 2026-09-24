/*
    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.
 */
package org.apache.wiki.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.regex.Pattern;

public class TimeLimitedRegexTest {

    @Test
    public void testBehavesLikePatternMatches() {
        final Pattern pattern = Pattern.compile(".*needle.*", Pattern.DOTALL);
        Assertions.assertTrue(TimeLimitedRegex.matches("haystack with a\nneedle in it", pattern));
        Assertions.assertFalse(TimeLimitedRegex.matches("haystack without one", pattern));
    }

    @Test
    public void testCatastrophicBacktrackingIsCutOff() {
        // Classic ReDoS pattern: exponential backtracking on a subject that almost matches.
        // Unbounded, this match would take longer than the age of the universe.
        final Pattern evil = Pattern.compile("(a+)+$");
        final String subject = "a".repeat(50_000) + "!";
        Assertions.assertTimeoutPreemptively(Duration.ofSeconds(3),
                () -> Assertions.assertFalse(TimeLimitedRegex.matches(subject, evil, 1_000L)));
    }

}
