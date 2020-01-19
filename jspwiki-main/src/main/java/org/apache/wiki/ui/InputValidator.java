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
package org.apache.wiki.ui;

import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiSession;
import org.apache.wiki.i18n.InternationalizationManager;
import org.apache.wiki.preferences.Preferences;

import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides basic validation services for HTTP parameters. Three standard validators are provided: email address, identifier and
 * standard input. Standard input validator will reject any HTML-like input, and any of a number of special characters. ID validator
 * rejects HTML and quoted strings, and a couple of special characters.
 *
 * @since 2.3.54
 */
public final class InputValidator {
    /** Standard input validator. */
    public static final int STANDARD       = 0;

    /** Input validator for e-mail addresses. **/
    public static final int EMAIL          = 1;

    /** @since 2.4.82 */
    public static final int ID             = 2;

    protected static final Pattern EMAIL_PATTERN  = Pattern.compile( "^[0-9a-zA-Z-_\\.\\+]+@([0-9a-zA-Z-_]+\\.)+[a-zA-Z]+$" );

    protected static final Pattern UNSAFE_PATTERN = Pattern.compile( "[\\x00\\r\\n\\x0f\"':<>\\[\\];#&@\\xff{}\\$%\\\\]" );

    /** Used when checking against IDs such as a full name when saving groups.
     *  @since 2.4.82 */
    protected static final Pattern ID_PATTERN     = Pattern.compile( "[\\x00\\r\\n\\x0f\"'<>;&\\xff{}]" );

    private final String           m_form;

    private final WikiSession      m_session;

    private final WikiContext      m_context;

    /**
     * Constructs a new input validator for a specific form and wiki session. When validation errors are detected, they will be added to
     * the wiki session's messages.
     *
     * @param form the ID or name of the form this validator should be associated with
     * @param context the wiki context
     */
    public InputValidator( final String form, final WikiContext context ) {
        m_form = form;
        m_context = context;
        m_session = context.getWikiSession();
    }

    /**
     * Validates a string against the {@link #STANDARD} validator and additionally checks that the value is not <code>null</code> or blank.
     *
     * @param input the string to validate
     * @param label the label for the string or field ("E-mail address")
     * @return returns <code>true</code> if valid, <code>false</code> otherwise
     */
    public boolean validateNotNull( final String input, final String label ) {
        return validateNotNull( input, label, STANDARD );
    }

    /**
     * Validates a string against a particular pattern type and additionally checks that the value is not <code>null</code> or blank.
     * Delegates to {@link #validate(String, String, int)}.
     *
     * @param input the string to validate
     * @param label the label for the string or field ("E-mail address")
     * @param type the pattern type to use (<em>e.g.</em>, {@link #STANDARD}, {@link #EMAIL}.
     * @return returns <code>true</code> if valid, <code>false</code> otherwise
     */
    public boolean validateNotNull( final String input, final String label, final int type ) {
        if ( isBlank( input ) ) {
            final ResourceBundle rb = Preferences.getBundle( m_context, InternationalizationManager.CORE_BUNDLE );
            m_session.addMessage( m_form, MessageFormat.format( rb.getString("validate.cantbenull"), label ) );
            return false;
        }
        return validate( input, label, type ) && !isBlank( input );
    }

    /**
     * Validates a string against a particular pattern type: e-mail address, standard HTML input, etc. Note that a blank or null string will
     * always validate.
     *
     * @param input the string to validate
     * @param label the label for the string or field ("E-mail address")
     * @param type the target pattern to validate against ({@link #STANDARD}, {@link #EMAIL})
     * @return returns <code>true</code> if valid, <code>false</code> otherwise
     */
    public boolean validate( final String input, final String label, final int type ) {
        // If blank, it's valid
        if ( isBlank( input ) ) {
            return true;
        }

        final ResourceBundle rb = Preferences.getBundle( m_context, InternationalizationManager.CORE_BUNDLE );

        // Otherwise, see if it matches the pattern for the target type
        final Matcher matcher;
        final boolean valid;
        switch( type ) {
        case STANDARD:
            matcher = UNSAFE_PATTERN.matcher( input );
            valid = !matcher.find();
            if ( !valid ) {
                // MessageTag already invokes replaceEntities()
                // Object[] args = { label, "&quot;&#39;&lt;&gt;;&amp;[]#\\@{}%$" };
                final Object[] args = { label, "\'\"<>;&[]#\\@{}%$" };
                m_session.addMessage( m_form, MessageFormat.format( rb.getString( "validate.unsafechars" ), args ) );
            }
            return valid;
        case EMAIL:
            matcher = EMAIL_PATTERN.matcher( input );
            valid = matcher.matches();
            if ( !valid ) {
                final Object[] args = { label };
                m_session.addMessage( m_form, MessageFormat.format( rb.getString( "validate.invalidemail" ), args ) );
            }
            return valid;
        case ID:
            matcher = ID_PATTERN.matcher( input );
            valid = !matcher.find();
            if ( !valid ) {
                // MessageTag already invokes replaceEntities()
                // Object[] args = { label, "&quot;&#39;&lt;&gt;;&amp;{}" };
                final Object[] args = { label, "\'\"<>;&{}" };
                m_session.addMessage( m_form, MessageFormat.format( rb.getString( "validate.unsafechars" ), args ) );
            }
            return valid;
         default:
             break;
        }
        throw new IllegalArgumentException( "Invalid input type." );
    }

    /**
     * Returns <code>true</code> if a supplied string is null or blank
     *
     * @param input the string to check
     * @return <code>true</code> if <code>null</code> or blank (zero-length); <code>false</code> otherwise
     */
    public static boolean isBlank( final String input ) {
        return input == null || input.trim().length() < 1;
    }

}
