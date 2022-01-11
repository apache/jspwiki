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
package org.apache.wiki.api.filters;

import org.apache.wiki.api.exceptions.FilterException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class PageFilterTest {

    @Test
    public void testLifeCycle() throws FilterException {
        final TestPageFilter filter = new TestPageFilter();
        Assertions.assertDoesNotThrow( () -> filter.initialize( null, null ) );
        Assertions.assertEquals( "duh", filter.preSave( null, "duh" ) );
        Assertions.assertEquals( "duh", filter.preTranslate( null, "duh" ) );
        Assertions.assertEquals( "duh", filter.postTranslate( null, "duh" ) );
        Assertions.assertDoesNotThrow( () -> filter.postSave( null, null ) );
        Assertions.assertDoesNotThrow( () -> filter.destroy( null ) );
    }

}
