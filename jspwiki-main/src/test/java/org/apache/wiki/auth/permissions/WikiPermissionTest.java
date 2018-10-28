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
package org.apache.wiki.auth.permissions;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.Assertions;


/**
 */
public class WikiPermissionTest
{

    @Test
    public void testWikiPermission()
    {
      WikiPermission p = new WikiPermission("*", "createPages");
      Assertions.assertEquals("*", p.getName());
      Assertions.assertEquals("*", p.getWiki());
      Assertions.assertEquals("createpages", p.getActions());
    }

    /*
     * Class under test for boolean equals(Object)
     */
    @Test
    public void testEqualsObject()
    {
      WikiPermission p1 = new WikiPermission("*", "createPages");
      WikiPermission p2 = new WikiPermission("*", "createPages");
      WikiPermission p3 = new WikiPermission("*", "createGroups");
      Assertions.assertTrue(p1.equals(p2));
      Assertions.assertTrue(p2.equals(p1));
      Assertions.assertFalse(p1.equals(p3));
      Assertions.assertFalse(p3.equals(p1));
      WikiPermission p4 = new WikiPermission("*", "createPages,createGroups");
      WikiPermission p5 = new WikiPermission("*", "createGroups,createPages");
      Assertions.assertTrue(p4.equals(p5));
    }

    /*
     * Class under test for boolean equals(Object)
     */
    @Test
    public void testEqualsObjectNSi()
    {
      WikiPermission p1 = new WikiPermission("mywiki", "createPages");
      WikiPermission p2 = new WikiPermission("*",      "createPages");
      WikiPermission p3 = new WikiPermission("mywiki", "createGroups");
      Assertions.assertFalse(p1.equals(p2));
      Assertions.assertFalse(p2.equals(p1));
      Assertions.assertFalse(p1.equals(p3));
      Assertions.assertFalse(p3.equals(p1));
      WikiPermission p4 = new WikiPermission("mywiki", "createPages,createGroups");
      WikiPermission p5 = new WikiPermission("*",      "createGroups,createPages");
      Assertions.assertFalse(p4.equals(p5));
    }

    /*
     * Class under test for String getActions()
     */
    @Test
    public void testGetActions()
    {
      WikiPermission p1 = new WikiPermission("*", "createPages,createGroups,editProfile");
      Assertions.assertEquals("creategroups,createpages,editprofile", p1.getActions());
      WikiPermission p2 = new WikiPermission("*", "createGroups,editProfile,createPages");
      Assertions.assertEquals("creategroups,createpages,editprofile", p2.getActions());
    }

    /*
     * Class under test for boolean implies(Permission)
     */
    @Test
    public void testImpliesPermission()
    {
      // Superset of actions implies all individual actions
      WikiPermission p1 = new WikiPermission("*", "createPages,createGroups,editProfile");
      WikiPermission p2 = new WikiPermission("*", "createPages");
      WikiPermission p3 = new WikiPermission("*", "createGroups");
      WikiPermission p5 = new WikiPermission("*", "editPreferences");
      WikiPermission p6 = new WikiPermission("*", "editProfile");
      Assertions.assertTrue(p1.implies(p2));
      Assertions.assertFalse(p2.implies(p1));
      Assertions.assertTrue(p1.implies(p3));
      Assertions.assertFalse(p3.implies(p1));
      Assertions.assertTrue(p1.implies(p6));
      Assertions.assertFalse(p6.implies(p1));

      // createGroups implies createPages
      Assertions.assertTrue(p3.implies(p2));
      Assertions.assertFalse(p2.implies(p3));

      // editProfile implies nothing
      Assertions.assertFalse(p6.implies(p5));
      Assertions.assertFalse(p6.implies(p3));
      Assertions.assertFalse(p6.implies(p3));
      Assertions.assertFalse(p6.implies(p1));
    }

  /*
   * Class under test for boolean implies(Permission)
   */
    @Test
    public void testImpliesPermissionNS()
    {
      // Superset of actions implies all individual actions
      WikiPermission p1 = new WikiPermission("*",      "createPages,createGroups,editProfile");
      WikiPermission p2 = new WikiPermission("mywiki", "createPages");
      WikiPermission p3 = new WikiPermission("mywiki", "createGroups");
      WikiPermission p4 = new WikiPermission("urwiki", "editProfile");
      WikiPermission p5 = new WikiPermission("*",      "editPreferences");
      Assertions.assertTrue(p1.implies(p2));
      Assertions.assertFalse(p2.implies(p1));
      Assertions.assertTrue(p1.implies(p3));
      Assertions.assertFalse(p3.implies(p1));
      Assertions.assertTrue(p1.implies(p4));
      Assertions.assertFalse(p4.implies(p1));

      // createGroups implies createPages
      Assertions.assertTrue(p3.implies(p2));
      Assertions.assertFalse(p2.implies(p3));

      // editPreferences does not imply editProfile
      Assertions.assertFalse(p5.implies(p4));
      Assertions.assertFalse(p4.implies(p5));
    }

  /*
   * Class under test for String toString()
   */
    @Test
    public void testToString()
    {
      WikiPermission p1 = new WikiPermission("*", "createPages,createGroups,editProfile");
      String result = "(\"org.apache.wiki.auth.permissions.WikiPermission\",\"*\",\"creategroups,createpages,editprofile\")";
      Assertions.assertEquals(result, p1.toString());
    }

    @Test
    public void testImpliedMask()
    {
      Assertions.assertEquals(3, WikiPermission.impliedMask(1));
      Assertions.assertEquals(2, WikiPermission.impliedMask(2));
      Assertions.assertEquals(4, WikiPermission.impliedMask(4));
    }

    @Test
    public void testCreateMask()
    {
      Assertions.assertEquals(1, WikiPermission.createMask("createGroups"));
      Assertions.assertEquals(2, WikiPermission.createMask("createPages"));
      Assertions.assertEquals(3, WikiPermission.createMask("createGroups,createPages"));
      Assertions.assertEquals(4, WikiPermission.createMask("editPreferences"));
      Assertions.assertEquals(5, WikiPermission.createMask("createGroups,editPreferences"));
      Assertions.assertEquals(6, WikiPermission.createMask("createPages,editPreferences"));
      Assertions.assertEquals(7, WikiPermission.createMask("createGroups,createPages,editPreferences"));
      Assertions.assertEquals(8, WikiPermission.createMask("editProfile"));
      Assertions.assertEquals(9, WikiPermission.createMask("createGroups,editProfile"));
      Assertions.assertEquals(16, WikiPermission.createMask("login"));
      Assertions.assertEquals(24, WikiPermission.createMask("login,editProfile"));
    }

}
