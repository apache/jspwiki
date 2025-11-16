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

import java.util.List;
import java.util.Set;
import org.apache.wiki.auth.AdvancedAuthorizationManager;
import org.apache.wiki.auth.acl.AdvancedAclManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * tests the rule parser for the advanced acl tool chain
 *
 * @see AdvancedAcl
 * @see AdvancedAclManager
 * @see AdvancedAuthorizationManager
 * @since 3.0.0
 */
public class RuleParserTest {

    public RuleParserTest() {
    }

    @Test
    public void simpleOrStatement() {
        RuleParser instance = new RuleParser("bob or mary");
        RuleNode result = instance.parse();
        Assertions.assertEquals(2, result.getAllRoles().size());
        Assertions.assertTrue(result.evaluate(Set.of("bob")));
        Assertions.assertTrue(result.evaluate(Set.of("mary")));
        Assertions.assertFalse(result.evaluate(Set.of("john")));
    }

    @Test
    public void simpleAndStatement() {
        RuleParser instance = new RuleParser("accounting AND finance");
        RuleNode result = instance.parse();
        Assertions.assertEquals(2, result.getAllRoles().size());
        Assertions.assertTrue(result.evaluate(Set.of("accounting", "finance")));
        Assertions.assertTrue(result.evaluate(Set.of("accounting", "finance", "otherDepartment")));
        Assertions.assertFalse(result.evaluate(Set.of("accounting")));
        Assertions.assertFalse(result.evaluate(Set.of("finance")));
        Assertions.assertFalse(result.evaluate(Set.of("john")));
    }

    @Test
    public void complexSetup() {
        RuleParser instance = new RuleParser("accounting AND (finance OR admin)");
        RuleNode result = instance.parse();
        Assertions.assertEquals(3, result.getAllRoles().size());
        Assertions.assertTrue(result.evaluate(Set.of("accounting", "finance")));
        Assertions.assertTrue(result.evaluate(Set.of("accounting", "finance", "otherDepartment")));
        Assertions.assertTrue(result.evaluate(Set.of("accounting", "finance", "admin")));
        Assertions.assertTrue(result.evaluate(Set.of("accounting", "admin")));
        Assertions.assertFalse(result.evaluate(Set.of("accounting")));
        Assertions.assertFalse(result.evaluate(Set.of("finance")));
        Assertions.assertFalse(result.evaluate(Set.of("john")));
    }

    @Test
    public void complexSetupNot() {
        RuleParser instance = new RuleParser("accounting AND finance AND NOT (bob)");
        instance.parse();

        instance = new RuleParser("(accounting AND finance AND NOT (bob))");
        RuleNode result = instance.parse();

        Assertions.assertEquals(3, result.getAllRoles().size());
        Assertions.assertTrue(result.evaluate(Set.of("accounting", "finance")));
        Assertions.assertTrue(result.evaluate(Set.of("accounting", "finance", "otherDepartment")));
        Assertions.assertFalse(result.evaluate(Set.of("accounting", "finance", "bob")));
        Assertions.assertFalse(result.evaluate(Set.of("accounting", "admin")));
        Assertions.assertFalse(result.evaluate(Set.of("accounting")));
        Assertions.assertFalse(result.evaluate(Set.of("finance")));
        Assertions.assertFalse(result.evaluate(Set.of("john")));
    }

    @Test
    public void mismatchedParan() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            RuleParser instance = new RuleParser("(accounting AND finance AND NOT(bob)");
            RuleNode parse = instance.parse();
            System.out.println(parse);
        });

        RuleParser instance = new RuleParser("(accounting AND finance AND NOT(bob))");
        instance.parse();

        List<String> invalidExprs = List.of(
                "role1 AND",
                "AND role1",
                "role1 OR OR role2",
                "role1 (role2 OR role3)",
                "(role1 AND role2",
                "role1 AND role2)",
                "role1 AND NOT",
                "NOT",
                "role1 OR AND role2",
                "()",
                "( )",
                "role1 AND (OR role2)",
                "role1 AND (role2 OR)",
                "role1 role2",
                "role1 AND (NOT)",
                "(role1 AND) OR role2",
                "role1 AND ((role2)",
                "role1 AND )role2(",
                "role1 AND NOT ( )",
                "NOT (role1 AND)"
        );

        for (String expr : invalidExprs) {
            try {
                RuleParser parser = new RuleParser(expr);
                parser.parse();
                System.out.println("❌ Should have failed but parsed: " + expr);
            } catch (Exception e) {
                System.out.println("✅ Correctly failed: " + expr + " → " + e.getMessage());
            }
        }
    }

}
