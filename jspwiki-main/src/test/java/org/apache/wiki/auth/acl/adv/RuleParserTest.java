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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

/**
 * Tests the parser of the expressions of advanced access rules.
 */
public class RuleParserTest {

    private static RuleNode parse( final String expression ) {
        return new RuleParser( expression ).parse();
    }

    @Test
    public void simpleOrStatement() {
        final RuleNode result = parse( "bob OR mary" );
        Assertions.assertEquals( 2, result.getAllRoles().size() );
        Assertions.assertTrue( result.evaluate( Set.of( "bob" ) ) );
        Assertions.assertTrue( result.evaluate( Set.of( "mary" ) ) );
        Assertions.assertFalse( result.evaluate( Set.of( "john" ) ) );
    }

    @Test
    public void simpleAndStatement() {
        final RuleNode result = parse( "accounting AND finance" );
        Assertions.assertEquals( 2, result.getAllRoles().size() );
        Assertions.assertTrue( result.evaluate( Set.of( "accounting", "finance" ) ) );
        Assertions.assertTrue( result.evaluate( Set.of( "accounting", "finance", "otherDepartment" ) ) );
        Assertions.assertFalse( result.evaluate( Set.of( "accounting" ) ) );
        Assertions.assertFalse( result.evaluate( Set.of( "finance" ) ) );
        Assertions.assertFalse( result.evaluate( Set.of( "john" ) ) );
    }

    @Test
    public void complexSetup() {
        final RuleNode result = parse( "accounting AND (finance OR admin)" );
        Assertions.assertEquals( 3, result.getAllRoles().size() );
        Assertions.assertTrue( result.evaluate( Set.of( "accounting", "finance" ) ) );
        Assertions.assertTrue( result.evaluate( Set.of( "accounting", "finance", "otherDepartment" ) ) );
        Assertions.assertTrue( result.evaluate( Set.of( "accounting", "finance", "admin" ) ) );
        Assertions.assertTrue( result.evaluate( Set.of( "accounting", "admin" ) ) );
        Assertions.assertFalse( result.evaluate( Set.of( "accounting" ) ) );
        Assertions.assertFalse( result.evaluate( Set.of( "finance" ) ) );
        Assertions.assertFalse( result.evaluate( Set.of( "john" ) ) );
    }

    @Test
    public void complexSetupNot() {
        final RuleNode result = parse( "(accounting AND finance AND NOT(bob))" );
        Assertions.assertEquals( 3, result.getAllRoles().size() );
        Assertions.assertTrue( result.evaluate( Set.of( "accounting", "finance" ) ) );
        Assertions.assertTrue( result.evaluate( Set.of( "accounting", "finance", "otherDepartment" ) ) );
        Assertions.assertFalse( result.evaluate( Set.of( "accounting", "finance", "bob" ) ) );
        Assertions.assertFalse( result.evaluate( Set.of( "accounting", "admin" ) ) );
        Assertions.assertFalse( result.evaluate( Set.of( "accounting" ) ) );
        Assertions.assertFalse( result.evaluate( Set.of( "john" ) ) );
    }

    @Test
    public void notBindsTighterThanAndAndAndTighterThanOr() {
        final RuleNode result = parse( "a OR b AND NOT c" );
        Assertions.assertTrue( result.evaluate( Set.of( "a", "c" ) ) );
        Assertions.assertTrue( result.evaluate( Set.of( "b" ) ) );
        Assertions.assertFalse( result.evaluate( Set.of( "b", "c" ) ) );
        Assertions.assertFalse( result.evaluate( Set.of() ) );
    }

    @Test
    public void roleNodesKeepTheOrderTheyWereWritten() {
        final RuleNode result = parse( "x AND (y OR NOT z)" );
        Assertions.assertEquals( 3, result.getRoleNodes().size() );
        Assertions.assertEquals( "x", result.getRoleNodes().get( 0 ).getRole() );
        Assertions.assertEquals( "z", result.getRoleNodes().get( 2 ).getRole() );
    }

    @Test
    public void operatorsAreUpperCase() {
        // a lower case word is a name, so this is two names without an operator between them
        Assertions.assertThrows( IllegalArgumentException.class, () -> parse( "bob or mary" ) );
        Assertions.assertEquals( Set.of( "and", "or", "not" ), parse( "and AND or AND not" ).getAllRoles() );
    }

    @Test
    public void printedRuleReadsBackTheSame() {
        final String expression = "x AND (y OR NOT z)";
        final RuleNode first = parse( expression );
        Assertions.assertEquals( first.toString(), parse( first.toString() ).toString() );
    }

    @ParameterizedTest
    @ValueSource( strings = { "", "   ", "role1 AND", "AND role1", "role1 OR OR role2", "role1 (role2 OR role3)", "(role1 AND role2",
                              "role1 AND role2)", "role1 AND NOT", "NOT", "role1 OR AND role2", "()", "( )", "role1 AND (OR role2)",
                              "role1 AND (role2 OR)", "role1 role2", "role1 AND (NOT)", "(role1 AND) OR role2", "role1 AND ((role2)",
                              "role1 AND )role2(", "role1 AND NOT ( )", "NOT (role1 AND)", "role1 OR role2, role3", "role1 , role2" } )
    public void invalidExpressionsAreRejected( final String expression ) {
        Assertions.assertThrows( IllegalArgumentException.class, () -> parse( expression ) );
    }

}
