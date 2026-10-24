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
package org.apache.wiki.http.filter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import jakarta.servlet.http.HttpServletRequest;


/**
 * Tests for {@link CsrfProtectionFilter#isCsrfProtectedPost(HttpServletRequest)}, which state-changing JSPs
 * (Delete.jsp, Rename.jsp, Workflow.jsp, Edit.jsp, Comment.jsp, DeleteGroup.jsp, etc.) rely on to reject
 * cross-site requests that arrive with a method other than POST or without a valid anti-CSRF token.
 */
public class CsrfProtectionFilterTest {

    @ParameterizedTest
    @ValueSource( strings = { "GET", "HEAD", "PUT", "DELETE", "OPTIONS", "TRACE", "PATCH" } )
    public void nonPostRequestIsNeverACsrfProtectedPost( final String method ) {
        final HttpServletRequest request = Mockito.mock( HttpServletRequest.class );
        Mockito.doReturn( method ).when( request ).getMethod();
        Assertions.assertFalse( CsrfProtectionFilter.isCsrfProtectedPost( request ),
                                method + " request must not be accepted as a CSRF-protected POST" );
    }

    @ParameterizedTest
    @ValueSource( strings = { "POST", "post", "Post" } )
    public void postMethodIsDetectedCaseInsensitively( final String method ) {
        final HttpServletRequest request = Mockito.mock( HttpServletRequest.class );
        Mockito.doReturn( method ).when( request ).getMethod();
        Assertions.assertTrue( CsrfProtectionFilter.isPost( request ) );
    }

}
