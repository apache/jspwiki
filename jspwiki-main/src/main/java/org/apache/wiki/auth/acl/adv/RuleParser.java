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
package org.apache.wiki.auth.acl.adv;

import java.util.*;

/**
 *
 * @author AO
 */
public class RuleParser {

    private final List<String> tokens;
    private int pos = 0;

    public RuleParser(String expr) {
        this.tokens = tokenize(expr);
    }

    public RuleNode parse() {
        RuleNode node = parseOr();
        if (pos < tokens.size()) {
            throw new IllegalArgumentException("Unexpected token: " + tokens.get(pos));
        }
        return node;
    }

    // Lowest precedence
    private RuleNode parseOr() {
        RuleNode left = parseAnd();
        while (match("OR")) {
            RuleNode right = parseAnd();
            left = new OperatorNode(OperatorNode.Operator.OR, left, right);
        }
        return left;
    }

    // Next precedence
    private RuleNode parseAnd() {
        RuleNode left = parseNot();
        while (match("AND")) {
            RuleNode right = parseNot();
            left = new OperatorNode(OperatorNode.Operator.AND, left, right);
        }
        return left;
    }

    // Handles unary NOT
    private RuleNode parseNot() {
        if (match("NOT")) {
            return new NotNode(parseNot());
        }
        return parsePrimary();
    }

    // Handles parentheses and role identifiers
    private RuleNode parsePrimary() {
        if (match("(")) {
            RuleNode expr = parseOr();
            expect(")");
            return expr;
        }
        return new RoleNode(expectIdentifier());
    }

    // --- Helpers ---
    private boolean match(String token) {
        if (pos < tokens.size() && tokens.get(pos).equalsIgnoreCase(token)) {
            pos++;
            return true;
        }
        return false;
    }

    private void expect(String token) {
        if (!match(token)) {
            throw new IllegalArgumentException("Expected '" + token + "'");
        }
    }

    private String expectIdentifier() {
        if (pos < tokens.size()) {
            String tok = tokens.get(pos++);
            if (!tok.equalsIgnoreCase("AND")
                    && !tok.equalsIgnoreCase("OR")
                    && !tok.equals("(")
                    && !tok.equals(")")) {
                return tok;
            }
        }
        throw new IllegalArgumentException("Expected identifier at position " + pos);
    }

    private static List<String> tokenize(String expr) {
        expr = expr
                .replaceAll("([()])", " $1 ")
                .replaceAll("(?i)(AND|OR|NOT)(?=\\()", "$1 ") // add space if operator followed by '('
                .replaceAll("\\s+", " ")
                .trim();
        return Arrays.asList(expr.split(" "));
    }
}
