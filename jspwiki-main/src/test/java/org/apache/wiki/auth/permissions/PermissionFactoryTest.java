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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PermissionFactoryTest
{

    @Test
    public void testHashCollidingPageNamesGetDistinctPermissions()
    {
        // "Aa" and "BB" have identical String hashCodes; the old XOR-of-hashCodes cache key
        // returned the first page's cached permission for the second page, so ACL checks
        // could be evaluated against the wrong page.
        Assertions.assertEquals( "Aa".hashCode(), "BB".hashCode() );

        final PagePermission p1 = PermissionFactory.getPagePermission( "Aa", "view" );
        final PagePermission p2 = PermissionFactory.getPagePermission( "BB", "view" );

        Assertions.assertEquals( "Aa", p1.getPage() );
        Assertions.assertEquals( "BB", p2.getPage() );
        Assertions.assertNotSame( p1, p2 );
    }

}
