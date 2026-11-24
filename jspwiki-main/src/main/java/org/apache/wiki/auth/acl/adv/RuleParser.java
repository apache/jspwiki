/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.wiki.auth.acl.adv;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Parses the expression of an advanced access rule. The operators are {@code AND}, {@code OR} and {@code NOT}, written in upper
 * case, and parentheses group. {@code NOT} binds tighter than {@code AND}, which binds tighter than {@code OR}.
 *
 * @since 3.0.2
 */
public class RuleParser {

    private static final Pattern PARENTHESES = Pattern.compile( "([()])" );
    private static final Pattern WHITESPACE = Pattern.compile( "\\s+" );

    private final List< String > tokens;
    private int pos;

    /**
     * Creates a parser.
     *
     * @param expr the expression, without the {@code ALLOW} keyword and the action.
     */
    public RuleParser( final String expr ) {
        tokens = Arrays.asList( WHITESPACE.split( PARENTHESES.matcher( expr ).replaceAll( " $1 " ).trim() ) );
    }

    /**
     * Parses the expression.
     *
     * @return the root of the expression tree.
     * @throws IllegalArgumentException if the expression is not valid.
     */
    public RuleNode parse() {
        final RuleNode node = parseOr();
        if( pos < tokens.size() ) {
            throw new IllegalArgumentException( "Unexpected token: " + tokens.get( pos ) );
        }
        return node;
    }

    private RuleNode parseOr() {
        RuleNode left = parseAnd();
        while( match( "OR" ) ) {
            left = new OperatorNode( OperatorNode.Operator.OR, left, parseAnd() );
        }
        return left;
    }

    private RuleNode parseAnd() {
        RuleNode left = parseNot();
        while( match( "AND" ) ) {
            left = new OperatorNode( OperatorNode.Operator.AND, left, parseNot() );
        }
        return left;
    }

    private RuleNode parseNot() {
        if( match( "NOT" ) ) {
            return new NotNode( parseNot() );
        }
        return parsePrimary();
    }

    private RuleNode parsePrimary() {
        if( match( "(" ) ) {
            final RuleNode expr = parseOr();
            expect( ")" );
            return expr;
        }
        return new RoleNode( expectIdentifier() );
    }

    private boolean match( final String token ) {
        if( pos < tokens.size() && tokens.get( pos ).equals( token ) ) {
            pos++;
            return true;
        }
        return false;
    }

    private void expect( final String token ) {
        if( !match( token ) ) {
            throw new IllegalArgumentException( "Expected '" + token + "'" );
        }
    }

    private String expectIdentifier() {
        if( pos < tokens.size() ) {
            final String token = tokens.get( pos );
            if( !token.isEmpty() && !isReserved( token ) ) {
                pos++;
                if( token.indexOf( ',' ) >= 0 ) {
                    throw new IllegalArgumentException( "A comma cannot be used together with AND, OR or NOT: " + token );
                }
                return token;
            }
        }
        throw new IllegalArgumentException( "Expected a name at position " + pos );
    }

    private static boolean isReserved( final String token ) {
        return "AND".equals( token ) || "OR".equals( token ) || "NOT".equals( token ) || "(".equals( token ) || ")".equals( token );
    }

}
