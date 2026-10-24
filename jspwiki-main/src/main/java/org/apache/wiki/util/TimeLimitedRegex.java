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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Evaluates {@link java.util.regex} matches with an upper bound on matching
 * time. The JDK regex engine backtracks, so a pathological pattern such as
 * {@code (a+)+$} matched against a long non-matching subject takes exponential
 * time and pins a CPU ("catastrophic backtracking" / ReDoS). When either the
 * pattern or the subject is user-controlled — as with wiki plugin parameters
 * matched against page content — every match must be time-boxed.
 * <p>
 * The subject is wrapped in a deadline-checking {@link CharSequence}: the
 * engine reads the subject on every matching step, so a runaway match is
 * aborted shortly after the deadline passes and reported as a non-match.
 *
 * @since 3.0.1
 */
public final class TimeLimitedRegex {

    private static final Logger LOG = LogManager.getLogger(TimeLimitedRegex.class);

    /**
     * Default time budget for a single match, in milliseconds. Value is
     * <tt>{@value}</tt>.
     */
    public static final long DEFAULT_TIMEOUT_MILLIS = 1_000L;

    /**
     * The deadline is re-checked every this many character accesses; must be a
     * power of two.
     */
    private static final int CHECK_MASK = 1_024 - 1;

    private TimeLimitedRegex() {
    }

    /**
     * Attempts to match the whole subject against the given pattern, giving up
     * after {@link #DEFAULT_TIMEOUT_MILLIS} milliseconds.
     *
     * @param subject the character sequence to match.
     * @param pattern the compiled pattern.
     * @return {@code true} if the whole subject matches; {@code false} if it
     * doesn't, or if the time budget was exceeded.
     */
    public static boolean matches(final CharSequence subject, final Pattern pattern) {
        return matches(subject, pattern, DEFAULT_TIMEOUT_MILLIS);
    }

    /**
     * Attempts to match the whole subject against the given pattern, giving up
     * after {@code timeoutMillis} milliseconds. A match that exceeds its budget
     * is logged as a warning and treated as a non-match.
     *
     * @param subject the character sequence to match.
     * @param pattern the compiled pattern.
     * @param timeoutMillis time budget for this match, in milliseconds.
     * @return {@code true} if the whole subject matches; {@code false} if it
     * doesn't, or if the time budget was exceeded.
     */
    public static boolean matches(final CharSequence subject, final Pattern pattern, final long timeoutMillis) {
        final Matcher matcher = pattern.matcher(new DeadlineCharSequence(subject, System.nanoTime() + timeoutMillis * 1_000_000L));
        try {
            return matcher.matches();
        } catch (final MatchTimeoutException e) {
            LOG.warn("Regular expression '{}' exceeded its {} ms matching budget on a {} character subject; treating as no match",
                    pattern.pattern(), timeoutMillis, subject.length());
            return false;
        }
    }

    /**
     * Thrown internally when the matching deadline has passed.
     */
    private static final class MatchTimeoutException extends RuntimeException {

        private static final long serialVersionUID = 1L;
    }

    /**
     * View over a subject that aborts any regex match still reading it after
     * the deadline.
     */
    private static final class DeadlineCharSequence implements CharSequence {

        private final CharSequence m_subject;
        private final long m_deadlineNanos;
        private int m_accesses;

        DeadlineCharSequence(final CharSequence subject, final long deadlineNanos) {
            m_subject = subject;
            m_deadlineNanos = deadlineNanos;
        }

        @Override
        public char charAt(final int index) {
            if ((++m_accesses & CHECK_MASK) == 0 && System.nanoTime() - m_deadlineNanos > 0) {
                throw new MatchTimeoutException();
            }
            return m_subject.charAt(index);
        }

        @Override
        public int length() {
            return m_subject.length();
        }

        @Override
        public CharSequence subSequence(final int start, final int end) {
            return new DeadlineCharSequence(m_subject.subSequence(start, end), m_deadlineNanos);
        }

        @Override
        public String toString() {
            return m_subject.toString();
        }

    }
}