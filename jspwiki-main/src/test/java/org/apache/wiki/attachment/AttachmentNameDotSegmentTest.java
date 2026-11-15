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
package org.apache.wiki.attachment;

import org.apache.wiki.api.exceptions.WikiException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** An attachment is stored in a directory of its own, so its name cannot be one of the names that stand for a directory. */
public class AttachmentNameDotSegmentTest {

    @Test
    public void testNamesThatEndAsASingleDotAreRefused() {
        // the path is cut at the last slash and the name is trimmed before the check, so all of these end as a single dot
        for( final String name : new String[] { ".", " .", ". ", "a/.", "a\\.", "./.", "../.", "/." } ) {
            Assertions.assertThrows( WikiException.class, () -> AttachmentManager.validateFileName( name ), "the name '" + name + "' is refused" );
        }
    }

    @Test
    public void testOrdinaryNamesStillPass() throws Exception {
        Assertions.assertEquals( "a.txt", AttachmentManager.validateFileName( "a.txt" ) );
        Assertions.assertEquals( "evil.txt", AttachmentManager.validateFileName( "../../evil.txt" ) );
        Assertions.assertEquals( "_", AttachmentManager.validateFileName( ".." ) );
        Assertions.assertEquals( "_hidden", AttachmentManager.validateFileName( "..hidden" ) );
        Assertions.assertEquals( "a_b.txt", AttachmentManager.validateFileName( "a..b.txt" ) );
    }

}
